package com.cardpulse.app.parser

import com.cardpulse.app.data.RawEmailData
import com.cardpulse.app.util.cleanCardName

object CardDetectionParser {

    private val bankPatterns = mapOf(
        // Order matters — more specific first
        "axis bank" to "Axis",
        "axisbank" to "Axis",
        "axis" to "Axis",
        "au bank" to "AU Bank",
        "au small finance" to "AU Bank",
        "aubank" to "AU Bank",
        "hdfc bank" to "HDFC",
        "hdfcbank" to "HDFC",
        "hdfc" to "HDFC",
        "icici bank" to "ICICI",
        "icicibankltd" to "ICICI",
        "icici" to "ICICI",
        "state bank" to "SBI",
        "sbicard" to "SBI",
        "sbi card" to "SBI",
        "sbicards" to "SBI",
        "sbi" to "SBI",
        "kotak mahindra" to "Kotak",
        "kotak" to "Kotak",
        "idfc first" to "IDFC First",
        "idfcfirst" to "IDFC First",
        "idfc" to "IDFC First",
        "yes bank" to "Yes Bank",
        "yesbank" to "Yes Bank",
        "indusind" to "IndusInd",
        "rbl bank" to "RBL",
        "rblbank" to "RBL",
        "rbl" to "RBL",
        "american express" to "Amex",
        "amex" to "Amex",
        "citi bank" to "Citi",
        "citibank" to "Citi",
        "hsbc" to "HSBC",
        "standard chartered" to "Standard Chartered",
        "sc bank" to "Standard Chartered",
        "bank of baroda" to "Bank of Baroda",
        "bob financial" to "Bank of Baroda",
        "federal bank" to "Federal Bank",
        "federalbank" to "Federal Bank",
        "onecard" to "OneCard",
        "one card" to "OneCard",
        "paytm sbi" to "SBI",
        "au bank credit card" to "AU Bank"
    )

    // Broad patterns for last 4 digits — Indian bank email formats
    private val last4Patterns = listOf(
        // "ending 1234" / "ending with 1234"
        Regex("""ending\s+(?:with\s+)?(?:x+|X+|\*+|•+)?(\d{4})\b""", RegexOption.IGNORE_CASE),
        // "card xx1234" / "card XXXX1234" / "card ****1234"
        Regex("""card\s+(?:no\.?\s*)?(?:x{2,}|X{2,}|\*{2,}|•{2,})(\d{4})\b""", RegexOption.IGNORE_CASE),
        // "a/c xx1234" / "account xx1234"
        Regex("""(?:a/c|account|acct)\s+(?:no\.?\s*)?(?:x{2,}|X{2,}|\*{2,}|•{2,})?(\d{4})\b""", RegexOption.IGNORE_CASE),
        // "XX1234" or "xx1234" directly
        Regex("""[xX]{2,}(\d{4})\b"""),
        // "****1234"
        Regex("""[*•]{2,}(\d{4})\b"""),
        // "1234 is debited" / "1234 was debited"
        Regex("""\b(\d{4})\s+(?:is|has been|was)\s+debited""", RegexOption.IGNORE_CASE),
        // SBI format: "Card 1234"
        Regex("""[Cc]ard\s+(\d{4})\b"""),
        // Axis format: "...1234)"  — digit4 in parens at end
        Regex("""(?:card|a/c)\s+[^)]*?(\d{4})\)""", RegexOption.IGNORE_CASE),
    )

    private val cardNamePatterns = listOf(
        Regex("""(infinia|regalia|millennia|moneyback|diners|magnus|neo|flipkart|cashback|platinum|gold|select|signature|world|prime|ace|ruby|titan|elite|privilege|vistara|atlas|reserve|ultra|freedom|coral|iocl|indianoil|easydiner|rewards|cashback|shoprite|simplysave|simplycash|doctor|business|corporate)""", RegexOption.IGNORE_CASE),
        Regex("""(?:your|an?)\s+([A-Za-z]+(?:\s+[A-Za-z]+)?)\s+(?:credit|debit)\s+card""", RegexOption.IGNORE_CASE)
    )

    data class DetectedCard(
        val bankName: String,
        val last4: String,
        val cardName: String,
        val cardType: String = "Credit Card"
    )

    fun detectCards(emails: List<RawEmailData>): List<DetectedCard> {
        val detected = mutableMapOf<String, DetectedCard>()
        for (email in emails) {
            // Use subject + from + first 500 chars of body (avoid HTML noise)
            val bodySnippet = email.body.take(500)
            val text = "${email.subject} ${email.from} $bodySnippet"
            val last4 = extractLast4(text) ?: continue
            if (detected.containsKey(last4)) continue
            val bankName = extractBankName(text, email.from) ?: continue
            val cardName = cleanCardName(extractCardName(text) ?: "$bankName Card", bankName)
            val cardType = when {
                text.contains(Regex("""\bdebit\b""", RegexOption.IGNORE_CASE)) &&
                !text.contains(Regex("""\bcredit\b""", RegexOption.IGNORE_CASE)) -> "Debit Card"
                else -> "Credit Card"
            }
            detected[last4] = DetectedCard(bankName, last4, cardName, cardType)
        }
        return detected.values.toList()
    }

    fun extractLast4(text: String): String? {
        for (pattern in last4Patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val digits = match.groupValues[1]
                if (digits.length == 4) return digits
            }
        }
        return null
    }

    fun extractBankName(text: String, from: String = ""): String? {
        // Check sender domain first — most reliable signal
        val domainBankMap = mapOf(
            "axisbank" to "Axis",
            "axis" to "Axis",
            "aubank" to "AU Bank",
            "hdfcbank" to "HDFC",
            "hdfc" to "HDFC",
            "icicibank" to "ICICI",
            "icici" to "ICICI",
            "sbicard" to "SBI",
            "sbi" to "SBI",
            "kotak" to "Kotak",
            "idfcfirstbank" to "IDFC First",
            "idfc" to "IDFC First",
            "yesbank" to "Yes Bank",
            "indusind" to "IndusInd",
            "rblbank" to "RBL",
            "amex" to "Amex",
            "americanexpress" to "Amex",
            "sc" to "Standard Chartered",
            "hsbc" to "HSBC",
            "federalbank" to "Federal Bank",
            "onecard" to "OneCard",
            "bobfinancial" to "Bank of Baroda",
            "au bank credit card" to "AU Bank",
            "au credit card" to "AU Bank",
            "sbi card transaction" to "SBI",
            "paytm sbi" to "SBI",
            "idfc first bank" to "IDFC First"
        )
        val fromLower = from.lowercase()
        for ((key, value) in domainBankMap) {
            if (fromLower.contains(key)) return value
        }
        // Fall back to text body search
        val lower = text.lowercase()
        for ((key, value) in bankPatterns) {
            if (lower.contains(key)) return value
        }
        return null
    }

    private fun extractCardName(text: String): String? {
        for (pattern in cardNamePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.length >= 3) return name.replaceFirstChar { it.uppercase() }
            }
        }
        return null
    }
}
