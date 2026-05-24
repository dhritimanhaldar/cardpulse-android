package com.cardpulse.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import com.cardpulse.app.parser.LedgerTransactionKind
import com.cardpulse.app.parser.TransactionKindClassifier
import com.cardpulse.app.parser.TransactionTagger
import com.cardpulse.app.util.cleanCardName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class SmsReader(private val context: Context) {

    private val repository = CardRepository(context)

    suspend fun parseTransactionsForCard(
        card: Card,
        sinceMillis: Long? = null
    ): List<Transaction> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("SmsReader", "READ_SMS permission not granted")
            return@withContext emptyList()
        }

        val transactions = mutableListOf<Transaction>()
        val defaultLookback = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
        val startMillis = sinceMillis ?: defaultLookback
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, "address", "body", "date"),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(startMillis.toString()),
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val address = it.getString(it.getColumnIndexOrThrow("address")) ?: continue
                val body = it.getString(it.getColumnIndexOrThrow("body")) ?: continue
                val dateMillis = it.getLong(it.getColumnIndexOrThrow("date"))
                val smsId = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))

                if (!isLikelyFromBank(address, card.bankName) && !body.contains(card.bankName, ignoreCase = true)) {
                    continue
                }

                val last4 = extractLast4Digits(body) ?: continue
                if (last4 != card.last4Digits) continue

                val transaction = parseTransactionSms(body, address, dateMillis, card.id, "sms:$smsId")
                transaction?.let { txn ->
                    val existing = repository.getTransactionByDetails(card.id, txn.amount, txn.date)
                    if (existing == null) {
                        transactions.add(txn)
                    }
                }
            }
        }

        transactions
    }

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
        return parseTransactionSms(body, sender, dateMillis, 0, null)
    }

    private fun parseTransactionSms(
        body: String,
        sender: String,
        dateMillis: Long,
        cardId: Int,
        sourceId: String?
    ): Transaction? {
        val transactionKind = TransactionKindClassifier.infer(body)
        if (transactionKind == LedgerTransactionKind.UNKNOWN) return null

        val amountPattern = """(?:Rs\.?|INR|₹)\s*([0-9,]+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE)
        val amountMatch = amountPattern.find(body)
        val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null

        val merchant = extractMerchant(body) ?: when (transactionKind) {
            LedgerTransactionKind.PAYMENT -> "Card Payment"
            LedgerTransactionKind.REFUND -> "Card Refund"
            LedgerTransactionKind.FEE -> "Card Fee"
            else -> "Unknown"
        }
        val tagging = TransactionTagger.infer(body, merchant, transactionKind)

        return Transaction(
            cardId = cardId,
            amount = amount,
            merchant = merchant,
            category = TransactionKindClassifier.categoryFor(transactionKind, categorizeTransaction(merchant, body)),
            date = dateMillis,
            source = TransactionSource.SMS,
            rawText = body,
            rawEmailId = sourceId,
            status = TransactionStatus.CONFIRMED,
            isCredit = TransactionKindClassifier.isCreditLike(transactionKind),
            isFlagged = false,
            flagReason = null,
            currency = "INR",
            isInternational = false,
            transactionKind = transactionKind.name,
            tags = TransactionTagger.serialize(tagging.tags),
            tagConfidence = tagging.confidence
        )
    }

    private fun extractLast4Digits(body: String): String? {
        val patterns = listOf(
            """(?:ending\s+(?:in|with)?\s*)(\d{4})""".toRegex(RegexOption.IGNORE_CASE),
            """(?:XX|xx|X{2,}|x{2,}|\*{2,})\s*(\d{4})""".toRegex(),
            """card\s+(?:no\.?\s*)?(?:ending\s*)?(\d{4})""".toRegex(RegexOption.IGNORE_CASE),
            """(?:card|a/c|account)[^\d]*(\d{4})""".toRegex(RegexOption.IGNORE_CASE)
        )
        return patterns.firstNotNullOfOrNull { it.find(body)?.groupValues?.get(1) }
    }

    private fun extractMerchant(body: String): String? {
        val patterns = listOf(
            """(?:at|to|for)\s+([A-Z][A-Za-z0-9\s&'._-]{2,35})""".toRegex(),
            """merchant\s*[:\-]\s*([A-Za-z0-9\s&'._-]{2,35})""".toRegex(RegexOption.IGNORE_CASE)
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(body)?.groupValues?.get(1)?.trim()?.trim('.', ',', '-')
        }
    }

    private fun categorizeTransaction(merchant: String, body: String): String {
        val text = "$merchant $body".lowercase()
        return when {
            text.contains("uber") || text.contains("ola") || text.contains("rapido") -> "Transport"
            text.contains("swiggy") || text.contains("zomato") || text.contains("restaurant") || text.contains("dining") -> "Dining"
            text.contains("amazon") || text.contains("flipkart") || text.contains("shopping") -> "Shopping"
            text.contains("irctc") || text.contains("makemytrip") || text.contains("goibibo") || text.contains("travel") -> "Travel"
            text.contains("netflix") || text.contains("prime") || text.contains("hotstar") -> "Entertainment"
            text.contains("electricity") || text.contains("water") || text.contains("gas") -> "Utilities"
            text.contains("fuel") || text.contains("petrol") || text.contains("diesel") -> "Fuel"
            text.contains("insurance") || text.contains("premium") -> "Insurance"
            else -> "Others"
        }
    }

    private fun isLikelyFromBank(sender: String, bankName: String): Boolean {
        val senderLower = sender.lowercase()
        val compactBank = bankName.lowercase().replace(Regex("[^a-z0-9]"), "")
        return senderLower.contains(compactBank) ||
                compactBank.split("bank", "card").filter { it.length >= 3 }.any { senderLower.contains(it) } ||
                senderLower.contains("card") ||
                senderLower.contains("bank")
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
