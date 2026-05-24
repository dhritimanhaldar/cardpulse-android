package com.cardpulse.app.parser

import android.util.Log
import com.cardpulse.app.data.RawEmailData
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import java.text.SimpleDateFormat
import java.util.Locale

object EmailTransactionParser {
    private const val TAG = "EmailTransactionParser"

    private val amountRegex = Regex(
        """(?:Rs\.?\s*|INR\s*|₹\s*)([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val merchantRegex = Regex(
        """(?:at|to|with|@)\s+([A-Za-z0-9 &'.\-]{3,40})(?:\s+on|\s+for|\.|\n|$)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        email: RawEmailData,
        cardIdByLast4: Map<String, Int>,
        classification: EmailClassification? = null
    ): Transaction? {
        val resolvedClassification = classification ?: EmailClassifier.classify(email)
        if (resolvedClassification.kind != EmailKind.TRANSACTION_ALERT &&
            resolvedClassification.kind != EmailKind.PAYMENT_ALERT
        ) {
            Log.d(TAG, "Ignored ${email.messageId}: kind=${resolvedClassification.kind}, reason=${resolvedClassification.reason}")
            return null
        }

        val text = "${email.subject} ${email.body}"
        val transactionKind = when (resolvedClassification.kind) {
            EmailKind.PAYMENT_ALERT -> LedgerTransactionKind.PAYMENT
            else -> TransactionKindClassifier.infer(
                text = text,
                fallbackSpend = resolvedClassification.kind == EmailKind.TRANSACTION_ALERT
            )
        }
        if (transactionKind == LedgerTransactionKind.UNKNOWN) {
            Log.d(TAG, "Ignored ${email.messageId}: transaction kind unknown")
            return null
        }

        val amount = amountRegex.find(text)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull() ?: return null

        if (amount <= 0 || amount > 10_000_000) return null

        val rawMerchant = merchantRegex.find(text)?.groupValues?.get(1)?.trim()
            ?: extractFallbackMerchant(email.subject)
            ?: defaultMerchantFor(transactionKind)

        if (transactionKind == LedgerTransactionKind.SPEND &&
            (rawMerchant.length < 3 || rawMerchant.matches(Regex("\\d+")) || rawMerchant.lowercase().contains("html"))
        ) return null

        val last4 = CardDetectionParser.extractLast4(text)
        val cardId = last4?.let { cardIdByLast4[it] } ?: cardIdByLast4.values.firstOrNull() ?: return null

        val dateMillis = tryParseDate(email.dateHeader) ?: System.currentTimeMillis()
        val finalCategory = TransactionKindClassifier.categoryFor(transactionKind, suggestCategory(rawMerchant))
        val tagging = TransactionTagger.infer(text, rawMerchant, transactionKind)

        Log.d(
            TAG,
            "Parsed ${email.messageId}: bank=${resolvedClassification.bankName}, kind=${resolvedClassification.kind}, " +
                "transactionKind=$transactionKind, amount=$amount, merchant=$rawMerchant"
        )
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
            isCredit = TransactionKindClassifier.isCreditLike(transactionKind),
            isFlagged = false,
            flagReason = "",
            currency = "INR",
            isInternational = false,
            transactionKind = transactionKind.name,
            tags = TransactionTagger.serialize(tagging.tags),
            tagConfidence = tagging.confidence
        )
    }

    private fun extractFallbackMerchant(subject: String): String? {
        val match = Regex(
            """:\s*(?:transaction\s+)?(?:at\s+)?([A-Za-z0-9 &]{3,30})""",
            RegexOption.IGNORE_CASE
        ).find(subject)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun defaultMerchantFor(kind: LedgerTransactionKind): String {
        return when (kind) {
            LedgerTransactionKind.PAYMENT -> "Card Payment"
            LedgerTransactionKind.REFUND -> "Card Refund"
            LedgerTransactionKind.FEE -> "Card Fee"
            else -> "Unknown Merchant"
        }
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
