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
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import com.cardpulse.app.parser.CardDetectionParser
import com.cardpulse.app.parser.EmailTransactionParser
import com.cardpulse.app.parser.SmsTransactionParser
import com.cardpulse.app.parser.StatementEmailParser
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

                // Auto-detect + create new cards
                val detectedCards = CardDetectionParser.detectCards(emails)
                Log.d("CardPulse", "Detected cards: ${detectedCards.map { "${it.bankName} xxxx${it.last4}" }}")
                for (detected in detectedCards) {
                    val alreadyExists = existingCards.any { it.last4Digits == detected.last4 }
                    if (!alreadyExists) {
                        repository.insertCard(
                            com.cardpulse.app.model.Card(
                                id = 0, bankName = detected.bankName, cardName = detected.cardName,
                                last4Digits = detected.last4, cardType = detected.cardType,
                                cardNetwork = detected.bankName,
                                color = defaultColorForBank(detected.bankName),
                                creditLimit = 0.0, billingCycleDay = 1,
                                statementDay = 1, dueDateOffset = 20,
                                isActive = true, annualFee = 0.0, addedOn = java.util.Date()
                            )
                        )
                    }
                }

                // Match transactions to correct card by last 4
                val allCards = repository.getAllCards()
                if (allCards.isEmpty()) { _syncState.value = SyncState.Done(0); return@launch }
                var cardIdByLast4 = allCards.associate { it.last4Digits to it.id }

                // Split emails: statements vs transactions
                val statementEmails = emails.filter { StatementEmailParser.isStatementEmail(it.subject) }
                val transactionEmails = emails.filter { !StatementEmailParser.isStatementEmail(it.subject) }

                // Process statements → update card outstanding/due
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
                    if (repository.getTransactionByEmailId(email.messageId) != null) continue // already saved

                    // Layer 1: fast regex parse
                    var txn = EmailTransactionParser.parse(email, cardIdByLast4)

                    // Layer 2: Gemini fallback for emails regex couldn't parse
                    if (txn == null && geminiCallCount < AppConfig.GEMINI_EMAIL_PARSE_LIMIT) {
                        geminiCallCount++
                        val geminiResult = geminiService.parseEmailTransaction(
                            subject = email.subject,
                            from = email.from,
                            bodySnippet = email.body.take(800)
                        )
                        if (geminiResult.isTransaction && geminiResult.amount != null) {
                            // Try to match card — use Gemini's last4 first, then fallback
                            val cardId = geminiResult.last4?.let { cardIdByLast4[it] }
                                ?: cardIdByLast4.values.firstOrNull()
                                ?: continue

                            // Auto-create card if Gemini detected a new bank+last4 combo
                            if (geminiResult.last4 != null && geminiResult.bankName != null
                                && !cardIdByLast4.containsKey(geminiResult.last4)
                            ) {
                                val newCardId = repository.insertCard(
                                    com.cardpulse.app.model.Card(
                                        id = 0,
                                        bankName = geminiResult.bankName,
                                        cardName = "${geminiResult.bankName} Card",
                                        last4Digits = geminiResult.last4,
                                        cardType = "Credit Card",
                                        cardNetwork = geminiResult.bankName,
                                        color = defaultColorForBank(geminiResult.bankName),
                                        creditLimit = 0.0, billingCycleDay = 1,
                                        statementDay = 1, dueDateOffset = 20,
                                        isActive = true, annualFee = 0.0,
                                        addedOn = java.util.Date()
                                    )
                                )
                                cardIdByLast4 = cardIdByLast4 + (geminiResult.last4 to newCardId.toInt())
                            }

                            txn = Transaction(
                                id = 0,
                                cardId = cardId,
                                amount = geminiResult.amount,
                                merchant = geminiResult.merchant ?: "Unknown",
                                date = java.util.Date(),
                                category = geminiResult.category ?: "Others",
                                isCredit = geminiResult.isCredit,
                                source = TransactionSource.GMAIL,
                                status = TransactionStatus.CONFIRMED,
                                rawEmailId = email.messageId,
                                isFlagged = false,
                                flagReason = null
                            )
                        }
                    }

                    Log.d(
                        "CardPulse",
                        "Parsed txn: ${txn?.merchant} ₹${txn?.amount} → card ${txn?.cardId} (email: ${email.subject})"
                    )
                    if (txn != null) {
                        repository.insertTransaction(txn)
                        newCount++
                    }
                }

                // SMS sync
                val smsList = smsReader.readBankSms()
                for (sms in smsList) {
                    // Deduplicate by timestamp+amount (SMS has no unique ID like emailId)
                    val dedupKey = "${sms.timestamp}_${sms.body.take(30)}"
                    if (repository.getTransactionByEmailId(dedupKey) != null) continue
                    val txn = SmsTransactionParser.parse(sms, cardIdByLast4) ?: continue
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
        "hdfc" -> "#004C8C"; "axis" -> "#800000"; "icici" -> "#F58220"
        "sbi" -> "#2D6DB5"; "kotak" -> "#EF3E42"; "amex" -> "#016FD0"
        "idfc first" -> "#7B1FA2"; "indusind" -> "#1A237E"
        "yes bank" -> "#0052A5"; "rbl" -> "#B71C1C"
        else -> "#37474F"
    }
}
