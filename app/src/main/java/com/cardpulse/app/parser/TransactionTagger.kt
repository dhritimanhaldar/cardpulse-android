package com.cardpulse.app.parser

import com.cardpulse.app.model.Transaction

data class TaggingResult(
    val tags: Set<String>,
    val confidence: Double
)

object TransactionTagger {
    private val rules = listOf(
        "fuel" to Regex("""fuel|petrol|diesel|hpcl|bpcl|iocl|indian\s*oil|shell|reliance\s+petroleum""", RegexOption.IGNORE_CASE),
        "dining" to Regex("""swiggy|zomato|restaurant|cafe|pizza|domino|mcdonald|kfc|eat|dining|food""", RegexOption.IGNORE_CASE),
        "grocery" to Regex("""grocery|bigbasket|blinkit|zepto|dmart|big\s+bazaar|reliance\s+fresh|supermarket""", RegexOption.IGNORE_CASE),
        "travel" to Regex("""flight|airline|indigo|vistara|air\s*india|hotel|makemytrip|goibibo|cleartrip|irctc|railway|uber|ola|rapido""", RegexOption.IGNORE_CASE),
        "transit" to Regex("""metro|uber|ola|rapido|parking|toll|fastag""", RegexOption.IGNORE_CASE),
        "utilities" to Regex("""electricity|water|gas|utility|billpay|broadband|airtel|jio|bsnl|recharge|postpaid""", RegexOption.IGNORE_CASE),
        "insurance" to Regex("""insurance|premium|lic|hdfc\s+life|icici\s+pru|max\s+life|policy""", RegexOption.IGNORE_CASE),
        "rent" to Regex("""rent|nobroker|housing\.com|property""", RegexOption.IGNORE_CASE),
        "emi" to Regex("""emi|equated\s+monthly|loan\s+repayment""", RegexOption.IGNORE_CASE),
        "wallet_load" to Regex("""wallet|paytm|mobikwik|amazon\s+pay|phonepe\s+wallet|load\s+money""", RegexOption.IGNORE_CASE),
        "education" to Regex("""school|college|university|tuition|education|fees""", RegexOption.IGNORE_CASE),
        "healthcare" to Regex("""hospital|clinic|doctor|pharmacy|apollo|medplus|pharmeasy|1mg""", RegexOption.IGNORE_CASE),
        "entertainment" to Regex("""netflix|prime|hotstar|spotify|bookmyshow|movie|zee5|sony\s*liv""", RegexOption.IGNORE_CASE),
        "government" to Regex("""government|govt|municipal|challan|passport|rto""", RegexOption.IGNORE_CASE),
        "tax" to Regex("""income\s*tax|gst|tax\s+payment""", RegexOption.IGNORE_CASE),
        "online" to Regex("""online|ecom|e-commerce|internet|app|swiggy|zomato|amazon|flipkart|myntra|nykaa|blinkit|zepto""", RegexOption.IGNORE_CASE),
        "offline" to Regex("""pos|swiped|terminal|petrol|fuel|restaurant|store|mart|pump""", RegexOption.IGNORE_CASE)
    )

    fun infer(text: String, merchant: String, kind: LedgerTransactionKind): TaggingResult {
        if (kind == LedgerTransactionKind.PAYMENT || kind == LedgerTransactionKind.FEE) {
            return TaggingResult(emptySet(), 1.0)
        }

        val haystack = "$merchant $text"
        val tags = linkedSetOf<String>()
        rules.forEach { (tag, regex) ->
            if (regex.containsMatchIn(haystack)) tags += tag
        }

        if (kind == LedgerTransactionKind.REFUND && tags.isEmpty()) {
            tags += "refund"
        }

        val confidence = when {
            tags.isEmpty() -> 0.35
            tags.size == 1 -> 0.68
            else -> 0.86
        }
        return TaggingResult(tags, confidence)
    }

    fun tagsOf(transaction: Transaction): Set<String> {
        val storedTags = transaction.tags
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()
        if (storedTags.isNotEmpty()) return storedTags
        return infer(
            text = "${transaction.rawText} ${transaction.category}",
            merchant = transaction.merchant,
            kind = TransactionKindClassifier.kindOf(transaction)
        ).tags
    }

    fun serialize(tags: Set<String>): String {
        return tags.map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .joinToString(",")
    }
}
