package com.cardpulse.app.parser

import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionStatus

enum class LedgerTransactionKind {
    SPEND,
    PAYMENT,
    REFUND,
    FEE,
    UNKNOWN
}

object TransactionKindClassifier {
    private val paymentRegex = Regex(
        """payment\s+(?:received|alert|credited|successful|of|towards)|bill\s+payment|amount\s+paid|paid\s+towards|autopay|auto\s*pay|outstanding\s+paid|minimum\s+due|total\s+amount\s+due\s+paid|card\s+payment|payment\s+has\s+been\s+received|thank\s+you\s+for\s+your\s+payment""",
        RegexOption.IGNORE_CASE
    )
    private val refundRegex = Regex(
        """refund|reversal|reversed|chargeback|cashback|credited\s+back|amount\s+credited|transaction\s+reversed""",
        RegexOption.IGNORE_CASE
    )
    private val feeRegex = Regex(
        """annual\s+fee|joining\s+fee|late\s+fee|finance\s+charge|interest\s+charge|forex\s+markup|fee\s+charged|gst\s+on\s+fee|overlimit\s+fee""",
        RegexOption.IGNORE_CASE
    )
    private val spendRegex = Regex(
        """debited|spent|purchase|transaction\s+(?:of|alert)|used\s+at|charged|paid\s+at|card\s+transaction|debit\s+alert|txn\s+done|swiped""",
        RegexOption.IGNORE_CASE
    )

    fun infer(text: String, fallbackSpend: Boolean = false): LedgerTransactionKind {
        return when {
            paymentRegex.containsMatchIn(text) -> LedgerTransactionKind.PAYMENT
            refundRegex.containsMatchIn(text) -> LedgerTransactionKind.REFUND
            feeRegex.containsMatchIn(text) -> LedgerTransactionKind.FEE
            spendRegex.containsMatchIn(text) -> LedgerTransactionKind.SPEND
            fallbackSpend -> LedgerTransactionKind.SPEND
            else -> LedgerTransactionKind.UNKNOWN
        }
    }

    fun categoryFor(kind: LedgerTransactionKind, fallbackCategory: String): String {
        return when (kind) {
            LedgerTransactionKind.PAYMENT -> "Payment"
            LedgerTransactionKind.REFUND -> "Refund"
            LedgerTransactionKind.FEE -> "Fee"
            LedgerTransactionKind.SPEND -> fallbackCategory
            LedgerTransactionKind.UNKNOWN -> fallbackCategory
        }
    }

    fun isCreditLike(kind: LedgerTransactionKind): Boolean {
        return kind == LedgerTransactionKind.PAYMENT || kind == LedgerTransactionKind.REFUND
    }

    fun kindOf(transaction: Transaction): LedgerTransactionKind {
        val stored = runCatching { LedgerTransactionKind.valueOf(transaction.transactionKind) }.getOrNull()
        if (stored != null && stored != LedgerTransactionKind.UNKNOWN) return stored
        return when {
            transaction.category.equals("Payment", ignoreCase = true) -> LedgerTransactionKind.PAYMENT
            transaction.category.equals("Refund", ignoreCase = true) -> LedgerTransactionKind.REFUND
            transaction.category.equals("Fee", ignoreCase = true) -> LedgerTransactionKind.FEE
            !transaction.isCredit -> LedgerTransactionKind.SPEND
            else -> LedgerTransactionKind.UNKNOWN
        }
    }

    fun countsTowardSpend(transaction: Transaction): Boolean {
        val excludedCategory = transaction.category.equals("Payment", ignoreCase = true) ||
            transaction.category.equals("Refund", ignoreCase = true) ||
            transaction.category.equals("Fee", ignoreCase = true)

        return kindOf(transaction) == LedgerTransactionKind.SPEND &&
            !transaction.isCredit &&
            transaction.status == TransactionStatus.CONFIRMED &&
            !excludedCategory
    }

    fun signedProgressAmount(transaction: Transaction): Double {
        return when (kindOf(transaction)) {
            LedgerTransactionKind.SPEND -> transaction.amount
            LedgerTransactionKind.REFUND -> -transaction.amount
            else -> 0.0
        }
    }
}
