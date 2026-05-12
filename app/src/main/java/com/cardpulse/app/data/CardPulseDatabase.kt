package com.cardpulse.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 1,
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
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}