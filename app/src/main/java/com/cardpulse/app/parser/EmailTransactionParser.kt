package com.cardpulse.app.parser

import com.cardpulse.app.data.RawEmailData
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EmailTransactionParser {

    // Matches: Rs. 1,234.56 / INR 1234 / ₹1,234.56 / Rs 1234
    private val amountRegex = Regex(
        """(?:Rs\.?\s*|INR\s*|₹\s*)([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Matches merchant after "at" / "to" / "with" e.g. "debited at Swiggy" / "paid to Amazon"
    private val merchantRegex = Regex(
        """(?:at|to|with|@)\s+([A-Za-z0-9 &'.\-]{3,40})(?:\s+on|\s+for|\.|\n|$)""",
        RegexOption.IGNORE_CASE
    )

    // Matches last 4 digits of card
    private val last4Regex = Regex("""(?:card|a/c|account)\s*(?:no\.?|ending|x+)?\s*[xX*]{0,8}(\d{4})""",
        RegexOption.IGNORE_CASE)

    fun parse(email: RawEmailData, cardIdByLast4: Map<String, Int>): Transaction? {
        val text = "${email.subject} ${email.body}"

        val amount = amountRegex.find(text)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull() ?: return null

        if (amount <= 0) return null

        val merchant = merchantRegex.find(text)?.groupValues?.get(1)?.trim()
            ?: extractFallbackMerchant(email.subject)
            ?: "Unknown"

        val last4 = last4Regex.find(text)?.groupValues?.get(1)
        val cardId = last4?.let { cardIdByLast4[it] } ?: cardIdByLast4.values.firstOrNull() ?: return null

        val date = tryParseDate(email.dateHeader) ?: Date()

        val isCredit = text.contains(Regex("credit|refund|cashback|reversal", RegexOption.IGNORE_CASE))

        return Transaction(
            id = 0,
            cardId = cardId,
            amount = amount,
            merchant = merchant,
            date = date,
            category = suggestCategory(merchant),
            isCredit = isCredit,
            source = TransactionSource.GMAIL,
            status = TransactionStatus.CONFIRMED,
            rawEmailId = email.messageId,
            isFlagged = false,
            flagReason = null
        )
    }

    private fun extractFallbackMerchant(subject: String): String? {
        // e.g. "HDFC Bank: Transaction at Amazon" → "Amazon"
        val match = Regex(""":\s*(?:transaction\s+)?(?:at\s+)?([A-Za-z0-9 &]{3,30})""",
            RegexOption.IGNORE_CASE).find(subject)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun suggestCategory(merchant: String): String {
        val m = merchant.lowercase()
        return when {
            m.contains(Regex("swiggy|zomato|domino|pizza|restaurant|cafe|eat")) -> "Food & Dining"
            m.contains(Regex("amazon|flipkart|myntra|meesho|ajio|nykaa")) -> "Shopping"
            m.contains(Regex("uber|ola|rapido|metro|irctc|makemytrip|air")) -> "Travel"
            m.contains(Regex("netflix|spotify|prime|hotstar|zee5|jio")) -> "Entertainment"
            m.contains(Regex("apollo|medplus|pharmeasy|hospital|clinic|doctor")) -> "Healthcare"
            m.contains(Regex("electricity|gas|water|broadband|airtel|jio|bsnl|recharge")) -> "Utilities"
            m.contains(Regex("fuel|petrol|diesel|hp|ioc|bpcl")) -> "Fuel"
            else -> "Others"
        }
    }

    private fun tryParseDate(dateStr: String): Date? {
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm:ss Z"
        )
        for (fmt in formats) {
            try {
                return SimpleDateFormat(fmt, Locale.ENGLISH).parse(dateStr)
            } catch (_: Exception) {}
        }
        return null
    }
}
