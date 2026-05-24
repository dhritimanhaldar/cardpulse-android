package com.cardpulse.app.data.dao

import androidx.room.*
import com.cardpulse.app.model.SpendRule

@Dao
interface SpendRuleDao {
    @Query("SELECT * FROM spend_rules WHERE cardId = :cardId")
    suspend fun getRulesForCard(cardId: Int): List<SpendRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: SpendRule): Long

    @Update
    suspend fun updateRule(rule: SpendRule)

    @Query("DELETE FROM spend_rules WHERE cardId = :cardId")
    suspend fun deleteRulesForCard(cardId: Int)

    @Query("DELETE FROM spend_rules")
    suspend fun deleteAllRules()
}
