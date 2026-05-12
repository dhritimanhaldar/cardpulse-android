package com.cardpulse.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bankName: String,
    val cardName: String,
    val last4Digits: String,
    val cardType: String,
    val cardNetwork: String,
    val creditLimit: Double,
    val billingCycleDay: Int,
    val statementDay: Int,
    val dueDateOffset: Int,
    val annualFee: Double,
    val isAutoFetched: Boolean = false,
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val addedOn: Long = System.currentTimeMillis(),
    val color: String = "#1A73E8",
    val currentOutstanding: Double = 0.0,
    val minimumDue: Double = 0.0,
    val paymentDueDate: String? = null
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val amount: Double,
    val merchant: String,
    val category: String,
    val date: Long = System.currentTimeMillis(),
    val source: TransactionSource = TransactionSource.MANUAL,
    val rawText: String = "",
    val rawEmailId: String? = null,
    val status: TransactionStatus = TransactionStatus.CONFIRMED,
    val isCredit: Boolean = false,
    val isFlagged: Boolean = false,
    val flagReason: String? = "",
    val currency: String = "INR",
    val isInternational: Boolean = false
)

enum class TransactionSource { MANUAL, GMAIL, SMS }
enum class TransactionStatus { CONFIRMED, PENDING, FLAGGED }

@Entity(tableName = "spend_rules")
data class SpendRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val ruleName: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val reward: String,
    val rewardType: String,
    val cycleType: String,
    val isAchieved: Boolean = false,
    val resetDay: Int = 1
)

@Entity(tableName = "lounge_access")
data class LoungeAccess(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val totalVisitsAllowed: Int,
    val visitsUsed: Int = 0,
    val visitsRemaining: Int,
    val resetPeriod: String,
    val lastVisitDate: Long? = null,
    val loungeNetwork: String
)

@Entity(tableName = "notification_log")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val title: String,
    val message: String,
    val type: String,
    val sentAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

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