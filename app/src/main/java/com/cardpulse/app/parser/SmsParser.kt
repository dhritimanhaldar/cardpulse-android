package com.cardpulse.app.parser

import com.cardpulse.app.model.Transaction
import java.util.Date
import java.util.regex.Pattern

object SmsParser {

    // Known bank SMS sender IDs
    private val BANK_SENDERS = setOf(
        "HDFCBK", "ICICIB", "SBIINB", "AXISBK", "KOTAKB",
        "INDUSB", "YESBNK", "RBLBNK", "SCBANK", "CITIBN",
        "AMEXIN", "PAYTMB", "IDFCBK", "BOIBNK", "PNBSMS",
        "CENTBK", "CANBNK", "UCOBNK", "SYNBNK", "FEDBKM"
    )

    // Regex patterns for transaction parsing
    private val DEBIT_PATTERNS = listOf(
        // HDFC: "INR 1,500.00 debited from card XX1234"
        Pattern.compile(
            """(?:INR|Rs\.?|₹)\s*([\d,]+\.?\d*)\s*(?:debited|spent|used|charged)""",
            Pattern.CASE_INSENSITIVE
        ),
        // ICICI: "Your a/c XX1234 debited for Rs.500"
        Pattern.compile(
            """(?:debited for|debit of|purchase of)\s*(?:INR|Rs\.?|₹)?\s*([\d,]+\.?\d*)""",
            Pattern.CASE_INSENSITIVE
        ),
        // Axis: "Rs.2000.00 spent on your Axis Bank Credit Card"
        Pattern.compile(
            """(?:Rs\.?|INR|₹)\s*([\d,]+\.?\d*)\s*(?:spent|debited|charged)""",
            Pattern.CASE_INSENSITIVE
        ),
        // Generic amount pattern
        Pattern.compile(
            """(?:amount|txn|transaction)\s*(?:of)?\s*(?:INR|Rs\.?|₹)?\s*([\d,]+\.?\d*)""",
            Pattern.CASE_INSENSITIVE
        )
    )

    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("""(?:at|to|merchant|for)\s+([A-Za-z0-9\s\-&'.]{3,40})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:purchase at|used at|spent at)\s+([A-Za-z0-9\s\-&'.]{3,40})""", Pattern.CASE_INSENSITIVE)
    )

    private val CARD_LAST4_PATTERN = Pattern.compile("""(?:XX|x{2}|ending|card no\.?)\s*(\d{4})""", Pattern.CASE_INSENSITIVE)

    fun isBankSms(sender: String, body: String): Boolean {
        val senderUpper = sender.uppercase()
        if (BANK_SENDERS.any { senderUpper.contains(it) }) return true
        // Fallback: check body for transaction keywords
        val bodyLower = body.lowercase()
        return bodyLower.contains("debited") &&
                (bodyLower.contains("card") || bodyLower.contains("a/c") || bodyLower.contains("account"))
    }

    fun parse(body: String, sender: String): Transaction? {
        val amount = extractAmount(body) ?: return null
        val merchant = extractMerchant(body) ?: "Unknown Merchant"
        val last4 = extractLast4(body)
        val isInternational = detectInternational(body)
        val category = categorize(merchant, body)

        return Transaction(
            cardId = 0,             // Will be matched to card by last4 in repository layer
            amount = amount,
            merchant = merchant,
            category = category,
            date = Date(),
            source = "SMS",
            rawText = body,
            isFlagged = amount >= com.cardpulse.app.config.AppConfig.FRAUD_LARGE_AMOUNT_THRESHOLD
                    || (isInternational && com.cardpulse.app.config.AppConfig.FRAUD_FOREIGN_CURRENCY_FLAG),
            flagReason = when {
                amount >= com.cardpulse.app.config.AppConfig.FRAUD_LARGE_AMOUNT_THRESHOLD -> "Large amount: ₹$amount"
                isInternational -> "International transaction"
                else -> ""
            },
            isConfirmed = amount < com.cardpulse.app.config.AppConfig.FRAUD_LARGE_AMOUNT_THRESHOLD && !isInternational,
            isInternational = isInternational,
            currency = if (isInternational) extractCurrency(body) else "INR"
        )
    }

    private fun extractAmount(body: String): Double? {
        for (pattern in DEBIT_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                return matcher.group(1)
                    ?.replace(",", "")
                    ?.toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractMerchant(body: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                return matcher.group(1)?.trim()?.take(40)
            }
        }
        return null
    }

    private fun extractLast4(body: String): String? {
        val matcher = CARD_LAST4_PATTERN.matcher(body)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun detectInternational(body: String): Boolean {
        val bodyUpper = body.uppercase()
        return bodyUpper.contains("USD") || bodyUpper.contains("EUR") ||
                bodyUpper.contains("GBP") || bodyUpper.contains("INTERNATIONAL") ||
                bodyUpper.contains("FOREIGN") || bodyUpper.contains("FOREX")
    }

    private fun extractCurrency(body: String): String {
        return when {
            body.contains("USD", true) -> "USD"
            body.contains("EUR", true) -> "EUR"
            body.contains("GBP", true) -> "GBP"
            body.contains("SGD", true) -> "SGD"
            body.contains("AED", true) -> "AED"
            else -> "FOREIGN"
        }
    }

    private fun categorize(merchant: String, body: String): String {
        val m = merchant.lowercase()
        val b = body.lowercase()
        return when {
            m.containsAny("swiggy", "zomato", "dunzo", "restaurant", "cafe", "food") -> "FOOD"
            m.containsAny("amazon", "flipkart", "myntra", "nykaa", "ajio") -> "SHOPPING"
            m.containsAny("irctc", "makemytrip", "cleartrip", "uber", "ola", "airlines") -> "TRAVEL"
            m.containsAny("petrol", "fuel", "hpcl", "iocl", "bpcl") -> "FUEL"
            m.containsAny("netflix", "hotstar", "spotify", "prime", "cinema") -> "ENTERTAINMENT"
            m.containsAny("electricity", "water", "broadband", "airtel", "jio") -> "UTILITIES"
            m.containsAny("hospital", "pharmacy", "medical", "apollo", "manipal") -> "HEALTH"
            else -> "OTHER"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it, ignoreCase = true) }
}
