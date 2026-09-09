package com.example.update.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.ui.downloads.formatBytes
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliGreenSuccess
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid
import com.example.update.model.UpdateInfo
import com.example.update.model.UpdateState

@Composable
fun UpdateDialog(
    state: UpdateState,
    onUpdateClick: (UpdateInfo) -> Unit,
    onCancelDownload: () -> Unit,
    onRetryInstall: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val info = when (state) {
        is UpdateState.UpdateAvailable -> state.info
        is UpdateState.Downloading -> state.info
        is UpdateState.Downloaded -> state.info
        is UpdateState.Installing -> state.info
        is UpdateState.PermissionRequired -> state.info
        else -> null
    } ?: return

    val isForce = info.forceUpdate

    Dialog(
        onDismissRequest = {
            if (!isForce && state !is UpdateState.Downloading && state !is UpdateState.Installing) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForce && state !is UpdateState.Downloading && state !is UpdateState.Installing,
            dismissOnClickOutside = !isForce && state !is UpdateState.Downloading && state !is UpdateState.Installing
        )
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = NeliSurfaceElevated,
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = NeliCyanAccent.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(22.dp)
                )
                .testTag("neliplay_update_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row with glowing icon badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(NeliBluePrimary, NeliCyanAccent.copy(alpha = 0.8f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = info.title.ifBlank { "New NeliPlay Update" },
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Version ${info.versionName} is available",
                            color = NeliCyanAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Version Badge Comparison
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeliSurface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT",
                            color = NeliTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = NeliCyanAccent,
                        modifier = Modifier.size(18.dp)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LATEST",
                            color = NeliCyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "v${info.versionName} (${info.versionCode})",
                            color = NeliCyanAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Release notes section
                if (info.releaseNotes.isNotEmpty()) {
                    Text(
                        text = "What's new:",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        info.releaseNotes.forEach { note ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp, end = 8.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeliCyanAccent)
                                )
                                Text(
                                    text = note,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Dynamic state display: Downloading / PermissionRequired / Installing / Idle
                when (state) {
                    is UpdateState.Downloading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeliSurface)
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading update...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                val pctText = if (state.progress >= 0f) {
                                    "${(state.progress * 100).toInt()}%"
                                } else {
                                    "..."
                                }
                                Text(
                                    text = pctText,
                                    color = NeliCyanAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (state.progress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = NeliCyanAccent,
                                    trackColor = NeliSurfaceVariant
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = NeliCyanAccent,
                                    trackColor = NeliSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatBytes(state.bytesDownloaded),
                                    color = NeliTextSecondary,
                                    fontSize = 11.sp
                                )
                                if (state.totalBytes > 0) {
                                    Text(
                                        text = formatBytes(state.totalBytes),
                                        color = NeliTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isForce) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onCancelDownload) {
                                    Text("Cancel", color = NeliTextSecondary)
                                }
                            }
                        }
                    }

                    is UpdateState.PermissionRequired -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeliSurface)
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = NeliCyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Installation Permission Needed",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Android requires permission to install APK updates from NeliPlay. Tap below to enable 'Install unknown apps' in settings, then return here to complete setup.",
                                color = NeliTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onOpenPermissionSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = NeliBluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("allow_installation_button")
                            ) {
                                Text(
                                    text = "ALLOW INSTALLATION",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = onRetryInstall,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("I Have Allowed Permission", color = NeliCyanAccent, fontSize = 12.sp)
                            }
                        }
                    }

                    is UpdateState.Installing -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeliSurface)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = NeliCyanAccent,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Opening package installer...",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }

                    is UpdateState.Downloaded -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeliGreenSuccess.copy(alpha = 0.15f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeliGreenSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Package downloaded. Ready to install.",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onRetryInstall,
                            colors = ButtonDefaults.buttonColors(containerColor = NeliBluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("install_now_button")
                        ) {
                            Text(
                                text = "INSTALL NOW",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    else -> {
                        // Standard UpdateAvailable state with action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isForce) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = NeliTextSecondary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("update_later_button")
                                ) {
                                    Text("LATER", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Button(
                                onClick = { onUpdateClick(info) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeliBluePrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(if (isForce) 1f else 1.2f)
                                    .testTag("update_now_button")
                            ) {
                                Text(
                                    text = "UPDATE NOW",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
