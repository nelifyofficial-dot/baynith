package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NeliMovieNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Run coroutine to fetch 2 safe (non-adult) movies
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                NeliPlayMovieNotifier.sendNonAdultMovieNotification(context)
            } catch (_: Exception) {
            } finally {
                // Reschedule for next 30 minutes to guarantee continuous delivery even if phone was in doze
                NeliPlayMovieNotifier.schedule30MinRepeating(context)
            }
        }
    }
}

class NeliBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            NeliPlayMovieNotifier.schedule30MinRepeating(context)
        }
    }
}

object NeliPlayMovieNotifier {

    const val CHANNEL_ID = "neliplay_recommendations_channel"
    private const val REQUEST_CODE = 4491
    private const val INTERVAL_MS = 30 * 60 * 1000L // 30 minutes

    fun schedule30MinRepeating(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, NeliMovieNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = SystemClock.elapsedRealtime() + INTERVAL_MS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (_: Exception) {}
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Filamu Mpya NeliPlay",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Mapendekezo ya filamu 2 za kutazama kila baada ya dakika 30"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    suspend fun sendNonAdultMovieNotification(context: Context) {
        createChannel(context)

        // Fetch movies from Firestore
        var safeMovies: List<Movie> = emptyList()
        try {
            val snapshot = FirebaseManager.moviesCollection
                .whereEqualTo("published", true)
                .limit(20)
                .get()
                .await()

            safeMovies = snapshot.documents.mapNotNull { Movie.fromDocument(it) }
                .filter { movie ->
                    // STRICT NON-ADULT FILTER: Not 18+, no adult keywords, safe for all audiences
                    !movie.title.contains("18+", ignoreCase = true) &&
                    !movie.title.contains("adult", ignoreCase = true) &&
                    !movie.genres.any { it.contains("erotic", ignoreCase = true) || it.contains("adult", ignoreCase = true) || it.contains("18+", ignoreCase = true) }
                }
        } catch (_: Exception) {}

        // Fallback default safe movies if offline
        val title1 = safeMovies.getOrNull(0)?.title ?: "Mchanganyiko wa Maisha"
        val title2 = safeMovies.getOrNull(1)?.title ?: "Kidudu Cha Mapenzi"
        val targetMovieId = safeMovies.firstOrNull()?.id

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!targetMovieId.isNullOrBlank()) {
                putExtra("navigate_to", "details")
                putExtra("movieId", targetMovieId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎬 Filamu 2 Kali Zinakusubiri!")
            .setContentText("1. $title1 • 2. $title2. Bofya hapa utazame sasa!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🎬 Leo tunakupendekezea kutazama:\n• $title1\n• $title2\n\nFilamu hizi zote zipo tayari katika lugha ya Kiswahili safi. Bofya hapa uanze kuburudika mara moja!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(2026, notification)
        } catch (_: SecurityException) {}
    }
}
