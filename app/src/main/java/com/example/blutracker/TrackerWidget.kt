package com.example.blutracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.blutracker.R

class TrackerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == "TOGGLE_TRACKING") {
            val prefs = context.getSharedPreferences("BluTrackerPrefs", Context.MODE_PRIVATE)
            val currentState = prefs.getBoolean("isTrackingEnabled", true)

            prefs.edit().putBoolean("isTrackingEnabled", !currentState).apply()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TrackerWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val prefs = context.getSharedPreferences("BluTrackerPrefs", Context.MODE_PRIVATE)
    val isTracking = prefs.getBoolean("isTrackingEnabled", true)

    val views = RemoteViews(context.packageName, R.layout.tracker_widget)

    if (isTracking) {
        views.setTextViewText(R.id.widget_text, "TRACKING ON")
        views.setTextColor(R.id.widget_text, Color.parseColor("#1E88E5"))
    } else {
        views.setTextViewText(R.id.widget_text, "TRACKING OFF")
        views.setTextColor(R.id.widget_text, Color.parseColor("#EF4444"))
    }

    val intent = Intent(context, TrackerWidget::class.java).apply {
        action = "TOGGLE_TRACKING"
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}