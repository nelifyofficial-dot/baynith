package com.example.cast

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.Movie
import com.example.data.model.TvChannel
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Singleton CastManager providing real Google Cast session management,
 * device discovery monitoring, and media playback control.
 */
class CastManager private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "NeliPlayCastManager"

        @Volatile
        private var instance: CastManager? = null

        fun getInstance(context: Context): CastManager {
            return instance ?: synchronized(this) {
                instance ?: CastManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var castContext: CastContext? = null
    private var currentCastSession: CastSession? = null
    private var remoteMediaClient: RemoteMediaClient? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackerJob: Job? = null

    // State flows
    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val _castState = MutableStateFlow<CastPlaybackState>(CastPlaybackState.Disconnected)
    val castState: StateFlow<CastPlaybackState> = _castState.asStateFlow()

    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            updatePlaybackStatus()
        }

        override fun onMetadataUpdated() {
            updatePlaybackStatus()
        }
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            Log.d(TAG, "Cast session starting...")
            _castState.value = CastPlaybackState.Connecting
            _deviceName.value = session.castDevice?.friendlyName
        }

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            Log.d(TAG, "Cast session started with device: ${session.castDevice?.friendlyName}")
            onCastSessionConnected(session)
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            Log.e(TAG, "Cast session start failed with code: $error")
            _castState.value = CastPlaybackState.Error("Unable to connect to ${session.castDevice?.friendlyName ?: "the TV"}.")
            _isCasting.value = false
            _deviceName.value = null
        }

        override fun onSessionEnding(session: CastSession) {
            Log.d(TAG, "Cast session ending...")
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            Log.d(TAG, "Cast session ended.")
            onCastSessionDisconnected()
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {
            Log.d(TAG, "Cast session resuming...")
            _castState.value = CastPlaybackState.Connecting
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            Log.d(TAG, "Cast session resumed.")
            onCastSessionConnected(session)
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            Log.e(TAG, "Cast session resume failed: $error")
            onCastSessionDisconnected()
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            Log.w(TAG, "Cast session suspended: $reason")
            _castState.value = CastPlaybackState.Connecting
        }
    }

    private var isCastSupported = false

    fun isCastSupported(): Boolean = isCastSupported

    init {
        try {
            val gmsResult = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext)
            if (gmsResult == ConnectionResult.SUCCESS) {
                castContext = CastContext.getSharedInstance(appContext)
                castContext?.sessionManager?.addSessionManagerListener(
                    sessionManagerListener,
                    CastSession::class.java
                )
                isCastSupported = true
                val currentSession = castContext?.sessionManager?.currentCastSession
                if (currentSession != null && currentSession.isConnected) {
                    onCastSessionConnected(currentSession)
                }
            } else {
                Log.i(TAG, "Google Play Services is not available (code: $gmsResult). Operating in safe standalone mode.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Google Cast is not available on this device: ${e.message}")
        }
    }

    private fun onCastSessionConnected(session: CastSession) {
        currentCastSession = session
        _isCasting.value = true
        _deviceName.value = session.castDevice?.friendlyName ?: "Cast Device"
        _castState.value = CastPlaybackState.Connected

        remoteMediaClient = session.remoteMediaClient
        remoteMediaClient?.registerCallback(remoteMediaClientCallback)

        startProgressTracking()
        updatePlaybackStatus()
    }

    private fun onCastSessionDisconnected() {
        stopProgressTracking()
        remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
        remoteMediaClient = null
        currentCastSession = null
        _isCasting.value = false
        _deviceName.value = null
        _castState.value = CastPlaybackState.Disconnected
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressTrackerJob = scope.launch {
            while (isActive) {
                remoteMediaClient?.let { client ->
                    val pos = client.approximateStreamPosition
                    val dur = client.streamDuration
                    if (pos >= 0) _currentPositionMs.value = pos
                    if (dur > 0) _durationMs.value = dur
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressTracking() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    private fun updatePlaybackStatus() {
        val client = remoteMediaClient ?: return
        val playerState = client.playerState

        when (playerState) {
            MediaStatus.PLAYER_STATE_PLAYING -> _castState.value = CastPlaybackState.Playing
            MediaStatus.PLAYER_STATE_PAUSED -> _castState.value = CastPlaybackState.Paused
            MediaStatus.PLAYER_STATE_BUFFERING -> _castState.value = CastPlaybackState.Buffering
            MediaStatus.PLAYER_STATE_IDLE -> {
                val idleReason = client.idleReason
                if (idleReason == MediaStatus.IDLE_REASON_ERROR) {
                    _castState.value = CastPlaybackState.Error("This video cannot be played on this Cast device.")
                } else {
                    _castState.value = CastPlaybackState.Connected
                }
            }
            else -> _castState.value = CastPlaybackState.Connected
        }

        val dur = client.streamDuration
        if (dur > 0) _durationMs.value = dur
        val pos = client.approximateStreamPosition
        if (pos >= 0) _currentPositionMs.value = pos
    }

    /**
     * Casts a movie with metadata to the connected Cast device.
     */
    fun castMovie(movie: Movie, startPositionMs: Long = 0L) {
        val client = remoteMediaClient
        if (client == null) {
            Log.w(TAG, "Cannot cast movie: RemoteMediaClient is null (no active Cast session)")
            return
        }

        try {
            val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, movie.title)
                if (movie.overview.isNotBlank()) {
                    putString(MediaMetadata.KEY_SUBTITLE, movie.overview.take(150))
                }
                movie.year?.let { putInt(MediaMetadata.KEY_RELEASE_DATE, it) }

                val imagePath = movie.backdropPath ?: movie.posterPath
                if (!imagePath.isNullOrBlank()) {
                    addImage(WebImage(Uri.parse(imagePath)))
                }
            }

            val contentType = if (movie.streamUrl.endsWith(".m3u8", ignoreCase = true)) {
                "application/x-mpegURL"
            } else {
                "video/mp4"
            }

            val mediaInfo = MediaInfo.Builder(movie.streamUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(contentType)
                .setMetadata(movieMetadata)
                .build()

            val request = MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setCurrentTime(startPositionMs)
                .setAutoplay(true)
                .build()

            client.load(request)
            Log.d(TAG, "Initiated cast of movie '${movie.title}' at position ${startPositionMs}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading movie onto Cast receiver: ${e.message}", e)
            _castState.value = CastPlaybackState.Error("Unable to start casting: ${e.message}")
        }
    }

    /**
     * Casts a Live TV channel HLS stream to the connected Cast device.
     */
    fun castTvChannel(channel: TvChannel) {
        val client = remoteMediaClient
        if (client == null) {
            Log.w(TAG, "Cannot cast TV: RemoteMediaClient is null (no active Cast session)")
            return
        }

        try {
            val tvMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_TV_SHOW).apply {
                putString(MediaMetadata.KEY_TITLE, channel.name)
                putString(MediaMetadata.KEY_SUBTITLE, channel.category ?: "Live TV")
                if (channel.logoUrl.isNotBlank()) {
                    addImage(WebImage(Uri.parse(channel.logoUrl)))
                }
            }

            val mediaInfo = MediaInfo.Builder(channel.streamUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType("application/x-mpegURL")
                .setMetadata(tvMetadata)
                .build()

            val request = MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build()

            client.load(request)
            Log.d(TAG, "Initiated cast of Live TV channel '${channel.name}'")
        } catch (e: Exception) {
            Log.e(TAG, "Error casting TV channel: ${e.message}", e)
            _castState.value = CastPlaybackState.Error("Unable to cast live TV: ${e.message}")
        }
    }

    fun play() {
        remoteMediaClient?.play()
    }

    fun pause() {
        remoteMediaClient?.pause()
    }

    fun togglePlayPause() {
        remoteMediaClient?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        remoteMediaClient?.seek(positionMs)
    }

    fun stop() {
        remoteMediaClient?.stop()
    }

    fun disconnect() {
        castContext?.sessionManager?.endCurrentSession(true)
    }

    fun showCastDialog(context: Context) {
        try {
            var ctx: Context? = context
            var activity: androidx.fragment.app.FragmentActivity? = null
            while (ctx is android.content.ContextWrapper) {
                if (ctx is androidx.fragment.app.FragmentActivity) {
                    activity = ctx
                    break
                }
                ctx = ctx.baseContext
            }

            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                if (_isCasting.value) disconnect()
                return
            }

            if (_isCasting.value) {
                val controllerDialog = androidx.mediarouter.app.MediaRouteControllerDialogFragment()
                controllerDialog.show(activity.supportFragmentManager, "neliplay_cast_controller")
            } else {
                val castCtx = castContext ?: CastContext.getSharedInstance(context.applicationContext)
                val chooserDialog = androidx.mediarouter.app.MediaRouteChooserDialogFragment()
                chooserDialog.routeSelector = castCtx.mergedSelector ?: androidx.mediarouter.media.MediaRouteSelector.EMPTY
                chooserDialog.show(activity.supportFragmentManager, "neliplay_cast_chooser")
            }
        } catch (e: Exception) {
            Log.w(TAG, "showCastDialog error: ${e.message}")
            if (_isCasting.value) disconnect()
        }
    }
}
