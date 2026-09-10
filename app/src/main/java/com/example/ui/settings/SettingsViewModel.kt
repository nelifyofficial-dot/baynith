package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SettingsUiState(
    val wifiOnly: Boolean = false,
    val autoPlay: Boolean = true,
    val autoSkipIntro: Boolean = false,
    val notifications: Boolean = true,
    val defaultQuality: String = "Auto",
    val cacheSizeBytes: Long = 0L
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val userDataRepo = UserDataRepository(application)
    private val context = application

    private val _cacheSize = MutableStateFlow(0L)

    private val playbackPrefsFlow = combine(
        userDataRepo.autoPlayPreference,
        userDataRepo.autoSkipIntroPreference,
        userDataRepo.videoQualityPreference
    ) { autoPlay, autoSkip, quality ->
        Triple(autoPlay, autoSkip, quality)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        userDataRepo.wifiOnlyPreference,
        userDataRepo.notificationsPreference,
        playbackPrefsFlow,
        _cacheSize
    ) { wifiOnly, notifs, (autoPlay, autoSkip, quality), cacheSize ->
        SettingsUiState(
            wifiOnly = wifiOnly,
            autoPlay = autoPlay,
            autoSkipIntro = autoSkip,
            notifications = notifs,
            defaultQuality = quality,
            cacheSizeBytes = cacheSize
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        calculateCacheSize()
    }

    private fun calculateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            val size = getDirSize(cacheDir)
            _cacheSize.value = size
        }
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.cacheDir.deleteRecursion()
            } catch (e: Exception) {
                // ignore
            }
            calculateCacheSize()
        }
    }

    private fun File.deleteRecursion() {
        if (isDirectory) {
            listFiles()?.forEach { it.deleteRecursion() }
        }
        delete()
    }

    fun toggleWifiOnly(enabled: Boolean) {
        viewModelScope.launch { userDataRepo.setWifiOnly(enabled) }
    }

    fun toggleAutoPlay(enabled: Boolean) {
        viewModelScope.launch { userDataRepo.setAutoPlay(enabled) }
    }

    fun toggleAutoSkipIntro(enabled: Boolean) {
        viewModelScope.launch { userDataRepo.setAutoSkipIntro(enabled) }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch { userDataRepo.setNotifications(enabled) }
    }

    fun setQuality(quality: String) {
        viewModelScope.launch { userDataRepo.setVideoQuality(quality) }
    }
}
