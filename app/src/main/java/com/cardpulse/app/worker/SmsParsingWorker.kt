package com.cardpulse.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.data.SmsReader
import java.util.concurrent.TimeUnit

class SmsParsingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = CardRepository(applicationContext)
        val smsReader = SmsReader(applicationContext)
        val prefs = applicationContext.getSharedPreferences("cardpulse_sync", Context.MODE_PRIVATE)
        val startedAt = System.currentTimeMillis()
        val sinceMillis = prefs.getLong("last_successful_sms_sync_at", 0L)
            .takeIf { it > 0L }
            ?.let { (it - 10L * 60 * 1000).coerceAtLeast(0L) }

        return try {
            repository.getAllCards().forEach { card ->
                smsReader.parseTransactionsForCard(card, sinceMillis).forEach { transaction ->
                    repository.upsertDedupedTransaction(transaction)
                }
            }
            prefs.edit().putLong("last_successful_sms_sync_at", startedAt).apply()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

fun scheduleSmsSync(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<SmsParsingWorker>(
        repeatInterval = 6,
        repeatIntervalTimeUnit = TimeUnit.HOURS
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "sms_sync",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}
