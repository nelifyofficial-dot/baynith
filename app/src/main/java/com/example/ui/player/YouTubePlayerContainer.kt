package com.example.ui.player

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onRetry: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(true) }
    var rewindAnimationVisible by remember { mutableStateOf(false) }
    var forwardAnimationVisible by remember { mutableStateOf(false) }

    // Auto-hide YouTube controls overlay after 3.5s of inactivity while playing
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3500)
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
        // --- 1. VIDEO RENDERING ENGINE (Native ExoPlayer) ---
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

        // --- 2. SIGNATURE YOUTUBE DOUBLE-TAP GESTURE DETECTOR ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.42f) {
                                // Rewind 10s
                                onRewind10()
                                rewindAnimationVisible = true
                                showControls = true
                            } else if (offset.x > screenWidth * 0.58f) {
                                // Forward 10s
                                onForward10()
                                forwardAnimationVisible = true
                                showControls = true
                            } else {
                                onPlayPause()
                            }
                        }
                    )
                }
        )

        // --- 3. YOUTUBE DOUBLE-TAP SEEK RIPPLE OVERLAYS ---
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

        // --- 4. YOUTUBE CONTROLS OVERLAY ---
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
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // TOP BAR (Inside Player Container)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(if (isFullscreen) 48.dp else 38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(if (isFullscreen) 26.dp else 22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Settings Icon
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.size(if (isFullscreen) 44.dp else 34.dp)
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
                        CircularProgressIndicator(
                            color = YouTubeRed,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(if (isFullscreen) 56.dp else 44.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isFullscreen) 36.dp else 22.dp)
                        ) {
                            // Rewind 10s
                            if (!isLive) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isFullscreen) 52.dp else 42.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable { onRewind10() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Rewind 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isFullscreen) 30.dp else 24.dp)
                                    )
                                }
                            }

                            // Center Play / Pause
                            Box(
                                modifier = Modifier
                                    .size(if (isFullscreen) 68.dp else 54.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .clickable { onPlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isFullscreen) 42.dp else 32.dp)
                                )
                            }

                            // Forward 10s
                            if (!isLive) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isFullscreen) 52.dp else 42.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable { onForward10() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Forward 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isFullscreen) 30.dp else 24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // BOTTOM CONTROLS: YOUTUBE RED SCRUBBER TRACK + DURATION + CONTROLS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
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
                                thumbColor = YouTubeRed,
                                activeTrackColor = YouTubeRed,
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(YouTubeRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Volume Toggle
                            IconButton(
                                onClick = onToggleMute,
                                modifier = Modifier.size(if (isFullscreen) 40.dp else 32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isFullscreen) 22.dp else 18.dp)
                                )
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
}
