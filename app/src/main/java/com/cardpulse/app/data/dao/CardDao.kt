package com.cardpulse.app.data.dao

import androidx.room.*
import com.cardpulse.app.model.Card
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE isActive = 1 ORDER BY addedOn DESC")
    suspend fun getAllCards(): List<Card>

    @Query("SELECT * FROM cards WHERE isActive = 1 ORDER BY addedOn DESC")
    fun getActiveCardsFlow(): Flow<List<Card>>

    @Query("SELECT * FROM cards ORDER BY addedOn DESC")
    fun getAllCardsFlow(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE isActive = 1 ORDER BY addedOn DESC")
    suspend fun getAllCardsSync(): List<Card>

    @Query("SELECT * FROM cards WHERE id = :cardId LIMIT 1")
    suspend fun getCardById(cardId: Int): Card?

    @Query(
        "SELECT * FROM cards WHERE lower(bankName) = lower(:bank) " +
                "AND lower(cardName) = lower(:name) AND last4Digits = :last4 LIMIT 1"
    )
    suspend fun getCardByDetails(bank: String, name: String, last4: String): Card?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card): Long

    @Update
    suspend fun updateCard(card: Card)

    @Query("UPDATE cards SET isActive = 0 WHERE id = :cardId")
    suspend fun softDeleteCard(cardId: Int)

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()
}
