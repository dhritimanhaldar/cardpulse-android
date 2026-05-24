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
    val category: String = "General",
    val transactionKind: LedgerTransactionKind = LedgerTransactionKind.UNKNOWN,
    val tags: Set<String> = emptySet(),
    val tagConfidence: Double = 0.0
)

object SmsTransactionParser {

    private val TAG = "SmsTransactionParser"

    private val DEBIT_PATTERNS = listOf(
        Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)[\s\S]{0,60}(?:debited|spent|used at|purchase)""", RegexOption.IGNORE_CASE),
        Regex("""(?:debited|spent|purchase)\s+(?:INR|Rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)\s+(?:debited|spent)""", RegexOption.IGNORE_CASE)
    )

    private val CREDIT_PATTERNS = listOf(
        Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)[\s\S]{0,80}(?:credited|refund|cashback|payment received|payment successful|paid towards)""", RegexOption.IGNORE_CASE),
        Regex("""(?:credited|refund|cashback|payment\s+(?:received|successful|of)|paid\s+towards)\s+(?:with\s+)?(?:INR|Rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    )

    private val LAST4_PATTERN = Regex("""(?:card|a/c|acct|account)[\s\S]{0,10}?(\d{4})\b""", RegexOption.IGNORE_CASE)

    private val MERCHANT_PATTERNS = listOf(
        Regex("""(?:at|to|towards|for)\s+([A-Za-z0-9 &'./-]{3,40})""", RegexOption.IGNORE_CASE),
        Regex("""(?:purchase at|txn at|used at)\s+([A-Za-z0-9 &'./-]{3,40})""", RegexOption.IGNORE_CASE)
    )

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
            category = result.category,
            date = sms.timestamp,
            source = TransactionSource.SMS,
            rawText = sms.body,
            rawEmailId = null,
            status = TransactionStatus.CONFIRMED,
            isCredit = result.isCredit,
            isFlagged = false,
            flagReason = null,
            currency = "INR",
            isInternational = false,
            transactionKind = result.transactionKind.name,
            tags = TransactionTagger.serialize(result.tags),
            tagConfidence = result.tagConfidence
        )
    }

    fun parse(smsBody: String, sender: String): SmsTransactionResult? {
        val body = smsBody.trim()
        val transactionKind = TransactionKindClassifier.infer(body)
        if (transactionKind == LedgerTransactionKind.UNKNOWN) {
            Log.d(TAG, "No ledger kind found in SMS from $sender")
            return null
        }

        val bankName = BANK_SENDER_MAP.entries.firstOrNull { (key, _) ->
            sender.uppercase().contains(key)
        }?.value

        var amount: Double? = null
        var isCredit = TransactionKindClassifier.isCreditLike(transactionKind)

        for (pattern in DEBIT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (amount != null) { isCredit = TransactionKindClassifier.isCreditLike(transactionKind); break }
            }
        }

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

        val last4 = LAST4_PATTERN.find(body)?.groupValues?.get(1)

        var merchant = when (transactionKind) {
            LedgerTransactionKind.PAYMENT -> "Card Payment"
            LedgerTransactionKind.REFUND -> "Card Refund"
            LedgerTransactionKind.FEE -> "Card Fee"
            else -> "Unknown"
        }
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val raw = match.groupValues[1].trim().trimEnd('.', ',', ' ')
                if (raw.length >= 3) { merchant = raw; break }
            }
        }

        val tagging = TransactionTagger.infer(body, merchant, transactionKind)

        Log.d(TAG, "Parsed SMS: amount=$amount, merchant=$merchant, last4=$last4, bank=$bankName, kind=$transactionKind")

        return SmsTransactionResult(
            amount = amount,
            merchant = merchant,
            last4Digits = last4,
            bankName = bankName,
            isCredit = isCredit,
            category = TransactionKindClassifier.categoryFor(transactionKind, inferCategory(merchant)),
            transactionKind = transactionKind,
            tags = tagging.tags,
            tagConfidence = tagging.confidence
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
