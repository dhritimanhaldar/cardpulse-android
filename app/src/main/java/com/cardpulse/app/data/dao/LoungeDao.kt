package com.cardpulse.app.data.dao

import androidx.room.*
import com.cardpulse.app.model.LoungeAccess

@Dao
interface LoungeDao {
    @Query("SELECT * FROM lounge_access WHERE cardId = :cardId LIMIT 1")
    suspend fun getLoungeForCard(cardId: Int): LoungeAccess?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(loungeAccess: LoungeAccess)

    @Update
    suspend fun update(loungeAccess: LoungeAccess)

    @Query("DELETE FROM lounge_access")
    suspend fun deleteAllLoungeAccess()
}
