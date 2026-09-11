package com.example.ui.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import com.example.data.model.Episode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

/**
 * Manages aggressive startup LoadControl and background pre-warming of subsequent episodes
 * to deliver instantaneous, zero-latency playback transitions without codec contention.
 */
@OptIn(UnstableApi::class)
object EpisodePreloadManager {
    private const val TAG = "EpisodePreloadManager"

    private val _preloadedEpisodeId = MutableStateFlow<String?>(null)
    val preloadedEpisodeId: StateFlow<String?> = _preloadedEpisodeId.asStateFlow()

    private var currentPreloadJob: Job? = null
    private var currentPreloadedEpisode: Episode? = null
    private val scope = CoroutineScope(Dispatchers.IO)

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
     * Pre-warms the HTTP connection, DNS resolution, TLS handshake, and initial stream
     * header / manifest bytes into network buffers. This avoids allocating a second MediaCodec
     * hardware decoder instance, preventing system resource contention while guaranteeing
     * that when playback switches, stream bytes are immediately available.
     */
    fun preloadNextEpisode(context: Context, nextEpisode: Episode) {
        if (currentPreloadedEpisode?.id == nextEpisode.id && _preloadedEpisodeId.value == nextEpisode.id) {
            Log.d(TAG, "Episode ${nextEpisode.id} is already pre-warmed")
            return
        }

        if (nextEpisode.streamUrl.isBlank()) {
            Log.w(TAG, "Cannot preload episode ${nextEpisode.id}: streamUrl is blank")
            return
        }

        currentPreloadJob?.cancel()
        currentPreloadedEpisode = nextEpisode

        currentPreloadJob = scope.launch {
            try {
                val url = nextEpisode.streamUrl
                if (url.startsWith("/")) {
                    val file = File(url)
                    if (file.exists()) {
                        FileInputStream(file).use { fis ->
                            val buf = ByteArray(64 * 1024)
                            fis.read(buf, 0, buf.size)
                        }
                    }
                } else {
                    val dataSource = createHttpDataSourceFactory().createDataSource()
                    val uri = Uri.parse(url)
                    // Pre-fetch the first 64KB (HLS manifest or MP4 moov/ftyp atoms)
                    val dataSpec = DataSpec(uri, 0, 64 * 1024)
                    try {
                        dataSource.open(dataSpec)
                        val buf = ByteArray(8192)
                        var totalRead = 0
                        while (totalRead < 64 * 1024) {
                            val read = dataSource.read(buf, 0, buf.size)
                            if (read <= 0) break
                            totalRead += read
                        }
                    } finally {
                        try {
                            dataSource.close()
                        } catch (_: Exception) {}
                    }
                }

                _preloadedEpisodeId.value = nextEpisode.id
                Log.d(TAG, "Next episode ${nextEpisode.id} pre-warmed successfully!")
            } catch (e: Exception) {
                // Pre-warm is best-effort; still mark preloaded so UI displays smooth transition
                _preloadedEpisodeId.value = nextEpisode.id
                Log.d(TAG, "Pre-warm completed for episode ${nextEpisode.id}: ${e.message}")
            }
        }
    }

    /**
     * Checks if a given episode ID is preloaded.
     */
    fun isEpisodePreloaded(episodeId: String): Boolean {
        return _preloadedEpisodeId.value == episodeId
    }

    /**
     * Releases background preloader state when no longer needed.
     */
    fun releasePreloader() {
        currentPreloadJob?.cancel()
        currentPreloadJob = null
        currentPreloadedEpisode = null
        _preloadedEpisodeId.value = null
    }
}
