package com.cardpulse.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cardpulse.app.config.AppConfig
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.data.GmailFetcher
import com.cardpulse.app.data.SmsReader
import com.cardpulse.app.gemini.GeminiService
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import com.cardpulse.app.parser.CardDetectionParser
import com.cardpulse.app.parser.EmailTransactionParser
import com.cardpulse.app.parser.SmsTransactionParser
import com.cardpulse.app.parser.StatementEmailParser
import com.cardpulse.app.util.cleanCardName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Done(val newCount: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

class GmailSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardRepository(application)
    private val gmailFetcher = GmailFetcher(application)
    private val smsReader = SmsReader(application)
    private val geminiService = GeminiService()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _hasAutoSynced = MutableStateFlow(false)

    fun autoSyncOnce() {
        if (_hasAutoSynced.value) return
        _hasAutoSynced.value = true
        syncNow()
    }

    fun syncNow() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                val existingCards = repository.getAllCards()
                val emails = gmailFetcher.fetchTransactionEmails(existingCards.map { it.last4Digits })

                val detectedCards = CardDetectionParser.detectCards(emails)
                Log.d("CardPulse", "Detected cards: ${detectedCards.map { "${it.bankName} xxxx${it.last4}" }}")
                for (detected in detectedCards) {
                    val alreadyExists = existingCards.any { it.last4Digits == detected.last4 }
                    if (!alreadyExists) {
                        repository.insertCard(
                            Card(
                                id = 0,
                                bankName = detected.bankName,
                                cardName = cleanCardName(detected.cardName, detected.bankName),
                                last4Digits = detected.last4,
                                cardType = detected.cardType,
                                cardNetwork = detected.bankName,
                                creditLimit = 0.0,
                                billingCycleDay = 1,
                                statementDay = 1,
                                dueDateOffset = 20,
                                annualFee = 0.0,
                                isAutoFetched = true,
                                isVerified = false,
                                isActive = true,
                                addedOn = System.currentTimeMillis(),
                                color = defaultColorForBank(detected.bankName),
                                currentOutstanding = 0.0,
                                minimumDue = 0.0,
                                paymentDueDate = null
                            )
                        )
                    }
                }

                val allCards = repository.getAllCards()
                if (allCards.isEmpty()) {
                    _syncState.value = SyncState.Done(0)
                    return@launch
                }
                var cardIdByLast4 = allCards.associate { it.last4Digits to it.id }

                val statementEmails = emails.filter { StatementEmailParser.isStatementEmail(it.subject) }
                val transactionEmails = emails.filter { !StatementEmailParser.isStatementEmail(it.subject) }

                for (email in statementEmails) {
                    val statement = StatementEmailParser.parse(email)
                    val last4 = statement.last4 ?: continue
                    val card = allCards.find { it.last4Digits == last4 } ?: continue
                    val updated = card.copy(
                        currentOutstanding = statement.totalOutstanding ?: card.currentOutstanding,
                        minimumDue = statement.minimumDue ?: card.minimumDue,
                        paymentDueDate = statement.dueDate ?: card.paymentDueDate,
                        creditLimit = statement.creditLimit?.takeIf { it > 0 } ?: card.creditLimit
                    )
                    repository.updateCard(updated)
                }

                var newCount = 0
                var geminiCallCount = 0
                for (email in transactionEmails) {
                    if (repository.getTransactionByEmailId(email.messageId) != null) continue

                    var txn = EmailTransactionParser.parse(email, cardIdByLast4)

                    if (txn == null && geminiCallCount < AppConfig.GEMINI_EMAIL_PARSE_LIMIT) {
                        geminiCallCount++
                        val geminiResult = geminiService.parseEmailTransaction(
                            subject = email.subject,
                            from = email.from,
                            bodySnippet = email.body.take(800)
                        )
                        if (geminiResult.isTransaction && geminiResult.amount != null) {
                            val cardId = geminiResult.last4?.let { cardIdByLast4[it] }
                                ?: cardIdByLast4.values.firstOrNull()
                                ?: continue

                            if (geminiResult.last4 != null && geminiResult.bankName != null
                                && !cardIdByLast4.containsKey(geminiResult.last4)
                            ) {
                                val newCardId = repository.insertCard(
                                    Card(
                                        id = 0,
                                        bankName = geminiResult.bankName,
                                        cardName = cleanCardName("${geminiResult.bankName} Card", geminiResult.bankName),
                                        last4Digits = geminiResult.last4,
                                        cardType = "Credit Card",
                                        cardNetwork = geminiResult.bankName,
                                        creditLimit = 0.0,
                                        billingCycleDay = 1,
                                        statementDay = 1,
                                        dueDateOffset = 20,
                                        annualFee = 0.0,
                                        isAutoFetched = true,
                                        isVerified = false,
                                        isActive = true,
                                        addedOn = System.currentTimeMillis(),
                                        color = defaultColorForBank(geminiResult.bankName),
                                        currentOutstanding = 0.0,
                                        minimumDue = 0.0,
                                        paymentDueDate = null
                                    )
                                )
                                cardIdByLast4 = cardIdByLast4 + (geminiResult.last4 to newCardId.toInt())
                            }

                            txn = Transaction(
                                id = 0,
                                cardId = cardId,
                                amount = geminiResult.amount,
                                merchant = geminiResult.merchant ?: "Unknown",
                                category = geminiResult.category ?: "Others",
                                date = System.currentTimeMillis(),
                                source = TransactionSource.GMAIL,
                                rawText = email.body,
                                rawEmailId = email.messageId,
                                status = TransactionStatus.CONFIRMED,
                                isCredit = geminiResult.isCredit,
                                isFlagged = false,
                                flagReason = null,
                                currency = "INR",
                                isInternational = false
                            )
                        }
                    }

                    if (txn != null) {
                        repository.insertTransaction(txn)
                        newCount++
                    }
                }

                val smsList = smsReader.readTransactionSms()
                for (txn in smsList) {
                    val dedupKey = "${txn.date}_${txn.amount}_${txn.merchant}"
                    if (repository.getTransactionByEmailId(dedupKey) != null) continue
                    val txnWithId = txn.copy(rawEmailId = dedupKey)
                    repository.insertTransaction(txnWithId)
                    newCount++
                }

                _syncState.value = SyncState.Done(newCount)
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            }
        }
    }

    private fun defaultColorForBank(bankName: String) = when (bankName.lowercase()) {
        "hdfc" -> "#004C8C"
        "axis" -> "#800000"
        "icici" -> "#F58220"
        "sbi" -> "#2D6DB5"
        "kotak" -> "#EF3E42"
        "amex" -> "#016FD0"
        "idfc first" -> "#7B1FA2"
        "indusind" -> "#1A237E"
        "yes bank" -> "#0052A5"
        "rbl" -> "#B71C1C"
        else -> "#37474F"
    }
}
