package com.stansa.bagtracker.worker

import android.content.Context
import androidx.work.*
import com.stansa.bagtracker.data.AppDatabase
import com.stansa.bagtracker.util.NotificationUtil
import java.util.concurrent.TimeUnit

class ReminderNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val now = System.currentTimeMillis()
        val due = db.reminderDao().getDueReminders(now)
        var notifId = 2000
        due.forEach { reminder ->
            val item = db.itemDao().getItemById(reminder.itemId)
            NotificationUtil.sendReminderNotification(
                applicationContext,
                item?.name ?: "Objet inconnu",
                reminder.label,
                notifId++
            )
            val next = now + TimeUnit.DAYS.toMillis(reminder.intervalDays.toLong())
            db.reminderDao().markTriggered(reminder.id, now, next)
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "reminder_check",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderNotificationWorker>(6, TimeUnit.HOURS).build()
            )
        }
    }
}