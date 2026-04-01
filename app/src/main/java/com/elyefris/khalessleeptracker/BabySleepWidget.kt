package com.elyefris.khalessleeptracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews

class BabySleepWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    // 1. Leer los datos guardados por la app principal
    val prefs = context.getSharedPreferences("BabySleepPrefs", Context.MODE_PRIVATE)
    val isSleeping = prefs.getBoolean("is_sleeping", false)
    val startTime = prefs.getLong("start_time", 0L)
    val sleepType = prefs.getString("sleep_type", "NOCHE")

    val views = RemoteViews(context.packageName, R.layout.widget_baby_sleep)

    // 2. Actualizar la Interfaz según el estado
    if (isSleeping) {
        val typeStr = if (sleepType == "SIESTA") "Siesta 🌤️" else "Noche 🌜"
        views.setTextViewText(R.id.tv_status, typeStr)

        // Android necesita el tiempo basado en el encendido del sistema (SystemClock) para el Chronometer
        val timeSinceStart = System.currentTimeMillis() - startTime
        val chronometerBase = SystemClock.elapsedRealtime() - timeSinceStart

        views.setChronometer(R.id.chronometer_sleep, chronometerBase, "%s", true)
        views.setViewVisibility(R.id.chronometer_sleep, View.VISIBLE)
        views.setViewVisibility(R.id.tv_awake, View.GONE)
    } else {
        views.setTextViewText(R.id.tv_status, "En Pausa ✨")
        views.setViewVisibility(R.id.chronometer_sleep, View.GONE)
        views.setViewVisibility(R.id.tv_awake, View.VISIBLE)
    }

    // 3. Al tocar todo el widget (widget_root), abrir la app
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}