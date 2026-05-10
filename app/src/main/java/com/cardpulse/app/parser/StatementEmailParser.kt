package com.cardpulse.app.parser

import com.cardpulse.app.data.RawEmailData

data class CardStatement(
    val last4: String?,
    val totalOutstanding: Double?,
    val minimumDue: Double?,
    val dueDate: String?,
    val creditLimit: Double?,
    val availableLimit: Double?,
    val statementAmount: Double?
)

object StatementEmailParser {

    private val outstandingRegex = Regex(
        """(?:total\s+)?(?:outstanding|amount\s+due|total\s+due|balance\s+due)[^\d]*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val minDueRegex = Regex(
        """(?:minimum\s+(?:amount\s+)?due|min(?:imum)?\s+due|mad)[^\d]*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val dueDateRegex = Regex(
        """(?:payment\s+due\s+date|due\s+date|pay\s+by)[^\d]*(\d{1,2}[\s\-/]\w+[\s\-/]\d{2,4})""",
        RegexOption.IGNORE_CASE
    )
    private val creditLimitRegex = Regex(
        """(?:credit\s+limit|total\s+limit)[^\d]*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val availLimitRegex = Regex(
        """(?:available\s+(?:credit\s+)?limit|available\s+balance)[^\d]*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    fun isStatementEmail(subject: String): Boolean {
        return subject.lowercase().contains(Regex(
            "statement|outstanding|amount due|payment due|bill generated|e-statement"
        ))
    }

    fun parse(email: RawEmailData): CardStatement {
        val text = email.body
        return CardStatement(
            last4 = CardDetectionParser.extractLast4(text),
            totalOutstanding = outstandingRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            minimumDue = minDueRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            dueDate = dueDateRegex.find(text)?.groupValues?.get(1)?.trim(),
            creditLimit = creditLimitRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            availableLimit = availLimitRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            statementAmount = null
        )
    }
}
