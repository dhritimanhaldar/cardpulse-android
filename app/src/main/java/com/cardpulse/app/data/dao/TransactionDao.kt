package com.cardpulse.app.data.dao

import androidx.room.*
import com.cardpulse.app.model.Transaction

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY date DESC")
    suspend fun getTransactionsForCard(cardId: Int): List<Transaction>

    @Query("SELECT * FROM transactions WHERE rawEmailId = :emailId LIMIT 1")
    suspend fun getTransactionByEmailId(emailId: String): Transaction?

    @Query(
        "SELECT * FROM transactions WHERE cardId = :cardId AND amount = :amount " +
                "AND ABS(date - :date) < 60000 LIMIT 1"
    )
    suspend fun getTransactionByDetails(cardId: Int, amount: Double, date: Long): Transaction?

    @Query(
        "SELECT * FROM transactions WHERE cardId = :cardId AND amount = :amount " +
            "AND ABS(date - :date) < :toleranceMillis"
    )
    suspend fun findPotentialDuplicates(
        cardId: Int,
        amount: Double,
        date: Long,
        toleranceMillis: Long
    ): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE cardId = :cardId AND isCredit = 0 AND lower(category) NOT IN ('payment', 'refund', 'fee') ORDER BY date DESC")
    suspend fun getConfirmedDebitsForCard(cardId: Int): List<Transaction>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId AND category = :category ORDER BY date DESC")
    suspend fun getTransactionsByCategory(cardId: Int, category: String): List<Transaction>

    @Query(
        "SELECT SUM(CASE " +
            "WHEN transactionKind = 'REFUND' OR lower(category) = 'refund' THEN -amount " +
            "WHEN transactionKind = 'SPEND' OR (isCredit = 0 AND lower(category) NOT IN ('payment', 'fee')) THEN amount " +
            "ELSE 0 END) FROM transactions WHERE cardId = :cardId"
    )
    suspend fun getTotalSpentForCard(cardId: Int): Double?

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
