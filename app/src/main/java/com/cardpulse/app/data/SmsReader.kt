package com.cardpulse.app.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.cardpulse.app.data.dao.CardDao
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import com.cardpulse.app.util.cleanCardName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

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
                val address = it.getString(it.getColumnIndexOrThrow("address")) ?: continue
                val body = it.getString(it.getColumnIndexOrThrow("body")) ?: continue
                val dateMillis = it.getLong(it.getColumnIndexOrThrow("date"))

                if (isTransactionSms(body, address)) {
                    val transaction = parseTransactionSms(body, address, dateMillis)
                    transaction?.let { txn ->
                        transactions.add(txn)

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
        val dao = CardPulseDatabase.getInstance(context).cardDao()
        val existingCards = dao.getAllCards()

        val exists = existingCards.any { card ->
            card.last4Digits == last4 &&
                    card.bankName.equals(bankName, ignoreCase = true)
        }

        if (!exists) {
            autoAddCardFromSms(last4, bankName)
        }
    }

    private suspend fun autoAddCardFromSms(last4: String, bankName: String) {
        val matchedBank = repository.getAllBanks().find {
            it.name.equals(bankName, ignoreCase = true)
        }

        val cleanedCardName = if (matchedBank != null) {
            cleanCardName("${matchedBank.name} Card", matchedBank.name)
        } else {
            cleanCardName("$bankName Card", bankName)
        }

        val cardColor = generateColorFromBank(matchedBank?.name ?: bankName)

        val card = Card(
            bankName = matchedBank?.name ?: bankName,
            cardName = cleanedCardName,
            last4Digits = last4,
            cardType = "Credit Card",
            cardNetwork = matchedBank?.name ?: bankName,
            creditLimit = 0.0,
            billingCycleDay = 1,
            statementDay = 1,
            dueDateOffset = 20,
            annualFee = 0.0,
            isAutoFetched = true,
            isVerified = false,
            isActive = true,
            addedOn = System.currentTimeMillis(),
            color = String.format("#%06X", 0xFFFFFF and cardColor),
            currentOutstanding = 0.0,
            minimumDue = 0.0,
            paymentDueDate = null
        )

        CardPulseDatabase.getInstance(context).cardDao().insertCard(card)
    }

    private fun generateColorFromBank(bankName: String): Int {
        val colors = listOf(
            0xFF1976D2.toInt(),
            0xFFD32F2F.toInt(),
            0xFF388E3C.toInt(),
            0xFFF57C00.toInt(),
            0xFF7B1FA2.toInt(),
            0xFF303F9F.toInt(),
            0xFF00796B.toInt(),
            0xFFC2185B.toInt()
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
        val amountPattern = """(?:Rs\.?|INR)\s*([0-9,]+\.?\d*)""".toRegex()
        val amountMatch = amountPattern.find(body)
        val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null

        val last4Pattern = """(?:XX|ending\s+|card\s+)(\d{4})""".toRegex()
        val last4 = last4Pattern.find(body)?.groupValues?.get(1) ?: "XXXX"

        val merchantPattern = """at\s+([A-Z\s]+)""".toRegex()
        val merchant = merchantPattern.find(body)?.groupValues?.get(1)?.trim() ?: "Unknown"

        return Transaction(
            cardId = 0,
            amount = amount,
            merchant = merchant,
            category = "General",
            date = dateMillis,
            source = TransactionSource.SMS,
            rawText = body,
            rawEmailId = null,
            status = TransactionStatus.CONFIRMED,
            isCredit = false,
            isFlagged = false,
            flagReason = null,
            currency = "INR",
            isInternational = false
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
