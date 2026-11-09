package com.stansa.bagtracker.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.stansa.bagtracker.MainActivity
import com.stansa.bagtracker.R
import com.stansa.bagtracker.data.AppDatabase
import kotlinx.coroutines.*

class BagTrackerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                val now = System.currentTimeMillis()
                val sevenDays = now + 7 * 24 * 60 * 60 * 1000L

                val allItems = db.itemDao().getAllItemsSnapshot()
                val expiredCount     = allItems.count { it.expirationDate != null && it.expirationDate < now }
                val expiringSoonCount = allItems.count {
                    it.expirationDate != null && it.expirationDate in now..sevenDays
                }

                val views = RemoteViews(context.packageName, R.layout.bag_tracker_widget)
                views.setTextViewText(R.id.widget_text_expired, "⛔ Périmés : $expiredCount")
                views.setTextViewText(R.id.widget_text_expiring, "🟠 Bientôt : $expiringSoonCount")

                val intent = Intent(context, MainActivity::class.java)
                val pi = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_root, pi)

                val searchIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("open_search", true)
                }
                val searchPi = PendingIntent.getActivity(context, 1, searchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_btn_search, searchPi)

                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}