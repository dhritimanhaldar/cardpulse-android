package com.cardpulse.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.CardWithProgress
import com.cardpulse.app.model.Milestone
import com.cardpulse.app.model.Perk
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionStatus
import com.cardpulse.app.parser.TransactionKindClassifier
import com.cardpulse.app.parser.MilestoneProgressCalculator
import com.cardpulse.app.parser.TransactionTagger
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

    private val _card = MutableStateFlow<Card?>(null)
    val card: StateFlow<Card?> = _card

    data class PerkProgress(
        val perk: Perk,
        val qualifyingTxns: List<Transaction>,
        val currentAmount: Double,
        val targetAmount: Double,
        val progress: Float,
        val isAchieved: Boolean
    )

    data class MilestoneProgress(
        val milestone: Milestone,
        val qualifyingTxns: List<Transaction>,
        val currentAmount: Double,
        val targetAmount: Double,
        val progress: Float,
        val isAchieved: Boolean
    )

    private val _perkProgressList = MutableStateFlow<List<PerkProgress>>(emptyList())
    val perkProgressList: StateFlow<List<PerkProgress>> = _perkProgressList

    private val _milestoneProgressList = MutableStateFlow<List<MilestoneProgress>>(emptyList())
    val milestoneProgressList: StateFlow<List<MilestoneProgress>> = _milestoneProgressList

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingMilestones = MutableStateFlow(false)
    val isLoadingMilestones: StateFlow<Boolean> = _isLoadingMilestones

    init {
        loadCard()
    }

    fun loadCard() {
        Log.d("CardDetailViewModel", "===== loadCard() called for cardId=$cardId =====")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val loadedCard = repository.getCardById(cardId)
                _card.value = loadedCard
                _isLoading.value = false
                _isLoadingMilestones.value = true

                val detail = repository.getCardWithProgress(cardId)
                _cardDetail.value = detail
                val allTxns = repository.getTransactionsForCard(cardId)
                _transactions.value = allTxns

                detail?.card?.let { card ->
                    val resolved = repository.matchCardFromCatalog(card)

                    resolved?.let { rc ->
                        Log.d("CardDetailViewModel", "Matched card: ${rc.bankName} ${rc.cardName}")
                        calculatePerkProgress(rc.perks, allTxns)
                        calculateMilestoneProgress(rc.milestones, allTxns)
                    } ?: run {
                        val (defaultPerks, defaultMilestones) = com.cardpulse.app.data.CardCatalogLoader.getDefaultPerksAndMilestones()
                        calculatePerkProgress(defaultPerks, allTxns)
                        calculateMilestoneProgress(defaultMilestones, allTxns)
                    }
                }
            } catch (e: Exception) {
                Log.e("CardDetailViewModel", "Error loading card: ${e.message}")
            } finally {
                _isLoading.value = false
                _isLoadingMilestones.value = false
            }
        }
    }

    private fun calculatePerkProgress(perks: List<Perk>, txns: List<Transaction>) {
        val progressList = perks.mapNotNull { perk ->
            val cycleStart = getCycleStart(perk.cy)
            val computed = MilestoneProgressCalculator.computeForPerk(perk, txns, cycleStart.time)
            PerkProgress(
                perk = perk,
                qualifyingTxns = computed.eligibleTransactions,
                currentAmount = computed.currentAmount,
                targetAmount = computed.denominator,
                progress = computed.progress,
                isAchieved = computed.isAchieved
            )
        }
        _perkProgressList.value = progressList
    }

    private fun calculateMilestoneProgress(milestones: List<Milestone>, txns: List<Transaction>) {
        val progressList = milestones.map { milestone ->
            val cycleStart = getCycleStart(milestone.cy)
            val computed = MilestoneProgressCalculator.computeForMilestone(milestone, txns, cycleStart.time)
            MilestoneProgress(
                milestone = milestone,
                qualifyingTxns = computed.eligibleTransactions,
                currentAmount = computed.currentAmount,
                targetAmount = computed.denominator,
                progress = computed.progress,
                isAchieved = computed.isAchieved
            )
        }
        _milestoneProgressList.value = progressList
    }

    fun groupTransactionsByPerk(
        transactions: List<Transaction>,
        perks: List<Perk>
    ): Map<Perk, List<Transaction>> {
        return perks.associateWith { perk ->
            val cycleStart = getCycleStart(perk.cy)
            transactions.filter {
                MilestoneProgressCalculator.doesTransactionQualify(
                    transaction = it,
                    ruleText = perk.n,
                    requiredTags = MilestoneProgressCalculator.inferRequiredTags(perk.n),
                    minTransactionAmount = perk.mn ?: 0,
                    cycleStartMillis = cycleStart.time,
                    exclusions = perk.x.orEmpty()
                )
            }
        }
    }

    fun calculatePerkProgress(
        transactions: List<Transaction>,
        perk: Perk
    ): Double {
        val cycleStart = getCycleStart(perk.cy)
        return transactions
            .filter {
                MilestoneProgressCalculator.doesTransactionQualify(
                    transaction = it,
                    ruleText = perk.n,
                    requiredTags = MilestoneProgressCalculator.inferRequiredTags(perk.n),
                    minTransactionAmount = perk.mn ?: 0,
                    cycleStartMillis = cycleStart.time,
                    exclusions = perk.x.orEmpty()
                )
            }
            .sumOf { TransactionKindClassifier.signedProgressAmount(it) }
    }

    private fun transactionCountsTowardMilestone(txn: Transaction, cycleStartMillis: Long): Boolean {
        return TransactionKindClassifier.countsTowardSpend(txn) &&
                txn.date >= cycleStartMillis
    }

    private fun transactionCountsTowardPerk(
        txn: Transaction,
        perk: Perk,
        cycleStartMillis: Long,
        minThreshold: Int
    ): Boolean {
        if (!transactionCountsTowardMilestone(txn, cycleStartMillis)) return false
        if (txn.amount < minThreshold) return false

        val excludedTerms = perk.x.orEmpty().map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val category = txn.category.lowercase()
        val merchant = txn.merchant.lowercase()

        return excludedTerms.none { excluded ->
            category.contains(excluded) || merchant.contains(excluded)
        }
    }

    private fun getCycleStart(cycleType: String): Date {
        val cal = Calendar.getInstance()
        return when (cycleType) {
            "o" -> Date(0)
            "m" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.time
            }
            "q" -> {
                val month = cal.get(Calendar.MONTH)
                val quarterStart = (month / 3) * 3
                cal.set(Calendar.MONTH, quarterStart)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.time
            }
            "a" -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.time
            }
            else -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.time
            }
        }
    }

    fun updateTransactionTagging(
        transactionId: Int,
        kind: String,
        tags: Set<String>,
        confidence: Double
    ) {
        viewModelScope.launch {
            val txn = _transactions.value.find { it.id == transactionId } ?: return@launch
            repository.updateTransaction(
                txn.copy(
                    transactionKind = kind,
                    tags = TransactionTagger.serialize(tags),
                    tagConfidence = confidence.coerceIn(0.0, 1.0),
                    isTagUserEdited = true,
                    isCredit = kind == "PAYMENT" || kind == "REFUND",
                    category = when (kind) {
                        "PAYMENT" -> "Payment"
                        "REFUND" -> "Refund"
                        "FEE" -> "Fee"
                        else -> txn.category
                    }
                )
            )
            loadCard()
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
