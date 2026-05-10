package com.cardpulse.app.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.CardWithProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(private val context: Context) : ViewModel() {

    private val repository = CardRepository(context)

    private val _cardsWithProgress = MutableStateFlow<List<CardWithProgress>>(emptyList())
    val cardsWithProgress: StateFlow<List<CardWithProgress>> = _cardsWithProgress

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _cardsWithProgress.value = repository.getAllCardsWithProgress()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load cards: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addCard(card: Card) {
        viewModelScope.launch {
            try {
                repository.insertCard(card)
                refreshDashboard()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add card: ${e.message}"
            }
        }
    }

    fun deleteCard(cardId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteCard(cardId)
                refreshDashboard()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete card: ${e.message}"
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DashboardViewModel(context.applicationContext) as T
            }
    }
}
