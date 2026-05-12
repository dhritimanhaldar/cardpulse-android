package com.cardpulse.app.data

import android.content.Context
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.CardWithProgress
import com.cardpulse.app.model.ResolvedCard
import com.cardpulse.app.model.SpendRule
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.util.cleanAndNormalizeBankName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CardRepository(private val context: Context) {

    private val db by lazy { CardPulseDatabase.getInstance(context) }
    private val cardDao by lazy { db.cardDao() }
    private val transactionDao by lazy { db.transactionDao() }
    private val spendRuleDao by lazy { db.spendRuleDao() }

    suspend fun getAllBanks(): List<BankOption> = withContext(Dispatchers.IO) {
        val catalog = CardCatalogLoader.loadCatalog(context) ?: return@withContext emptyList()
        catalog.banks
            .map { bank ->
                BankOption(
                    code = bank.b,
                    name = cleanAndNormalizeBankName(bank.b)
                )
            }
            .filter { it.name.isNotBlank() && !it.name.equals("unknown", ignoreCase = true) }
            .distinctBy { it.name.lowercase() }
            .sortedBy { it.name }
    }

    suspend fun getCardVariantsForBank(bankCode: String): List<CardVariantOption> =
        withContext(Dispatchers.IO) {
            val catalog = CardCatalogLoader.loadCatalog(context) ?: return@withContext emptyList()
            val bank = catalog.banks.find { it.b.equals(bankCode, ignoreCase = true) }
                ?: return@withContext emptyList()

            val variants = mutableListOf<CardVariantOption>()
            bank.g.orEmpty().forEach { group ->
                group.c.orEmpty().forEach { card ->
                    variants.add(
                        CardVariantOption(
                            cardId = card.n,
                            name = card.n,
                            groupName = group.n,
                            displayName = "${bank.b} ${group.n} ${card.n}"
                                .replace("\\s+".toRegex(), " ")
                                .trim()
                        )
                    )
                }
            }
            variants.sortedBy { it.displayName }
        }

    suspend fun matchCardByBin(bin: String): ResolvedCard? = withContext(Dispatchers.IO) {
        val catalog = CardCatalogLoader.loadCatalog(context) ?: return@withContext null
        CardDataParser.matchCard(catalog, bin, null, null)
    }

    suspend fun searchCardsInCatalog(query: String): List<ResolvedCard> = withContext(Dispatchers.IO) {
        val catalog = CardCatalogLoader.loadCatalog(context) ?: return@withContext emptyList()
        CardDataParser.searchCards(catalog, query)
    }

    suspend fun matchCardFromCatalog(card: Card): ResolvedCard? = withContext(Dispatchers.IO) {
        val catalog = CardCatalogLoader.loadCatalog(context) ?: return@withContext null
        CardDataParser.matchCard(catalog, card.last4Digits.padStart(6, '0'), card.bankName, card.cardName)
            ?: CardDataParser.searchCards(catalog, "${card.bankName} ${card.cardName}").firstOrNull()
    }

    suspend fun getAllCards(): List<Card> = withContext(Dispatchers.IO) {
        cardDao.getAllCards()
    }

    suspend fun getAllCardsSync(): List<Card> = withContext(Dispatchers.IO) {
        cardDao.getAllCardsSync()
    }

    suspend fun getCardById(cardId: Int): Card? = withContext(Dispatchers.IO) {
        cardDao.getCardById(cardId)
    }

    fun getActiveCardsFlow(): Flow<List<Card>> = cardDao.getActiveCardsFlow()

    fun getAllCardsWithProgressFlow(): Flow<List<CardWithProgress>> {
        return cardDao.getActiveCardsFlow().map { cards ->
            cards.map { card ->
                val rules = spendRuleDao.getRulesForCard(card.id)
                val txns = transactionDao.getTransactionsForCard(card.id)
                val lounge = db.loungeDao().getLoungeForCard(card.id)
                val totalSpent = txns.filter { !it.isCredit }.sumOf { it.amount }
                CardWithProgress(
                    card = card,
                    spendRules = rules,
                    totalSpentThisCycle = totalSpent,
                    recentTransactions = txns.take(10),
                    loungeAccess = lounge
                )
            }
        }
    }

    suspend fun insertCard(card: Card): Long = withContext(Dispatchers.IO) {
        cardDao.insertCard(card)
    }

    suspend fun updateCard(card: Card) = withContext(Dispatchers.IO) {
        cardDao.updateCard(card)
    }

    suspend fun deleteCard(cardId: Int) = withContext(Dispatchers.IO) {
        cardDao.softDeleteCard(cardId)
    }

    suspend fun getCardWithProgress(cardId: Int): CardWithProgress? = withContext(Dispatchers.IO) {
        val card = cardDao.getCardById(cardId) ?: return@withContext null
        val rules = spendRuleDao.getRulesForCard(cardId)
        val txns = transactionDao.getTransactionsForCard(cardId)
        val lounge = db.loungeDao().getLoungeForCard(cardId)
        val totalSpent = txns.filter { !it.isCredit }.sumOf { it.amount }
        CardWithProgress(
            card = card,
            spendRules = rules,
            totalSpentThisCycle = totalSpent,
            recentTransactions = txns.take(10),
            loungeAccess = lounge
        )
    }

    suspend fun getTransactionsForCard(cardId: Int): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.getTransactionsForCard(cardId)
    }

    suspend fun getTransactionByEmailId(emailId: String): Transaction? = withContext(Dispatchers.IO) {
        transactionDao.getTransactionByEmailId(emailId)
    }

    suspend fun getCardByDetails(bank: String, name: String, last4: String): Card? = withContext(Dispatchers.IO) {
        cardDao.getCardByDetails(bank, name, last4)
    }

    suspend fun getTransactionByDetails(cardId: Int, amount: Double, date: Long): Transaction? =
        withContext(Dispatchers.IO) {
            transactionDao.getTransactionByDetails(cardId, amount, date)
        }

    suspend fun insertTransaction(txn: Transaction): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(txn)
    }

    suspend fun updateTransaction(txn: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(txn)
    }

    suspend fun replaceSpendRules(cardId: Int, rules: List<SpendRule>) = withContext(Dispatchers.IO) {
        spendRuleDao.deleteRulesForCard(cardId)
        rules.forEach { spendRuleDao.insertRule(it) }
    }

    suspend fun recalculateSpendProgress(cardId: Int) {
        // Placeholder for future milestone recalculation.
    }

    suspend fun getAllCardsWithProgress(): List<CardWithProgress> = withContext(Dispatchers.IO) {
        val cards = cardDao.getAllCards()
        cards.map { card ->
            val rules = spendRuleDao.getRulesForCard(card.id)
            val txns = transactionDao.getTransactionsForCard(card.id)
            val lounge = db.loungeDao().getLoungeForCard(card.id)
            val totalSpent = txns.filter { !it.isCredit }.sumOf { it.amount }
            CardWithProgress(
                card = card,
                spendRules = rules,
                totalSpentThisCycle = totalSpent,
                recentTransactions = txns.take(10),
                loungeAccess = lounge
            )
        }
    }
}

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
