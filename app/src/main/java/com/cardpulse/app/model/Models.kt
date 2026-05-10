package com.cardpulse.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

// ─── Type Converters ───────────────────────────────────────────
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

// ─── Card ──────────────────────────────────────────────────────
@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bankName: String,
    val cardName: String,
    val last4Digits: String,
    val cardType: String,           // VISA / MASTERCARD / RUPAY / AMEX
    val cardNetwork: String,        // e.g. "HDFC Infinia", "Axis Magnus"
    val creditLimit: Double,
    val billingCycleDay: Int,       // day of month billing cycle resets
    val statementDay: Int,          // day statement is generated
    val dueDateOffset: Int,         // days after statement day payment is due
    val annualFee: Double,
    val isActive: Boolean = true,
    val addedOn: Date = Date(),
    val color: String = "#1A73E8"   // hex color for card UI
)

// ─── Transaction ───────────────────────────────────────────────
@Entity(tableName = "transactions")
@TypeConverters(Converters::class)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val amount: Double,
    val merchant: String,
    val category: String,           // FOOD / TRAVEL / SHOPPING / FUEL / etc.
    val date: Date,
    val source: String,             // SMS / GMAIL / MANUAL / OCR
    val rawText: String = "",       // original SMS or email text
    val isFlagged: Boolean = false, // fraud / duplicate flag
    val flagReason: String = "",    // reason if flagged
    val isConfirmed: Boolean = true,// false = pending user confirmation
    val currency: String = "INR",
    val isInternational: Boolean = false
)

// ─── Spend Rule ────────────────────────────────────────────────
@Entity(tableName = "spend_rules")
data class SpendRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val ruleName: String,           // e.g. "Milestone 1 - Spend ₹1.5L"
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val reward: String,             // e.g. "10,000 bonus points"
    val rewardType: String,         // POINTS / CASHBACK / VOUCHER / LOUNGE
    val cycleType: String,          // MONTHLY / QUARTERLY / ANNUAL
    val isAchieved: Boolean = false,
    val resetDay: Int = 1           // day of month the cycle resets
)

// ─── Lounge Access ─────────────────────────────────────────────
@Entity(tableName = "lounge_access")
@TypeConverters(Converters::class)
data class LoungeAccess(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val totalVisitsAllowed: Int,
    val visitsUsed: Int = 0,
    val visitsRemaining: Int,
    val resetPeriod: String,        // QUARTERLY / ANNUAL
    val lastVisitDate: Date? = null,
    val loungeNetwork: String       // DREAMFOLKS / PRIORITY_PASS / DINERS
)

// ─── Notification Log ──────────────────────────────────────────
@Entity(tableName = "notification_log")
@TypeConverters(Converters::class)
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val title: String,
    val message: String,
    val type: String,               // MILESTONE / FRAUD / DUE_DATE / LOUNGE
    val sentAt: Date = Date(),
    val isRead: Boolean = false
)

// ─── UI State Models (not stored in DB) ────────────────────────
data class CardWithProgress(
    val card: Card,
    val spendRules: List<SpendRule>,
    val totalSpentThisCycle: Double,
    val recentTransactions: List<Transaction>,
    val loungeAccess: LoungeAccess?
)

data class TransactionFlag(
    val transaction: Transaction,
    val reason: FlagReason
)

enum class FlagReason {
    DUPLICATE,
    LARGE_AMOUNT,
    INTERNATIONAL,
    UNUSUAL_MERCHANT,
    RAPID_SUCCESSION
}

enum class TransactionCategory {
    FOOD, TRAVEL, SHOPPING, FUEL, ENTERTAINMENT,
    UTILITIES, HEALTH, EDUCATION, INSURANCE, OTHER
}
