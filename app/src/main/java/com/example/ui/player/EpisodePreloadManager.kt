package com.example.ui.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.model.Episode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Manages aggressive startup LoadControl and background preloading of subsequent episodes
 * to deliver instantaneous, zero-latency playback transitions.
 */
@OptIn(UnstableApi::class)
object EpisodePreloadManager {
    private const val TAG = "EpisodePreloadManager"

    private val _preloadedEpisodeId = MutableStateFlow<String?>(null)
    val preloadedEpisodeId: StateFlow<String?> = _preloadedEpisodeId.asStateFlow()

    private var preloaderPlayer: ExoPlayer? = null
    private var currentPreloadedEpisode: Episode? = null

    /**
     * Builds a DefaultLoadControl with aggressive buffering parameters:
     * - bufferForPlaybackMs = 500ms (playback triggers almost instantly without waiting)
     * - bufferForPlaybackAfterRebufferMs = 1000ms (fast rebuffer recovery)
     * - minBufferMs = 15,000ms
     * - maxBufferMs = 50,000ms
     * - prioritizeTimeOverSizeThresholds = true
     * - backBuffer = 10,000ms for immediate backwards seeking
     */
    fun buildAggressiveLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 500, // Aggressive 500ms playback start!
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(/* backBufferDurationMs = */ 10_000, /* retainBackBufferFromKeyframe = */ true)
            .build()
    }

    /**
     * Creates an optimized HttpDataSource Factory with quick connect & read timeouts.
     */
    fun createHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent("NeliPlay/2.0 (Android; ExoPlayer-Aggressive-Preload)")
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(10_000)
            .setAllowCrossProtocolRedirects(true)
    }

    /**
     * Preloads the next episode in the background.
     * Initializes headers, manifest, and audio/video initialization segments
     * so that when the user taps Next Episode (or the current episode ends),
     * playback starts instantly.
     */
    fun preloadNextEpisode(context: Context, nextEpisode: Episode) {
        if (currentPreloadedEpisode?.id == nextEpisode.id && preloaderPlayer != null) {
            Log.d(TAG, "Episode ${nextEpisode.id} is already preloaded")
            return
        }

        if (nextEpisode.streamUrl.isBlank()) {
            Log.w(TAG, "Cannot preload episode ${nextEpisode.id}: streamUrl is blank")
            return
        }

        try {
            releasePreloader()

            val uri = if (nextEpisode.streamUrl.startsWith("/")) {
                Uri.fromFile(File(nextEpisode.streamUrl))
            } else {
                Uri.parse(nextEpisode.streamUrl)
            }

            val mimeType = if (nextEpisode.effectivePlaybackType.equals("m3u8", ignoreCase = true) ||
                nextEpisode.streamUrl.contains(".m3u8", ignoreCase = true)
            ) {
                MimeTypes.APPLICATION_M3U8
            } else {
                MimeTypes.VIDEO_MP4
            }

            val mediaItem = MediaItem.Builder()
                .setMediaId(nextEpisode.id)
                .setUri(uri)
                .setMimeType(mimeType)
                .build()

            val httpSourceFactory = createHttpDataSourceFactory()
            val mediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(httpSourceFactory)

            val preloadLoadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 8_000,
                    /* maxBufferMs = */ 15_000,
                    /* bufferForPlaybackMs = */ 500,
                    /* bufferForPlaybackAfterRebufferMs = */ 800
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            val renderersFactory = DefaultRenderersFactory(context).apply {
                setEnableDecoderFallback(true)
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            }

            val player = ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(preloadLoadControl)
                .build()

            player.playWhenReady = false // Don't play yet, buffer only
            player.setMediaItem(mediaItem)
            player.prepare()

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        Log.d(TAG, "Next episode ${nextEpisode.id} successfully preloaded and ready for instant playback!")
                        _preloadedEpisodeId.value = nextEpisode.id
                    }
                }
            })

            preloaderPlayer = player
            currentPreloadedEpisode = nextEpisode
            _preloadedEpisodeId.value = nextEpisode.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preload next episode: ${e.message}", e)
        }
    }

    /**
     * Checks if a given episode ID is preloaded.
     */
    fun isEpisodePreloaded(episodeId: String): Boolean {
        return _preloadedEpisodeId.value == episodeId
    }

    /**
     * Releases background preloader player instance when no longer needed.
     */
    fun releasePreloader() {
        try {
            preloaderPlayer?.stop()
            preloaderPlayer?.release()
            preloaderPlayer = null
            currentPreloadedEpisode = null
            _preloadedEpisodeId.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing preloader: ${e.message}", e)
        }
    }
}
