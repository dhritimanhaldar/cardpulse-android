package com.cardpulse.app.data

import android.content.Context
import com.cardpulse.app.model.*
import java.util.Calendar
import java.util.Date

class CardRepository(context: Context) {

    private val db = CardPulseDatabase.getInstance(context)
    private val cardDao = db.cardDao()
    private val transactionDao = db.transactionDao()
    private val spendRuleDao = db.spendRuleDao()
    private val loungeDao = db.loungeDao()

    // ─── Cards ─────────────────────────────────────────────────
    suspend fun getAllCards(): List<Card> = cardDao.getAllCards()
    suspend fun getCardById(id: Int): Card? = cardDao.getCardById(id)
    suspend fun insertCard(card: Card): Long = cardDao.insertCard(card)
    suspend fun updateCard(card: Card) = cardDao.updateCard(card)
    suspend fun deleteCard(cardId: Int) = cardDao.softDeleteCard(cardId)

    // ─── Transactions ──────────────────────────────────────────
    suspend fun getTransactionsForCard(cardId: Int): List<Transaction> =
        transactionDao.getTransactionsForCard(cardId)

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction)

    suspend fun getPendingFlaggedTransactions(): List<Transaction> =
        transactionDao.getPendingFlaggedTransactions()

    suspend fun getTransactionByEmailId(emailId: String): Transaction? =
        transactionDao.getTransactionByEmailId(emailId)

    suspend fun getTotalSpentThisCycle(card: Card): Double {
        val cycleStart = getBillingCycleStart(card.billingCycleDay)
        return transactionDao.getTotalSpentSince(card.id, cycleStart.time) ?: 0.0
    }

    // ─── Spend Rules ───────────────────────────────────────────
    suspend fun getRulesForCard(cardId: Int): List<SpendRule> =
        spendRuleDao.getRulesForCard(cardId)

    suspend fun insertRule(rule: SpendRule): Long = spendRuleDao.insertRule(rule)
    suspend fun updateRule(rule: SpendRule) = spendRuleDao.updateRule(rule)

    // ─── Lounge ────────────────────────────────────────────────
    suspend fun getLoungeForCard(cardId: Int): LoungeAccess? =
        loungeDao.getLoungeForCard(cardId)

    suspend fun insertOrUpdateLounge(lounge: LoungeAccess) =
        loungeDao.insertOrUpdate(lounge)

    // ─── Composite ─────────────────────────────────────────────
    suspend fun getCardWithProgress(cardId: Int): CardWithProgress? {
        val card = cardDao.getCardById(cardId) ?: return null
        val rules = spendRuleDao.getRulesForCard(cardId)
        val totalSpent = getTotalSpentThisCycle(card)
        val recentTxns = transactionDao.getTransactionsSince(
            cardId, getBillingCycleStart(card.billingCycleDay).time
        )
        val lounge = loungeDao.getLoungeForCard(cardId)
        return CardWithProgress(card, rules, totalSpent, recentTxns, lounge)
    }

    suspend fun getAllCardsWithProgress(): List<CardWithProgress> =
        getAllCards().mapNotNull { getCardWithProgress(it.id) }

    // ─── Billing cycle helper ──────────────────────────────────
    private fun getBillingCycleStart(billingCycleDay: Int): Date {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        if (today < billingCycleDay) cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, billingCycleDay)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }
}
