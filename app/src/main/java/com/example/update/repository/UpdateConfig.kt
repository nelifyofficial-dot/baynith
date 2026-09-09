package com.example.update.repository

/**
 * Configuration constants for the NeliPlay update subsystem.
 */
object UpdateConfig {
    const val GITHUB_OWNER = "nelifyofficial-dot"
    const val GITHUB_REPO = "neliplay-updates"
    const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    // Cooldown intervals to prevent excessive GitHub API calls
    const val AUTO_CHECK_COOLDOWN_MS = 30 * 60 * 1000L // 30 minutes
    const val NOTIFICATION_COOLDOWN_MS = 24 * 60 * 60 * 1000L // 24 hours between update notifications
}
