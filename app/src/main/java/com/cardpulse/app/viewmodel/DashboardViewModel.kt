package com.cardpulse.app.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.CardWithProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardLoadingState(
    val isVisible: Boolean = false,
    val title: String = "",
    val description: String = "",
    val percentage: Int = 0,
    val stepNumber: Int = 0,
    val totalSteps: Int = 4
)

class DashboardViewModel(private val context: Context) : ViewModel() {

    private val repository = CardRepository(context)
    private var hasCompletedInitialDashboardLoad = false

    val cardsWithProgress: StateFlow<List<CardWithProgress>> = repository
        .getAllCardsWithProgressFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _dashboardLoadingState = MutableStateFlow(
        DashboardLoadingState(
            isVisible = true,
            title = "Preparing Dashboard",
            description = "Getting your cards ready.",
            percentage = 10,
            stepNumber = 1
        )
    )
    val dashboardLoadingState: StateFlow<DashboardLoadingState> = _dashboardLoadingState

    init {
        initializeDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.getAllCardsWithProgress()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load cards: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun initializeDashboard() {
        if (hasCompletedInitialDashboardLoad) {
            _dashboardLoadingState.value = DashboardLoadingState(isVisible = false)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                showDashboardStep(
                    stepNumber = 1,
                    title = "Loading Cards",
                    description = "Reading your saved cards from the device.",
                    percentage = 25
                )
                val existingDashboard = repository.getAllCardsWithProgress()
                if (existingDashboard.isNotEmpty()) {
                    hasCompletedInitialDashboardLoad = true
                    _dashboardLoadingState.value = DashboardLoadingState(isVisible = false)
                    return@launch
                }
                delay(700)

                showDashboardStep(
                    stepNumber = 2,
                    title = "Checking Transactions",
                    description = "Collecting linked transaction history for each card.",
                    percentage = 50
                )
                delay(700)

                showDashboardStep(
                    stepNumber = 3,
                    title = "Calculating Rewards",
                    description = "Refreshing milestones, perks, and verification status.",
                    percentage = 75
                )
                delay(700)

                showDashboardStep(
                    stepNumber = 4,
                    title = "Finishing Dashboard",
                    description = "Putting everything in place for the dashboard.",
                    percentage = 100
                )
                delay(500)

                hasCompletedInitialDashboardLoad = true
                _dashboardLoadingState.value = DashboardLoadingState(isVisible = false)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load cards: ${e.message}"
                hasCompletedInitialDashboardLoad = true
                _dashboardLoadingState.value = DashboardLoadingState(isVisible = false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun showDashboardStep(
        stepNumber: Int,
        title: String,
        description: String,
        percentage: Int
    ) {
        _dashboardLoadingState.value = DashboardLoadingState(
            isVisible = true,
            title = title,
            description = description,
            percentage = percentage,
            stepNumber = stepNumber
        )
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
