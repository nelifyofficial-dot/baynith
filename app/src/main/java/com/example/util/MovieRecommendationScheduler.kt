package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Ensures rich, clean, family-friendly movie recommendation notifications
 * are reliably triggered every 30 minutes, even when the device is locked,
 * in doze mode, or the app is closed.
 */
object MovieRecommendationScheduler {
    private const val TAG = "MovieNotification"
    private const val REQUEST_CODE = 4501
    const val INTERVAL_MILLIS = 30 * 60 * 1000L // Exactly 30 minutes
    const val CHANNEL_ID = "neliplay_recommendations_channel"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "NeliPlay Mapendekezo ya Filamu"
            val descriptionText = "Arifa za filamu kali zilizotafsiriwa kila dakika 30"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleNext(context: Context, delayMillis: Long = INTERVAL_MILLIS) {
        try {
            createChannel(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PeriodicMovieNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAtMillis = System.currentTimeMillis() + delayMillis

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled next movie recommendation notification in ${delayMillis / 1000 / 60} minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule notification alarm: ${e.message}", e)
        }
    }

    fun cancel(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PeriodicMovieNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notification alarm: ${e.message}")
        }
    }
}

class PeriodicMovieNotificationReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Curated family-friendly clean Swahili & blockbuster fallback movies (never adult)
    private val safeFallbackMovies = listOf(
        Pair("Blue Beetle (Kiswahili DJ)", "Tazama kijana Jaime Reyes akipata suti ya kichawi ya mende yenye nguvu za ajabu!"),
        Pair("Avatar: Fire and Ash", "Mchanganyiko wa maisha na vita vya viumbe wa anga za juu huko Pandora."),
        Pair("Spider-Man: Across the Multiverse", "Miles Morales anapambana kuokoa ulimwengu wote na mashujaa wenzake."),
        Pair("Joker (DJ Afro)", "Kisa cha kusisimua cha maisha ya Arthur Fleck kilichotafsiriwa kwa ufundi."),
        Pair("Black Panther: Wakanda", "Ufalme wa Wakanda unalinda rasilimali na amani ya watu wake dhidi ya maadui."),
        Pair("Jumanji: The Next Level", "Vituko vya ajabu ndani ya mchezo wa msituni na wanyama wakali."),
        Pair("Fast X (DJ Murphy)", "Mbio za kasi na mapigano ya kifamilia ya Dominic Toretto na kikosi chake.")
    )

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("PeriodicMovieReceiver", "Alarm fired! Preparing 30-minute notification for 2 movies...")

        // Always reschedule immediately so the cycle continues uninterrupted every 30 minutes
        MovieRecommendationScheduler.scheduleNext(context, MovieRecommendationScheduler.INTERVAL_MILLIS)

        scope.launch {
            try {
                showTwoMoviesNotification(context)
            } catch (e: Exception) {
                Log.e("PeriodicMovieReceiver", "Failed to show 2-movie notification: ${e.message}", e)
            }
        }
    }

    private suspend fun showTwoMoviesNotification(context: Context) {
        MovieRecommendationScheduler.createChannel(context)

        // Try to fetch latest clean published movies from Firestore
        var movie1Title = ""
        var movie2Title = ""
        var targetMovieId = ""
        var artworkUrl: String? = null

        try {
            val firestore = FirebaseManager.firestore
            val querySnapshot = firestore.collection("movies")
                .whereEqualTo("published", true)
                .limit(10)
                .get()
                .await()

            val cleanMovies = querySnapshot.documents.mapNotNull { doc ->
                val movie = Movie.fromDocument(doc)
                // Filter out any adult or sensitive genres/keywords
                val isAdultOrRestricted = movie.genres.any { g ->
                    g.contains("erotic", ignoreCase = true) ||
                    g.contains("adult", ignoreCase = true) ||
                    g.contains("18+", ignoreCase = true) ||
                    g.contains("xxx", ignoreCase = true)
                }
                if (!isAdultOrRestricted && movie.title.isNotBlank()) movie else null
            }

            if (cleanMovies.size >= 2) {
                val shuffled = cleanMovies.shuffled()
                val m1 = shuffled[0]
                val m2 = shuffled[1]
                movie1Title = m1.title
                movie2Title = m2.title
                targetMovieId = m1.id
                artworkUrl = m1.posterPath.ifBlank { m1.backdropPath }
            }
        } catch (e: Exception) {
            Log.w("PeriodicMovieReceiver", "Firestore fetch failed, using curated safe movies: ${e.message}")
        }

        // Use curated safe fallback list if needed
        if (movie1Title.isBlank() || movie2Title.isBlank()) {
            val pair1 = safeFallbackMovies.random()
            var pair2 = safeFallbackMovies.random()
            while (pair2.first == pair1.first) {
                pair2 = safeFallbackMovies.random()
            }
            movie1Title = pair1.first
            movie2Title = pair2.first
        }

        // Build notification
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (targetMovieId.isNotBlank()) {
                putExtra("EXTRA_NAVIGATE_MOVIE_ID", targetMovieId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            2002,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

        val titleText = "🔥 Filamu 2 Kali Zinazovuma Sasa!"
        val contentSummary = "1. $movie1Title • 2. $movie2Title"
        val bigText = "🎬 Mapendekezo ya Filamu za Leo:\n" +
                "1️⃣ $movie1Title\n" +
                "2️⃣ $movie2Title\n\n" +
                "Gusa hapa kutazama bila matangazo au pakua sasa!"

        val builder = NotificationCompat.Builder(context, MovieRecommendationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_neliplay_logo)
            .setContentTitle(titleText)
            .setContentText(contentSummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_neliplay_logo,
                "▶ Tazama Sasa",
                pendingIntent
            )

        if (posterBitmap != null) {
            builder.setLargeIcon(posterBitmap)
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(7701, builder.build())
            Log.d("PeriodicMovieReceiver", "30-min movie recommendation notification sent successfully!")
        } catch (e: SecurityException) {
            Log.w("PeriodicMovieReceiver", "Notification permission not granted: ${e.message}")
        }
    }
}

class MovieNotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("MovieBootReceiver", "Device boot detected. Re-scheduling 30-minute movie notifications...")
            // First notification fires 5 minutes after reboot, then every 30 minutes
            MovieRecommendationScheduler.scheduleNext(context, 5 * 60 * 1000L)
        }
    }
}
