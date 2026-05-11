package com.cardpulse.app.viewmodel

import android.content.Context
import androidx.lifecycle.*
import android.util.Log
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.model.CardWithProgress
import com.cardpulse.app.model.Milestone
import com.cardpulse.app.model.Perk
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class CardDetailViewModel(
    private val context: Context,
    private val cardId: Int
) : ViewModel() {

    private val repository = CardRepository(context)

    private val _cardDetail = MutableStateFlow<CardWithProgress?>(null)
    val cardDetail: StateFlow<CardWithProgress?> = _cardDetail

    data class PerkProgress(
        val perk: Perk,
        val qualifyingTxns: List<Transaction>,
        val currentAmount: Double,
        val progress: Float,
        val isAchieved: Boolean
    )

    private val _perkProgressList = MutableStateFlow<List<PerkProgress>>(emptyList())
    val perkProgressList: StateFlow<List<PerkProgress>> = _perkProgressList

    private val _milestoneProgressList = MutableStateFlow<List<MilestoneProgress>>(emptyList())
    val milestoneProgressList: StateFlow<List<MilestoneProgress>> = _milestoneProgressList

    data class MilestoneProgress(
        val milestone: Milestone,
        val currentAmount: Double,
        val progress: Float,
        val isAchieved: Boolean
    )

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadCard()
    }

    fun loadCard() {
        Log.d("CardDetailViewModel", "===== loadCard() called for cardId=$cardId =====")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val detail = repository.getCardWithProgress(cardId)
                _cardDetail.value = detail

                val allTxns = repository.getTransactionsForCard(cardId)
                _transactions.value = allTxns

                // Load ResolvedCard for perks/milestones
                detail?.card?.let { card ->
                    // Try BIN match first
                    var resolved = repository.matchCardByBin(
                        card.last4Digits.padStart(6, '0'),
                        card.bankName,
                        card.cardName
                    )

                    // Fallback: search by name if BIN fails
                    if (resolved == null) {
                        val searchResults = repository.searchCardsInCatalog("${card.bankName} ${card.cardName}")
                        resolved = searchResults.firstOrNull()
                    }

                    resolved?.let { rc ->
                        Log.d("CardDetailViewModel", "Matched card: ${rc.bankName} ${rc.cardName}, ${rc.perks.size} perks, ${rc.milestones.size} milestones")
                        calculatePerkProgress(rc.perks, allTxns)
                        calculateMilestoneProgress(rc.milestones, allTxns)
                    } ?: run {
                        Log.w("CardDetailViewModel", "No match found for ${card.bankName} ${card.cardName}. Using defaults.")
                        val (defaultPerks, defaultMilestones) = com.cardpulse.app.data.CardCatalogLoader.getDefaultPerksAndMilestones()
                        calculatePerkProgress(defaultPerks, allTxns)
                        calculateMilestoneProgress(defaultMilestones, allTxns)
                    }
                }
            } catch (e: Exception) {
                Log.e("CardDetailViewModel", "Error loading card: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun calculatePerkProgress(perks: List<Perk>, txns: List<Transaction>) {
        val progressList = perks.mapNotNull { perk ->
            val minThreshold = perk.mn ?: 0
            val cycleStart = getCycleStart(perk.cy)

            val qualifyingTxns = txns.filter { txn ->
                !txn.isCredit &&
                        txn.status == TransactionStatus.CONFIRMED &&
                        txn.amount >= minThreshold &&
                        txn.date >= cycleStart
            }

            var currentAmount = qualifyingTxns.sumOf { it.amount }

            // Apply upto cap if present
            perk.up?.let { cap ->
                if (cap.t == "sp") currentAmount = currentAmount.coerceAtMost(cap.v.toDouble())
            }

            val targetAmount = perk.up?.v?.toDouble() ?: Double.MAX_VALUE
            if (targetAmount == Double.MAX_VALUE) return@mapNotNull null

            val progress = (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f)
            val isAchieved = currentAmount >= targetAmount

            PerkProgress(perk, qualifyingTxns, currentAmount, progress, isAchieved)
        }
        _perkProgressList.value = progressList
    }

    private fun calculateMilestoneProgress(milestones: List<Milestone>, txns: List<Transaction>) {
        val progressList = milestones.map { milestone ->
            val cycleStart = getCycleStart(milestone.cy)
            val qualifyingTxns = txns.filter { !it.isCredit && it.date >= cycleStart }
            val currentAmount = qualifyingTxns.sumOf { it.amount }
            val targetAmount = milestone.ta.toDouble()
            val progress = (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f)
            val isAchieved = currentAmount >= targetAmount

            MilestoneProgress(milestone, currentAmount, progress, isAchieved)
        }
        _milestoneProgressList.value = progressList
    }

    private fun getCycleStart(cycleType: String): Date {
        val cal = Calendar.getInstance()
        return when (cycleType) {
            "o" -> Date(0) // one-time: include all
            "m" -> { // monthly
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.time
            }
            "q" -> { // quarterly
                val month = cal.get(Calendar.MONTH)
                val quarterStart = (month / 3) * 3
                cal.set(Calendar.MONTH, quarterStart)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.time
            }
            "a" -> { // annual
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.time
            }
            else -> Date(0)
        }
    }

    fun confirmTransaction(transactionId: Int) {
        viewModelScope.launch {
            val txn = _transactions.value.find { it.id == transactionId } ?: return@launch
            repository.updateTransaction(txn.copy(status = TransactionStatus.CONFIRMED, isFlagged = false))
            loadCard()
        }
    }

    fun deleteTransaction(transactionId: Int) {
        viewModelScope.launch {
            val txn = _transactions.value.find { it.id == transactionId } ?: return@launch
            repository.updateTransaction(txn.copy(status = TransactionStatus.PENDING))
            loadCard()
        }
    }

    fun deleteCard(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
            onDeleted()
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
