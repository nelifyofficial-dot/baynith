package com.example.update.model

/**
 * Encapsulates release metadata for an available NeliPlay application update.
 */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val apkFileName: String,
    val fileSize: Long = 0L,
    val forceUpdate: Boolean = false,
    val title: String = "New NeliPlay Update",
    val message: String = "A new version of NeliPlay is available.",
    val releaseNotes: List<String> = emptyList(),
    val sha256: String? = null,
    val releaseDate: String? = null
)
