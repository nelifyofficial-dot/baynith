package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.data.firebase.FirebaseManager
import com.example.ui.navigation.NeliPlayApp
import com.example.ui.theme.NeliPlayTheme
import com.example.util.NeliPlayNotificationManager

class MainActivity : FragmentActivity() {

    private var initialPlayMovieId by mutableStateOf<String?>(null)
    private var initialNavigateMovieId by mutableStateOf<String?>(null)
    private var showUpdateDialogOnStart by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase and Firestore with offline cache
        FirebaseManager.init(this)
        com.example.data.repository.SubscriptionManager.init(this)

        // Sanitize WebView cache directory structure to prevent Chromium SimpleCache ENOENT/corrupt index errors
        com.example.ui.player.embed.NeliPlayEmbedUtils.sanitizeWebViewEnvironment(this)

        // Initialize NeliPlay notification channel
        NeliPlayNotificationManager.createNotificationChannel(this)
        com.example.util.NeliNotificationManager.initChannels(this)
        com.example.util.MovieRecommendationScheduler.createChannel(this)
        com.example.util.MovieRecommendationScheduler.scheduleNext(this)
        com.example.util.MovieRecommendationScheduler.scheduleDailyNoonCheck(this)

        // Record app open timestamp for noon notification check
        getSharedPreferences("neliplay_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putLong("last_app_open_time", System.currentTimeMillis())
            .apply()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        handleIntent(intent)

        enableEdgeToEdge()

        setContent {
            NeliPlayTheme {
                NeliPlayApp(
                    initialPlayMovieId = initialPlayMovieId,
                    initialNavigateMovieId = initialNavigateMovieId,
                    showUpdateDialogOnStart = showUpdateDialogOnStart
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences("neliplay_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putLong("last_app_open_time", System.currentTimeMillis())
            .apply()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val playId = intent.getStringExtra("EXTRA_PLAY_MOVIE_ID")
        val navigateId = intent.getStringExtra("EXTRA_NAVIGATE_MOVIE_ID") ?: intent.getStringExtra("movieId")
        val showUpdate = intent.getBooleanExtra(com.example.update.manager.UpdateManager.EXTRA_SHOW_UPDATE_DIALOG, false)
        if (showUpdate) {
            showUpdateDialogOnStart = true
        }
        if (!playId.isNullOrBlank()) {
            initialPlayMovieId = playId
        } else if (!navigateId.isNullOrBlank()) {
            initialNavigateMovieId = navigateId
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPlayerActive && isPlayerPlaying) {
            enterPictureInPicture()
        }
    }

    private val pipReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                ACTION_PIP_PLAY_PAUSE -> {
                    playerActionCallback?.onPlayPause()
                }
                ACTION_PIP_REWIND -> {
                    playerActionCallback?.onRewind(10000L)
                }
                ACTION_PIP_FORWARD -> {
                    playerActionCallback?.onForward(10000L)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = android.content.IntentFilter().apply {
            addAction(ACTION_PIP_PLAY_PAUSE)
            addAction(ACTION_PIP_REWIND)
            addAction(ACTION_PIP_FORWARD)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this, pipReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(this, pipReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(pipReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }

    fun buildPipParams(isPlaying: Boolean): android.app.PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return try {
            val builder = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9))

            // Rewind 10s Remote Action
            val rewindIntent = Intent(ACTION_PIP_REWIND).setPackage(packageName)
            val rewindPendingIntent = android.app.PendingIntent.getBroadcast(
                this,
                101,
                rewindIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val rewindIcon = android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_rew)
            val rewindAction = android.app.RemoteAction(rewindIcon, "Rewind 10s", "Rewind 10 seconds", rewindPendingIntent)

            // Play / Pause Toggle Remote Action
            val playPauseIntent = Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName)
            val playPausePendingIntent = android.app.PendingIntent.getBroadcast(
                this,
                102,
                playPauseIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val playPauseIconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            val playPauseLabel = if (isPlaying) "Pause" else "Play"
            val playPauseIcon = android.graphics.drawable.Icon.createWithResource(this, playPauseIconRes)
            val playPauseAction = android.app.RemoteAction(playPauseIcon, playPauseLabel, playPauseLabel, playPausePendingIntent)

            // Forward 10s Remote Action
            val forwardIntent = Intent(ACTION_PIP_FORWARD).setPackage(packageName)
            val forwardPendingIntent = android.app.PendingIntent.getBroadcast(
                this,
                103,
                forwardIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val forwardIcon = android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_ff)
            val forwardAction = android.app.RemoteAction(forwardIcon, "Forward 10s", "Forward 10 seconds", forwardPendingIntent)

            builder.setActions(listOf(rewindAction, playPauseAction, forwardAction))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(isPlayerActive && isPlayerPlaying)
            }

            builder.build()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed building PiP params: ${e.message}")
            null
        }
    }

    fun updatePipActions(isPlaying: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buildPipParams(isPlaying)?.let { params ->
                try {
                    setPictureInPictureParams(params)
                } catch (e: Exception) {
                    // Ignore if not in PiP
                }
            }
        }
    }

    fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = buildPipParams(isPlayerPlaying) ?: android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed entering PiP: ${e.message}")
            }
        }
    }

    interface PlayerActionCallback {
        fun onPlayPause()
        fun onRewind(ms: Long = 10000L)
        fun onForward(ms: Long = 10000L)
    }

    companion object {
        const val ACTION_PIP_PLAY_PAUSE = "com.example.PIP_PLAY_PAUSE"
        const val ACTION_PIP_REWIND = "com.example.PIP_REWIND"
        const val ACTION_PIP_FORWARD = "com.example.PIP_FORWARD"

        var isPlayerActive: Boolean = false
        var isPlayerPlaying: Boolean = false
        var playerActionCallback: PlayerActionCallback? = null
    }
}
