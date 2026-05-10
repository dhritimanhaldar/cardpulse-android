package com.cardpulse.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.SpendRule
import com.cardpulse.app.model.LoungeAccess
import com.cardpulse.app.model.NotificationLog

// ─── DAOs ──────────────────────────────────────────────────────

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE isActive = 1 ORDER BY addedOn DESC")
    suspend fun getAllCards(): List<Card>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardById(cardId: Int): Card?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card): Long

    @Update
    suspend fun updateCard(card: Card)

    @Query("UPDATE cards SET isActive = 0 WHERE id = :cardId")
    suspend fun softDeleteCard(cardId: Int)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY date DESC")
    suspend fun getTransactionsForCard(cardId: Int): List<Transaction>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId AND date >= :fromDate ORDER BY date DESC")
    suspend fun getTransactionsSince(cardId: Int, fromDate: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE status = 'FLAGGED' OR status = 'PENDING'")
    suspend fun getPendingFlaggedTransactions(): List<Transaction>

    @Query("""
        SELECT * FROM transactions 
        WHERE cardId = :cardId 
        AND ABS(amount - :amount) < 1.0 
        AND ABS(date - :date) < :windowMs
        LIMIT 1
    """)
    suspend fun findDuplicate(cardId: Int, amount: Double, date: Long, windowMs: Long): Transaction?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE rawEmailId = :emailId LIMIT 1")
    suspend fun getTransactionByEmailId(emailId: String): Transaction?

    @Query("SELECT SUM(amount) FROM transactions WHERE cardId = :cardId AND date >= :fromDate")
    suspend fun getTotalSpentSince(cardId: Int, fromDate: Long): Double?
}

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
}

@Dao
interface LoungeDao {
    @Query("SELECT * FROM lounge_access WHERE cardId = :cardId LIMIT 1")
    suspend fun getLoungeForCard(cardId: Int): LoungeAccess?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(loungeAccess: LoungeAccess)

    @Update
    suspend fun update(loungeAccess: LoungeAccess)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_log ORDER BY sentAt DESC LIMIT 50")
    suspend fun getRecentNotifications(): List<NotificationLog>

    @Insert
    suspend fun insert(log: NotificationLog)

    @Query("UPDATE notification_log SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)
}

// ─── Database ──────────────────────────────────────────────────

@Database(
    entities = [
        Card::class,
        Transaction::class,
        SpendRule::class,
        LoungeAccess::class,
        NotificationLog::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CardPulseDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun transactionDao(): TransactionDao
    abstract fun spendRuleDao(): SpendRuleDao
    abstract fun loungeDao(): LoungeDao
    abstract fun notificationLogDao(): NotificationLogDao

    companion object {
        @Volatile private var INSTANCE: CardPulseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val cursor = database.query("PRAGMA table_info(transactions)")
                val existingColumns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    existingColumns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()
                if (!existingColumns.contains("rawEmailId"))
                    database.execSQL("ALTER TABLE transactions ADD COLUMN rawEmailId TEXT")
                if (!existingColumns.contains("source"))
                    database.execSQL("ALTER TABLE transactions ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'")
                if (!existingColumns.contains("status"))
                    database.execSQL("ALTER TABLE transactions ADD COLUMN status TEXT NOT NULL DEFAULT 'CONFIRMED'")
                if (!existingColumns.contains("isCredit"))
                    database.execSQL("ALTER TABLE transactions ADD COLUMN isCredit INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): CardPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CardPulseDatabase::class.java,
                    "cardpulse_db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
