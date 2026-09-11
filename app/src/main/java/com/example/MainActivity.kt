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

        // Prewarm WebView cache directory structure to prevent Chromium ENOENT directory scan errors
        com.example.ui.player.embed.NeliPlayEmbedUtils.prewarmWebViewEnvironment(this)

        // Initialize NeliPlay notification channel
        NeliPlayNotificationManager.createNotificationChannel(this)
        com.example.util.NeliNotificationManager.initChannels(this)

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
}
