package com.cardpulse.app.data

import android.content.Context
import com.cardpulse.app.model.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CardRepository(private val context: Context) {

    // Get all unique banks from catalog
    suspend fun getAllBanks(): List<BankOption> = withContext(Dispatchers.IO) {
        val catalog = CardCatalogLoader.loadCatalog(context) ?: return@withContext emptyList()
        catalog.banks.map { bank ->
            BankOption(
                code = bank.code,
                name = bank.name
            )
        }.sortedBy { it.name }
    }

    // Get all card variants for a specific bank
    suspend fun getCardVariantsForBank(bankCode: String): List<CardVariantOption> =
        withContext(Dispatchers.IO) {
            val catalog = CardCatalogLoader.loadCatalog(context) ?: return@withContext emptyList()
            val bank = catalog.banks.find { it.code == bankCode } ?: return@withContext emptyList()

            val variants = mutableListOf<CardVariantOption>()
            bank.groups.forEach { group ->
                group.cards.forEach { card ->
                    variants.add(CardVariantOption(
                        cardId = card.id,
                        name = card.name,
                        groupName = group.name,
                        displayName = formatCardDisplayName(bank.name, group.name, card.name)
                    ))
                }
            }
            variants.sortedBy { it.displayName }
        }

    // Smart name formatter to avoid duplication
    private fun formatCardDisplayName(bankName: String, groupName: String?, cardName: String): String {
        val cleanCardName = cardName
            .replace(bankName, "", ignoreCase = true)
            .trim()

        val finalCardName = if (groupName != null) {
            cleanCardName.replace(groupName, "", ignoreCase = true).trim()
        } else {
            cleanCardName
        }

        return when {
            groupName != null && finalCardName.isNotBlank() ->
                "$bankName $groupName $finalCardName"
            groupName != null ->
                "$bankName $groupName"
            finalCardName.isNotBlank() ->
                "$bankName $finalCardName"
            else ->
                "$bankName Card"
        }.replace("\\s+".toRegex(), " ").trim()
    }

    // Match card by BIN
    suspend fun matchCardByBin(bin: String): ResolvedCard? = withContext(Dispatchers.IO) {
        val catalog = CardCatalogLoader.loadCatalog(context) ?: return@withContext null
        val parser = CardDataParser(catalog)
        parser.matchCardByBin(bin)
    }

    // Search cards in catalog
    suspend fun searchCardsInCatalog(query: String): List<ResolvedCard> = withContext(Dispatchers.IO) {
        val catalog = CardCatalogLoader.loadCatalog(context) ?: return@withContext emptyList()
        val parser = CardDataParser(catalog)

        val results = mutableListOf<ResolvedCard>()
        catalog.banks.forEach { bank ->
            bank.groups.forEach { group ->
                group.cards.forEach { card ->
                    val resolved = parser.resolveCard(bank.code, group.name, card.id)
                    if (resolved != null &&
                        (resolved.cardName.contains(query, ignoreCase = true) ||
                                resolved.bankName.contains(query, ignoreCase = true))) {
                        results.add(resolved)
                    }
                }
            }
        }
        results
    }

    // Rematch and refresh card after edit
    suspend fun refreshCardMetrics(cardId: Long) = withContext(Dispatchers.IO) {
        val dao = CardPulseDatabase.getDatabase(context).cardDao()
        val card = dao.getCardById(cardId) ?: return@withContext

        // 1. Rematch card to JSON catalog
        val resolved = matchCardByBin(card.cardNumber.take(6))

        // 2. Update card with catalog metadata
        val updatedCard = card.copy(
            catalogId = resolved?.catalogId,
            cardType = resolved?.cardType,
            network = resolved?.network,
            isVerified = true
        )
        dao.updateCard(updatedCard)

        // 3. Rematch all SMS transactions to this card
        rematchTransactionsForCard(cardId, card.cardNumber.takeLast(4), card.bankName)
    }

    // Rematch SMS transactions after bank/card change
    private suspend fun rematchTransactionsForCard(
        cardId: Long,
        last4Digits: String,
        bankName: String
    ) {
        val transactionDao = CardPulseDatabase.getDatabase(context).transactionDao()

        val potentialMatches = transactionDao.getAllTransactions().filter { txn ->
            txn.cardNumber.endsWith(last4Digits) &&
                    txn.description.contains(bankName, ignoreCase = true)
        }

        potentialMatches.forEach { txn ->
            transactionDao.updateTransaction(txn.copy(cardId = cardId))
        }
    }
}

// Data classes for bank and card selection
data class BankOption(
    val code: String,
    val name: String
)

data class CardVariantOption(
    val cardId: String,
    val name: String,
    val groupName: String?,
    val displayName: String
)