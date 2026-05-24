package com.cardpulse.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cardpulse.app.data.dao.CardDao
import com.cardpulse.app.data.dao.LoungeDao
import com.cardpulse.app.data.dao.NotificationLogDao
import com.cardpulse.app.data.dao.SpendRuleDao
import com.cardpulse.app.data.dao.TransactionDao
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.LoungeAccess
import com.cardpulse.app.model.NotificationLog
import com.cardpulse.app.model.SpendRule
import com.cardpulse.app.model.Transaction

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
        @Volatile
        private var INSTANCE: CardPulseDatabase? = null

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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN transactionKind TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN tagConfidence REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isTagUserEdited INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceFingerprint TEXT")
            }
        }
    }
}
