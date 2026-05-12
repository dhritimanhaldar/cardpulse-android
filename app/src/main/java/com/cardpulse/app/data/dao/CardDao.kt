package com.cardpulse.app.data.dao

import androidx.room.*
import com.cardpulse.app.model.Card

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE isActive = 1 ORDER BY addedOn DESC")
    suspend fun getAllCards(): List<Card>

    @Query("SELECT * FROM cards WHERE isActive = 1 ORDER BY addedOn DESC")
    suspend fun getAllCardsSync(): List<Card>

    @Query("SELECT * FROM cards WHERE id = :cardId LIMIT 1")
    suspend fun getCardById(cardId: Int): Card?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card): Long

    @Update
    suspend fun updateCard(card: Card)

    @Query("UPDATE cards SET isActive = 0 WHERE id = :cardId")
    suspend fun softDeleteCard(cardId: Int)
}