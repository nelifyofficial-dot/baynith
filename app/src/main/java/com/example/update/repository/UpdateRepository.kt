package com.example.update.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import com.example.update.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class CheckResult {
    data class Available(val info: UpdateInfo) : CheckResult()
    object UpToDate : CheckResult()
    object NoRelease : CheckResult()
    data class Error(val message: String) : CheckResult()
    object NoInternet : CheckResult()
}

class UpdateRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        private const val TAG = "UpdateRepository"
    }

    /**
     * Checks if the device has an active internet connection.
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Fetches release information from GitHub Releases API and evaluates if a newer version exists.
     */
    suspend fun fetchLatestRelease(): CheckResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext CheckResult.NoInternet
        }

        try {
            val request = Request.Builder()
                .url(UpdateConfig.GITHUB_LATEST_RELEASE_URL)
                .header("User-Agent", "NeliPlay-Android/${BuildConfig.VERSION_NAME}")
                .header("Accept", "application/vnd.github+json")
                .build()

            val response = client.newCall(request).execute()

            response.use { res ->
                val code = res.code
                if (code == 404) {
                    Log.d(TAG, "GitHub repository has no published release yet (HTTP 404).")
                    return@withContext CheckResult.NoRelease
                }

                if (code == 403 || code == 429) {
                    Log.w(TAG, "GitHub API rate limit or access restricted (HTTP $code).")
                    return@withContext CheckResult.Error("GitHub update service is currently rate limited. Please try again later.")
                }

                if (!res.isSuccessful) {
                    Log.w(TAG, "GitHub API returned unsuccessful HTTP $code")
                    return@withContext CheckResult.Error("Unable to check for updates (HTTP $code).")
                }

                val bodyString = res.body?.string()
                if (bodyString.isNullOrBlank()) {
                    return@withContext CheckResult.NoRelease
                }

                val releaseJson = JSONObject(bodyString)
                val tagName = releaseJson.optString("tag_name", "")
                val releaseName = releaseJson.optString("name", tagName)
                val releaseBody = releaseJson.optString("body", "")
                val publishedAt = releaseJson.optString("published_at", "")
                val assets = releaseJson.optJSONArray("assets") ?: JSONArray()

                // Check for optional version.json metadata asset
                var versionMetadata: JSONObject? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.equals("version.json", ignoreCase = true)) {
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (downloadUrl.isNotBlank()) {
                            versionMetadata = fetchVersionJson(downloadUrl)
                        }
                        break
                    }
                }

                // Extract or synthesize target APK asset information
                val updateInfo = parseUpdateInfo(
                    releaseJson = releaseJson,
                    versionMetadata = versionMetadata,
                    tagName = tagName,
                    releaseName = releaseName,
                    releaseBody = releaseBody,
                    publishedAt = publishedAt,
                    assets = assets
                ) ?: return@withContext CheckResult.Error("Update package is currently unavailable. Please try again later.")

                val currentCode = BuildConfig.VERSION_CODE
                Log.d(TAG, "Installed versionCode: $currentCode, Remote versionCode: ${updateInfo.versionCode}")

                if (updateInfo.versionCode > currentCode) {
                    CheckResult.Available(updateInfo)
                } else {
                    CheckResult.UpToDate
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network IO failure checking for update: ${e.message}")
            CheckResult.Error("Unable to check for updates. Please check your internet connection.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error evaluating release: ${e.message}", e)
            CheckResult.Error("Failed to process update information.")
        }
    }

    /**
     * Downloads and parses the version.json asset if provided in the release.
     */
    private fun fetchVersionJson(url: String): JSONObject? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "NeliPlay-Android/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    resp.body?.string()?.let { JSONObject(it) }
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download version.json: ${e.message}")
            null
        }
    }

    /**
     * Resolves UpdateInfo by prioritizing version.json metadata with intelligent fallbacks.
     */
    private fun parseUpdateInfo(
        releaseJson: JSONObject,
        versionMetadata: JSONObject?,
        tagName: String,
        releaseName: String,
        releaseBody: String,
        publishedAt: String,
        assets: JSONArray
    ): UpdateInfo? {
        // Preferred values from version.json if available
        val metaVersionCode = versionMetadata?.optInt("versionCode", -1) ?: -1
        val metaVersionName = versionMetadata?.optString("versionName", "") ?: ""
        val metaApkFileName = versionMetadata?.optString("apkFileName", "") ?: ""
        val metaForceUpdate = versionMetadata?.optBoolean("forceUpdate", false) ?: false
        val metaTitle = versionMetadata?.optString("title", "") ?: ""
        val metaMessage = versionMetadata?.optString("message", "") ?: ""
        val metaSha256 = versionMetadata?.optString("sha256", null)

        val metaNotes = mutableListOf<String>()
        versionMetadata?.optJSONArray("releaseNotes")?.let { notesArray ->
            for (i in 0 until notesArray.length()) {
                val note = notesArray.optString(i)
                if (!note.isNullOrBlank()) metaNotes.add(note)
            }
        }

        // 1. Determine APK asset and download URL
        var resolvedApkUrl = ""
        var resolvedApkName = ""
        var resolvedSize = 0L

        // Try exact match from version.json apkFileName
        if (metaApkFileName.isNotBlank()) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                if (name.equals(metaApkFileName, ignoreCase = true)) {
                    resolvedApkUrl = asset.optString("browser_download_url", "")
                    resolvedApkName = name
                    resolvedSize = asset.optLong("size", 0L)
                    break
                }
            }
        }

        // Fallback: search for any asset ending with .apk
        if (resolvedApkUrl.isBlank()) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    resolvedApkUrl = asset.optString("browser_download_url", "")
                    resolvedApkName = name
                    resolvedSize = asset.optLong("size", 0L)
                    break
                }
            }
        }

        if (resolvedApkUrl.isBlank()) {
            Log.w(TAG, "No valid .apk asset found in release $tagName")
            return null
        }

        // 2. Determine numeric versionCode
        val finalVersionCode: Int = if (metaVersionCode > 0) {
            metaVersionCode
        } else {
            extractVersionCodeFromText(releaseBody) ?: parseVersionCodeFromTag(tagName)
        }

        // 3. Determine versionName
        val finalVersionName = if (metaVersionName.isNotBlank()) {
            metaVersionName
        } else {
            tagName.trim().removePrefix("v").removePrefix("V")
        }

        // 4. Determine release notes
        val finalNotes: List<String> = if (metaNotes.isNotEmpty()) {
            metaNotes
        } else {
            parseReleaseNotesFromBody(releaseBody)
        }

        // 5. Determine forceUpdate
        val finalForceUpdate = metaForceUpdate || isForceUpdateInText(releaseBody)

        val title = if (metaTitle.isNotBlank()) metaTitle else "New NeliPlay Update"
        val message = if (metaMessage.isNotBlank()) metaMessage else "Version $finalVersionName is ready to install."

        return UpdateInfo(
            versionCode = finalVersionCode,
            versionName = finalVersionName,
            downloadUrl = resolvedApkUrl,
            apkFileName = resolvedApkName,
            fileSize = resolvedSize,
            forceUpdate = finalForceUpdate,
            title = title,
            message = message,
            releaseNotes = finalNotes,
            sha256 = metaSha256,
            releaseDate = publishedAt
        )
    }

    /**
     * Extracts versionCode from markdown body using patterns like "versionCode: 2" or "versionCode = 2".
     */
    private fun extractVersionCodeFromText(text: String): Int? {
        val regex = Regex("""(?i)(?:versionCode|version_code|build)\s*[:=]\s*(\d+)""")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Parses semantic version tag (e.g. "v1.2.3") into a standard numeric version code.
     */
    private fun parseVersionCodeFromTag(tag: String): Int {
        val cleaned = tag.trim().removePrefix("v").removePrefix("V")
        val parts = cleaned.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
        return when (parts.size) {
            0 -> 1
            1 -> parts[0] * 10000
            2 -> parts[0] * 10000 + parts[1] * 100
            else -> parts[0] * 10000 + parts[1] * 100 + parts[2]
        }
    }

    /**
     * Extracts bullet points from release body description.
     */
    private fun parseReleaseNotesFromBody(body: String): List<String> {
        if (body.isBlank()) {
            return listOf(
                "Improved video playback stability",
                "Performance optimizations",
                "Bug fixes and enhancements"
            )
        }
        val lines = body.lines()
        val notes = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
                val cleanNote = trimmed.substring(2).trim()
                if (cleanNote.isNotBlank()) notes.add(cleanNote)
            }
        }
        return if (notes.isNotEmpty()) {
            notes
        } else {
            lines.map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }.take(4)
        }
    }

    private fun isForceUpdateInText(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("[force]") || lower.contains("forceupdate: true") || lower.contains("force_update: true")
    }
}
