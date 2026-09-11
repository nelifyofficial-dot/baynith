package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NeliNotificationManager {

    const val CHANNEL_ENTERTAINMENT = "neliplay_entertainment"
    const val CHANNEL_DOWNLOADS = "neliplay_downloads"
    const val CHANNEL_UPDATES = "neliplay_updates"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val entertainmentChannel = NotificationChannel(
                CHANNEL_ENTERTAINMENT,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableLights(true)
                enableVibration(true)
            }

            val downloadsChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "NeliPlay Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Download status and offline playback ready alerts"
            }

            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES,
                context.getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.update_channel_description)
            }

            notificationManager.createNotificationChannels(
                listOf(entertainmentChannel, downloadsChannel, updatesChannel)
            )
        }
    }

    fun showDownloadCompletedNotification(context: Context, movieId: String, title: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "details")
                putExtra("movieId", movieId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                movieId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Download Complete")
                .setContentText("\"$title\" is ready to watch offline.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val manager = NotificationManagerCompat.from(context)
            manager.notify(movieId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission not yet granted on Android 13+
        } catch (_: Exception) {}
    }

    fun showFeaturedReleaseNotification(context: Context, title: String, overview: String, movieId: String? = null) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (movieId != null) {
                    putExtra("navigate_to", "details")
                    putExtra("movieId", movieId)
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                (movieId ?: title).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ENTERTAINMENT)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("New on NeliPlay: $title")
                .setContentText(overview.ifBlank { "Stream now in HD on NeliPlay" })
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val manager = NotificationManagerCompat.from(context)
            manager.notify((movieId ?: title).hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission not yet granted on Android 13+
        } catch (_: Exception) {}
    }
}
