package com.example.ui.player

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VideoLibrary
import com.example.cast.NeliPlayCastButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.Episode
import com.example.ui.components.resolveImageUrl
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import kotlinx.coroutines.delay

val YouTubeRed = Color(0xFFFF0000)

@Composable
fun YouTubePlayerContainer(
    streamUrl: String,
    posterUrl: String,
    title: String,
    isLive: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isMuted: Boolean,
    playerError: String?,
    playbackPosition: Long,
    playbackDuration: Long,
    isFullscreen: Boolean,
    exoPlayer: ExoPlayer,
    playerView: PlayerView,
    isMovie: Boolean = false,
    isSeries: Boolean = false,
    seasons: List<Int> = emptyList(),
    selectedSeason: Int = 1,
    currentSeasonEpisodes: List<Episode> = emptyList(),
    currentEpisodeId: String? = null,
    onSelectSeason: ((Int) -> Unit)? = null,
    onSelectEpisode: ((Episode) -> Unit)? = null,
    previousEpisode: Episode? = null,
    onPlayPreviousEpisode: (() -> Unit)? = null,
    nextEpisode: Episode? = null,
    onPlayNextEpisode: (() -> Unit)? = null,
    autoSkipIntro: Boolean = true,
    showAutoSkippedNotice: Boolean = false,
    onDismissAutoSkipNotice: (() -> Unit)? = null,
    onToggleAutoSkipIntro: (() -> Unit)? = null,
    onSkipIntro: (() -> Unit)? = null,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onRetry: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onEnterPip: (() -> Unit)? = null,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(true) }
    var rewindAnimationVisible by remember { mutableStateOf(false) }
    var forwardAnimationVisible by remember { mutableStateOf(false) }
    var showVodDrawer by remember { mutableStateOf(false) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var volumeLevel by remember { mutableFloatStateOf(exoPlayer.volume.coerceIn(0f, 1f)) }

    // Auto-hide controls overlay after 3.8s of inactivity while playing
    LaunchedEffect(showControls, isPlaying, showVodDrawer) {
        if (showControls && isPlaying && !showVodDrawer) {
            delay(3800)
            showControls = false
        }
    }

    // Auto-hide seek ripples
    LaunchedEffect(rewindAnimationVisible) {
        if (rewindAnimationVisible) {
            delay(750)
            rewindAnimationVisible = false
        }
    }
    LaunchedEffect(forwardAnimationVisible) {
        if (forwardAnimationVisible) {
            delay(750)
            forwardAnimationVisible = false
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .testTag("youtube_player_container")
    ) {
        // --- 1. VIDEO RENDERING ENGINE (Native ExoPlayer via SurfaceView) ---
        AndroidView(
            factory = {
                (playerView.parent as? ViewGroup)?.removeView(playerView)
                playerView
            },
            update = { view ->
                if (view.player != exoPlayer) {
                    view.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- 2. GESTURE DETECTOR (Tap to show/hide, Double-tap to seek 10s) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isControlsLocked) {
                    detectTapGestures(
                        onTap = {
                            if (!isControlsLocked) {
                                showControls = !showControls
                            } else {
                                // If locked, briefly reveal unlock button
                                showControls = true
                            }
                        },
                        onDoubleTap = { offset ->
                            if (!isControlsLocked) {
                                val screenWidth = size.width
                                if (offset.x < screenWidth * 0.42f) {
                                    onRewind10()
                                    rewindAnimationVisible = true
                                    showControls = true
                                } else if (offset.x > screenWidth * 0.58f) {
                                    onForward10()
                                    forwardAnimationVisible = true
                                    showControls = true
                                } else {
                                    onPlayPause()
                                }
                            }
                        }
                    )
                }
        )

        // --- 3. DOUBLE-TAP SEEK RIPPLE OVERLAYS ---
        AnimatedVisibility(
            visible = rewindAnimationVisible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.38f)
                    .background(
                        Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(topEnd = 120.dp, bottomEnd = 120.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (isFullscreen) 44.dp else 34.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-10 seconds",
                        color = Color.White,
                        fontSize = if (isFullscreen) 13.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = forwardAnimationVisible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.38f)
                    .background(
                        Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(topStart = 120.dp, bottomStart = 120.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (isFullscreen) 44.dp else 34.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+10 seconds",
                        color = Color.White,
                        fontSize = if (isFullscreen) 13.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- 4. CONTROLS OVERLAY ---
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.78f),
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            ) {
                // If controls are locked, only show unlock button
                if (isControlsLocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 24.dp)
                    ) {
                        IconButton(
                            onClick = { isControlsLocked = false },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, NeliCyanAccent, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Unlock Controls",
                                tint = NeliCyanAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    // TOP BAR (Inside Player Container)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(if (isFullscreen) 46.dp else 38.dp)
                                    .testTag("player_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isFullscreen) 26.dp else 22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = if (isFullscreen) 16.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Auto-Skip Toggle Chip (for Movies)
                            if (isMovie) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (autoSkipIntro) NeliCyanAccent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.12f))
                                        .border(1.dp, if (autoSkipIntro) NeliCyanAccent.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                        .clickable { onToggleAutoSkipIntro?.invoke() }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.FastForward,
                                            contentDescription = "Auto-Skip 5:30",
                                            tint = if (autoSkipIntro) NeliCyanAccent else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (autoSkipIntro) "Auto-Skip 5:30" else "Auto-Skip Off",
                                            color = if (autoSkipIntro) Color.White else Color.White.copy(alpha = 0.7f),
                                            fontSize = if (isFullscreen) 11.sp else 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Cast to TV Button
                            NeliPlayCastButton(
                                modifier = Modifier.size(if (isFullscreen) 42.dp else 34.dp),
                                sizeDp = if (isFullscreen) 24 else 20
                            )

                            // Lock Screen Controls
                            IconButton(
                                onClick = { isControlsLocked = true },
                                modifier = Modifier.size(if (isFullscreen) 42.dp else 34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Lock Controls",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(if (isFullscreen) 22.dp else 18.dp)
                                )
                            }

                            // Settings Icon
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.size(if (isFullscreen) 42.dp else 34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isFullscreen) 24.dp else 18.dp)
                                )
                            }
                        }
                    }

                    // AUTO-SKIPPED NOTICE BANNER
                    AnimatedVisibility(
                        visible = showAutoSkippedNotice,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = if (isFullscreen) 56.dp else 42.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xE60F172A))
                                .border(1.dp, NeliCyanAccent.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = null,
                                    tint = NeliCyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Intro Auto-Skipped (Started at 05:30)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Undo",
                                    color = NeliCyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            onSeek(0L)
                                            onDismissAutoSkipNotice?.invoke()
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // CENTER CONTROLS: REWIND 10s, PLAY/PAUSE, FORWARD 10s
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        if (playerError != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = playerError,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(containerColor = NeliBluePrimary),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (isBuffering) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = NeliCyanAccent,
                                    strokeWidth = 3.5.dp,
                                    modifier = Modifier.size(if (isFullscreen) 56.dp else 44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Inapakia...",
                                    color = Color.White,
                                    fontSize = if (isFullscreen) 13.sp else 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (isFullscreen) 28.dp else 16.dp)
                            ) {
                                // Previous Episode Button (for Series)
                                if (isSeries) {
                                    val hasPrev = previousEpisode != null
                                    Box(
                                        modifier = Modifier
                                            .size(if (isFullscreen) 50.dp else 40.dp)
                                            .clip(CircleShape)
                                            .background(if (hasPrev) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.25f))
                                            .border(1.dp, if (hasPrev) Color.White.copy(alpha = 0.35f) else Color.Transparent, CircleShape)
                                            .clickable(enabled = hasPrev) { onPlayPreviousEpisode?.invoke() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = "Previous Episode",
                                            tint = if (hasPrev) Color.White else Color.White.copy(alpha = 0.35f),
                                            modifier = Modifier.size(if (isFullscreen) 28.dp else 22.dp)
                                        )
                                    }
                                }

                                // Rewind 10s
                                if (!isLive) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isFullscreen) 56.dp else 44.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.55f))
                                            .clickable { onRewind10() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = "Rewind 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(if (isFullscreen) 32.dp else 24.dp)
                                        )
                                    }
                                }

                                // Center Play / Pause
                                Box(
                                    modifier = Modifier
                                        .size(if (isFullscreen) 72.dp else 56.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .border(1.5.dp, NeliCyanAccent.copy(alpha = 0.6f), CircleShape)
                                        .clickable { onPlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isFullscreen) 44.dp else 34.dp)
                                    )
                                }

                                // Forward 10s
                                if (!isLive) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isFullscreen) 56.dp else 44.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.55f))
                                            .clickable { onForward10() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = "Forward 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(if (isFullscreen) 32.dp else 24.dp)
                                        )
                                    }
                                }

                                // Next Episode Button (for Series)
                                if (isSeries) {
                                    val hasNext = nextEpisode != null
                                    Box(
                                        modifier = Modifier
                                            .size(if (isFullscreen) 50.dp else 40.dp)
                                            .clip(CircleShape)
                                            .background(if (hasNext) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.25f))
                                            .border(1.dp, if (hasNext) Color.White.copy(alpha = 0.35f) else Color.Transparent, CircleShape)
                                            .clickable(enabled = hasNext) { onPlayNextEpisode?.invoke() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Next Episode",
                                            tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.35f),
                                            modifier = Modifier.size(if (isFullscreen) 28.dp else 22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // BOTTOM CONTROLS: SLEEK WHITE/CYAN SCRUBBER TRACK + VOLUME + CONTROLS
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Skip Intro (05:30) Quick Action Button (for Movies in first 5m 30s)
                        if (!isLive && isMovie && playbackPosition < 330_000L && playbackDuration > 330_000L) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF0F172A).copy(alpha = 0.9f))
                                        .border(1.dp, NeliCyanAccent.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                                        .clickable {
                                            onSkipIntro?.invoke() ?: onSeek(330_000L)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.FastForward,
                                            contentDescription = "Skip Intro",
                                            tint = NeliCyanAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Skip Intro (05:30)",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

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
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isFullscreen) 24.dp else 16.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: Play/Pause, Rewind, Forward, Volume slider & Time
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = onPlayPause,
                                    modifier = Modifier.size(if (isFullscreen) 38.dp else 30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isFullscreen) 24.dp else 20.dp)
                                    )
                                }

                                if (!isLive) {
                                    IconButton(
                                        onClick = onRewind10,
                                        modifier = Modifier.size(if (isFullscreen) 36.dp else 28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = "Rewind 10",
                                            tint = Color.White,
                                            modifier = Modifier.size(if (isFullscreen) 22.dp else 18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = onForward10,
                                        modifier = Modifier.size(if (isFullscreen) 36.dp else 28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = "Forward 10",
                                            tint = Color.White,
                                            modifier = Modifier.size(if (isFullscreen) 22.dp else 18.dp)
                                        )
                                    }
                                }

                                // Volume Icon & Slider (as seen in screenshot)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = onToggleMute,
                                        modifier = Modifier.size(if (isFullscreen) 36.dp else 28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isMuted || volumeLevel <= 0.01f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Volume",
                                            tint = Color.White,
                                            modifier = Modifier.size(if (isFullscreen) 20.dp else 16.dp)
                                        )
                                    }

                                    Slider(
                                        value = if (isMuted) 0f else volumeLevel,
                                        onValueChange = { newVol ->
                                            volumeLevel = newVol
                                            exoPlayer.volume = newVol
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .width(if (isFullscreen) 80.dp else 55.dp)
                                            .height(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                if (isLive) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(YouTubeRed)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LIVE",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "${formatDuration(currentPos)} / ${formatDuration(playbackDuration)}",
                                        color = Color.White,
                                        fontSize = if (isFullscreen) 13.sp else 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Right side: Episodes (VOD), PiP, Fullscreen
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // For Series: Video on Demand (VOD) Episodes Button
                                if (isSeries && currentSeasonEpisodes.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (showVodDrawer) NeliCyanAccent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, if (showVodDrawer) NeliCyanAccent else Color.White.copy(alpha = 0.28f)),
                                        modifier = Modifier
                                            .clickable { showVodDrawer = !showVodDrawer }
                                            .testTag("vod_episodes_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VideoLibrary,
                                                contentDescription = "Episodes VOD",
                                                tint = if (showVodDrawer) NeliCyanAccent else Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Episodes",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Picture-in-Picture Toggle
                                if (onEnterPip != null) {
                                    IconButton(
                                        onClick = onEnterPip,
                                        modifier = Modifier.size(if (isFullscreen) 40.dp else 32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureInPicture,
                                            contentDescription = "Picture in Picture",
                                            tint = Color.White,
                                            modifier = Modifier.size(if (isFullscreen) 22.dp else 18.dp)
                                        )
                                    }
                                }

                                // Fullscreen Toggle
                                IconButton(
                                    onClick = onToggleFullscreen,
                                    modifier = Modifier.size(if (isFullscreen) 40.dp else 32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isFullscreen) 24.dp else 20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. SERIES VIDEO ON DEMAND (VOD) DRAWER OVERLAY ---
        AnimatedVisibility(
            visible = showVodDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.46f)
                    .background(Color(0xF20A0F1D))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
                    .clickable(enabled = false) {}
                    .testTag("vod_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Header: "Episodes (VOD)" and Close '✕'
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = NeliCyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Episodes (VOD)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { showVodDrawer = false },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Season chips (if multiple seasons)
                    if (seasons.size > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(seasons) { seasonNum ->
                                val isSel = seasonNum == selectedSeason
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) NeliCyanAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, if (isSel) NeliCyanAccent else Color.Transparent),
                                    modifier = Modifier.clickable { onSelectSeason?.invoke(seasonNum) }
                                ) {
                                    Text(
                                        text = "Season $seasonNum",
                                        color = if (isSel) NeliCyanAccent else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // List of episodes for current season
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(currentSeasonEpisodes) { ep ->
                            val isCurrent = ep.id == currentEpisodeId
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) NeliCyanAccent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) NeliCyanAccent else Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectEpisode?.invoke(ep)
                                        showVodDrawer = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(76.dp)
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black)
                                    ) {
                                        if (ep.stillPath.isNotBlank()) {
                                            AsyncImage(
                                                model = resolveImageUrl(ep.stillPath),
                                                contentDescription = ep.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.45f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = NeliCyanAccent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "EP ${ep.episodeNumber}",
                                                color = if (isCurrent) NeliCyanAccent else Color.White.copy(alpha = 0.7f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isCurrent) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(NeliCyanAccent)
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "PLAYING",
                                                        color = Color.Black,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = ep.title.ifBlank { "Episode ${ep.episodeNumber}" },
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if ((ep.runtime ?: 0) > 0) {
                                            Text(
                                                text = "${ep.runtime} min",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 10.sp
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

        // --- 6. UP NEXT EPISODE AUTO-PLAY PROMPT (Near episode end) ---
        if (isSeries && nextEpisode != null && playbackDuration > 20000L && playbackPosition > (playbackDuration - 18000L)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 70.dp, end = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xF00F172A))
                    .border(1.dp, NeliCyanAccent, RoundedCornerShape(14.dp))
                    .clickable { onPlayNextEpisode?.invoke() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Episode",
                        tint = NeliCyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("UP NEXT", color = NeliCyanAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = nextEpisode.title.ifBlank { "Episode ${nextEpisode.episodeNumber}" },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeliCyanAccent)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("PLAY", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
