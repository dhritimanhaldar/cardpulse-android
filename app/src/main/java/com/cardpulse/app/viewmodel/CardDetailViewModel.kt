package com.cardpulse.app.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.model.CardWithProgress
import com.cardpulse.app.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CardDetailViewModel(
    private val context: Context,
    private val cardId: Int
) : ViewModel() {

    private val repository = CardRepository(context)

    private val _cardDetail = MutableStateFlow<CardWithProgress?>(null)
    val cardDetail: StateFlow<CardWithProgress?> = _cardDetail

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadCard()
    }

    fun loadCard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _cardDetail.value = repository.getCardWithProgress(cardId)
                _transactions.value = repository.getTransactionsForCard(cardId)
            } catch (e: Exception) {
                // silently fail for now
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmTransaction(transactionId: Int) {
        viewModelScope.launch {
            val txn = _transactions.value.find { it.id == transactionId } ?: return@launch
            repository.updateTransaction(txn.copy(isConfirmed = true, isFlagged = false))
            loadCard()
        }
    }

    fun deleteTransaction(transactionId: Int) {
        viewModelScope.launch {
            val txn = _transactions.value.find { it.id == transactionId } ?: return@launch
            repository.updateTransaction(txn.copy(isConfirmed = false))
            loadCard()
        }
    }

    companion object {
        fun factory(context: Context, cardId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CardDetailViewModel(context.applicationContext, cardId) as T
            }
    }
}
