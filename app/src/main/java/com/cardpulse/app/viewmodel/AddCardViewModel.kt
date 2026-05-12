package com.cardpulse.app.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
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
import com.cardpulse.app.util.cleanCardName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddCardViewModel : ViewModel() {
    private var editingCardId: Int? = null
    private lateinit var repository: CardRepository

    val cardName = mutableStateOf("")
    val last4Digits = mutableStateOf("")
    val cardHolderName = mutableStateOf("")
    val bankName = mutableStateOf("")
    val cardNickname = mutableStateOf("")
    var selectedColor by mutableStateOf(Color(0xFF1976D2))

    val availableBanks = mutableStateOf<List<BankOption>>(emptyList())
    val selectedBank = mutableStateOf<BankOption?>(null)

    val availableCardVariants = mutableStateOf<List<CardVariantOption>>(emptyList())
    val selectedCardVariant = mutableStateOf<CardVariantOption?>(null)

    val showBankDropdown = mutableStateOf(false)
    val showCardVariantDropdown = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val isSaving = mutableStateOf(false)

    fun initialize(context: Context) {
        repository = CardRepository(context)
        viewModelScope.launch {
            availableBanks.value = repository.getAllBanks()
        }
    }

    fun onBankSelected(bank: BankOption) {
        selectedBank.value = bank
        bankName.value = bank.name
        showBankDropdown.value = false

        viewModelScope.launch {
            availableCardVariants.value = repository.getCardVariantsForBank(bank.code)
            selectedCardVariant.value = null
        }
    }

    fun onCardVariantSelected(variant: CardVariantOption) {
        selectedCardVariant.value = variant
        showCardVariantDropdown.value = false

        if (cardNickname.value.isBlank()) {
            cardNickname.value = cleanCardName(variant.displayName, selectedBank.value?.name.orEmpty())
        }
    }

    fun loadCardForEdit(context: Context, cardId: Long) {
        repository = CardRepository(context)
        viewModelScope.launch {
            if (availableBanks.value.isEmpty()) {
                availableBanks.value = repository.getAllBanks()
            }

            val dao = CardPulseDatabase.getInstance(context).cardDao()
            val card = dao.getCardById(cardId.toInt())
            card?.let {
                editingCardId = it.id
                cardName.value = it.cardName
                last4Digits.value = it.last4Digits
                cardHolderName.value = it.cardName
                cardNickname.value = it.cardName
                bankName.value = it.bankName
                selectedColor = runCatching { Color(android.graphics.Color.parseColor(it.color)) }
                    .getOrElse { Color(0xFF1976D2) }

                val matchingBank = availableBanks.value.find { bank ->
                    bank.name.equals(it.bankName, ignoreCase = true)
                }
                selectedBank.value = matchingBank

                matchingBank?.let { bank ->
                    availableCardVariants.value = repository.getCardVariantsForBank(bank.code)
                    selectedCardVariant.value = availableCardVariants.value.find { variant ->
                        variant.name.equals(it.cardName, ignoreCase = true) ||
                                variant.displayName.equals(it.cardName, ignoreCase = true)
                    }
                }
            }
        }
    }

    fun saveCard(context: Context, navController: NavController) {
        if (!::repository.isInitialized) {
            repository = CardRepository(context)
        }

        viewModelScope.launch {
            isSaving.value = true
            errorMessage.value = null
            try {
                val dao = CardPulseDatabase.getInstance(context).cardDao()
                val existingCard = editingCardId?.let { dao.getCardById(it) }
                val finalBankName = selectedBank.value?.name ?: bankName.value.ifBlank { existingCard?.bankName.orEmpty() }
                val finalCardName = cleanCardName(
                    cardNickname.value.ifBlank { cardName.value.ifBlank { existingCard?.cardName ?: "Card" } },
                    finalBankName
                )

                val card = (existingCard ?: Card(
                    id = 0,
                    bankName = finalBankName,
                    cardName = finalCardName,
                    last4Digits = last4Digits.value.ifBlank { "XXXX" },
                    cardType = selectedCardVariant.value?.name ?: "Credit Card",
                    cardNetwork = selectedBank.value?.name ?: finalBankName,
                    creditLimit = 0.0,
                    billingCycleDay = 1,
                    statementDay = 1,
                    dueDateOffset = 20,
                    annualFee = 0.0,
                    isAutoFetched = false,
                    isVerified = true,
                    isActive = true,
                    addedOn = System.currentTimeMillis(),
                    color = String.format("#%06X", 0xFFFFFF and selectedColor.toArgb()),
                    currentOutstanding = 0.0,
                    minimumDue = 0.0,
                    paymentDueDate = null
                )).copy(
                    bankName = finalBankName,
                    cardName = finalCardName,
                    last4Digits = last4Digits.value.ifBlank { existingCard?.last4Digits ?: "XXXX" },
                    cardType = selectedCardVariant.value?.name ?: existingCard?.cardType ?: "Credit Card",
                    cardNetwork = selectedBank.value?.name ?: existingCard?.cardNetwork ?: finalBankName,
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

    private suspend fun refreshSmsTransactions(context: Context, card: Card) {
        withContext(Dispatchers.IO) {
            runCatching {
                val smsReader = SmsReader(context)
                smsReader.parseTransactionsForCard(card).forEach { transaction ->
                    val dedupKey = "${transaction.date}_${transaction.amount}_${transaction.merchant}"
                    if (repository.getTransactionByEmailId(dedupKey) == null &&
                        repository.getTransactionByDetails(transaction.cardId, transaction.amount, transaction.date) == null
                    ) {
                        repository.insertTransaction(transaction.copy(rawEmailId = dedupKey))
                    }
                }
            }
        }
    }
}
