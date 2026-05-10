package com.cardpulse.app.data

import android.content.Context
import android.provider.Telephony
import com.cardpulse.app.config.AppConfig
import com.cardpulse.app.parser.RawSmsData
import com.cardpulse.app.parser.SmsTransactionParser

class SmsReader(private val context: Context) {

    fun readBankSms(): List<RawSmsData> {
        val results = mutableListOf<RawSmsData>()
        val cutoff = System.currentTimeMillis() - AppConfig.GMAIL_LOOKBACK_DAYS * 86400_000L
        val uri = Telephony.Sms.CONTENT_URI
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(cutoff.toString()),
            "${Telephony.Sms.DATE} DESC"
        ) ?: return emptyList()

        cursor.use {
            while (it.moveToNext()) {
                val sender = it.getString(0) ?: continue
                val body = it.getString(1) ?: continue
                val timestamp = it.getLong(2)
                if (SmsTransactionParser.isBankSms(sender)) {
                    results.add(RawSmsData(sender, body, timestamp))
                }
            }
        }
        return results
    }
}
