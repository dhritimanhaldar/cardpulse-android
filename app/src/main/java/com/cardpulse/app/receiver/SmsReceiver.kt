package com.cardpulse.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionSource
import com.cardpulse.app.model.TransactionStatus
import com.cardpulse.app.parser.SmsTransactionParser
import com.cardpulse.app.parser.TransactionKindClassifier
import com.cardpulse.app.parser.TransactionTagger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val repository = CardRepository(context)

        messages.forEach { sms ->
            val sender = sms.originatingAddress ?: return@forEach
            val body = sms.messageBody ?: return@forEach

            Log.d("SmsReceiver", "SMS from $sender: ${body.take(80)}")

            val result = SmsTransactionParser.parse(body, sender) ?: return@forEach

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val cards = repository.getAllCardsSync()
                    val matchedCard = result.last4Digits?.let { last4 ->
                        cards.firstOrNull { it.last4Digits == last4 }
                    } ?: result.bankName?.let { bank ->
                        cards.firstOrNull { it.bankName.contains(bank, ignoreCase = true) }
                    } ?: return@launch

                    val txn = Transaction(
                        id = 0,
                        cardId = matchedCard.id,
                        amount = result.amount,
                        merchant = result.merchant,
                        category = result.category,
                        date = System.currentTimeMillis(),
                        source = TransactionSource.SMS,
                        rawText = body,
                        rawEmailId = null,
                        status = TransactionStatus.CONFIRMED,
                        isCredit = result.isCredit,
                        isFlagged = false,
                        flagReason = null,
                        currency = "INR",
                        isInternational = false,
                        transactionKind = result.transactionKind.name,
                        tags = TransactionTagger.serialize(result.tags),
                        tagConfidence = result.tagConfidence
                    )

                    repository.upsertDedupedTransaction(txn)

                    if (TransactionKindClassifier.countsTowardSpend(txn)) {
                        repository.recalculateSpendProgress(matchedCard.id)
                    }

                    Log.d("SmsReceiver", "Saved SMS txn: ${result.merchant} ₹${result.amount} → card ${matchedCard.cardName}")
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Failed to save SMS txn: ${e.message}")
                }
            }
        }
    }
}
