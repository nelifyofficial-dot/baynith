package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import com.example.data.model.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages rich Android notifications for NeliPlay.
 * Designed according to NeliPlay notification specs:
 * Shows NeliPlay branding, movie poster art, title, synopsis, and an actionable "▶ Play" button.
 */
object NeliPlayNotificationManager {
    const val CHANNEL_ID = "neliplay_entertainment_channel"
    private const val NOTIFICATION_ID_BASE = 1001
    const val PLAYBACK_NOTIFICATION_ID = 1002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(false)
                setShowBadge(false)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows playback notification with exact metadata for active media (Movie, Episode, or Live TV).
     */
    fun showPlaybackNotification(
        context: Context,
        title: String,
        subtitle: String,
        artworkUrl: String?,
        contentId: String,
        isLive: Boolean = false
    ) {
        createNotificationChannel(context)

        CoroutineScope(Dispatchers.IO).launch {
            val posterBitmap: Bitmap? = try {
                if (!artworkUrl.isNullOrBlank()) {
                    val loader = ImageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(artworkUrl)
                        .allowHardware(false)
                        .build()
                    val result = (loader.execute(req) as? SuccessResult)?.drawable
                    (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
                } else {
                    BitmapFactory.decodeResource(context.resources, R.drawable.ic_neliplay_logo)
                }
            } catch (e: Exception) {
                try {
                    BitmapFactory.decodeResource(context.resources, R.drawable.ic_neliplay_logo)
                } catch (ex: Exception) {
                    null
                }
            }

            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_NAVIGATE_MOVIE_ID", contentId)
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                PLAYBACK_NOTIFICATION_ID,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_neliplay_logo)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setSubText(if (isLive) "LIVE TV" else "NeliPlay")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentPendingIntent)

            if (posterBitmap != null) {
                builder.setLargeIcon(posterBitmap)
            }

            try {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(PLAYBACK_NOTIFICATION_ID, builder.build())
            } catch (e: SecurityException) {
                android.util.Log.w("NeliPlayNotification", "Notification permission not granted: ${e.message}")
            }
        }
    }

    /**
     * Clears active media playback notification when player stops or activity disposes.
     */
    fun clearPlaybackNotification(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(PLAYBACK_NOTIFICATION_ID)
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Posts a rich notification for a movie with a direct "▶ Play" action.
     */
    fun showMovieNotification(
        context: Context,
        movie: Movie,
        notificationId: Int = NOTIFICATION_ID_BASE
    ) {
        createNotificationChannel(context)

        CoroutineScope(Dispatchers.IO).launch {
            // Load poster bitmap asynchronously via Coil
            val posterBitmap: Bitmap? = try {
                val imageUrl = movie.posterPath ?: movie.backdropPath
                if (!imageUrl.isNullOrBlank()) {
                    val loader = ImageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .allowHardware(false)
                        .build()
                    val result = (loader.execute(req) as? SuccessResult)?.drawable
                    (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
                } else {
                    BitmapFactory.decodeResource(context.resources, R.drawable.ic_neliplay_logo)
                }
            } catch (e: Exception) {
                try {
                    BitmapFactory.decodeResource(context.resources, R.drawable.ic_neliplay_logo)
                } catch (ex: Exception) {
                    null
                }
            }

            // Main Open Intent
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_NAVIGATE_MOVIE_ID", movie.id)
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Direct "▶ Play" Action Intent
            val playIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_PLAY_MOVIE_ID", movie.id)
            }
            val playPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 500,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_neliplay_logo)
                .setContentTitle(movie.title)
                .setContentText(movie.overview.ifBlank { "Now streaming on NeliPlay" })
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)

            if (posterBitmap != null) {
                builder.setLargeIcon(posterBitmap)
                // Use BigPictureStyle or BigTextStyle
                builder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(movie.title)
                        .bigText(movie.overview.ifBlank { "Now streaming in ultra HD on NeliPlay." })
                        .setSummaryText("Neliplay")
                )
            }

            // Prominent "▶ Play" Action
            builder.addAction(
                android.R.drawable.ic_media_play,
                "▶ Play",
                playPendingIntent
            )

            try {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                android.util.Log.w("NeliPlayNotification", "POST_NOTIFICATIONS permission not granted: ${e.message}")
            }
        }
    }
}
