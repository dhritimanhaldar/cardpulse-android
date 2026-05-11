package com.cardpulse.app.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.cardpulse.app.data.CardPulseDatabase
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SmsReader(private val context: Context) {

    private val repository = CardRepository(context)

    suspend fun readTransactionSms(): List<Transaction> = withContext(Dispatchers.IO) {
        val transactions = mutableListOf<Transaction>()
        val uri: Uri = Telephony.Sms.CONTENT_URI
        val cursor: Cursor? = context.contentResolver.query(
            uri,
            arrayOf("address", "body", "date"),
            null,
            null,
            "date DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val address = it.getString(it.getColumnIndexOrThrow("address"))
                val body = it.getString(it.getColumnIndexOrThrow("body"))
                val dateMillis = it.getLong(it.getColumnIndexOrThrow("date"))

                if (isTransactionSms(body, address)) {
                    val transaction = parseTransactionSms(body, address, dateMillis)
                    transaction?.let { txn ->
                        transactions.add(txn)

                        // Auto-add card if not exists
                        val last4 = extractLast4Digits(body)
                        val bankName = mapSenderToBank(address)
                        if (last4 != null && bankName != null) {
                            autoAddCardIfNotExists(last4, bankName)
                        }
                    }
                }
            }
        }

        transactions
    }

    private suspend fun autoAddCardIfNotExists(last4: String, bankName: String) {
        val dao = CardPulseDatabase.getDatabase(context).cardDao()
        val existingCards = dao.getAllCards()

        // Check if card already exists
        val exists = existingCards.any { card ->
            card.cardNumber.endsWith(last4) &&
                    card.bankName.equals(bankName, ignoreCase = true)
        }

        if (!exists) {
            autoAddCardFromSms(last4, bankName)
        }
    }

    // Updated auto-add method with clean naming
    private suspend fun autoAddCardFromSms(last4: String, bankName: String) {
        // Try to match to catalog first
        val matchedBank = repository.getAllBanks().find {
            it.name.equals(bankName, ignoreCase = true)
        }

        // Create clean card name without duplication
        val cleanCardName = if (matchedBank != null) {
            "${matchedBank.name} Card •${last4}"  // Simple, clean name
        } else {
            "$bankName Card •${last4}"
        }

        // Generate color based on bank
        val cardColor = generateColorFromBank(matchedBank?.name ?: bankName)

        val card = Card(
            cardNumber = "XXXX XXXX XXXX $last4",  // Masked
            cardHolderName = "Card Holder",
            bankName = matchedBank?.name ?: bankName,
            cardNickname = cleanCardName,
            expiryMonth = "",
            expiryYear = "",
            color = cardColor,
            catalogId = null,
            isAutoFetched = true,   // Mark as auto-fetched
            isVerified = false       // Needs verification
        )

        CardPulseDatabase.getDatabase(context).cardDao().insertCard(card)
    }

    private fun generateColorFromBank(bankName: String): Int {
        // Generate consistent color based on bank name hash
        val colors = listOf(
            0xFF1976D2.toInt(), // Blue
            0xFFD32F2F.toInt(), // Red
            0xFF388E3C.toInt(), // Green
            0xFFF57C00.toInt(), // Orange
            0xFF7B1FA2.toInt(), // Purple
            0xFF303F9F.toInt(), // Indigo
            0xFF00796B.toInt(), // Teal
            0xFFC2185B.toInt()  // Pink
        )

        val hash = bankName.hashCode()
        return colors[Math.abs(hash) % colors.size]
    }

    private fun isTransactionSms(body: String, sender: String): Boolean {
        val transactionKeywords = listOf(
            "debited", "spent", "purchase", "payment", "transaction",
            "withdrawn", "charged", "paid", "Rs.", "INR"
        )

        val bankSenders = listOf(
            "HDFCBK", "ICICIB", "SBICRD", "AXISBK", "KOTAKB",
            "SCBANK", "CITIBK", "HSBCIN", "YESBNK", "INDUSB"
        )

        return transactionKeywords.any { body.contains(it, ignoreCase = true) } &&
                bankSenders.any { sender.contains(it, ignoreCase = true) }
    }

    private fun parseTransactionSms(body: String, sender: String, dateMillis: Long): Transaction? {
        // Extract amount
        val amountPattern = """(?:Rs\.?|INR)\s*([0-9,]+\.?\d*)""".toRegex()
        val amountMatch = amountPattern.find(body)
        val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null

        // Extract last 4 digits
        val last4Pattern = """(?:XX|ending\s+|card\s+)(\d{4})""".toRegex()
        val last4 = last4Pattern.find(body)?.groupValues?.get(1) ?: "XXXX"

        // Extract merchant (simplified)
        val merchantPattern = """at\s+([A-Z\s]+)""".toRegex()
        val merchant = merchantPattern.find(body)?.groupValues?.get(1)?.trim() ?: "Unknown"

        // Map bank
        val bankName = mapSenderToBank(sender) ?: "Unknown Bank"

        return Transaction(
            amount = amount,
            merchant = merchant,
            date = Date(dateMillis),
            cardNumber = "XXXX$last4",
            description = body,
            category = "General",
            cardId = 0L  // Will be matched later
        )
    }

    private fun extractLast4Digits(body: String): String? {
        val last4Pattern = """(?:XX|ending\s+|card\s+)(\d{4})""".toRegex()
        return last4Pattern.find(body)?.groupValues?.get(1)
    }

    private fun mapSenderToBank(sender: String): String? {
        return when {
            sender.contains("HDFCBK", ignoreCase = true) -> "HDFC Bank"
            sender.contains("ICICIB", ignoreCase = true) -> "ICICI Bank"
            sender.contains("SBICRD", ignoreCase = true) -> "SBI Card"
            sender.contains("AXISBK", ignoreCase = true) -> "Axis Bank"
            sender.contains("KOTAKB", ignoreCase = true) -> "Kotak Bank"
            sender.contains("SCBANK", ignoreCase = true) -> "Standard Chartered"
            sender.contains("CITIBK", ignoreCase = true) -> "Citi Bank"
            sender.contains("HSBCIN", ignoreCase = true) -> "HSBC"
            sender.contains("YESBNK", ignoreCase = true) -> "Yes Bank"
            sender.contains("INDUSB", ignoreCase = true) -> "IndusInd Bank"
            else -> null
        }
    }
}