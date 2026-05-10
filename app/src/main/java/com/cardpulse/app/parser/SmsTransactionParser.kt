package com.cardpulse.app.parser

import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import java.util.Date

data class RawSmsData(
    val sender: String,
    val body: String,
    val timestamp: Long
)

object SmsTransactionParser {

    // Indian bank SMS sender IDs
    private val bankSenderPatterns = mapOf(
        "hdfcbk" to "HDFC", "hdfcbank" to "HDFC",
        "axisbk" to "Axis", "axisbank" to "Axis",
        "icicibk" to "ICICI", "icicibank" to "ICICI",
        "sbicrd" to "SBI", "sbicard" to "SBI", "sbi" to "SBI",
        "kotakbk" to "Kotak", "kotak" to "Kotak",
        "idfcfb" to "IDFC First", "idfcbank" to "IDFC First",
        "yesbank" to "Yes Bank", "yesbk" to "Yes Bank",
        "indbnk" to "IndusInd", "indusind" to "IndusInd",
        "rblbk" to "RBL", "rblbank" to "RBL",
        "aubank" to "AU Bank", "aubkcc" to "AU Bank",
        "amexin" to "Amex", "scbnk" to "Standard Chartered"
    )

    private val amountRegex = Regex(
        """(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val last4Regex = Regex(
        """(?:card|a/c|ac|ending|xx|XX|\*{2,})\s*(?:no\.?\s*)?(?:xx|XX|\*+)?(\d{4})\b""",
        RegexOption.IGNORE_CASE
    )

    private val merchantRegex = Regex(
        """(?:at|to|for|with|@)\s+([A-Za-z0-9 &'.\-]{3,35})(?:\s+on|\s+via|\s+ref|\.|\n|${'$'})""",
        RegexOption.IGNORE_CASE
    )

    private val creditKeywords = Regex(
        "credited|refund|cashback|reversal|payment received|received|added",
        RegexOption.IGNORE_CASE
    )

    private val paymentKeywords = Regex(
        "payment|bill pay|due paid|minimum due|autopay|paid towards",
        RegexOption.IGNORE_CASE
    )

    fun isBankSms(sender: String): Boolean {
        val lower = sender.lowercase().replace("-", "")
        return bankSenderPatterns.keys.any { lower.contains(it) }
    }

    fun extractBankName(sender: String): String? {
        val lower = sender.lowercase().replace("-", "")
        for ((key, value) in bankSenderPatterns) {
            if (lower.contains(key)) return value
        }
        return null
    }

    fun parse(sms: RawSmsData, cardIdByLast4: Map<String, Int>): Transaction? {
        val body = sms.body
        val amount = amountRegex.find(body)
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        if (amount <= 0 || amount > 10_000_000) return null

        val last4 = last4Regex.find(body)?.groupValues?.get(1)
        val cardId = last4?.let { cardIdByLast4[it] }
            ?: cardIdByLast4.values.firstOrNull()
            ?: return null

        val isPayment = paymentKeywords.containsMatchIn(body)
        val isCredit = isPayment || creditKeywords.containsMatchIn(body)

        val merchant = merchantRegex.find(body)?.groupValues?.get(1)?.trim()
            ?: if (isPayment) "Card Payment" else "Unknown"

        return Transaction(
            id = 0,
            cardId = cardId,
            amount = amount,
            merchant = merchant.trim(),
            date = Date(sms.timestamp),
            category = if (isPayment) "Payment" else suggestCategory(merchant),
            isCredit = isCredit,
            source = TransactionSource.SMS,
            status = TransactionStatus.CONFIRMED,
            rawEmailId = null,
            isFlagged = false,
            flagReason = null
        )
    }

    private fun suggestCategory(merchant: String): String {
        val m = merchant.lowercase()
        return when {
            m.contains(Regex("swiggy|zomato|domino|pizza|restaurant|cafe|blinkit|zepto")) -> "Food & Dining"
            m.contains(Regex("amazon|flipkart|myntra|meesho|ajio|nykaa|bigbasket")) -> "Shopping"
            m.contains(Regex("uber|ola|rapido|irctc|makemytrip|goibibo|air|metro")) -> "Travel"
            m.contains(Regex("netflix|spotify|prime|hotstar|zee5|jio")) -> "Entertainment"
            m.contains(Regex("apollo|pharmeasy|hospital|clinic|medplus")) -> "Healthcare"
            m.contains(Regex("electricity|gas|water|broadband|airtel|jio|bsnl|recharge")) -> "Utilities"
            m.contains(Regex("petrol|diesel|hp|ioc|bpcl|fuel")) -> "Fuel"
            else -> "Others"
        }
    }
}
