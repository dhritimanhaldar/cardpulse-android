package com.cardpulse.app.parser

import android.util.Log
import com.cardpulse.app.data.RawEmailData

enum class EmailKind {
    TRANSACTION_ALERT,
    PAYMENT_ALERT,
    STATEMENT,
    PROMOTION,
    CARD_SERVICE,
    UNRELATED,
    UNKNOWN
}

data class EmailClassification(
    val kind: EmailKind,
    val bankName: String?,
    val reason: String
)

object EmailClassifier {
    private const val TAG = "EmailClassifier"

    private val unrelatedSenderRegex = Regex(
        """cleartrip|airtel|indmoney|ind\s*money|groww|zerodha|policybazaar|newsletter|makemytrip|goibibo|irctc""",
        RegexOption.IGNORE_CASE
    )
    private val telecomRegex = Regex(
        """airtel|jio|vi\s+bill|vodafone|idea|mobile\s+bill|broadband\s+bill|postpaid""",
        RegexOption.IGNORE_CASE
    )
    private val promotionRegex = Regex(
        """increase\s+credit\s+limit|check\s+your\s+new\s+credit\s+limit|credit\s+limit\s+just\s+got\s+a\s+boost|higher\s+credit\s+limit|last\s+day\s+for\s+a\s+higher\s+credit\s+limit|limited\s+time\s+offer|complimentary\s+add-?on|gift\s+your\s+loved|offer|cashback\s+offer|sale|discount|pre-approved|personal\s+loan|emi\s+offer|newsletter|marketing""",
        RegexOption.IGNORE_CASE
    )
    private val serviceRegex = Regex(
        """terms\s+and\s+conditions|important\s+update|information\s+regarding|welcome|privacy|kyc|otp|one\s+time\s+password|password|pin|statement\s+password|ensure\s+access|downtime|maintenance|service\s+request""",
        RegexOption.IGNORE_CASE
    )
    private val statementRegex = Regex(
        """credit\s+card\s+(?:e-?)?statement|monthly\s+statement|e-?statement|statement\s+for\s+the\s+period|bill\s+generated|statement\s+-?\s*[a-z]{3,9}\s+\d{4}""",
        RegexOption.IGNORE_CASE
    )
    private val paymentRegex = Regex(
        """payment\s+alert|payment\s+received|bill\s+payment|amount\s+paid|paid\s+towards|payment\s+successful|autopay|auto\s*pay|card\s+payment""",
        RegexOption.IGNORE_CASE
    )
    private val transactionRegex = Regex(
        """transaction\s+alert|debit\s+alert|credit\s+alert|card\s+transaction\s+alert|has\s+been\s+debited|amount\s+debited|spent|purchase|used\s+at|charged""",
        RegexOption.IGNORE_CASE
    )

    fun classify(email: RawEmailData): EmailClassification {
        return classify(
            from = email.from,
            subject = email.subject,
            body = email.body
        ).also { classification ->
            Log.d(
                TAG,
                "from=${email.from.take(80)}, subject=${email.subject.take(120)}, " +
                    "bank=${classification.bankName}, kind=${classification.kind}, reason=${classification.reason}"
            )
        }
    }

    fun classify(from: String, subject: String, body: String = ""): EmailClassification {
        val normalizedFrom = normalize(from)
        val normalizedSubject = normalize(subject)
        val normalizedBody = normalize(body.take(1200))
        val headerText = "$normalizedFrom $normalizedSubject"
        val text = "$headerText $normalizedBody"
        val bankName = CardDetectionParser.extractBankName(text, from)

        if (unrelatedSenderRegex.containsMatchIn(headerText) || telecomRegex.containsMatchIn(headerText)) {
            return EmailClassification(EmailKind.UNRELATED, bankName, "external sender or telecom bill")
        }

        if (promotionRegex.containsMatchIn(headerText)) {
            return EmailClassification(EmailKind.PROMOTION, bankName, "promotional or limit-increase email")
        }

        if (serviceRegex.containsMatchIn(headerText) && !statementRegex.containsMatchIn(text)) {
            return EmailClassification(EmailKind.CARD_SERVICE, bankName, "service/information email")
        }

        if (statementRegex.containsMatchIn(text) && bankName != null) {
            return EmailClassification(EmailKind.STATEMENT, bankName, "bank statement pattern")
        }

        if (paymentRegex.containsMatchIn(text) && bankName != null) {
            return EmailClassification(EmailKind.PAYMENT_ALERT, bankName, "bank payment pattern")
        }

        if (transactionRegex.containsMatchIn(text) && bankName != null) {
            return EmailClassification(EmailKind.TRANSACTION_ALERT, bankName, "bank transaction pattern")
        }

        return if (bankName == null && transactionRegex.containsMatchIn(headerText)) {
            EmailClassification(EmailKind.UNRELATED, null, "transaction wording from non-bank sender")
        } else {
            EmailClassification(EmailKind.UNKNOWN, bankName, "no ledger pattern")
        }
    }

    private fun normalize(value: String): String {
        return value
            .lowercase()
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
