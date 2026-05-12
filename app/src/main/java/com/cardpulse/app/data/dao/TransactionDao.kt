package com.cardpulse.app.data.dao

import androidx.room.*
import com.cardpulse.app.model.Transaction

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY date DESC")
    suspend fun getTransactionsForCard(cardId: Int): List<Transaction>

    @Query("SELECT * FROM transactions WHERE rawEmailId = :emailId LIMIT 1")
    suspend fun getTransactionByEmailId(emailId: String): Transaction?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE cardId = :cardId AND isCredit = 0 ORDER BY date DESC")
    suspend fun getConfirmedDebitsForCard(cardId: Int): List<Transaction>

    @Query("SELECT SUM(amount) FROM transactions WHERE cardId = :cardId AND isCredit = 0")
    suspend fun getTotalSpentForCard(cardId: Int): Double?
}