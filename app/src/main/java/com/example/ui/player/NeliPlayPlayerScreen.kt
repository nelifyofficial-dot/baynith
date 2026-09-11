package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.cast.CastManager
import com.example.cast.NeliPlayCastButton
import com.example.data.local.entities.DownloadState
import com.example.data.model.CastMember
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.ui.components.resolveBackdropUrl
import com.example.ui.details.formatRuntime
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliGreenSuccess
import com.example.ui.theme.NeliSurface
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
        onMovieClick = onMovieClick,
        onSearchClick = onSearchClick
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun NeliPlayExoPlayerContent(
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    isLive: Boolean,
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var playbackDuration by remember { mutableLongStateOf(0L) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var selectedQuality by remember { mutableStateOf("1080p HD") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMenuOptions by remember { mutableStateOf(false) }

    // Aggressive LoadControl ExoPlayer instance
    val exoPlayer = remember(context) {
        val isEmulator = Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.MODEL.contains("google_sdk") ||
                Build.PRODUCT.contains("sdk")

        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            if (isEmulator) {
                setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                    val decoders = androidx.media3.exoplayer.mediacodec.MediaCodecUtil.getDecoderInfos(
                        mimeType,
                        requiresSecureDecoder,
                        requiresTunnelingDecoder
                    )
                    decoders.sortedBy { decoder ->
                        if (decoder.name.startsWith("c2.android.") || decoder.name.startsWith("OMX.google.")) 0 else 1
                    }
                }
            }
        }

        // ExoPlayer DefaultLoadControl with aggressive buffering parameters
        val loadControl = EpisodePreloadManager.buildAggressiveLoadControl()
        val httpSourceFactory = EpisodePreloadManager.createHttpDataSourceFactory()
        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpSourceFactory)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // Cast synchronization
    val castManager = remember { CastManager.getInstance(context) }
    val isCasting by castManager.isCasting.collectAsStateWithLifecycle()
    var wasCasting by remember { mutableStateOf(false) }

    LaunchedEffect(isCasting) {
        if (isCasting && !wasCasting) {
            wasCasting = true
            val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
            exoPlayer.pause()
            uiState.movie?.let { movie ->
                castManager.castMovie(movie, currentPos)
            }
            uiState.tvChannel?.let { channel ->
                castManager.castTvChannel(channel)
            }
        } else if (!isCasting && wasCasting) {
            wasCasting = false
            exoPlayer.play()
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

    // Controls auto-hide
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Back handler
    BackHandler {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            isFullscreen = false
        } else {
            onBack()
        }
    }

    // Clean up player on leave
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
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                        // Automatically play next preloaded episode if available!
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
                playerError = "Playback error: ${error.localizedMessage ?: "Unable to stream media"}"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            NeliPlayNotificationManager.clearPlaybackNotification(context)
            viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Progress updates
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

    // Set MediaItem in ExoPlayer with aggressive playlist preloading
    LaunchedEffect(uiState.mediaUrl, uiState.nextEpisode) {
        if (uiState.mediaUrl.isNotBlank()) {
            playerError = null
            isBuffering = true

            val uri = if (uiState.isOffline) {
                Uri.fromFile(File(uiState.mediaUrl))
            } else {
                Uri.parse(uiState.mediaUrl)
            }

            val mimeType = if (isLive || uiState.playbackType.equals("m3u8", ignoreCase = true) || uiState.mediaUrl.contains(".m3u8", ignoreCase = true)) {
                MimeTypes.APPLICATION_M3U8
            } else {
                MimeTypes.VIDEO_MP4
            }

            val currentItem = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(mimeType)
                .build()

            exoPlayer.clearMediaItems()
            exoPlayer.addMediaItem(currentItem)

            // Add next episode to ExoPlayer playlist for preloading
            val nextEp = uiState.nextEpisode
            if (nextEp != null && nextEp.streamUrl.isNotBlank()) {
                val nextUri = Uri.parse(nextEp.streamUrl)
                val nextMime = if (nextEp.effectivePlaybackType.equals("m3u8", ignoreCase = true) || nextEp.streamUrl.contains(".m3u8", ignoreCase = true)) {
                    MimeTypes.APPLICATION_M3U8
                } else {
                    MimeTypes.VIDEO_MP4
                }
                val nextItem = MediaItem.Builder()
                    .setMediaId(nextEp.id)
                    .setUri(nextUri)
                    .setMimeType(nextMime)
                    .build()
                exoPlayer.addMediaItem(nextItem)
            }

            if (uiState.initialPositionMs > 0 && !isLive) {
                exoPlayer.seekTo(uiState.initialPositionMs)
            }
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    // Fullscreen toggle logic
    fun toggleFullscreen() {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            isFullscreen = false
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            isFullscreen = true
        }
    }

    // Mute toggle
    fun toggleMute() {
        isMuted = !isMuted
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Share action
    fun shareContent() {
        val shareTitle = uiState.displayTitle
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, shareTitle)
            putExtra(Intent.EXTRA_TEXT, "Watch '$shareTitle' on NeliPlay: ${uiState.mediaUrl.ifBlank { "https://neliplay.com" }}")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share '$shareTitle'"))
    }

    val displayPoster = uiState.movie?.posterPath?.ifBlank { uiState.movie?.backdropPath }
        ?: uiState.episode?.stillPath
        ?: uiState.series?.posterPath
        ?: ""

    val movieYear = uiState.movie?.year?.toString()
        ?: uiState.series?.year?.toString()
        ?: "2022"

    val movieRuntime = formatRuntime(uiState.movie?.runtime ?: uiState.episode?.runtime ?: 161)
    val displayRuntime = if (movieRuntime.isNotBlank()) movieRuntime else "2h 41m"

    val genresList = (uiState.movie?.genres ?: uiState.series?.genres ?: listOf("Action", "Adventure", "Drama", "Sci-Fi"))
        .filter { it.isNotBlank() }
        .ifEmpty { listOf("Action", "Adventure", "Drama", "Sci-Fi") }

    val displayGenres = genresList.take(2).joinToString(" • ")

    val movieOverview = uiState.movie?.overview?.ifBlank { null }
        ?: uiState.episode?.overview?.ifBlank { null }
        ?: uiState.series?.overview?.ifBlank { null }
        ?: "After the death of King T'Challa, the people of Wakanda must face new threats and protect their nation from powerful enemies. A story of courage, unity and legacy continues."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeliVoid)
    ) {
        if (isFullscreen) {
            // Immersive Fullscreen Video Player
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showControls = !showControls
                    }
                    .testTag("fullscreen_video_container")
            ) {
                AndroidView(
                    factory = { ctx ->
                        val view = android.view.LayoutInflater.from(ctx)
                            .inflate(com.example.R.layout.neliplay_player_view, null) as PlayerView
                        view.apply {
                            player = exoPlayer
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Fullscreen Controls Overlay
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    PlayerControlsOverlay(
                        title = uiState.displayTitle,
                        isLive = isLive,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        isMuted = isMuted,
                        playbackPosition = playbackPosition,
                        playbackDuration = playbackDuration,
                        selectedQuality = selectedQuality,
                        isFullscreen = true,
                        onBack = { toggleFullscreen() },
                        onPlayPause = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        onSeek = { targetMs ->
                            exoPlayer.seekTo(targetMs)
                        },
                        onToggleMute = { toggleMute() },
                        onToggleFullscreen = { toggleFullscreen() },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
            }
        } else {
            // Standard Layout: Top App Bar + Video Player + Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // NeliPlay Logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.linearGradient(listOf(NeliBluePrimary, NeliCyanAccent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NeliPlay",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }

                    NeliPlayCastButton()

                    Box {
                        IconButton(
                            onClick = { showMenuOptions = !showMenuOptions },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMenuOptions,
                            onDismissRequest = { showMenuOptions = false },
                            modifier = Modifier.background(NeliSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Quality: $selectedQuality", color = Color.White) },
                                onClick = {
                                    showMenuOptions = false
                                    showSettingsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Playback Speed: ${currentSpeed}x", color = Color.White) },
                                onClick = {
                                    showMenuOptions = false
                                    showSettingsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share", color = Color.White) },
                                onClick = {
                                    showMenuOptions = false
                                    shareContent()
                                }
                            )
                        }
                    }
                }

                // Scrollable View containing Video Player and Content Details
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Video Player Item
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showControls = !showControls
                                }
                                .testTag("neliplay_player_window")
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    val view = android.view.LayoutInflater.from(ctx)
                                        .inflate(com.example.R.layout.neliplay_player_view, null) as PlayerView
                                    view.apply {
                                        player = exoPlayer
                                        useController = false
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Overlay Controls
                            if (showControls) {
                                PlayerControlsOverlay(
                                    title = uiState.displayTitle,
                                    isLive = isLive,
                                    isPlaying = isPlaying,
                                    isBuffering = isBuffering,
                                    isMuted = isMuted,
                                    playbackPosition = playbackPosition,
                                    playbackDuration = playbackDuration,
                                    selectedQuality = selectedQuality,
                                    isFullscreen = false,
                                    onBack = onBack,
                                    onPlayPause = {
                                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    },
                                    onSeek = { targetMs ->
                                        exoPlayer.seekTo(targetMs)
                                    },
                                    onToggleMute = { toggleMute() },
                                    onToggleFullscreen = { toggleFullscreen() },
                                    onOpenSettings = { showSettingsDialog = true }
                                )
                            }
                        }
                    }

                    // Preload Indicator Banner for Next Episode
                    if (uiState.nextEpisode != null) {
                        item {
                            val nextEp = uiState.nextEpisode!!
                            Surface(
                                color = NeliBluePrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Preload",
                                        tint = NeliCyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Next: Episode ${nextEp.episodeNumber} • ${nextEp.title.ifBlank { "Next Episode" }}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (uiState.isNextEpisodePreloaded) "Instant playback pre-buffered" else "Preloading in background...",
                                            color = NeliCyanAccent,
                                            fontSize = 10.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.loadMedia(nextEp.id, false) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Play Next",
                                            tint = NeliCyanAccent
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Movie Info Row: Poster on Left, Title/Metadata/Synopsis on Right
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            // Poster Thumbnail
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

                            // Details Column
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.displayTitle,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Metadata Row: HD badge, 2022, Action, Adventure, 2h 41m
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // HD pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeliBluePrimary)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "HD",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = movieYear, color = NeliTextSecondary, fontSize = 12.sp)

                                    if (displayGenres.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "•", color = NeliTextSecondary, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = displayGenres, color = NeliTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "•", color = NeliTextSecondary, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = displayRuntime, color = NeliTextSecondary, fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Synopsis Overview
                                Text(
                                    text = movieOverview,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Action Buttons Row: Play, Watchlist, Download, Share
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Play Button
                            Button(
                                onClick = {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeliBluePrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(42.dp)
                                    .testTag("action_play_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPlaying) "Pause" else "Play",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            // 2. Watchlist Button
                            Button(
                                onClick = { viewModel.toggleFavorite() },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeliSurfaceElevated),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(42.dp)
                                    .testTag("action_watchlist_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isFavorite) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = "Watchlist",
                                    tint = if (uiState.isFavorite) NeliCyanAccent else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isFavorite) "Saved" else "Watchlist",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }

                            // 3. Download Button
                            Button(
                                onClick = { viewModel.startDownload() },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeliSurfaceElevated),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(42.dp)
                                    .testTag("action_download_button")
                            ) {
                                val isDownloaded = uiState.downloadEntity?.status == DownloadState.COMPLETED
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Default.FileDownloadDone else Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = if (isDownloaded) NeliGreenSuccess else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isDownloaded) "Ready" else "Download",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }

                            // 4. Share Button
                            Button(
                                onClick = { shareContent() },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeliSurfaceElevated),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("action_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Share",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Cast & Crew Section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
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
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { /* See All */ }
                                ) {
                                    Text(
                                        text = "See All",
                                        color = NeliCyanAccent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "See All",
                                        tint = NeliCyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.castMembers) { cast ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(74.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .background(NeliSurfaceElevated)
                                        ) {
                                            AsyncImage(
                                                model = cast.profileUrl,
                                                contentDescription = cast.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
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
                                            color = NeliTextSecondary,
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

                    // About This Movie Section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp)
                        ) {
                            Text(
                                text = "About This Movie",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3 Info Cards Row: Release Date, Language, Subtitles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Release Date Card
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NeliSurfaceElevated)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = "Release Date",
                                            tint = NeliCyanAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = "Release Date", color = NeliTextSecondary, fontSize = 10.sp)
                                        Text(
                                            text = uiState.movie?.releaseDate ?: "Nov 11, 2022",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // 2. Language Card
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NeliSurfaceElevated)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = "Language",
                                            tint = NeliCyanAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = "Language", color = NeliTextSecondary, fontSize = 10.sp)
                                        Text(
                                            text = uiState.movie?.audioLanguage ?: "English",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // 3. Subtitles Card
                                Box(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NeliSurfaceElevated)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Icon(
                                            imageVector = Icons.Default.ClosedCaption,
                                            contentDescription = "Subtitles",
                                            tint = NeliCyanAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = "Subtitles", color = NeliTextSecondary, fontSize = 10.sp)
                                        Text(
                                            text = "English, Swahili, French and more",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Genre Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                genresList.forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(NeliSurfaceElevated)
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // If Series: Episodes List
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
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Season Tabs
                                if (uiState.seasons.size > 1) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    Spacer(modifier = Modifier.height(10.dp))
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
                                            if (uiState.nextEpisode?.id == ep.id && uiState.isNextEpisodePreloaded) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(NeliCyanAccent.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Preloaded",
                                                        color = NeliCyanAccent,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
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

                    // More Like This Section
                    if (uiState.moreLikeThis.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "More Like This",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "More",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(uiState.moreLikeThis) { simMovie ->
                                        Column(
                                            modifier = Modifier
                                                .width(110.dp)
                                                .clickable {
                                                    viewModel.loadMedia(simMovie.id, false)
                                                    onMovieClick(simMovie.id)
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(155.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(NeliSurfaceElevated)
                                            ) {
                                                AsyncImage(
                                                    model = simMovie.posterPath.ifBlank { simMovie.backdropPath },
                                                    contentDescription = simMovie.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = simMovie.title,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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

        // Settings Dialog (Quality & Speed)
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Playback Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
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

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Playback Speed", color = NeliCyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
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

@Composable
private fun PlayerControlsOverlay(
    title: String,
    isLive: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isMuted: Boolean,
    playbackPosition: Long,
    playbackDuration: Long,
    selectedQuality: String,
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleMute: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            )
    ) {
        // Top Bar: HD badge on Left, Cast & Fullscreen/PiP on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isFullscreen) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White
                    )
                }
            } else {
                // HD badge pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HD",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                NeliPlayCastButton()
                IconButton(onClick = onToggleFullscreen, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.PictureInPictureAlt,
                        contentDescription = "Expand",
                        tint = Color.White
                    )
                }
            }
        }

        // Center Play / Pause button
        Box(
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    color = NeliCyanAccent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Bottom Controls Bar: Progress bar, time, mute, settings, fullscreen
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Slider / Progress bar
            var isDragging by remember { mutableStateOf(false) }
            var dragPosition by remember { mutableFloatStateOf(0f) }

            val totalDur = playbackDuration.coerceAtLeast(1L)
            val currentPos = if (isDragging) (dragPosition * totalDur).toLong() else playbackPosition

            if (!isLive) {
                Slider(
                    value = if (totalDur > 0) (currentPos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f) else 0f,
                    onValueChange = { frac ->
                        isDragging = true
                        dragPosition = frac
                    },
                    onValueChangeFinished = {
                        val targetMs = (dragPosition * totalDur).toLong()
                        onSeek(targetMs)
                        isDragging = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = NeliCyanAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                )
            }

            // Bottom row: Time counter and right control icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time counter e.g. "0:00 / 2:41:15"
                Text(
                    text = if (isLive) "LIVE" else "${formatDuration(currentPos)} / ${formatDuration(playbackDuration)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mute / Unmute
                    IconButton(onClick = onToggleMute, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Settings
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Fullscreen Toggle
                    IconButton(onClick = onToggleFullscreen, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
