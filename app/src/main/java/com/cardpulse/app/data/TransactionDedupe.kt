package com.cardpulse.app.data

import com.cardpulse.app.model.Transaction
import com.cardpulse.app.parser.TransactionKindClassifier
import kotlin.math.abs

object TransactionDedupe {
    const val TIME_TOLERANCE_MILLIS: Long = 24L * 60 * 60 * 1000

    fun fingerprint(cardLast4: String, transaction: Transaction): String {
        return listOf(
            cardLast4,
            "%.2f".format(transaction.amount),
            normalizeMerchant(transaction.merchant).take(18),
            TransactionKindClassifier.kindOf(transaction).name,
            transaction.date / TIME_TOLERANCE_MILLIS
        ).joinToString("|")
    }

    fun isSameLogicalTransaction(left: Transaction, right: Transaction): Boolean {
        if (left.cardId != right.cardId) return false
        if (abs(left.amount - right.amount) > 0.01) return false
        if (TransactionKindClassifier.kindOf(left) != TransactionKindClassifier.kindOf(right)) return false
        if (abs(left.date - right.date) > TIME_TOLERANCE_MILLIS) return false

        val leftMerchant = normalizeMerchant(left.merchant)
        val rightMerchant = normalizeMerchant(right.merchant)
        if (leftMerchant.isBlank() || rightMerchant.isBlank()) return true
        return leftMerchant.contains(rightMerchant) || rightMerchant.contains(leftMerchant) ||
            leftMerchant.take(8) == rightMerchant.take(8)
    }

    fun richerOf(existing: Transaction, incoming: Transaction): Transaction {
        val incomingScore = metadataScore(incoming)
        val existingScore = metadataScore(existing)
        return if (incomingScore > existingScore) {
            incoming.copy(
                id = existing.id,
                rawEmailId = existing.rawEmailId ?: incoming.rawEmailId,
                sourceFingerprint = existing.sourceFingerprint ?: incoming.sourceFingerprint
            )
        } else {
            existing
        }
    }

    private fun metadataScore(transaction: Transaction): Int {
        var score = 0
        if (transaction.source.name == "SMS") score += 1
        if (transaction.merchant.isNotBlank() && !transaction.merchant.equals("Unknown", true)) score += 3
        if (transaction.tags.isNotBlank()) score += 2
        if (transaction.rawText.length > 40) score += 1
        if (transaction.rawEmailId != null) score += 1
        return score
    }

    private fun normalizeMerchant(value: String): String {
        return value.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }
}
