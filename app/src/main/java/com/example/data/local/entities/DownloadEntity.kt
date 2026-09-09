package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadState {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val movieId: String,
    val title: String,
    val posterPath: String,
    val streamUrl: String,
    val localFilePath: String = "",
    val status: DownloadState = DownloadState.PENDING,
    val progress: Float = 0f, // 0.0f to 1.0f
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
