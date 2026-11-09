package com.stansa.bagtracker.worker

import android.content.Context
import androidx.work.*
import com.stansa.bagtracker.data.AppDatabase
import com.stansa.bagtracker.util.NotificationUtil
import java.util.concurrent.TimeUnit

class ExpiryNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val now = System.currentTimeMillis()
        val threeDays = now + TimeUnit.DAYS.toMillis(3)

        val cursor = db.openHelper.readableDatabase.query(
            "SELECT name, expirationDate FROM items WHERE expirationDate IS NOT NULL AND expirationDate >= ? AND expirationDate <= ?",
            arrayOf(now.toString(), threeDays.toString())
        )
        var notifId = 1000
        while (cursor.moveToNext()) {
            val name = cursor.getString(0)
            val expDate = cursor.getLong(1)
            val daysLeft = ((expDate - now) / TimeUnit.DAYS.toMillis(1)).toInt()
            NotificationUtil.sendExpiryNotification(applicationContext, name, daysLeft, notifId++)
        }
        cursor.close()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "expiry_check",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ExpiryNotificationWorker>(1, TimeUnit.DAYS).build()
            )
        }
    }
}