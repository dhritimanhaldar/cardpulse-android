package com.cardpulse.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.cardpulse.app.parser.SmsParser
import com.cardpulse.app.data.CardPulseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullText = messages.joinToString("") { it.messageBody }
        val sender = messages.firstOrNull()?.originatingAddress ?: ""

        // Only process bank SMS
        if (!SmsParser.isBankSms(sender, fullText)) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = CardPulseDatabase.getInstance(context)
                val parsed = SmsParser.parse(fullText, sender)
                if (parsed != null) {
                    // Check for duplicate before saving
                    val windowMs = com.cardpulse.app.config.AppConfig
                        .TRANSACTION_DUPLICATE_WINDOW_MINUTES * 60 * 1000
                    val existing = db.transactionDao().findDuplicate(
                        cardId = parsed.cardId,
                        amount = parsed.amount,
                        date = parsed.date.time,
                        windowMs = windowMs
                    )
                    if (existing == null) {
                        db.transactionDao().insertTransaction(parsed)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
