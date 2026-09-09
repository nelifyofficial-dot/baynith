package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cast.CastManager
import com.example.cast.CastPlaybackState
import com.example.cast.NeliPlayCastButton
import com.example.util.NeliPlayNotificationManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.player.embed.NeliPlayEmbeddedPlayer
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliLiveRed
import com.example.ui.theme.NeliSurfaceElevated
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
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Composable
fun NeliPlayPlayerScreen(
    contentId: String,
    isLive: Boolean,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(contentId, isLive) {
        viewModel.loadMedia(contentId, isLive)
    }

    if (uiState.isEmbed) {
        NeliPlayEmbeddedPlayer(
            embedCode = uiState.embedCode,
            title = uiState.displayTitle,
            onBack = onBack
        )
    } else {
        NeliPlayExoPlayerContent(
            uiState = uiState,
            viewModel = viewModel,
            isLive = isLive,
            onBack = onBack
        )
    }
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

    var isFullscreen by remember { mutableStateOf(false) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var playbackDuration by remember { mutableLongStateOf(0L) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var selectedQuality by remember { mutableStateOf("720p") }

    // Initialize ExoPlayer
    val exoPlayer = remember(context) {
        val isEmulator = android.os.Build.HARDWARE.contains("goldfish") ||
                android.os.Build.HARDWARE.contains("ranchu") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.PRODUCT.contains("sdk")

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            if (isEmulator) {
                setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                    val decoders = androidx.media3.exoplayer.mediacodec.MediaCodecUtil.getDecoderInfos(
                        mimeType,
                        requiresSecureDecoder,
                        requiresTunnelingDecoder
                    )
                    // Prioritize robust software decoders on emulator to avoid C2 hardware query failures
                    decoders.sortedBy { decoder ->
                        if (decoder.name.startsWith("c2.android.") || decoder.name.startsWith("OMX.google.")) 0 else 1
                    }
                }
            }
        }
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    val castManager = remember { CastManager.getInstance(context) }
    val isCasting by castManager.isCasting.collectAsStateWithLifecycle()
    val castState by castManager.castState.collectAsStateWithLifecycle()
    val castDeviceName by castManager.deviceName.collectAsStateWithLifecycle()
    val castPosition by castManager.currentPositionMs.collectAsStateWithLifecycle()
    val castDuration by castManager.durationMs.collectAsStateWithLifecycle()
    var wasCasting by remember { mutableStateOf(false) }

    // Synchronize local player and Cast receiver seamlessly
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
            // User disconnected from TV; resume local playback if movie was playing
            if (castPosition > 0 && !isLive) {
                exoPlayer.seekTo(castPosition)
                exoPlayer.play()
            }
        }
    }

    // Post rich notification when movie is loaded and playing
    LaunchedEffect(uiState.movie) {
        uiState.movie?.let { movie ->
            NeliPlayNotificationManager.showMovieNotification(context, movie)
        }
    }

    // Auto-hide controls after 3.5 seconds
    LaunchedEffect(showControls, isPlaying, isControlsLocked) {
        if (showControls && isPlaying && !isControlsLocked) {
            delay(3500)
            showControls = false
        }
    }

    // Exit fullscreen on system back if fullscreen, else go back
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
            viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Update position timer while playing
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

    // Prepare MediaItem when mediaUrl is ready
    LaunchedEffect(uiState.mediaUrl) {
        if (uiState.mediaUrl.isNotBlank()) {
            playerError = null
            isBuffering = true

            val uri = if (uiState.isOffline) {
                Uri.fromFile(File(uiState.mediaUrl))
            } else {
                Uri.parse(uiState.mediaUrl)
            }

            val mediaItemBuilder = MediaItem.Builder().setUri(uri)

            // Explicitly set M3U8 MIME type for live HLS streams or m3u8 playback type
            if (isLive || uiState.playbackType.equals("m3u8", ignoreCase = true) || uiState.mediaUrl.contains(".m3u8", ignoreCase = true)) {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            } else {
                mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
            }

            val mediaItem = mediaItemBuilder.build()
            exoPlayer.setMediaItem(mediaItem)
            if (uiState.initialPositionMs > 0 && !isLive) {
                exoPlayer.seekTo(uiState.initialPositionMs)
            }
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

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
            .testTag("neliplay_player_container")
    ) {
        // Player Surface
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

        // Buffering Indicator
        if ((isBuffering || castState is CastPlaybackState.Buffering) && playerError == null && !isCasting) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(52.dp),
                color = NeliCyanAccent,
                strokeWidth = 3.dp
            )
        }

        // Casting overlay when actively casting to a remote TV
        if (isCasting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CastConnected,
                        contentDescription = "Casting to $castDeviceName",
                        tint = NeliCyanAccent,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Casting to ${castDeviceName ?: "Google Cast"}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (castState) {
                            is CastPlaybackState.Playing -> "Playing on TV"
                            is CastPlaybackState.Paused -> "Paused on TV"
                            is CastPlaybackState.Buffering -> "Buffering on TV..."
                            is CastPlaybackState.Error -> (castState as CastPlaybackState.Error).message
                            else -> "Connected to receiver"
                        },
                        color = NeliCyanAccent,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { castManager.disconnect() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B1414)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Disconnect", color = Color(0xFFFF5252), fontSize = 13.sp)
                    }
                }
            }
        }

        // Error Dialog / View
        if (playerError != null || uiState.error != null) {
            val msg = playerError ?: uiState.error ?: "Unable to play video."
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Retry",
                        tint = NeliCyanAccent,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                playerError = null
                                exoPlayer.prepare()
                                exoPlayer.play()
                            }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to retry",
                        color = NeliCyanAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            playerError = null
                            exoPlayer.prepare()
                            exoPlayer.play()
                        }
                    )
                }
            }
        }

        // Custom UI Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isFullscreen) {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                isFullscreen = false
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val titleText = if (isLive) {
                        uiState.tvChannel?.name ?: "Live TV"
                    } else {
                        uiState.displayTitle
                    }

                    Text(
                        text = titleText,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isLive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeliLiveRed)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else if (uiState.isOffline) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeliBluePrimary)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "OFFLINE",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    NeliPlayCastButton(
                        modifier = Modifier.size(36.dp)
                    )
                }

                // CENTER CONTROLS (Rewind, Play/Pause, Forward)
                if (!isControlsLocked) {
                    val isEffectivePlaying = if (isCasting) {
                        castState is CastPlaybackState.Playing
                    } else {
                        isPlaying
                    }

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isLive) {
                            IconButton(
                                onClick = {
                                    if (isCasting) {
                                        val newPos = (castPosition - 10000).coerceAtLeast(0)
                                        castManager.seekTo(newPos)
                                    } else {
                                        val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                        exoPlayer.seekTo(newPos)
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Rewind 10 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Play/Pause button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NeliBluePrimary)
                                .clickable {
                                    if (isCasting) {
                                        castManager.togglePlayPause()
                                    } else {
                                        if (exoPlayer.isPlaying) {
                                            exoPlayer.pause()
                                        } else {
                                            exoPlayer.play()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isEffectivePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isEffectivePlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        if (!isLive) {
                            IconButton(
                                onClick = {
                                    if (isCasting) {
                                        val newPos = (castPosition + 10000).coerceAtMost(castDuration)
                                        castManager.seekTo(newPos)
                                    } else {
                                        val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                        exoPlayer.seekTo(newPos)
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Forward 10 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                // BOTTOM CONTROLS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (!isLive && !isControlsLocked) {
                        val activePosition = if (isCasting) castPosition else playbackPosition
                        val activeDuration = if (isCasting) castDuration else playbackDuration

                        // Slider Scrub Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatDuration(activePosition),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = if (activeDuration > 0) {
                                    (activePosition.toFloat() / activeDuration.toFloat()).coerceIn(0f, 1f)
                                } else 0f,
                                onValueChange = { fraction ->
                                    val targetMs = (fraction * activeDuration).toLong()
                                    if (isCasting) {
                                        castManager.seekTo(targetMs)
                                    } else {
                                        playbackPosition = targetMs
                                        exoPlayer.seekTo(targetMs)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = NeliCyanAccent,
                                    activeTrackColor = NeliBluePrimary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                )
                            )

                            Text(
                                text = formatDuration(activeDuration),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Bottom bar icons and quality pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lock Controls Button
                        IconButton(
                            onClick = { isControlsLocked = !isControlsLocked }
                        ) {
                            Icon(
                                imageVector = if (isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock Screen",
                                tint = if (isControlsLocked) NeliCyanAccent else Color.White
                            )
                        }

                        if (!isControlsLocked) {
                            // Speed button (cycles 1.0x -> 1.25x -> 1.5x -> 0.75x)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeliSurfaceElevated)
                                    .clickable {
                                        val nextSpeed = when (currentSpeed) {
                                            0.75f -> 1.0f
                                            1.0f -> 1.25f
                                            1.25f -> 1.5f
                                            1.5f -> 2.0f
                                            else -> 0.75f
                                        }
                                        currentSpeed = nextSpeed
                                        exoPlayer.playbackParameters = PlaybackParameters(nextSpeed)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Speed",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${currentSpeed}x",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Quality pills
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Auto", "720p", "1080p").forEach { quality ->
                                    val isSelected = selectedQuality == quality
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) NeliBluePrimary else NeliSurfaceElevated)
                                            .clickable { selectedQuality = quality }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = quality,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            // Fullscreen / Landscape Toggle
                            IconButton(
                                onClick = {
                                    isFullscreen = !isFullscreen
                                    if (isFullscreen) {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    } else {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
