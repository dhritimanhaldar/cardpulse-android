package com.cardpulse.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.data.GmailFetcher
import com.cardpulse.app.parser.CardDetectionParser
import com.cardpulse.app.parser.EmailTransactionParser
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
                val cardIdByLast4 = allCards.associate { it.last4Digits to it.id }

                var newCount = 0
                for (email in emails) {
                    val txn = EmailTransactionParser.parse(email, cardIdByLast4)
                    Log.d("CardPulse", "Parsed txn: ${txn?.merchant} ₹${txn?.amount} → card ${txn?.cardId} (email: ${email.subject})")
                    if (txn == null) continue
                    // Deduplicate by rawEmailId
                    val existing = repository.getTransactionByEmailId(txn.rawEmailId ?: "")
                    if (existing == null) {
                        repository.insertTransaction(txn)
                        newCount++
                    }
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
