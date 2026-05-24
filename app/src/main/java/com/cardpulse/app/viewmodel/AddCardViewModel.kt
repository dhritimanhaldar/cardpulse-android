package com.cardpulse.app.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.cardpulse.app.data.BankOption
import com.cardpulse.app.data.CardPulseDatabase
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.data.CardVariantOption
import com.cardpulse.app.data.SmsReader
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.CardMatchState
import com.cardpulse.app.model.MatchSpecificity
import com.cardpulse.app.model.ResolvedCardCandidate
import com.cardpulse.app.util.cleanAndNormalizeBankName
import com.cardpulse.app.util.cleanCardName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddCardViewModel : ViewModel() {
    private var editingCardId: Int? = null
    private var existingCard: Card? = null
    private lateinit var repository: CardRepository

    val digitGroups = mutableStateListOf("", "", "", "")
    val cardHolderName = mutableStateOf("")
    val cardNickname = mutableStateOf("")
    var selectedColor by mutableStateOf(Color(0xFF1976D2))

    val availableBanks = mutableStateOf<List<BankOption>>(emptyList())
    val availableCardVariants = mutableStateOf<List<CardVariantOption>>(emptyList())
    val candidateBanks = mutableStateOf<List<BankOption>>(emptyList())
    val candidateVariants = mutableStateOf<List<CardVariantOption>>(emptyList())

    val selectedBank = mutableStateOf<BankOption?>(null)
    val selectedCardVariant = mutableStateOf<CardVariantOption?>(null)
    val matchState = mutableStateOf<CardMatchState>(CardMatchState.Empty)
    val matchedBank = mutableStateOf<BankOption?>(null)
    val matchedBankIconKey = mutableStateOf<String?>(null)

    val fallbackMode = mutableStateOf(false)
    val lastGroupEditable = mutableStateOf(true)
    val showBankDropdown = mutableStateOf(false)
    val showCardVariantDropdown = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val isSaving = mutableStateOf(false)

    var userEditedNickname by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        if (!::repository.isInitialized) {
            repository = CardRepository(context)
        }
        viewModelScope.launch {
            availableBanks.value = repository.getAllBanks()
        }
    }

    fun onDigitGroupChanged(index: Int, value: String) {
        if (index !in digitGroups.indices) return
        if (index == LAST_GROUP_INDEX && !lastGroupEditable.value) return

        val digitsOnly = value.filter(Char::isDigit)
        if (digitsOnly.length > DIGITS_PER_GROUP) {
            onDigitsPasted(digitsOnly, index)
            return
        }

        digitGroups[index] = digitsOnly
        errorMessage.value = null
        recomputeCardMatch()
    }

    fun onDigitsPasted(value: String, startIndex: Int = 0) {
        val digitsOnly = value.filter(Char::isDigit)
        if (digitsOnly.isBlank()) return

        var cursor = 0
        for (index in startIndex..LAST_GROUP_INDEX) {
            if (index == LAST_GROUP_INDEX && !lastGroupEditable.value) {
                continue
            }
            if (cursor >= digitsOnly.length) break
            val end = minOf(cursor + DIGITS_PER_GROUP, digitsOnly.length)
            digitGroups[index] = digitsOnly.substring(cursor, end)
            cursor = end
        }

        errorMessage.value = null
        recomputeCardMatch()
    }

    fun onNicknameChanged(value: String) {
        userEditedNickname = true
        cardNickname.value = value
    }

    fun onResolvedVariantSelected(variant: CardVariantOption) {
        selectedCardVariant.value = variant
        selectedBank.value = BankOption(
            code = variant.bankCode.ifBlank { selectedBank.value?.code.orEmpty() },
            name = variant.bankName.ifBlank { selectedBank.value?.name.orEmpty() }
        )
        updateMatchedBank(selectedBank.value)
        maybePrefillNickname(variant.name, selectedBank.value?.name.orEmpty())
        showCardVariantDropdown.value = false
    }

    fun onCandidateBankSelected(bank: BankOption) {
        selectedBank.value = bank
        updateMatchedBank(bank)
        candidateVariants.value = candidateVariantsForBank(bank)
        selectedCardVariant.value = null
        showBankDropdown.value = false
    }

    fun onFallbackBankSelected(bank: BankOption) {
        selectedBank.value = bank
        updateMatchedBank(bank)
        showBankDropdown.value = false

        viewModelScope.launch {
            availableCardVariants.value = repository.getCardVariantsForBank(bank.code)
            selectedCardVariant.value = null
        }
    }

    fun onFallbackVariantSelected(variant: CardVariantOption) {
        selectedCardVariant.value = variant
        selectedBank.value = BankOption(
            code = variant.bankCode.ifBlank { selectedBank.value?.code.orEmpty() },
            name = variant.bankName.ifBlank { selectedBank.value?.name.orEmpty() }
        )
        updateMatchedBank(selectedBank.value)
        maybePrefillNickname(variant.name, selectedBank.value?.name.orEmpty())
        showCardVariantDropdown.value = false
    }

    fun loadCardForEdit(context: Context, cardId: Long) {
        if (!::repository.isInitialized) {
            repository = CardRepository(context)
        }

        viewModelScope.launch {
            if (availableBanks.value.isEmpty()) {
                availableBanks.value = repository.getAllBanks()
            }

            val dao = CardPulseDatabase.getInstance(context).cardDao()
            val card = dao.getCardById(cardId.toInt()) ?: return@launch
            existingCard = card
            editingCardId = card.id

            digitGroups[0] = ""
            digitGroups[1] = ""
            digitGroups[2] = ""
            digitGroups[3] = card.last4Digits
            lastGroupEditable.value = false

            cardHolderName.value = ""
            userEditedNickname = false
            cardNickname.value = cleanCardName(card.cardName, card.bankName)
            selectedColor = runCatching { Color(android.graphics.Color.parseColor(card.color)) }
                .getOrElse { Color(0xFF1976D2) }

            val bankOption = availableBanks.value.firstOrNull { bank ->
                cleanAndNormalizeBankName(bank.name).equals(
                    cleanAndNormalizeBankName(card.bankName),
                    ignoreCase = true
                )
            } ?: BankOption(
                code = card.bankName,
                name = cleanAndNormalizeBankName(card.bankName)
            )

            selectedBank.value = bankOption
            matchedBank.value = bankOption
            matchedBankIconKey.value = repository.getBankIconKey(bankOption.code)
                ?: repository.getBankIconKey(bankOption.name)

            val variants = repository.getCardVariantsForBank(bankOption.code)
            availableCardVariants.value = variants
            selectedCardVariant.value = variants.firstOrNull { variant ->
                variant.name.equals(card.cardName, ignoreCase = true) ||
                    variant.displayName.equals(card.cardName, ignoreCase = true)
            } ?: CardVariantOption(
                bankCode = bankOption.code,
                bankName = bankOption.name,
                cardId = card.cardName,
                name = card.cardName,
                groupName = null,
                displayName = cleanCardName(card.cardName, bankOption.name),
                cardType = card.cardType,
                cardNetwork = card.cardNetwork,
                annualFee = card.annualFee,
                color = card.color
            )

            val savedCandidate = selectedCardVariant.value?.toResolvedCandidate(bankOption)
            matchState.value = savedCandidate?.let { CardMatchState.Exact(it) } ?: CardMatchState.Empty
            fallbackMode.value = false
            candidateBanks.value = emptyList()
            candidateVariants.value = emptyList()
        }
    }

    fun saveCard(context: Context, navController: NavController) {
        if (!::repository.isInitialized) {
            repository = CardRepository(context)
        }

        val finalBank = selectedBank.value
        val finalVariant = selectedCardVariant.value
        val existing = existingCard

        if (!canSave()) {
            errorMessage.value = when (matchState.value) {
                is CardMatchState.Multiple -> "Choose a card variant before saving."
                CardMatchState.NoMatch -> "Select a bank and card variant to continue."
                CardMatchState.NotEnoughDigits -> "Enter at least the first 6 digits to verify this card."
                CardMatchState.Matching -> "Please wait while we match the card."
                else -> "Complete the card details before saving."
            }
            return
        }

        if (finalBank == null || finalVariant == null) {
            errorMessage.value = "Select a bank and card variant before saving."
            return
        }

        viewModelScope.launch {
            isSaving.value = true
            errorMessage.value = null
            try {
                val dao = CardPulseDatabase.getInstance(context).cardDao()
                val existingCard = editingCardId?.let { dao.getCardById(it) }
                val cleanedBankName = cleanAndNormalizeBankName(finalBank.name)
                val cleanedCardName = cleanCardName(finalVariant.name, cleanedBankName)
                val last4 = digitGroups[LAST_GROUP_INDEX].ifBlank {
                    existingCard?.last4Digits ?: "0000"
                }

                val card = (existingCard ?: Card(
                    id = 0,
                    bankName = cleanedBankName,
                    cardName = cleanedCardName,
                    last4Digits = last4,
                    cardType = finalVariant.cardType,
                    cardNetwork = finalVariant.cardNetwork,
                    creditLimit = 0.0,
                    billingCycleDay = 1,
                    statementDay = 1,
                    dueDateOffset = 20,
                    annualFee = finalVariant.annualFee,
                    isAutoFetched = false,
                    isVerified = true,
                    isActive = true,
                    addedOn = System.currentTimeMillis(),
                    color = finalVariant.color.ifBlank {
                        String.format("#%06X", 0xFFFFFF and selectedColor.toArgb())
                    },
                    currentOutstanding = 0.0,
                    minimumDue = 0.0,
                    paymentDueDate = null
                )).copy(
                    bankName = cleanedBankName,
                    cardName = cleanedCardName,
                    last4Digits = last4,
                    cardType = finalVariant.cardType.ifBlank { existingCard?.cardType ?: "Credit Card" },
                    cardNetwork = finalVariant.cardNetwork.ifBlank { existingCard?.cardNetwork ?: "Unknown" },
                    annualFee = finalVariant.annualFee.takeIf { it > 0.0 } ?: existingCard?.annualFee ?: 0.0,
                    isVerified = true,
                    isActive = true,
                    color = String.format("#%06X", 0xFFFFFF and selectedColor.toArgb())
                )

                if (editingCardId != null) {
                    dao.updateCard(card)
                    repository.recalculateSpendProgress(card.id)
                    refreshSmsTransactions(context, card)
                } else {
                    val newId = dao.insertCard(card).toInt()
                    repository.recalculateSpendProgress(newId)
                    refreshSmsTransactions(context, card.copy(id = newId))
                }

                navController.popBackStack()
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to save card"
            } finally {
                isSaving.value = false
            }
        }
    }

    fun getEnteredPrefix(): String {
        val builder = StringBuilder()
        for (index in 0..LAST_GROUP_INDEX) {
            val group = digitGroups[index]
            if (group.isBlank()) break
            builder.append(group)
            if (group.length < DIGITS_PER_GROUP) break
        }
        return builder.toString()
    }

    fun getFullDisplayNumber(): String {
        return digitGroups.joinToString(" ").trim()
    }

    fun canSave(): Boolean {
        val hasLast4 = digitGroups[LAST_GROUP_INDEX].length == DIGITS_PER_GROUP
        if (!hasLast4) return false

        val prefix = getEnteredPrefix()
        val hasVerifiedExistingSelection = editingCardId != null &&
            prefix.isBlank() &&
            selectedBank.value != null &&
            selectedCardVariant.value != null

        if (hasVerifiedExistingSelection) return true
        if (prefix.length < MIN_PREFIX_DIGITS) return false

        return when (matchState.value) {
            is CardMatchState.Exact -> selectedBank.value != null && selectedCardVariant.value != null
            is CardMatchState.Multiple -> selectedBank.value != null && selectedCardVariant.value != null
            CardMatchState.NoMatch -> selectedBank.value != null && selectedCardVariant.value != null
            else -> false
        }
    }

    private fun recomputeCardMatch() {
        if (!::repository.isInitialized) return

        viewModelScope.launch {
            val prefix = getEnteredPrefix()

            if (prefix.isBlank()) {
                if (editingCardId != null && existingCard != null) {
                    restoreSavedSelection()
                } else {
                    clearSelectionState()
                    matchState.value = CardMatchState.Empty
                }
                return@launch
            }

            clearResolvedSelection()

            if (prefix.length < MIN_PREFIX_DIGITS) {
                fallbackMode.value = false
                matchState.value = CardMatchState.NotEnoughDigits
                return@launch
            }

            matchState.value = CardMatchState.Matching
            val rawCandidates = repository.matchCardCandidatesByPrefix(prefix)
            val candidates = filterByLength(rawCandidates)
            val bestCandidates = com.cardpulse.app.data.CardDataParser.narrowToBestCandidates(candidates)

            when {
                bestCandidates.isEmpty() -> enterFallbackMode()
                bestCandidates.size == 1 -> applyExactCandidate(bestCandidates.first())
                else -> applyMultipleCandidates(bestCandidates)
            }
        }
    }

    private suspend fun enterFallbackMode() {
        fallbackMode.value = true
        matchState.value = CardMatchState.NoMatch
        candidateBanks.value = emptyList()
        candidateVariants.value = emptyList()
        selectedBank.value = null
        selectedCardVariant.value = null
        matchedBank.value = null
        matchedBankIconKey.value = null

        if (availableBanks.value.isEmpty()) {
            availableBanks.value = repository.getAllBanks()
        }
    }

    private fun applyExactCandidate(candidate: ResolvedCardCandidate) {
        fallbackMode.value = false
        val bank = BankOption(
            code = candidate.bankCode,
            name = cleanAndNormalizeBankName(candidate.bankName)
        )
        val variant = candidate.toVariantOption(bank)

        candidateBanks.value = listOf(bank)
        candidateVariants.value = listOf(variant)
        selectedBank.value = bank
        selectedCardVariant.value = variant
        updateMatchedBank(bank)
        maybePrefillNickname(variant.name, bank.name)
        matchState.value = CardMatchState.Exact(candidate)
    }

    private fun applyMultipleCandidates(candidates: List<ResolvedCardCandidate>) {
        fallbackMode.value = false
        selectedCardVariant.value = null
        candidateBanks.value = candidates
            .map { candidate ->
                BankOption(
                    code = candidate.bankCode,
                    name = cleanAndNormalizeBankName(candidate.bankName)
                )
            }
            .distinctBy { it.name.lowercase() }
            .sortedBy { it.name }

        val fixedBank = candidateBanks.value.singleOrNull()
        selectedBank.value = fixedBank
        updateMatchedBank(fixedBank)
        candidateVariants.value = fixedBank?.let { candidateVariantsForBank(it, candidates) } ?: emptyList()
        matchState.value = CardMatchState.Multiple(
            fixedBank = fixedBank?.name,
            candidates = candidates
        )
    }

    private fun candidateVariantsForBank(
        bank: BankOption,
        source: List<ResolvedCardCandidate> = when (val current = matchState.value) {
            is CardMatchState.Multiple -> current.candidates
            else -> emptyList()
        }
    ): List<CardVariantOption> {
        return source
            .filter { candidate ->
                cleanAndNormalizeBankName(candidate.bankName).equals(
                    cleanAndNormalizeBankName(bank.name),
                    ignoreCase = true
                )
            }
            .map { candidate -> candidate.toVariantOption(bank) }
            .distinctBy { it.displayName.lowercase() }
            .sortedBy { it.displayName }
    }

    private fun maybePrefillNickname(cardName: String, bankName: String) {
        if (userEditedNickname) return
        cardNickname.value = cleanCardName(cardName, bankName)
    }

    private fun updateMatchedBank(bank: BankOption?) {
        matchedBank.value = bank
        matchedBankIconKey.value = bank?.let {
            repository.getBankIconKey(it.code) ?: repository.getBankIconKey(it.name)
        }
    }

    private fun clearSelectionState() {
        fallbackMode.value = false
        clearResolvedSelection()
        candidateBanks.value = emptyList()
        candidateVariants.value = emptyList()
    }

    private fun clearResolvedSelection() {
        selectedBank.value = null
        selectedCardVariant.value = null
        matchedBank.value = null
        matchedBankIconKey.value = null
        showBankDropdown.value = false
        showCardVariantDropdown.value = false
    }

    private fun restoreSavedSelection() {
        val currentCard = existingCard ?: return
        val bank = selectedBank.value ?: BankOption(
            code = currentCard.bankName,
            name = cleanAndNormalizeBankName(currentCard.bankName)
        )
        selectedBank.value = bank
        updateMatchedBank(bank)
        selectedCardVariant.value = selectedCardVariant.value ?: CardVariantOption(
            bankCode = bank.code,
            bankName = bank.name,
            cardId = currentCard.cardName,
            name = currentCard.cardName,
            groupName = null,
            displayName = cleanCardName(currentCard.cardName, bank.name),
            cardType = currentCard.cardType,
            cardNetwork = currentCard.cardNetwork,
            annualFee = currentCard.annualFee,
            color = currentCard.color
        )
        val savedCandidate = selectedCardVariant.value?.toResolvedCandidate(bank)
        matchState.value = savedCandidate?.let { CardMatchState.Exact(it) } ?: CardMatchState.Empty
        fallbackMode.value = false
    }

    private fun filterByLength(candidates: List<ResolvedCardCandidate>): List<ResolvedCardCandidate> {
        val enteredLength = getEnteredPrefix().length
        if (enteredLength < FULL_LENGTH_THRESHOLD) return candidates
        val filtered = candidates.filter { candidate ->
            candidate.supportedLengths.isEmpty() || candidate.supportedLengths.contains(enteredLength)
        }
        return filtered.ifEmpty { candidates }
    }

    private suspend fun refreshSmsTransactions(context: Context, card: Card) {
        withContext(Dispatchers.IO) {
            runCatching {
                val smsReader = SmsReader(context)
                smsReader.parseTransactionsForCard(card).forEach { transaction ->
                    repository.upsertDedupedTransaction(transaction)
                }
            }
        }
    }

    private fun CardVariantOption.toResolvedCandidate(bank: BankOption): ResolvedCardCandidate {
        return ResolvedCardCandidate(
            bankCode = bank.code,
            bankName = bank.name,
            groupName = groupName.orEmpty(),
            cardName = name,
            cardNetwork = cardNetwork,
            cardType = cardType,
            annualFee = annualFee.toInt(),
            color = color,
            specificity = MatchSpecificity.CARD,
            matchedPrefixSource = "selected",
            supportedLengths = listOf(16),
            displayName = displayName
        )
    }

    private fun ResolvedCardCandidate.toVariantOption(bank: BankOption = BankOption(bankCode, cleanAndNormalizeBankName(bankName))): CardVariantOption {
        return CardVariantOption(
            bankCode = bank.code,
            bankName = bank.name,
            cardId = cardName,
            name = cardName,
            groupName = groupName.takeIf { it.isNotBlank() },
            displayName = displayName,
            cardType = cardType,
            cardNetwork = cardNetwork,
            annualFee = annualFee.toDouble(),
            color = color
        )
    }

    companion object {
        private const val DIGITS_PER_GROUP = 4
        private const val LAST_GROUP_INDEX = 3
        private const val MIN_PREFIX_DIGITS = 6
        private const val FULL_LENGTH_THRESHOLD = 14
    }
}
