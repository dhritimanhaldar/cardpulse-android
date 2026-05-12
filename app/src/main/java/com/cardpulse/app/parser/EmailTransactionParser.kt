package com.cardpulse.app.parser

import com.cardpulse.app.data.RawEmailData
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import java.text.SimpleDateFormat
import java.util.Locale

object EmailTransactionParser {

    private val amountRegex = Regex(
        """(?:Rs\.?\s*|INR\s*|₹\s*)([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val merchantRegex = Regex(
        """(?:at|to|with|@)\s+([A-Za-z0-9 &'.\-]{3,40})(?:\s+on|\s+for|\.|\n|$)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(email: RawEmailData, cardIdByLast4: Map<String, Int>): Transaction? {
        val isDefinitelyTransaction = email.subject.lowercase().let {
            it.contains("transaction alert") || it.contains("debit alert") ||
                    it.contains("credit alert") || it.contains("payment alert") ||
                    it.contains("amount debited") || it.contains("has been debited")
        }

        if (!isDefinitelyTransaction) {
            val junkSubjects = listOf(
                "view this message in html", "html version", "unsubscribe",
                "confirm your", "verify your", "welcome to", "ensure access",
                "mother's day", "summer travel", "chapter", "credit limit", "imposters",
                "easy emi", "gift card", "forex card", "personal loan", "higher education",
                "happy", "meet the", "zero markup", "delivering strong", "think before",
                "otp for", "one time password", "downtime notification"
            )
            if (junkSubjects.any { email.subject.lowercase().contains(it) }) return null
            if (email.subject.lowercase().contains("view this message")) return null
        }

        val text = "${email.subject} ${email.body}"

        val amount = amountRegex.find(text)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull() ?: return null

        if (amount <= 0 || amount > 10_000_000) return null

        val rawMerchant = merchantRegex.find(text)?.groupValues?.get(1)?.trim()
            ?: extractFallbackMerchant(email.subject)
            ?: return null

        if (rawMerchant.length < 3 || rawMerchant.matches(Regex("\\d+")) || rawMerchant.lowercase().contains("html")) return null

        val last4 = CardDetectionParser.extractLast4(text)
        val cardId = last4?.let { cardIdByLast4[it] } ?: cardIdByLast4.values.firstOrNull() ?: return null

        val dateMillis = tryParseDate(email.dateHeader) ?: System.currentTimeMillis()
        val isCredit = text.contains(Regex("credit|refund|cashback|reversal|credited", RegexOption.IGNORE_CASE))

        val isPayment = text.contains(
            Regex(
                "payment received|bill payment|amount paid|payment of|paid towards|payment credited|due paid|minimum due|outstanding paid|autopay|payment successful",
                RegexOption.IGNORE_CASE
            )
        )
        val finalIsCredit = isPayment || isCredit
        val finalCategory = when {
            isPayment -> "Payment"
            else -> suggestCategory(rawMerchant)
        }

        return Transaction(
            id = 0,
            cardId = cardId,
            amount = amount,
            merchant = rawMerchant,
            category = finalCategory,
            date = dateMillis,
            source = TransactionSource.GMAIL,
            rawText = "${email.subject}\n${email.body}",
            rawEmailId = email.messageId,
            status = TransactionStatus.CONFIRMED,
            isCredit = finalIsCredit,
            isFlagged = false,
            flagReason = "",
            currency = "INR",
            isInternational = false
        )
    }

    private fun extractFallbackMerchant(subject: String): String? {
        val match = Regex(
            """:\s*(?:transaction\s+)?(?:at\s+)?([A-Za-z0-9 &]{3,30})""",
            RegexOption.IGNORE_CASE
        ).find(subject)
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

    private fun tryParseDate(dateStr: String): Long? {
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm:ss Z"
        )
        for (fmt in formats) {
            try {
                return SimpleDateFormat(fmt, Locale.ENGLISH).parse(dateStr)?.time
            } catch (_: Exception) {}
        }
        return null
    }
}