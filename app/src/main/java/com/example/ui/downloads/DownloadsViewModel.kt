package com.example.ui.downloads

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NeliPlayDatabase
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.DownloadState
import com.example.data.repository.DownloadRepository
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class DownloadsUiState(
    val inProgressDownloads: List<DownloadEntity> = emptyList(),
    val completedDownloads: List<DownloadEntity> = emptyList(),
    val totalStorageUsedBytes: Long = 0L,
    val wifiOnly: Boolean = false
)

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = NeliPlayDatabase.getDatabase(application)
    private val downloadRepo = DownloadRepository(application, db.downloadDao())
    private val userDataRepo = UserDataRepository(application)
    private val context = application

    val uiState: StateFlow<DownloadsUiState> = combine(
        downloadRepo.getAllDownloads(),
        userDataRepo.wifiOnlyPreference
    ) { downloads, wifiOnly ->
        val inProgress = downloads.filter { it.status == DownloadState.DOWNLOADING || it.status == DownloadState.PAUSED || it.status == DownloadState.PENDING }
        val completed = downloads.filter { it.status == DownloadState.COMPLETED }

        var totalBytes = 0L
        for (item in completed) {
            if (item.localFilePath.isNotBlank()) {
                val f = File(item.localFilePath)
                if (f.exists()) totalBytes += f.length()
            }
        }

        DownloadsUiState(
            inProgressDownloads = inProgress,
            completedDownloads = completed,
            totalStorageUsedBytes = totalBytes,
            wifiOnly = wifiOnly
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DownloadsUiState()
    )

    fun pauseDownload(movieId: String) {
        downloadRepo.pauseDownload(movieId)
    }

    fun resumeDownload(item: DownloadEntity) {
        // Build minimal Movie instance to restart download
        val movie = com.example.data.model.Movie(
            id = item.movieId,
            title = item.title,
            posterPath = item.posterPath,
            streamUrl = item.streamUrl,
            downloadEnabled = true
        )
        downloadRepo.startDownload(movie, uiState.value.wifiOnly)
    }

    fun deleteDownload(movieId: String) {
        downloadRepo.deleteDownload(movieId)
    }

    fun toggleWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            userDataRepo.setWifiOnly(enabled)
        }
    }
}
