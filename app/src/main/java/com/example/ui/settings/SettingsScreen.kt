package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.components.NeliPlayLogo
import com.example.ui.downloads.formatBytes
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliGreenSuccess
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid
import com.example.update.model.UpdateState
import com.example.update.ui.UpdateViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState = updateViewModel?.state?.collectAsState()?.value ?: UpdateState.Idle
    val manualMessage = updateViewModel?.manualMessage?.collectAsState()?.value
    val snackbarHostState = remember { SnackbarHostState() }
    var showQualityDialog by remember { mutableStateOf(false) }

    LaunchedEffect(manualMessage) {
        if (!manualMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(manualMessage)
            updateViewModel?.clearManualMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeliVoid)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = NeliVoid,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Playback Preferences
            item {
                SettingsSectionTitle(title = "Playback Preferences")
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeliSurface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsClickableRow(
                            icon = Icons.Default.HighQuality,
                            title = "Default Streaming Quality",
                            subtitle = uiState.defaultQuality,
                            onClick = { showQualityDialog = true }
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            icon = Icons.Default.PlayCircleOutline,
                            title = "Auto-play Next",
                            subtitle = "Automatically stream subsequent recommendations",
                            checked = uiState.autoPlay,
                            onCheckedChange = { viewModel.toggleAutoPlay(it) }
                        )
                    }
                }
            }

            // Offline & Downloads
            item {
                SettingsSectionTitle(title = "Downloads & Offline")
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeliSurface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsSwitchRow(
                            icon = Icons.Default.Wifi,
                            title = "Download on Wi-Fi Only",
                            subtitle = "Prevent downloads over cellular data connections",
                            checked = uiState.wifiOnly,
                            onCheckedChange = { viewModel.toggleWifiOnly(it) }
                        )

                        SettingsDivider()

                        SettingsClickableRow(
                            icon = Icons.Default.Storage,
                            title = "Storage Location",
                            subtitle = "Internal App-Scoped Media Storage",
                            onClick = {}
                        )
                    }
                }
            }

            // Storage & Data
            item {
                SettingsSectionTitle(title = "Data & Notifications")
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeliSurface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsClickableRow(
                            icon = Icons.Default.CleaningServices,
                            title = "Clear Cache",
                            subtitle = "${formatBytes(uiState.cacheSizeBytes)} cached files",
                            onClick = { viewModel.clearCache() }
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            icon = Icons.Default.Notifications,
                            title = "Push Notifications",
                            subtitle = "New releases, featured movies, and live events",
                            checked = uiState.notifications,
                            onCheckedChange = { viewModel.toggleNotifications(it) }
                        )
                    }
                }
            }

            // About & Updates
            item {
                SettingsSectionTitle(title = "About & Updates")
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeliSurface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsClickableRow(
                            icon = Icons.Default.Info,
                            title = "Current Version",
                            subtitle = "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            onClick = {}
                        )

                        SettingsDivider()

                        val updateSubtitle = when (val s = updateState) {
                            is UpdateState.Checking -> "Checking for updates..."
                            is UpdateState.UpdateAvailable -> "Update available: v${s.info.versionName} • Tap to view"
                            is UpdateState.Downloading -> {
                                val pct = if (s.progress >= 0f) "${(s.progress * 100).toInt()}%" else "..."
                                "Downloading update ($pct)"
                            }
                            is UpdateState.Downloaded -> "Package downloaded • Tap to install"
                            is UpdateState.Installing -> "Installing update..."
                            is UpdateState.PermissionRequired -> "Permission required • Tap to enable"
                            is UpdateState.UpToDate -> "You are using the latest version of NeliPlay."
                            is UpdateState.NoRelease -> "You are using the latest version of NeliPlay."
                            is UpdateState.NoInternet -> "Unable to check for updates (No internet)"
                            is UpdateState.Error -> s.message
                            else -> manualMessage ?: "Tap to check for latest release"
                        }

                        SettingsClickableRow(
                            icon = Icons.Default.SystemUpdate,
                            title = "Check for Updates",
                            subtitle = updateSubtitle,
                            onClick = {
                                if (updateState is UpdateState.UpdateAvailable ||
                                    updateState is UpdateState.Downloaded ||
                                    updateState is UpdateState.Downloading ||
                                    updateState is UpdateState.PermissionRequired
                                ) {
                                    updateViewModel?.showUpdateDialog()
                                } else {
                                    updateViewModel?.checkForUpdates(isManual = true)
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = {
                Text("Select Default Streaming Quality", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Auto (Recommended)", "1080p (Full HD)", "720p (HD)", "480p (Data Saver)").forEach { q ->
                        val cleanQ = q.split(" ").first()
                        val isSelected = uiState.defaultQuality == cleanQ
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeliBluePrimary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    viewModel.setQuality(cleanQ)
                                    showQualityDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = q,
                                color = if (isSelected) NeliCyanAccent else Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Done", color = NeliCyanAccent)
                }
            },
            containerColor = NeliSurfaceElevated
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = NeliTextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeliCyanAccent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = NeliTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeliCyanAccent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = NeliTextSecondary,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeliBluePrimary,
                uncheckedThumbColor = NeliTextSecondary,
                uncheckedTrackColor = NeliSurfaceVariant
            )
        )
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
            .background(NeliSurfaceVariant.copy(alpha = 0.5f))
    )
}
