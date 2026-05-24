package com.cardpulse.app.parser

import com.cardpulse.app.model.Milestone
import com.cardpulse.app.model.Perk
import com.cardpulse.app.model.Transaction
import kotlin.math.max

data class ProgressComputation(
    val eligibleTransactions: List<Transaction>,
    val currentAmount: Double,
    val denominator: Double,
    val progress: Float,
    val isAchieved: Boolean
)

object MilestoneProgressCalculator {
    fun computeForMilestone(
        milestone: Milestone,
        transactions: List<Transaction>,
        cycleStartMillis: Long
    ): ProgressComputation {
        val denominator = milestone.ta.toDouble().coerceAtLeast(0.0)
        val eligible = transactions.filter {
            doesTransactionQualify(
                transaction = it,
                ruleText = "${milestone.n} ${milestone.rw}",
                requiredTags = inferRequiredTags("${milestone.n} ${milestone.rw}"),
                minTransactionAmount = 0,
                cycleStartMillis = cycleStartMillis
            )
        }
        return buildProgress(eligible, denominator)
    }

    fun computeForPerk(
        perk: Perk,
        transactions: List<Transaction>,
        cycleStartMillis: Long
    ): ProgressComputation {
        val ruleText = perk.n
        val denominator = computePerkDenominator(perk)
        val eligible = transactions.filter {
            doesTransactionQualify(
                transaction = it,
                ruleText = ruleText,
                requiredTags = inferRequiredTags(ruleText),
                minTransactionAmount = perk.mn ?: 0,
                cycleStartMillis = cycleStartMillis,
                exclusions = perk.x.orEmpty()
            )
        }
        return buildProgress(eligible, denominator)
    }

    fun computePerkDenominator(perk: Perk): Double {
        val up = perk.up ?: return 0.0
        val limit = up.v.toDouble()
        if (limit <= 0.0) return 0.0
        if (up.t == "sp") return limit

        val percentRate = extractPercentRate(perk.n)
        if (percentRate != null && percentRate > 0.0) {
            return limit / percentRate
        }

        val pointsRate = extractPointsRate(perk.n)
        if (pointsRate != null && pointsRate > 0.0) {
            return limit / pointsRate
        }

        return limit
    }

    fun doesTransactionQualify(
        transaction: Transaction,
        ruleText: String,
        requiredTags: Set<String>,
        minTransactionAmount: Int,
        cycleStartMillis: Long,
        exclusions: List<String> = emptyList()
    ): Boolean {
        val kind = TransactionKindClassifier.kindOf(transaction)
        if (kind != LedgerTransactionKind.SPEND && kind != LedgerTransactionKind.REFUND) return false
        if (transaction.date < cycleStartMillis) return false
        if (kind == LedgerTransactionKind.SPEND && transaction.amount < minTransactionAmount) return false

        val tags = TransactionTagger.tagsOf(transaction)
        if (requiredTags.isNotEmpty() && tags.intersect(requiredTags).isEmpty()) return false

        val haystack = "${transaction.merchant} ${transaction.category} ${transaction.rawText} ${tags.joinToString(" ")}".lowercase()
        val hasExcludedTerm = exclusions.any { exclusion ->
            val normalized = exclusion.trim().lowercase()
            normalized.isNotBlank() && haystack.contains(normalized)
        }
        if (hasExcludedTerm) return false

        return ruleText.isNotBlank() || requiredTags.isEmpty()
    }

    fun inferRequiredTags(ruleText: String): Set<String> {
        val normalized = ruleText.lowercase()
        val tags = linkedSetOf<String>()
        listOf(
            "fuel" to Regex("fuel|petrol|diesel"),
            "dining" to Regex("dining|food|restaurant|swiggy|zomato"),
            "grocery" to Regex("grocery|supermarket"),
            "travel" to Regex("travel|flight|hotel|rail"),
            "utilities" to Regex("utility|utilities|electricity|water|gas|telecom|bill"),
            "insurance" to Regex("insurance|premium"),
            "rent" to Regex("rent"),
            "emi" to Regex("emi"),
            "wallet_load" to Regex("wallet"),
            "education" to Regex("education|school|college"),
            "online" to Regex("online|ecom|e-commerce"),
            "offline" to Regex("offline|pos")
        ).forEach { (tag, regex) ->
            if (regex.containsMatchIn(normalized)) tags += tag
        }
        return tags
    }

    private fun buildProgress(eligible: List<Transaction>, denominator: Double): ProgressComputation {
        val signedAmount = eligible.sumOf { TransactionKindClassifier.signedProgressAmount(it) }
        val currentAmount = max(0.0, signedAmount)
        val progress = if (denominator <= 0.0) 0f else (currentAmount / denominator).toFloat().coerceIn(0f, 1f)
        return ProgressComputation(
            eligibleTransactions = eligible,
            currentAmount = currentAmount,
            denominator = denominator,
            progress = progress,
            isAchieved = denominator > 0.0 && currentAmount >= denominator
        )
    }

    private fun extractPercentRate(text: String): Double? {
        val percent = Regex("""(\d+(?:\.\d+)?)\s*%""").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        return percent?.div(100.0)
    }

    private fun extractPointsRate(text: String): Double? {
        val pointsPerHundred = Regex("""(\d+(?:\.\d+)?)\s*(?:points|pts)[^\d]{0,20}(?:per|/)\s*(?:rs\.?|₹)?\s*100""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        if (pointsPerHundred != null) return pointsPerHundred / 100.0

        val multiplier = Regex("""(\d+(?:\.\d+)?)\s*x""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        return multiplier
    }
}
