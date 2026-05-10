package com.cardpulse.app.parser

import android.util.Log
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import java.util.Date

data class RawSmsData(
    val sender: String,
    val body: String,
    val timestamp: Long
)

data class SmsTransactionResult(
    val amount: Double,
    val merchant: String,
    val last4Digits: String?,
    val bankName: String?,
    val isCredit: Boolean,
    val category: String = "General"
)

object SmsTransactionParser {

    private val TAG = "SmsTransactionParser"

    // Debit patterns: "debited", "spent", "withdrawn", "payment of"
    private val DEBIT_PATTERNS = listOf(
        Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)[\s\S]{0,60}(?:debited|spent|used at|payment of)""", RegexOption.IGNORE_CASE),
        Regex("""(?:debited|spent|payment of)\s+(?:INR|Rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)\s+(?:debited|spent)""", RegexOption.IGNORE_CASE)
    )

    // Credit patterns: "credited", "refund", "cashback", "payment received"
    private val CREDIT_PATTERNS = listOf(
        Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)[\s\S]{0,60}(?:credited|refund|cashback|payment received)""", RegexOption.IGNORE_CASE),
        Regex("""(?:credited|refund|cashback)\s+(?:with\s+)?(?:INR|Rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    )

    private val LAST4_PATTERN = Regex("""(?:card|a/c|acct|account)[\s\S]{0,10}?(\d{4})\b""", RegexOption.IGNORE_CASE)

    private val MERCHANT_PATTERNS = listOf(
        Regex("""(?:at|to|towards|for)\s+([A-Za-z0-9 &'./-]{3,40})""", RegexOption.IGNORE_CASE),
        Regex("""(?:purchase at|txn at|used at)\s+([A-Za-z0-9 &'./-]{3,40})""", RegexOption.IGNORE_CASE)
    )

    // Known bank SMS sender addresses
    private val BANK_SENDER_MAP = mapOf(
        "HDFCBK" to "HDFC Bank",
        "SBIINB" to "SBI",
        "ICICIB" to "ICICI Bank",
        "AXISBK" to "Axis Bank",
        "KOTAKB" to "Kotak Bank",
        "IDFCBK" to "IDFC First Bank",
        "SCBANK" to "Standard Chartered",
        "AMEXIN" to "American Express",
        "INDUSB" to "IndusInd Bank",
        "YESBNK" to "Yes Bank",
        "RBLBNK" to "RBL Bank",
        "BOIIND" to "Bank of India",
        "PNBSMS" to "Punjab National Bank",
        "CANBNK" to "Canara Bank",
        "CENTBK" to "Central Bank"
    )

    fun isBankSms(sender: String): Boolean {
        val lower = sender.lowercase().replace("-", "")
        return BANK_SENDER_MAP.keys.any { lower.contains(it) }
    }

    fun parse(sms: RawSmsData, cardIdByLast4: Map<String, Int>): Transaction? {
        val result = parse(sms.body, sms.sender) ?: return null

        val cardId = result.last4Digits?.let { cardIdByLast4[it] }
            ?: cardIdByLast4.values.firstOrNull()
            ?: return null

        return Transaction(
            id = 0,
            cardId = cardId,
            amount = result.amount,
            merchant = result.merchant,
            date = Date(sms.timestamp),
            category = result.category,
            isCredit = result.isCredit,
            source = TransactionSource.SMS,
            status = TransactionStatus.CONFIRMED,
            rawEmailId = null,
            isFlagged = false,
            flagReason = null
        )
    }

    fun parse(smsBody: String, sender: String): SmsTransactionResult? {
        val body = smsBody.trim()

        // Identify bank
        val bankName = BANK_SENDER_MAP.entries.firstOrNull { (key, _) ->
            sender.uppercase().contains(key)
        }?.value

        // Try debit
        var amount: Double? = null
        var isCredit = false

        for (pattern in DEBIT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (amount != null) { isCredit = false; break }
            }
        }

        // Try credit if not found as debit
        if (amount == null) {
            for (pattern in CREDIT_PATTERNS) {
                val match = pattern.find(body)
                if (match != null) {
                    amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                    if (amount != null) { isCredit = true; break }
                }
            }
        }

        if (amount == null || amount <= 0.0) {
            Log.d(TAG, "No amount found in SMS from $sender")
            return null
        }

        // Extract last 4
        val last4 = LAST4_PATTERN.find(body)?.groupValues?.get(1)

        // Extract merchant
        var merchant = "Unknown"
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val raw = match.groupValues[1].trim().trimEnd('.', ',', ' ')
                if (raw.length >= 3) { merchant = raw; break }
            }
        }

        Log.d(TAG, "Parsed SMS: amount=$amount, merchant=$merchant, last4=$last4, bank=$bankName, credit=$isCredit")

        return SmsTransactionResult(
            amount = amount,
            merchant = merchant,
            last4Digits = last4,
            bankName = bankName,
            isCredit = isCredit,
            category = inferCategory(merchant)
        )
    }

    private fun inferCategory(merchant: String): String {
        val m = merchant.lowercase()
        return when {
            m.contains("swiggy") || m.contains("zomato") || m.contains("food") -> "Food & Dining"
            m.contains("amazon") || m.contains("flipkart") || m.contains("myntra") -> "Shopping"
            m.contains("uber") || m.contains("ola") || m.contains("rapido") -> "Transport"
            m.contains("netflix") || m.contains("hotstar") || m.contains("spotify") -> "Entertainment"
            m.contains("hospital") || m.contains("pharmacy") || m.contains("apollo") -> "Health"
            m.contains("fuel") || m.contains("petrol") || m.contains("bpcl") || m.contains("iocl") -> "Fuel"
            m.contains("irctc") || m.contains("makemytrip") || m.contains("goibibo") -> "Travel"
            else -> "General"
        }
    }
}
