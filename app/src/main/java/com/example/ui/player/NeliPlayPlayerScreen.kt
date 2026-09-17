package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.MainActivity
import com.example.data.local.entities.DownloadState
import com.example.ui.player.embed.NeliPlayEmbeddedPlayer
import com.example.ui.details.formatRuntime
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliGreenSuccess
import com.example.data.model.CastMember
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid
import com.example.util.NeliPlayNotificationManager
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

@Composable
fun NeliPlayPlayerScreen(
    contentId: String,
    isLive: Boolean,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(contentId, isLive) {
        viewModel.loadMedia(contentId, isLive)
    }

    NeliPlayExoPlayerContent(
        uiState = uiState,
        viewModel = viewModel,
        isLive = isLive,
        onBack = onBack,
        onMovieClick = onMovieClick
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun NeliPlayExoPlayerContent(
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    isLive: Boolean,
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var isFullscreen by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var playbackDuration by remember { mutableLongStateOf(0L) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var selectedQuality by remember { mutableStateOf("1080p HD") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var pendingResumePosition by remember { mutableLongStateOf(0L) }
    var hasHandledResumeForCurrentMedia by remember { mutableStateOf(false) }
    var showAutoSkippedNotice by remember { mutableStateOf(false) }
    var hasAutoSkippedMovieForCurrentMedia by remember { mutableStateOf(false) }

    // Auto-dismiss the intro auto-skipped notice after 4 seconds
    LaunchedEffect(showAutoSkippedNotice) {
        if (showAutoSkippedNotice) {
            delay(4000)
            showAutoSkippedNotice = false
        }
    }

    // Cast Profile Dialog state
    var selectedCastForProfile by remember { mutableStateOf<CastMember?>(null) }

    // Toggle fullscreen and hide mobile top banner (battery %, wifi, clock)
    val window = activity?.window
    val insetsController = remember(window) {
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }

    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController?.apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Media3 ExoPlayer instance with hardware/software decoder fallback
    val exoPlayer = remember {
        val codecSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val defaultDecoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder
            )
            val isEmu = com.example.ui.player.embed.NeliPlayEmbedUtils.isEmulator()
            if (isEmu) {
                // In emulator environments, demote goldfish/ranchu virtual hardware codecs that fail
                // system resource queries (error 6) and prioritize stable software decoders (c2.android / OMX.google)
                defaultDecoders.sortedWith(
                    compareByDescending<MediaCodecInfo> {
                        !it.name.contains("goldfish", ignoreCase = true) && !it.name.contains("ranchu", ignoreCase = true)
                    }.thenByDescending {
                        it.softwareOnly || it.name.startsWith("c2.android.") || it.name.startsWith("OMX.google.")
                    }
                )
            } else {
                // On real hardware devices, keep default hardware decoder order but demote broken virtual decoders
                defaultDecoders.sortedByDescending {
                    !it.name.contains("goldfish", ignoreCase = true) && !it.name.contains("ranchu", ignoreCase = true)
                }
            }
        }

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setEnableDecoderFallback(true)
            .setMediaCodecSelector(codecSelector)

        val loadControl = EpisodePreloadManager.buildAggressiveLoadControl()
        val httpSourceFactory = EpisodePreloadManager.createHttpDataSourceFactory()
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // SurfaceView PlayerView (Hardware Composited)
    val playerView = remember(exoPlayer) {
        val view = android.view.LayoutInflater.from(context)
            .inflate(com.example.R.layout.neliplay_player_view, null) as PlayerView
        view.apply {
            player = exoPlayer
            useController = false
        }
    }

    // Sync active player status with MainActivity for PiP and auto-pause
    DisposableEffect(Unit) {
        MainActivity.isPlayerActive = true
        onDispose {
            MainActivity.isPlayerActive = false
            MainActivity.isPlayerPlaying = false
        }
    }
    LaunchedEffect(isPlaying) {
        MainActivity.isPlayerPlaying = isPlaying
    }

    // Lifecycle Observer: Auto-pause when leaving app unless entering Picture-in-Picture mode
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        activity?.isInPictureInPictureMode == true
                    } else false
                    if (!inPip) {
                        exoPlayer.pause()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Notification metadata
    LaunchedEffect(uiState.movie, uiState.episode, uiState.tvChannel) {
        val movie = uiState.movie
        val episode = uiState.episode
        val channel = uiState.tvChannel
        if (movie != null) {
            NeliPlayNotificationManager.showPlaybackNotification(
                context = context,
                title = movie.title,
                subtitle = "NeliPlay Movie",
                artworkUrl = movie.posterPath.ifBlank { movie.backdropPath },
                contentId = movie.id,
                isLive = false
            )
        } else if (episode != null) {
            val epCode = "S${String.format("%02d", episode.seasonNumber)} E${String.format("%02d", episode.episodeNumber)}"
            val epTitle = episode.title.ifBlank { "Episode ${episode.episodeNumber}" }
            NeliPlayNotificationManager.showPlaybackNotification(
                context = context,
                title = "$epCode — $epTitle",
                subtitle = uiState.seriesName ?: "NeliPlay Series",
                artworkUrl = episode.stillPath,
                contentId = episode.id,
                isLive = false
            )
        } else if (channel != null) {
            NeliPlayNotificationManager.showPlaybackNotification(
                context = context,
                title = channel.name,
                subtitle = "Live TV • ${channel.category ?: "Streaming"}",
                artworkUrl = channel.logoUrl,
                contentId = channel.id,
                isLive = true
            )
        }
    }

    // Back handling: exit fullscreen first if active
    BackHandler {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            isFullscreen = false
        } else {
            onBack()
        }
    }

    // ExoPlayer event listeners and cleanup
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        playerError = null
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        playerError = null
                        playbackDuration = exoPlayer.duration.coerceAtLeast(0L)

                        // If movie has autoSkip enabled and is in intro (< 5m 30s), cut and jump to 05:30
                        if (uiState.isMovie && uiState.autoSkipIntro && !hasAutoSkippedMovieForCurrentMedia) {
                            val cutOffset = MOVIE_AUTOSKIP_OFFSET_MS
                            val dur = exoPlayer.duration
                            if (exoPlayer.currentPosition < cutOffset && (dur <= 0L || dur > cutOffset)) {
                                hasAutoSkippedMovieForCurrentMedia = true
                                exoPlayer.seekTo(cutOffset)
                                showAutoSkippedNotice = true
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                        val nextEp = uiState.nextEpisode
                        if (nextEp != null && nextEp.streamUrl.isNotBlank()) {
                            viewModel.loadMedia(nextEp.id, false)
                        }
                    }
                    Player.STATE_IDLE -> {
                        isBuffering = false
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                isPlaying = false
                Log.e("NeliPlayPlayer", "ExoPlayer playback error: ${error.errorCodeName} (${error.errorCode})", error)
                playerError = "Unable to play this title. Tap retry to reconnect."
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            NeliPlayNotificationManager.clearPlaybackNotification(context)
            viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
            playerView.player = null
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Progress updates & saving
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            playbackPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            playbackDuration = exoPlayer.duration.coerceAtLeast(0L)
            if (playbackPosition > 0 && playbackDuration > 0 && !isLive) {
                viewModel.saveProgress(playbackPosition, playbackDuration)
            }
            delay(1000)
        }
    }

    // Set MediaItem in ExoPlayer & check Resume prompt
    LaunchedEffect(uiState.mediaUrl) {
        if (uiState.mediaUrl.isNotBlank()) {
            playerError = null
            isBuffering = true
            hasHandledResumeForCurrentMedia = false
            hasAutoSkippedMovieForCurrentMedia = false

            val uri = if (uiState.isOffline) {
                Uri.fromFile(File(uiState.mediaUrl))
            } else {
                Uri.parse(uiState.mediaUrl)
            }

            val currentBuilder = MediaItem.Builder().setUri(uri)
            if (isLive || uiState.playbackType.equals("m3u8", ignoreCase = true) || uiState.mediaUrl.contains(".m3u8", ignoreCase = true)) {
                currentBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            } else if (uiState.playbackType.equals("mp4", ignoreCase = true) || uiState.mediaUrl.contains(".mp4", ignoreCase = true)) {
                currentBuilder.setMimeType(MimeTypes.VIDEO_MP4)
            }
            val currentItem = currentBuilder.build()

            exoPlayer.clearMediaItems()
            exoPlayer.addMediaItem(currentItem)

            // Add next episode for smooth playback
            val nextEp = uiState.nextEpisode
            if (nextEp != null && nextEp.streamUrl.isNotBlank()) {
                val nextUri = Uri.parse(nextEp.streamUrl)
                val nextBuilder = MediaItem.Builder()
                    .setMediaId(nextEp.id)
                    .setUri(nextUri)
                if (nextEp.effectivePlaybackType.equals("m3u8", ignoreCase = true) || nextEp.streamUrl.contains(".m3u8", ignoreCase = true)) {
                    nextBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                } else if (nextEp.effectivePlaybackType.equals("mp4", ignoreCase = true) || nextEp.streamUrl.contains(".mp4", ignoreCase = true)) {
                    nextBuilder.setMimeType(MimeTypes.VIDEO_MP4)
                }
                val nextItem = nextBuilder.build()
                exoPlayer.addMediaItem(nextItem)
            }

            exoPlayer.prepare()

            // Check if user has an unfinished progress (> 5 seconds)
            if (uiState.initialPositionMs > 5000L && !isLive && !hasHandledResumeForCurrentMedia) {
                hasHandledResumeForCurrentMedia = true
                if (uiState.movieAutoCutApplied && uiState.initialPositionMs == MOVIE_AUTOSKIP_OFFSET_MS) {
                    // Automatic start at 5m 30s for movies
                    hasAutoSkippedMovieForCurrentMedia = true
                    exoPlayer.seekTo(uiState.initialPositionMs)
                    exoPlayer.play()
                    showAutoSkippedNotice = true
                } else {
                    pendingResumePosition = uiState.initialPositionMs
                    showResumeDialog = true
                }
            } else {
                if (uiState.initialPositionMs > 0 && !isLive) {
                    exoPlayer.seekTo(uiState.initialPositionMs)
                }
                exoPlayer.play()
            }
        }
    }

    fun toggleFullscreen() {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            isFullscreen = false
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            isFullscreen = true
        }
    }

    fun toggleMute() {
        isMuted = !isMuted
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    val displayPoster = uiState.movie?.posterPath?.ifBlank { uiState.movie?.backdropPath }
        ?: uiState.episode?.stillPath
        ?: uiState.series?.posterPath
        ?: ""

    val movieYear = uiState.movie?.year?.toString()
        ?: uiState.series?.year?.toString()
        ?: "2024"

    val movieRuntime = formatRuntime(uiState.movie?.runtime ?: uiState.episode?.runtime ?: 120)
    val displayRuntime = if (movieRuntime.isNotBlank()) movieRuntime else "2h"

    val genresList = (uiState.movie?.genres ?: uiState.series?.genres ?: listOf("Action", "Drama"))
        .filter { it.isNotBlank() }
        .ifEmpty { listOf("Action", "Drama") }

    val movieOverview = uiState.movie?.overview?.ifBlank { null }
        ?: uiState.episode?.overview?.ifBlank { null }
        ?: uiState.series?.overview?.ifBlank { null }
        ?: "An exciting streaming title on NeliPlay. Watch now in high definition."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeliVoid)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isEmbed || (uiState.mediaUrl.isBlank() && uiState.embedCode.isNotBlank())) {
                NeliPlayEmbeddedPlayer(
                    embedCode = uiState.embedCode,
                    title = uiState.displayTitle,
                    onBack = onBack,
                    contentId = uiState.movie?.id ?: uiState.episode?.id ?: "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
            } else {
                // --- FIXED STICKY YOUTUBE PLAYER CONTAINER ---
                // Starts and covers directly from the top! Back button and controls are hosted within the player overlay.
                YouTubePlayerContainer(
                    streamUrl = uiState.mediaUrl,
                    posterUrl = displayPoster,
                    title = uiState.displayTitle,
                    isLive = isLive,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    isMuted = isMuted,
                    playerError = playerError,
                    playbackPosition = playbackPosition,
                    playbackDuration = playbackDuration,
                    isFullscreen = isFullscreen,
                    exoPlayer = exoPlayer,
                    playerView = playerView,
                    isMovie = uiState.isMovie,
                    autoSkipIntro = uiState.autoSkipIntro,
                    showAutoSkippedNotice = showAutoSkippedNotice,
                    onDismissAutoSkipNotice = { showAutoSkippedNotice = false },
                    onToggleAutoSkipIntro = { viewModel.toggleAutoSkipIntro(!uiState.autoSkipIntro) },
                    onSkipIntro = {
                        exoPlayer.seekTo(MOVIE_AUTOSKIP_OFFSET_MS)
                        showAutoSkippedNotice = true
                    },
                    onBack = {
                        if (isFullscreen) {
                            toggleFullscreen()
                        } else {
                            onBack()
                        }
                    },
                    onPlayPause = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    onSeek = { targetMs -> exoPlayer.seekTo(targetMs) },
                    onRewind10 = {
                        val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                        exoPlayer.seekTo(newPos)
                    },
                    onForward10 = {
                        val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                        exoPlayer.seekTo(newPos)
                    },
                    onRetry = {
                        playerError = null
                        exoPlayer.prepare()
                        exoPlayer.play()
                    },
                    onToggleMute = { toggleMute() },
                    onToggleFullscreen = { toggleFullscreen() },
                    onEnterPip = {
                        activity?.let { act ->
                            if (act is MainActivity) {
                                act.enterPictureInPicture()
                            }
                        }
                    },
                    onOpenSettings = { showSettingsDialog = true },
                    modifier = if (isFullscreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    }
                )
            }

            if (!isFullscreen) {
                // --- 2. SCROLLABLE CONTENT (Passes DOWN underneath the sticky player) ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Movie Title and Star Rating ONLY
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = uiState.displayTitle,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val ratingVal = uiState.movie?.rating ?: uiState.series?.rating ?: 8.5
                                val formattedRating = String.format(Locale.US, "%.1f", ratingVal)

                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = formattedRating,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = " / 10",
                                    color = NeliTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Action Buttons Row: Favorite, Watch Later, Download, and for Series: Preview & Next (NO Share)
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Favorite Button
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (uiState.isFavorite) NeliBluePrimary.copy(alpha = 0.25f) else NeliSurfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (uiState.isFavorite) NeliCyanAccent else Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier
                                        .clickable { viewModel.toggleFavorite() }
                                        .testTag("action_favorite_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (uiState.isFavorite) Color(0xFFFF4081) else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (uiState.isFavorite) "Favorited" else "Favorite",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Watch Later Button
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (uiState.isWatchLater) NeliBluePrimary.copy(alpha = 0.25f) else NeliSurfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (uiState.isWatchLater) NeliCyanAccent else Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier
                                        .clickable { viewModel.toggleWatchLater() }
                                        .testTag("action_watch_later_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isWatchLater) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Watch Later",
                                            tint = if (uiState.isWatchLater) NeliCyanAccent else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (uiState.isWatchLater) "Saved" else "Watch Later",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Download Button (hidden for live TV)
                            if (!isLive) {
                                item {
                                    val isDownloaded = uiState.downloadEntity?.status == DownloadState.COMPLETED
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isDownloaded) NeliGreenSuccess.copy(alpha = 0.2f) else NeliSurfaceElevated,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isDownloaded) NeliGreenSuccess else Color.White.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier
                                            .clickable { viewModel.startDownload() }
                                            .testTag("action_download_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isDownloaded) Icons.Default.FileDownloadDone else Icons.Default.Download,
                                                contentDescription = "Download",
                                                tint = if (isDownloaded) NeliGreenSuccess else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (isDownloaded) "Downloaded" else "Download",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            // Series Only: Preview (Previous) Episode Button
                            if (uiState.isSeries) {
                                item {
                                    val hasPrev = uiState.previousEpisode != null
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (hasPrev) NeliSurfaceElevated else NeliSurfaceElevated.copy(alpha = 0.5f),
                                        border = BorderStroke(
                                            1.dp,
                                            Color.White.copy(alpha = if (hasPrev) 0.15f else 0.05f)
                                        ),
                                        modifier = Modifier
                                            .clickable(enabled = hasPrev) { viewModel.playPreviousEpisode() }
                                            .testTag("action_preview_episode_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SkipPrevious,
                                                contentDescription = "Preview Episode",
                                                tint = if (hasPrev) Color.White else Color.White.copy(alpha = 0.35f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Preview",
                                                color = if (hasPrev) Color.White else Color.White.copy(alpha = 0.35f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Series Only: Next Episode Button
                                item {
                                    val hasNext = uiState.nextEpisode != null
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (hasNext) NeliBluePrimary.copy(alpha = 0.2f) else NeliSurfaceElevated.copy(alpha = 0.5f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (hasNext) NeliCyanAccent else Color.White.copy(alpha = 0.05f)
                                        ),
                                        modifier = Modifier
                                            .clickable(enabled = hasNext) { viewModel.playNextEpisode() }
                                            .testTag("action_next_episode_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SkipNext,
                                                contentDescription = "Next Episode",
                                                tint = if (hasNext) NeliCyanAccent else Color.White.copy(alpha = 0.35f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Next",
                                                color = if (hasNext) Color.White else Color.White.copy(alpha = 0.35f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // "About this movie" section with poster, synopsis, metadata, cast, and more to watch
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = if (uiState.isSeries) "About this series" else "About this movie",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Poster image
                                Box(
                                    modifier = Modifier
                                        .width(95.dp)
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NeliSurfaceElevated)
                                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(10.dp))
                                ) {
                                    AsyncImage(
                                        model = displayPoster,
                                        contentDescription = uiState.displayTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Synopsis & Quick Specs
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = movieOverview,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Release Year & Runtime
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = "Year: $movieYear", color = NeliCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Text(text = "•", color = NeliTextSecondary, fontSize = 10.sp)
                                        Text(text = displayRuntime, color = NeliTextSecondary, fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Genres
                                    Text(
                                        text = "Genres: ${genresList.joinToString(", ")}",
                                        color = NeliTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // ENHANCED CAST SECTION WITH IMAGES AND CLICKABLE PROFILES
                    if (uiState.castMembers.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cast & Crew",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tap to view profile",
                                        color = NeliTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(uiState.castMembers) { cast ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(82.dp)
                                                .clickable { selectedCastForProfile = cast }
                                                .testTag("cast_card_${cast.name.replace(" ", "_")}")
                                        ) {
                                            // Circular Cast Photo with Cyan outline
                                            Box(
                                                modifier = Modifier
                                                    .size(70.dp)
                                                    .clip(CircleShape)
                                                    .border(2.dp, NeliCyanAccent.copy(alpha = 0.6f), CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(NeliBluePrimary, Color(0xFF1E293B))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Fallback Initials
                                                val initials = cast.name.split(" ")
                                                    .mapNotNull { it.firstOrNull()?.toString() }
                                                    .take(2)
                                                    .joinToString("")
                                                Text(
                                                    text = initials,
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                if (!cast.profileUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = cast.profileUrl,
                                                        contentDescription = cast.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(70.dp)
                                                            .clip(CircleShape)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = cast.name,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Text(
                                                text = cast.role,
                                                color = NeliCyanAccent,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Series Episodes Selector (if series)
                    if (uiState.episodes.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Episodes",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (uiState.seasons.size > 1) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    ) {
                                        items(uiState.seasons) { sNum ->
                                            val isSel = sNum == uiState.selectedSeason
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) NeliBluePrimary else NeliSurfaceElevated)
                                                    .clickable { viewModel.selectSeason(sNum) }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "Season $sNum",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                uiState.currentSeasonEpisodes.forEach { ep ->
                                    val isCurrent = ep.id == uiState.episode?.id
                                    Surface(
                                        color = if (isCurrent) NeliBluePrimary.copy(alpha = 0.2f) else NeliSurfaceElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { viewModel.loadMedia(ep.id, false) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${ep.episodeNumber}",
                                                color = if (isCurrent) NeliCyanAccent else Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(28.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ep.title.ifBlank { "Episode ${ep.episodeNumber}" },
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (ep.runtime != null && ep.runtime > 0) {
                                                    Text(
                                                        text = "${ep.runtime}m",
                                                        color = NeliTextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            IconButton(onClick = { viewModel.loadMedia(ep.id, false) }) {
                                                Icon(
                                                    imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = if (isCurrent) NeliCyanAccent else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // More to Watch Section
                    if (uiState.moreLikeThis.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "More to Watch",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(uiState.moreLikeThis) { simMovie ->
                                        Column(
                                            modifier = Modifier
                                                .width(115.dp)
                                                .clickable {
                                                    viewModel.loadMedia(simMovie.id, false)
                                                    onMovieClick(simMovie.id)
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(160.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(NeliSurfaceElevated)
                                            ) {
                                                AsyncImage(
                                                    model = simMovie.posterPath.ifBlank { simMovie.backdropPath },
                                                    contentDescription = simMovie.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = simMovie.title,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Rating",
                                                    tint = Color(0xFFFFC107),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = String.format(Locale.US, "%.1f", simMovie.rating),
                                                    color = NeliTextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CAST PROFILE DIALOG (When user taps any cast member)
        val activeCast = selectedCastForProfile
        if (activeCast != null) {
            CastProfileDialog(
                cast = activeCast,
                onDismiss = { selectedCastForProfile = null }
            )
        }

        // Resume Playback Dialog
        if (showResumeDialog) {
            AlertDialog(
                onDismissRequest = {
                    showResumeDialog = false
                    exoPlayer.seekTo(pendingResumePosition)
                    exoPlayer.play()
                },
                title = {
                    Text(
                        text = "Resume Playback",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Continue watching from ${formatDuration(pendingResumePosition)}?",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResumeDialog = false
                            exoPlayer.seekTo(pendingResumePosition)
                            exoPlayer.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeliBluePrimary)
                    ) {
                        Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showResumeDialog = false
                            val startPos = if (uiState.isMovie && uiState.autoSkipIntro) MOVIE_AUTOSKIP_OFFSET_MS else 0L
                            exoPlayer.seekTo(startPos)
                            exoPlayer.play()
                            if (startPos > 0L) {
                                showAutoSkippedNotice = true
                            }
                        }
                    ) {
                        Text("Restart", color = NeliCyanAccent)
                    }
                },
                containerColor = NeliSurfaceElevated
            )
        }

        // Playback Settings Dialog (Engine, Quality, Speed, Ads)
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Playback Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        // Quality
                        Text("Stream Quality", color = NeliCyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        listOf("1080p HD", "720p HD", "480p SD", "Auto (Dynamic)").forEach { q ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedQuality = q }
                            ) {
                                RadioButton(
                                    selected = selectedQuality == q,
                                    onClick = { selectedQuality = q },
                                    colors = RadioButtonDefaults.colors(selectedColor = NeliCyanAccent)
                                )
                                Text(text = q, color = Color.White, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Playback Speed
                        Text("Playback Speed", color = NeliCyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (currentSpeed == speed) NeliBluePrimary else NeliSurfaceElevated)
                                        .clickable {
                                            currentSpeed = speed
                                            exoPlayer.playbackParameters = PlaybackParameters(speed)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Auto-Skip for Movies
                        if (uiState.isMovie) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Auto-Skip Intro", color = NeliCyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleAutoSkipIntro(!uiState.autoSkipIntro) }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto-skip Movie Intro",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Start all movies at 5m 30s (cuts start intro)",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = uiState.autoSkipIntro,
                                    onCheckedChange = { viewModel.toggleAutoSkipIntro(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = NeliCyanAccent
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Done", color = NeliCyanAccent, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = NeliSurfaceElevated
            )
        }
    }
}
