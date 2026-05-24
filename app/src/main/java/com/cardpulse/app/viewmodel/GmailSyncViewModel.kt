package com.cardpulse.app.viewmodel

import android.app.Application
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
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
import com.cardpulse.app.parser.EmailClassifier
import com.cardpulse.app.parser.EmailKind
import com.cardpulse.app.parser.EmailTransactionParser
import com.cardpulse.app.parser.TransactionKindClassifier
import com.cardpulse.app.parser.TransactionTagger
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
    private val syncPrefs = application.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _hasAutoSynced = MutableStateFlow(false)

    fun hasPreviousSuccessfulSync(): Boolean {
        return syncPrefs.getLong(KEY_LAST_SUCCESSFUL_GMAIL_SYNC_AT, 0L) > 0L
    }

    fun autoSyncOnce() {
        if (_hasAutoSynced.value) return
        _hasAutoSynced.value = true
        syncNow()
    }

    fun syncNow() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val syncStartedAt = System.currentTimeMillis()
            val lastSuccessfulRefresh = syncPrefs.getLong(KEY_LAST_SUCCESSFUL_GMAIL_SYNC_AT, 0L)
                .takeIf { it > 0L }
            val lastSuccessfulSmsRefresh = syncPrefs.getLong(KEY_LAST_SUCCESSFUL_SMS_SYNC_AT, 0L)
                .takeIf { it > 0L }
            try {
                val existingCards = repository.getAllCards()
                var newCount = 0
                val hasSmsPermission = ContextCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
                val smsSinceMillis = lastSuccessfulSmsRefresh?.let {
                    (it - AppConfig.GMAIL_INCREMENTAL_OVERLAP_MINUTES * 60 * 1000L).coerceAtLeast(0L)
                }

                if (hasSmsPermission) {
                    existingCards.forEach { card ->
                        smsReader.parseTransactionsForCard(card, smsSinceMillis).forEach { txn ->
                            repository.upsertDedupedTransaction(txn)
                            newCount++
                        }
                    }
                } else {
                    Log.w("CardPulse", "READ_SMS permission missing; skipping SMS ingestion until permission is granted")
                }

                val statementEmailsFromGmail = gmailFetcher.fetchStatementEmails(lastSuccessfulRefresh)
                val transactionEmailsFromGmail = gmailFetcher.fetchTransactionEmails(
                    cardLast4 = existingCards.map { it.last4Digits },
                    sinceMillis = lastSuccessfulRefresh
                )
                val emails = (statementEmailsFromGmail + transactionEmailsFromGmail)
                    .distinctBy { it.messageId }
                val classifiedEmails = emails.map { email -> email to EmailClassifier.classify(email) }
                val ledgerEmails = classifiedEmails
                    .filter { (_, classification) ->
                        classification.kind == EmailKind.STATEMENT ||
                            classification.kind == EmailKind.TRANSACTION_ALERT ||
                            classification.kind == EmailKind.PAYMENT_ALERT
                    }
                    .map { (email, _) -> email }

                val detectedCards = CardDetectionParser.detectCards(ledgerEmails)
                Log.d("CardPulse", "Detected cards: ${detectedCards.map { "${it.bankName} xxxx${it.last4}" }}")
                for (detected in detectedCards) {
                    val alreadyExists = repository.getAllCards().any { it.last4Digits == detected.last4 }
                    if (!alreadyExists) {
                        val matchedCatalog = repository.searchCardsInCatalog("${detected.bankName} ${detected.cardName}").firstOrNull()
                        repository.insertCard(
                            Card(
                                id = 0,
                                bankName = detected.bankName,
                                cardName = cleanCardName(detected.cardName, detected.bankName),
                                last4Digits = detected.last4,
                                cardType = matchedCatalog?.cardType ?: detected.cardType,
                                cardNetwork = matchedCatalog?.cardNetwork ?: detected.bankName,
                                creditLimit = 0.0,
                                billingCycleDay = 1,
                                statementDay = 1,
                                dueDateOffset = 20,
                                annualFee = matchedCatalog?.annualFee?.toDouble() ?: 0.0,
                                isAutoFetched = true,
                                isVerified = false,
                                isActive = true,
                                addedOn = System.currentTimeMillis(),
                                color = matchedCatalog?.color ?: defaultColorForBank(detected.bankName),
                                currentOutstanding = 0.0,
                                minimumDue = 0.0,
                                paymentDueDate = null
                            )
                        )
                    }
                }

                val allCards = repository.getAllCards()
                if (allCards.isEmpty()) {
                    syncPrefs.edit()
                        .putLong(KEY_LAST_SUCCESSFUL_GMAIL_SYNC_AT, syncStartedAt)
                        .apply()
                    _syncState.value = SyncState.Done(0)
                    return@launch
                }
                var cardIdByLast4 = allCards.associate { it.last4Digits to it.id }

                if (hasSmsPermission) {
                    allCards
                        .filterNot { existing -> existingCards.any { it.id == existing.id } }
                        .forEach { newCard ->
                            smsReader.parseTransactionsForCard(newCard, smsSinceMillis).forEach { txn ->
                                repository.upsertDedupedTransaction(txn)
                                newCount++
                            }
                        }
                }

                val statementEmails = classifiedEmails.filter { (_, classification) ->
                    classification.kind == EmailKind.STATEMENT
                }
                val transactionEmails = classifiedEmails.filter { (_, classification) ->
                    classification.kind == EmailKind.TRANSACTION_ALERT ||
                        classification.kind == EmailKind.PAYMENT_ALERT
                }

                for ((email, classification) in statementEmails) {
                    Log.d("CardPulse", "Parsing statement email ${email.messageId}: bank=${classification.bankName}")
                    val statement = StatementEmailParser.parse(email)
                    val last4 = statement.last4 ?: continue
                    val card = repository.getAllCards().find { it.last4Digits == last4 } ?: continue
                    val updated = card.copy(
                        currentOutstanding = statement.totalOutstanding ?: card.currentOutstanding,
                        minimumDue = statement.minimumDue ?: card.minimumDue,
                        paymentDueDate = statement.dueDate ?: card.paymentDueDate,
                        creditLimit = statement.creditLimit?.takeIf { it > 0 } ?: card.creditLimit
                    )
                    repository.updateCard(updated)
                }

                var geminiCallCount = 0
                for ((email, classification) in transactionEmails) {
                    if (repository.getTransactionByEmailId(email.messageId) != null) continue

                    var txn = EmailTransactionParser.parse(email, cardIdByLast4, classification)

                    if (txn == null && geminiCallCount < AppConfig.GEMINI_EMAIL_PARSE_LIMIT) {
                        geminiCallCount++
                        val geminiResult = geminiService.parseEmailTransaction(
                            subject = email.subject,
                            from = email.from,
                            bodySnippet = email.body.take(800)
                        )
                        if (geminiResult.isTransaction && geminiResult.amount != null) {
                            val inferredKind = TransactionKindClassifier.infer(
                                "${email.subject} ${email.body.take(800)}",
                                fallbackSpend = classification.kind == EmailKind.TRANSACTION_ALERT
                            )
                            var cardId = geminiResult.last4?.let { cardIdByLast4[it] }
                            if (geminiResult.last4 != null && geminiResult.bankName != null
                                && !cardIdByLast4.containsKey(geminiResult.last4)
                            ) {
                                val matchedCatalog = repository.searchCardsInCatalog("${geminiResult.bankName} Card").firstOrNull()
                                val newCardId = repository.insertCard(
                                    Card(
                                        id = 0,
                                        bankName = geminiResult.bankName,
                                        cardName = cleanCardName("${geminiResult.bankName} Card", geminiResult.bankName),
                                        last4Digits = geminiResult.last4,
                                        cardType = matchedCatalog?.cardType ?: "Credit Card",
                                        cardNetwork = matchedCatalog?.cardNetwork ?: geminiResult.bankName,
                                        creditLimit = 0.0,
                                        billingCycleDay = 1,
                                        statementDay = 1,
                                        dueDateOffset = 20,
                                        annualFee = matchedCatalog?.annualFee?.toDouble() ?: 0.0,
                                        isAutoFetched = true,
                                        isVerified = false,
                                        isActive = true,
                                        addedOn = System.currentTimeMillis(),
                                        color = matchedCatalog?.color ?: defaultColorForBank(geminiResult.bankName),
                                        currentOutstanding = 0.0,
                                        minimumDue = 0.0,
                                        paymentDueDate = null
                                    )
                                )
                                cardIdByLast4 = cardIdByLast4 + (geminiResult.last4 to newCardId.toInt())
                                cardId = newCardId.toInt()
                            }

                            cardId = cardId ?: cardIdByLast4.values.firstOrNull() ?: continue
                            val geminiKind = if (inferredKind.name == "UNKNOWN" && geminiResult.isCredit) {
                                com.cardpulse.app.parser.LedgerTransactionKind.REFUND
                            } else {
                                inferredKind
                            }
                            val geminiMerchant = geminiResult.merchant ?: "Unknown"
                            val geminiTags = TransactionTagger.infer(email.body, geminiMerchant, geminiKind)

                            txn = Transaction(
                                id = 0,
                                cardId = cardId,
                                amount = geminiResult.amount,
                                merchant = geminiMerchant,
                                category = TransactionKindClassifier.categoryFor(
                                    geminiKind,
                                    geminiResult.category ?: "Others"
                                ),
                                date = System.currentTimeMillis(),
                                source = TransactionSource.GMAIL,
                                rawText = email.body,
                                rawEmailId = email.messageId,
                                status = TransactionStatus.CONFIRMED,
                                isCredit = TransactionKindClassifier.isCreditLike(geminiKind) || geminiResult.isCredit,
                                isFlagged = false,
                                flagReason = null,
                                currency = "INR",
                                isInternational = false,
                                transactionKind = geminiKind.name,
                                tags = TransactionTagger.serialize(geminiTags.tags),
                                tagConfidence = geminiTags.confidence
                            )
                        }
                    }

                    if (txn != null) {
                        repository.upsertDedupedTransaction(txn)
                        newCount++
                    }
                }

                syncPrefs.edit()
                    .putLong(KEY_LAST_SUCCESSFUL_GMAIL_SYNC_AT, syncStartedAt)
                    .apply()
                if (hasSmsPermission) {
                    syncPrefs.edit()
                        .putLong(KEY_LAST_SUCCESSFUL_SMS_SYNC_AT, syncStartedAt)
                        .apply()
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

    companion object {
        private const val SYNC_PREFS = "cardpulse_sync"
        private const val KEY_LAST_SUCCESSFUL_GMAIL_SYNC_AT = "last_successful_gmail_sync_at"
        private const val KEY_LAST_SUCCESSFUL_SMS_SYNC_AT = "last_successful_sms_sync_at"
    }
}
