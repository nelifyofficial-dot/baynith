package com.example.update.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.update.manager.UpdateManager
import com.example.update.model.UpdateInfo
import com.example.update.model.UpdateState
import com.example.update.repository.CheckResult
import com.example.update.repository.UpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application
    private val repository = UpdateRepository(application)
    private val updateManager = UpdateManager(application)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _isDialogVisible = MutableStateFlow(false)
    val isDialogVisible: StateFlow<Boolean> = _isDialogVisible.asStateFlow()

    private val _manualMessage = MutableStateFlow<String?>(null)
    val manualMessage: StateFlow<String?> = _manualMessage.asStateFlow()

    private var downloadJob: Job? = null

    companion object {
        private const val TAG = "UpdateViewModel"
    }

    /**
     * Checks for updates from the official GitHub Releases API.
     * @param isManual When true, ignores automatic check cooldown and produces user-facing feedback.
     */
    fun checkForUpdates(isManual: Boolean = false) {
        if (!isManual && !updateManager.shouldPerformAutoCheck()) {
            Log.d(TAG, "Skipping auto update check: cooldown active.")
            return
        }

        // Avoid interrupting active download or installation flows
        val currentState = _state.value
        if (currentState is UpdateState.Downloading || currentState is UpdateState.Installing) {
            if (isManual) {
                _isDialogVisible.value = true
            }
            return
        }

        _state.value = UpdateState.Checking
        if (isManual) {
            _manualMessage.value = "Checking for updates..."
        }

        viewModelScope.launch {
            when (val result = repository.fetchLatestRelease()) {
                is CheckResult.Available -> {
                    Log.d(TAG, "Update available: v${result.info.versionName} (build ${result.info.versionCode})")
                    _state.value = UpdateState.UpdateAvailable(result.info)
                    _isDialogVisible.value = true
                    _manualMessage.value = null
                    updateManager.recordAutoCheckPerformed()

                    if (!isManual) {
                        updateManager.showUpdateNotification(result.info)
                    }
                }

                is CheckResult.UpToDate -> {
                    Log.d(TAG, "App is up to date: v${BuildConfig.VERSION_NAME}")
                    _state.value = UpdateState.UpToDate(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
                    updateManager.recordAutoCheckPerformed()
                    if (isManual) {
                        _manualMessage.value = "You are using the latest version of NeliPlay."
                    }
                }

                is CheckResult.NoRelease -> {
                    Log.d(TAG, "No GitHub release found.")
                    _state.value = UpdateState.NoRelease(isManualCheck = isManual)
                    updateManager.recordAutoCheckPerformed()
                    if (isManual) {
                        _manualMessage.value = "You are using the latest version of NeliPlay."
                    }
                }

                is CheckResult.NoInternet -> {
                    Log.d(TAG, "No internet connection during update check.")
                    _state.value = UpdateState.NoInternet(isManualCheck = isManual)
                    if (isManual) {
                        _manualMessage.value = "Unable to check for updates. Please check your internet connection."
                    }
                }

                is CheckResult.Error -> {
                    Log.w(TAG, "Update check error: ${result.message}")
                    _state.value = UpdateState.Error(result.message, isManualCheck = isManual)
                    if (isManual) {
                        _manualMessage.value = result.message
                    }
                }
            }
        }
    }

    /**
     * Starts downloading the update package with progress reporting.
     */
    fun startDownload(info: UpdateInfo) {
        downloadJob?.cancel()

        _state.value = UpdateState.Downloading(
            info = info,
            progress = 0f,
            bytesDownloaded = 0L,
            totalBytes = info.fileSize
        )
        _isDialogVisible.value = true

        downloadJob = viewModelScope.launch {
            val result = updateManager.downloadApk(info) { progress, downloaded, total ->
                _state.value = UpdateState.Downloading(
                    info = info,
                    progress = progress,
                    bytesDownloaded = downloaded,
                    totalBytes = total
                )
            }

            result.fold(
                onSuccess = { apkFile ->
                    Log.d(TAG, "Download completed successfully: ${apkFile.absolutePath}")
                    _state.value = UpdateState.Downloaded(info, apkFile)

                    // Check unknown sources installation permission
                    if (updateManager.canInstallPackages()) {
                        triggerInstallation(info, apkFile)
                    } else {
                        _state.value = UpdateState.PermissionRequired(info, apkFile)
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Update download failed: ${error.message}", error)
                    _state.value = UpdateState.Error("Update download failed. Please try again.")
                }
            )
        }
    }

    /**
     * Cancels an ongoing download.
     */
    fun cancelDownload() {
        val currentState = _state.value
        if (currentState is UpdateState.Downloading && !currentState.info.forceUpdate) {
            downloadJob?.cancel()
            _state.value = UpdateState.UpdateAvailable(currentState.info)
        }
    }

    /**
     * Invokes the Android OS package installer.
     */
    fun triggerInstallation(info: UpdateInfo, apkFile: File) {
        if (!updateManager.canInstallPackages()) {
            _state.value = UpdateState.PermissionRequired(info, apkFile)
            return
        }

        _state.value = UpdateState.Installing(info, apkFile)
        val installResult = updateManager.startInstallation(apkFile)

        if (installResult.isFailure) {
            _state.value = UpdateState.Error("Unable to install the update. Please try again.")
        }
    }

    /**
     * Directs the user to system settings to authorize unknown app installation.
     */
    fun openUnknownAppSourcesSettings() {
        try {
            val intent = updateManager.getUnknownAppSourcesSettingsIntent()
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening unknown app sources settings: ${e.message}")
        }
    }

    /**
     * Called on Activity resume to check if permission has been granted.
     */
    fun onResumeCheckPermissions() {
        val currentState = _state.value
        if (currentState is UpdateState.PermissionRequired) {
            if (updateManager.canInstallPackages()) {
                Log.d(TAG, "Unknown sources permission now granted! Proceeding with installation.")
                triggerInstallation(currentState.info, currentState.apkFile)
            }
        }
    }

    /**
     * Shows update dialog (e.g. from notification or settings).
     */
    fun showUpdateDialog() {
        _isDialogVisible.value = true
    }

    /**
     * Dismisses the dialog if not a mandatory force update.
     */
    fun dismissDialog() {
        val currentState = _state.value
        val isForced = when (currentState) {
            is UpdateState.UpdateAvailable -> currentState.info.forceUpdate
            is UpdateState.Downloading -> currentState.info.forceUpdate
            is UpdateState.Downloaded -> currentState.info.forceUpdate
            is UpdateState.Installing -> currentState.info.forceUpdate
            is UpdateState.PermissionRequired -> currentState.info.forceUpdate
            else -> false
        }

        if (!isForced) {
            _isDialogVisible.value = false
        }
    }

    fun clearManualMessage() {
        _manualMessage.value = null
    }
}
