package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.MainActivity
import com.example.ui.player.embed.NeliPlayEmbeddedPlayer
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliSurfaceElevated
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
        onBack = onBack
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun NeliPlayExoPlayerContent(
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    isLive: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

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

    // MANDATORY FULLSCREEN LANDSCAPE MODE ON LAUNCH
    // As requested: "nataka player ikiwa kwenye Full landscape screen yani player ifunguke full pasipo kingine"
    val window = activity?.window
    val insetsController = remember(window) {
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        insetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Media3 ExoPlayer instance with robust software/hardware decoder fallback for emulator stability
    val exoPlayer = remember {
        val codecSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val defaultDecoders = try {
                MediaCodecSelector.DEFAULT.getDecoderInfos(
                    mimeType,
                    requiresSecureDecoder,
                    requiresTunnelingDecoder
                )
            } catch (e: Throwable) {
                emptyList()
            }

            // Strictly filter out virtual hardware codecs (goldfish, ranchu) that fail component interface queries with error 6
            val nonVirtualDecoders = defaultDecoders.filterNot { decoder ->
                decoder.name.contains("goldfish", ignoreCase = true) ||
                decoder.name.contains("ranchu", ignoreCase = true)
            }

            val isEmu = com.example.ui.player.embed.NeliPlayEmbedUtils.isEmulatorEnvironment()
            if (isEmu) {
                // In emulator/virtualized environments, strictly use standard Google software decoders
                val softwareDecoders = nonVirtualDecoders.filter { decoder ->
                    decoder.softwareOnly ||
                    decoder.name.startsWith("c2.android.", ignoreCase = true) ||
                    decoder.name.startsWith("OMX.google.", ignoreCase = true)
                }
                if (softwareDecoders.isNotEmpty()) {
                    softwareDecoders
                } else if (nonVirtualDecoders.isNotEmpty()) {
                    nonVirtualDecoders
                } else {
                    defaultDecoders.filterNot { it.name.contains("goldfish", ignoreCase = true) }
                }
            } else {
                if (nonVirtualDecoders.isNotEmpty()) nonVirtualDecoders else defaultDecoders
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

    // TextureView PlayerView (Hardware Composited, smooth PiP & virtualized environment stability)
    val playerView = remember(exoPlayer) {
        val view = android.view.LayoutInflater.from(context)
            .inflate(com.example.R.layout.neliplay_player_view, null) as PlayerView
        view.apply {
            player = exoPlayer
            useController = false
        }
    }

    // Connect with MainActivity for Picture-in-Picture (PiP) actions & corner dragging
    DisposableEffect(exoPlayer) {
        MainActivity.isPlayerActive = true
        MainActivity.playerActionCallback = object : MainActivity.PlayerActionCallback {
            override fun onPlayPause() {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            }

            override fun onRewind(ms: Long) {
                val newPos = (exoPlayer.currentPosition - ms).coerceAtLeast(0L)
                exoPlayer.seekTo(newPos)
            }

            override fun onForward(ms: Long) {
                val newPos = (exoPlayer.currentPosition + ms).coerceAtMost(exoPlayer.duration)
                exoPlayer.seekTo(newPos)
            }
        }
        onDispose {
            MainActivity.isPlayerActive = false
            MainActivity.isPlayerPlaying = false
            MainActivity.playerActionCallback = null
        }
    }

    LaunchedEffect(isPlaying) {
        MainActivity.isPlayerPlaying = isPlaying
        (activity as? MainActivity)?.updatePipActions(isPlaying)
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

    // Back handling: Restores portrait mode & returns back to Movie/Series Details page
    val handleExitPlayer = {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        insetsController?.show(WindowInsetsCompat.Type.systemBars())
        onBack()
    }

    BackHandler {
        handleExitPlayer()
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

            exoPlayer.prepare()

            // Check if user has an unfinished progress (> 5 seconds)
            if (uiState.initialPositionMs > 5000L && !isLive && !hasHandledResumeForCurrentMedia) {
                hasHandledResumeForCurrentMedia = true
                if (uiState.movieAutoCutApplied && uiState.initialPositionMs == MOVIE_AUTOSKIP_OFFSET_MS) {
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

    val displayPoster = uiState.movie?.posterPath?.ifBlank { uiState.movie?.backdropPath }
        ?: uiState.episode?.stillPath
        ?: uiState.series?.posterPath
        ?: ""

    val isSeriesContent = uiState.isSeries || uiState.seasons.isNotEmpty() || uiState.episode != null

    // PURE FULLSCREEN LANDSCAPE PLAYER LAYOUT - NOTHING ELSE!
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeliVoid)
    ) {
        if (uiState.isEmbed || (uiState.mediaUrl.isBlank() && uiState.embedCode.isNotBlank())) {
            NeliPlayEmbeddedPlayer(
                embedCode = uiState.embedCode,
                title = uiState.displayTitle,
                onBack = handleExitPlayer,
                contentId = uiState.movie?.id ?: uiState.episode?.id ?: "",
                modifier = Modifier.fillMaxSize()
            )
        } else {
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
                isFullscreen = true, // Always fullscreen landscape!
                exoPlayer = exoPlayer,
                playerView = playerView,
                isMovie = uiState.isMovie,
                isSeries = isSeriesContent,
                seasons = uiState.seasons,
                selectedSeason = uiState.selectedSeason,
                currentSeasonEpisodes = uiState.currentSeasonEpisodes,
                currentEpisodeId = uiState.episode?.id,
                onSelectSeason = { seasonNum -> viewModel.selectSeason(seasonNum) },
                onSelectEpisode = { ep -> viewModel.loadMedia(ep.id, false) },
                previousEpisode = uiState.previousEpisode,
                onPlayPreviousEpisode = { viewModel.playPreviousEpisode() },
                nextEpisode = uiState.nextEpisode,
                onPlayNextEpisode = { viewModel.playNextEpisode() },
                autoSkipIntro = uiState.autoSkipIntro,
                showAutoSkippedNotice = showAutoSkippedNotice,
                onDismissAutoSkipNotice = { showAutoSkippedNotice = false },
                onToggleAutoSkipIntro = { viewModel.toggleAutoSkipIntro(!uiState.autoSkipIntro) },
                onSkipIntro = {
                    exoPlayer.seekTo(MOVIE_AUTOSKIP_OFFSET_MS)
                    showAutoSkippedNotice = true
                },
                onBack = handleExitPlayer,
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
                onToggleMute = {
                    isMuted = !isMuted
                    exoPlayer.volume = if (isMuted) 0f else 1f
                },
                onToggleFullscreen = {
                    // Toggling fullscreen in dedicated player exits to details screen
                    handleExitPlayer()
                },
                onEnterPip = {
                    activity?.let { act ->
                        if (act is MainActivity) {
                            act.enterPictureInPicture()
                        }
                    }
                },
                onOpenSettings = { showSettingsDialog = true },
                modifier = Modifier.fillMaxSize()
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

        // Playback Settings Dialog (Stream Quality, Playback Speed, Auto-Skip)
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
