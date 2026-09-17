package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class NeliPlayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_neliplay)

            val prefs = context.getSharedPreferences("neliplay_widget_prefs", Context.MODE_PRIVATE)
            val latestTitle = prefs.getString("latest_movie_title", null)
            val latestMovieId = prefs.getString("latest_movie_id", null)

            if (!latestTitle.isNullOrBlank()) {
                views.setTextViewText(R.id.widget_subtitle, "🔥 Mpya: $latestTitle")
                views.setTextViewText(R.id.widget_status, "Movie Mpya")
            } else {
                views.setTextViewText(R.id.widget_subtitle, "Movies, Series & Swahili Cinema")
                views.setTextViewText(R.id.widget_status, "Ready to Stream")
            }

            // Intent for Watch / Home or Direct Play
            val watchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (!latestMovieId.isNullOrBlank()) {
                    putExtra("EXTRA_NAVIGATE_MOVIE_ID", latestMovieId)
                } else {
                    putExtra("navigate_to", "home")
                }
            }
            val watchPendingIntent = PendingIntent.getActivity(
                context, 101, watchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_watch, watchPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, watchPendingIntent)

            // Intent for Search
            val searchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "search")
            }
            val searchPendingIntent = PendingIntent.getActivity(
                context, 102, searchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_search, searchPendingIntent)

            // Intent for Live TV
            val tvIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "tv")
            }
            val tvPendingIntent = PendingIntent.getActivity(
                context, 103, tvIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_tv, tvPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateLatestMovie(context: Context, movieTitle: String, movieId: String? = null) {
            try {
                context.getSharedPreferences("neliplay_widget_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("latest_movie_title", movieTitle)
                    .putString("latest_movie_id", movieId)
                    .apply()
                notifyWidgetUpdate(context)
            } catch (_: Exception) {}
        }

        fun requestPinWidget(context: Context): Boolean {
            return try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                    if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                        val pinWidgetProvider = ComponentName(context, NeliPlayWidgetProvider::class.java)
                        appWidgetManager.requestPinAppWidget(pinWidgetProvider, null, null)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            } catch (_: Exception) {
                false
            }
        }

        fun notifyWidgetUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context, NeliPlayWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(component)
                if (ids != null && ids.isNotEmpty()) {
                    val intent = Intent(context, NeliPlayWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (_: Exception) {}
        }
    }
}
