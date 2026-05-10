package com.cardpulse.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.data.GmailFetcher
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

    fun syncNow() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                val cards = repository.getAllCards()
                if (cards.isEmpty()) {
                    _syncState.value = SyncState.Done(0)
                    return@launch
                }
                val cardIdByLast4 = cards.associate { it.last4Digits to it.id }
                val emails = gmailFetcher.fetchTransactionEmails(cards.map { it.last4Digits })
                var newCount = 0
                for (email in emails) {
                    val txn = EmailTransactionParser.parse(email, cardIdByLast4) ?: continue
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
}
