package com.stansa.bagtracker.ui.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Provider utilitaire permettant de forcer le rafraîchissement
 * du widget depuis n'importe quel endroit de l'app.
 */
object BagTrackerWidgetProvider {

    /**
     * Déclenche une mise à jour manuelle de tous les widgets BagTracker actifs.
     * À appeler après chaque insertion/modification/suppression d'item.
     */
    fun refresh(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, BagTrackerWidget::class.java)
        )
        if (widgetIds.isEmpty()) return
        widgetIds.forEach { id ->
            BagTrackerWidget.updateWidget(context, appWidgetManager, id)
        }
    }
}