package com.cardpulse.app.parser

import com.cardpulse.app.data.RawEmailData

data class CardStatement(
    val last4: String?,
    val totalOutstanding: Double?,
    val minimumDue: Double?,
    val dueDate: String?,
    val creditLimit: Double?,
    val availableLimit: Double?,
    val statementAmount: Double?,
    val statementPeriod: String? = null,
    val statementMonth: String? = null
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
        """(?:payment\s+due\s+date|due\s+date|pay\s+by)[^\d]*(\d{1,2}[\s\-/]\w+[\s\-/]\d{2,4}|\d{1,2}[\-/]\d{1,2}[\-/]\d{2,4})""",
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
    private val periodRegex = Regex(
        """statement\s+for\s+the\s+period\s+([a-z]{3,9}\s+\d{1,2}\s+\d{4}\s+to\s+[a-z]{3,9}\s+\d{1,2}\s+\d{4}|\d{1,2}[\-/]\d{1,2}[\-/]\d{2,4}\s+to\s+\d{1,2}[\-/]\d{1,2}[\-/]\d{2,4})""",
        RegexOption.IGNORE_CASE
    )
    private val statementMonthRegex = Regex(
        """(?:statement\s*-?\s*|monthly\s+statement\s*-?\s*)([a-z]{3,9}\s+\d{4})""",
        RegexOption.IGNORE_CASE
    )

    fun isStatementEmail(subject: String): Boolean {
        return EmailClassifier.classify(from = "", subject = subject, body = "").kind == EmailKind.STATEMENT ||
            subject.contains(Regex("credit\\s+card\\s+(?:e-?)?statement|monthly\\s+statement|statement\\s+for\\s+the\\s+period", RegexOption.IGNORE_CASE))
    }

    fun parse(email: RawEmailData): CardStatement {
        val text = "${email.subject}\n${email.body}"
        return CardStatement(
            last4 = CardDetectionParser.extractLast4(text),
            totalOutstanding = outstandingRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            minimumDue = minDueRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            dueDate = dueDateRegex.find(text)?.groupValues?.get(1)?.trim(),
            creditLimit = creditLimitRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            availableLimit = availLimitRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            statementAmount = outstandingRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            statementPeriod = periodRegex.find(text)?.groupValues?.get(1)?.trim(),
            statementMonth = statementMonthRegex.find(text)?.groupValues?.get(1)?.trim()
        )
    }
}
