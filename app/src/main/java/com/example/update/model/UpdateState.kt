package com.example.update.model

import java.io.File

/**
 * State machine representing the complete lifecycle of an update check, download, and installation.
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpToDate(val currentVersion: String, val currentVersionCode: Int) : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    data class Downloading(
        val info: UpdateInfo,
        val progress: Float, // 0.0f to 1.0f
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateState()
    data class Downloaded(val info: UpdateInfo, val apkFile: File) : UpdateState()
    data class Installing(val info: UpdateInfo, val apkFile: File) : UpdateState()
    data class PermissionRequired(val info: UpdateInfo, val apkFile: File) : UpdateState()
    data class Error(val message: String, val isManualCheck: Boolean = false) : UpdateState()
    data class NoInternet(val isManualCheck: Boolean = false) : UpdateState()
    data class NoRelease(val isManualCheck: Boolean = false) : UpdateState()
}
