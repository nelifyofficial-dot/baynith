package com.example.update.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.MainActivity
import com.example.R
import com.example.update.model.UpdateInfo
import com.example.update.repository.UpdateConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class UpdateManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("neliplay_update_prefs", Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        private const val TAG = "UpdateManager"
        const val UPDATE_CHANNEL_ID = "neliplay_updates_channel"
        private const val NOTIFICATION_ID = 2001

        private const val PREF_LAST_CHECK_TIME = "pref_last_update_check_time"
        private const val PREF_LAST_NOTIFIED_VERSION = "pref_last_notified_version"
        private const val PREF_LAST_NOTIFICATION_TIME = "pref_last_notification_time"
        private const val PREF_ACKNOWLEDGED_VERSION = "pref_acknowledged_version"

        const val EXTRA_SHOW_UPDATE_DIALOG = "EXTRA_SHOW_UPDATE_DIALOG"
    }

    init {
        createUpdateNotificationChannel()
    }

    /**
     * Creates notification channel for update alerts on Android 8.0+.
     */
    fun createUpdateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.update_channel_name)
            val descriptionText = context.getString(R.string.update_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(UPDATE_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Evaluates if enough time has elapsed to perform an automatic update check.
     */
    fun shouldPerformAutoCheck(): Boolean {
        val lastCheck = prefs.getLong(PREF_LAST_CHECK_TIME, 0L)
        val now = System.currentTimeMillis()
        return (now - lastCheck) >= UpdateConfig.AUTO_CHECK_COOLDOWN_MS
    }

    fun recordAutoCheckPerformed() {
        prefs.edit().putLong(PREF_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
    }

    fun getAcknowledgedVersion(): String =
        prefs.getString(PREF_ACKNOWLEDGED_VERSION, "") ?: ""

    fun setAcknowledgedVersion(version: String) {
        prefs.edit().putString(PREF_ACKNOWLEDGED_VERSION, version).apply()
    }

    /**
     * Checks if NeliPlay has permission to install unknown apps (Android 8.0+).
     */
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Returns an Intent to take the user directly to the system settings for unknown app installation.
     */
    fun getUnknownAppSourcesSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Downloads the APK file with real-time byte progress callbacks.
     */
    suspend fun downloadApk(
        info: UpdateInfo,
        onProgress: (progress: Float, downloaded: Long, total: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        }

        // Clean previous APK files to prevent space leaks
        updatesDir.listFiles()?.forEach { oldFile ->
            if (oldFile.name.endsWith(".apk", ignoreCase = true) || oldFile.name.endsWith(".tmp", ignoreCase = true)) {
                try { oldFile.delete() } catch (_: Exception) {}
            }
        }

        val targetApkName = if (info.apkFileName.isNotBlank()) info.apkFileName else "NeliPlay-v${info.versionName}.apk"
        val destinationFile = File(updatesDir, targetApkName)
        val tempFile = File(updatesDir, "$targetApkName.tmp")

        try {
            val request = Request.Builder()
                .url(info.downloadUrl)
                .header("User-Agent", "NeliPlay-Android")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Server returned HTTP ${response.code} when downloading update."))
            }

            val body = response.body ?: return@withContext Result.failure(IOException("Empty download response body."))
            val contentLength = body.contentLength().let { if (it > 0) it else info.fileSize }

            body.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var totalRead: Long = 0
                    var lastReportedTime = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (!coroutineContext.isActive) {
                            tempFile.delete()
                            throw CancellationException("Update download was cancelled.")
                        }

                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val currentTime = System.currentTimeMillis()
                        // Throttle progress emissions to prevent UI thread thrashing (every 50ms)
                        if (currentTime - lastReportedTime > 50 || totalRead == contentLength) {
                            lastReportedTime = currentTime
                            val progress = if (contentLength > 0) {
                                (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                -1f
                            }
                            onProgress(progress, totalRead, contentLength)
                        }
                    }
                    outputStream.flush()
                }
            }

            // Verify SHA-256 integrity if provided
            if (!info.sha256.isNullOrBlank()) {
                val computedHash = calculateSha256(tempFile)
                if (!computedHash.equals(info.sha256.trim(), ignoreCase = true)) {
                    tempFile.delete()
                    Log.e(TAG, "SHA-256 verification failed! Expected: ${info.sha256}, Computed: $computedHash")
                    return@withContext Result.failure(
                        SecurityException("Update integrity verification failed. Please try downloading again.")
                    )
                }
                Log.d(TAG, "SHA-256 verification succeeded!")
            }

            // Rename tmp file to target apk
            if (destinationFile.exists()) destinationFile.delete()
            if (!tempFile.renameTo(destinationFile)) {
                return@withContext Result.failure(IOException("Failed to finalize downloaded APK package."))
            }

            Result.success(destinationFile)
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            Log.e(TAG, "Download failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Prompts the Android OS package installer using a secure FileProvider URI.
     */
    fun startInstallation(apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(IllegalStateException("Downloaded APK file not found."))
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Shows a system notification alerting the user of an available update.
     */
    fun showUpdateNotification(info: UpdateInfo) {
        val now = System.currentTimeMillis()
        val lastNotifiedTime = prefs.getLong(PREF_LAST_NOTIFICATION_TIME, 0L)
        val lastVersion = prefs.getString(PREF_LAST_NOTIFIED_VERSION, "")

        // Respect notification cooldown unless it's a newer version than previously notified
        if (lastVersion == info.versionName && (now - lastNotifiedTime) < UpdateConfig.NOTIFICATION_COOLDOWN_MS) {
            Log.d(TAG, "Suppressing update notification due to 24-hour cooldown for v${info.versionName}")
            return
        }

        // Notification permission check on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Cannot post update notification: POST_NOTIFICATIONS permission not granted.")
                return
            }
        }

        val notifyIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SHOW_UPDATE_DIALOG, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_neliplay_logo)
            .setContentTitle("NeliPlay Update Available")
            .setContentText("NeliPlay ${info.versionName} is ready to install. Tap to update.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("New NeliPlay Update: v${info.versionName}")
                    .bigText("A new update with improved playback and bug fixes is ready to install. Tap to view release details and update.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            prefs.edit()
                .putLong(PREF_LAST_NOTIFICATION_TIME, now)
                .putString(PREF_LAST_NOTIFIED_VERSION, info.versionName)
                .apply()
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification post security exception: ${e.message}")
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { isStream ->
            val buffer = ByteArray(8192)
            var read: Int
            while (isStream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
