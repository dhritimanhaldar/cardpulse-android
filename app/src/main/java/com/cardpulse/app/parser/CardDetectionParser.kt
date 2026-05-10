package com.cardpulse.app.parser

import com.cardpulse.app.data.RawEmailData

object CardDetectionParser {

    private val bankPatterns = mapOf(
        "hdfc" to "HDFC", "axis" to "Axis", "icici" to "ICICI",
        "sbi" to "SBI", "kotak" to "Kotak", "idfc" to "IDFC First",
        "yes bank" to "Yes Bank", "indusind" to "IndusInd", "rbl" to "RBL",
        "amex" to "Amex", "citi" to "Citi", "hsbc" to "HSBC",
        "au bank" to "AU Bank", "standard chartered" to "Standard Chartered"
    )

    private val last4Patterns = listOf(
        Regex("""(?:card|a/c|account|no\.?)\s*(?:ending|ending in|x+|[*]{0,8}|[•]{0,8})?\s*(\d{4})\b""", RegexOption.IGNORE_CASE),
        Regex("""[xX*•]{4,}\s*(\d{4})\b"""),
        Regex("""(?:XX|xx)(\d{4})\b"""),
        Regex("""\b(\d{4})\s*(?:is debited|has been debited|was debited|was charged)""", RegexOption.IGNORE_CASE)
    )

    private val cardNamePatterns = listOf(
        Regex("""(?:your|hdfc|axis|icici|sbi|kotak)\s+([A-Za-z]+(?:\s+[A-Za-z]+)?)\s+(?:credit|debit|card)""", RegexOption.IGNORE_CASE),
        Regex("""(infinia|regalia|millennia|moneyback|diners|magnus|neo|flipkart|cashback|platinum|gold|select|signature|world|prime|ace|ruby|titan|elite|privilege|vistara|atlas|reserve|ultra|freedom|coral)""", RegexOption.IGNORE_CASE)
    )

    data class DetectedCard(val bankName: String, val last4: String, val cardName: String, val cardType: String = "Credit Card")

    fun detectCards(emails: List<RawEmailData>): List<DetectedCard> {
        val detected = mutableMapOf<String, DetectedCard>()
        for (email in emails) {
            val text = "${email.subject} ${email.from} ${email.body}"
            val last4 = extractLast4(text) ?: continue
            if (detected.containsKey(last4)) continue
            val bankName = extractBankName(text) ?: continue
            val cardName = extractCardName(text) ?: "$bankName Card"
            val cardType = if (text.contains(Regex("debit", RegexOption.IGNORE_CASE)) &&
                !text.contains(Regex("credit", RegexOption.IGNORE_CASE))) "Debit Card" else "Credit Card"
            detected[last4] = DetectedCard(bankName, last4, cardName, cardType)
        }
        return detected.values.toList()
    }

    private fun extractLast4(text: String): String? {
        for (pattern in last4Patterns) { val m = pattern.find(text); if (m != null) return m.groupValues[1] }
        return null
    }

    private fun extractBankName(text: String): String? {
        val lower = text.lowercase()
        for ((key, value) in bankPatterns) { if (lower.contains(key)) return value }
        return null
    }

    private fun extractCardName(text: String): String? {
        for (pattern in cardNamePatterns) {
            val m = pattern.find(text); if (m != null) return m.groupValues[1].trim().replaceFirstChar { it.uppercase() }
        }
        return null
    }
}
