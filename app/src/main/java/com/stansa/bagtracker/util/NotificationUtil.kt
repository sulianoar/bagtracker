package com.stansa.bagtracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.stansa.bagtracker.MainActivity
import com.stansa.bagtracker.R

object NotificationUtil {

    const val CHANNEL_EXPIRY = "channel_expiry"
    const val CHANNEL_REMINDER = "channel_reminder"
    const val CHANNEL_GENERAL = "channel_general"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            NotificationChannel(CHANNEL_EXPIRY, "Péremptions", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Alertes péremption" },
            NotificationChannel(CHANNEL_REMINDER, "Rappels", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Rappels personnalisés" },
            NotificationChannel(CHANNEL_GENERAL, "Général", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Infos générales" }
        ).forEach { manager.createNotificationChannel(it) }
    }

    fun sendExpiryNotification(context: Context, itemName: String, daysLeft: Int, notifId: Int) {
        val message = when (daysLeft) {
            0    -> "⛔ $itemName périme aujourd'hui !"
            1    -> "🔴 $itemName périme demain"
            else -> "🟠 $itemName périme dans $daysLeft jours"
        }
        send(context, CHANNEL_EXPIRY, "Péremption imminente", message, notifId)
    }

    fun sendReminderNotification(context: Context, itemName: String, label: String, notifId: Int) {
        send(context, CHANNEL_REMINDER, "Rappel", "🔔 $label — $itemName", notifId)
    }

    private fun send(context: Context, channel: String, title: String, message: String, notifId: Int) {
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(notifId, notif)
    }
}