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

        return try {
            repository.getAllCards().forEach { card ->
                smsReader.parseTransactionsForCard(card).forEach { transaction ->
                    val dedupKey = "${transaction.date}_${transaction.amount}_${transaction.merchant}"
                    if (repository.getTransactionByEmailId(dedupKey) == null &&
                        repository.getTransactionByDetails(transaction.cardId, transaction.amount, transaction.date) == null
                    ) {
                        repository.insertTransaction(transaction.copy(rawEmailId = dedupKey))
                    }
                }
            }
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
