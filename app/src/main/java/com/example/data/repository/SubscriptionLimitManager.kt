package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject

enum class LimitType {
    WATCH_FREE,
    DOWNLOAD_FREE,
    DOWNLOAD_DAILY
}

sealed class LimitCheckResult {
    data class Allowed(
        val isUnlimited: Boolean,
        val adsEnabled: Boolean,
        val remaining: Int? = null,
        val remainingDaily: Int? = null
    ) : LimitCheckResult()

    data class LimitReached(
        val reason: String,
        val limitType: LimitType,
        val currentCount: Int,
        val maxAllowed: Int
    ) : LimitCheckResult()
}

/**
 * Manages playback, download limits, and region enforcement for NeliPlay (Tanzania only):
 * - Free tier: Up to 3 movies watched and 3 movies downloaded total (free with ads)
 * - Daily plan (TSh 1,000): Unlimited online 24h, up to 10 downloads per day, no ads
 * - Weekly plan (TSh 3,000): Unlimited online & downloads for 7 days, no ads
 * - Monthly plan (TSh 10,000): Unlimited online & downloads for 30 days, no ads
 * - Yearly plan (TSh 100,000): Unlimited online & downloads for 365 days, no ads
 */
class SubscriptionLimitManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "neliplay_subscription_limits"
        const val FREE_TIER_MAX_MOVIES = 3
        const val DAILY_PLAN_MAX_DOWNLOADS = 10
        const val TANZANIA_COUNTRY_CODE = "TZ"
        const val TANZANIA_PHONE_PREFIX = "+255"

        @Volatile
        private var INSTANCE: SubscriptionLimitManager? = null

        fun getInstance(context: Context): SubscriptionLimitManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SubscriptionLimitManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun getWatchedKey(userId: String) = "watched_movies_$userId"
    private fun getDownloadsKey(userId: String) = "downloads_history_$userId"

    fun getWatchedMovieIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        return prefs.getStringSet(getWatchedKey(userId), emptySet()) ?: emptySet()
    }

    fun getWatchedCount(userId: String): Int {
        return getWatchedMovieIds(userId).size
    }

    fun hasWatchedMovie(userId: String, movieId: String): Boolean {
        return getWatchedMovieIds(userId).contains(movieId)
    }

    fun recordMovieWatched(userId: String, movieId: String) {
        if (userId.isBlank() || movieId.isBlank()) return
        val current = getWatchedMovieIds(userId).toMutableSet()
        current.add(movieId)
        prefs.edit().putStringSet(getWatchedKey(userId), current).apply()
    }

    fun getDownloadedMovieIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        val jsonStr = prefs.getString(getDownloadsKey(userId), "[]") ?: "[]"
        val set = mutableSetOf<String>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                set.add(obj.optString("movieId"))
            }
        } catch (_: Exception) {}
        return set
    }

    fun getDownloadedCount(userId: String): Int {
        return getDownloadedMovieIds(userId).size
    }

    fun hasDownloadedMovie(userId: String, movieId: String): Boolean {
        return getDownloadedMovieIds(userId).contains(movieId)
    }

    fun getDailyDownloadsCount(userId: String): Int {
        if (userId.isBlank()) return 0
        val jsonStr = prefs.getString(getDownloadsKey(userId), "[]") ?: "[]"
        val oneDayAgo = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        var count = 0
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val ts = obj.optLong("timestamp", 0L)
                if (ts >= oneDayAgo) {
                    count++
                }
            }
        } catch (_: Exception) {}
        return count
    }

    fun recordMovieDownloaded(userId: String, movieId: String) {
        if (userId.isBlank() || movieId.isBlank()) return
        val jsonStr = prefs.getString(getDownloadsKey(userId), "[]") ?: "[]"
        val arr = try { JSONArray(jsonStr) } catch (_: Exception) { JSONArray() }
        val now = System.currentTimeMillis()
        val obj = JSONObject().apply {
            put("movieId", movieId)
            put("timestamp", now)
        }
        arr.put(obj)
        prefs.edit().putString(getDownloadsKey(userId), arr.toString()).apply()
    }

    /**
     * Checks if user can watch this movie under their current subscription.
     */
    fun canWatchMovie(userId: String, movieId: String, profile: UserProfile?): LimitCheckResult {
        // Active subscription or Admin: Unlimited online viewing
        if (profile?.isSubscriptionActive == true || profile?.isAdmin == true) {
            return LimitCheckResult.Allowed(
                isUnlimited = true,
                adsEnabled = false
            )
        }

        // Free tier: Only 3 movies total, with ads
        if (hasWatchedMovie(userId, movieId)) {
            val watched = getWatchedCount(userId)
            return LimitCheckResult.Allowed(
                isUnlimited = false,
                adsEnabled = true,
                remaining = maxOf(0, FREE_TIER_MAX_MOVIES - watched)
            )
        }

        val watchedCount = getWatchedCount(userId)
        if (watchedCount < FREE_TIER_MAX_MOVIES) {
            return LimitCheckResult.Allowed(
                isUnlimited = false,
                adsEnabled = true,
                remaining = FREE_TIER_MAX_MOVIES - watchedCount - 1
            )
        }

        return LimitCheckResult.LimitReached(
            reason = "Umefikia kikomo cha filamu 3 za bure. Ili kuendelea kutazama filamu bila kikomo, tafadhali boresha hadi NeliPlay Premium kuanzia TSh 1,000 tu!",
            limitType = LimitType.WATCH_FREE,
            currentCount = watchedCount,
            maxAllowed = FREE_TIER_MAX_MOVIES
        )
    }

    /**
     * Checks if user can download this movie under their current subscription.
     */
    fun canDownloadMovie(userId: String, movieId: String, profile: UserProfile?): LimitCheckResult {
        // Active subscription or Admin:
        if (profile?.isSubscriptionActive == true || profile?.isAdmin == true) {
            val plan = profile.premiumPlan?.lowercase() ?: ""
            if (plan == "daily") {
                // Daily plan: up to 10 downloads per day (24 hours)
                val dailyDownloads = getDailyDownloadsCount(userId)
                if (dailyDownloads < DAILY_PLAN_MAX_DOWNLOADS) {
                    return LimitCheckResult.Allowed(
                        isUnlimited = false,
                        adsEnabled = false,
                        remainingDaily = DAILY_PLAN_MAX_DOWNLOADS - dailyDownloads - 1
                    )
                } else {
                    return LimitCheckResult.LimitReached(
                        reason = "Umefikia kikomo cha kupakua filamu 10 kwa siku kwenye kifurushi cha Siku 1. Boresha hadi kifurushi cha Wiki (TSh 3,000) au Mwezi (TSh 10,000) kupakua bila kikomo!",
                        limitType = LimitType.DOWNLOAD_DAILY,
                        currentCount = dailyDownloads,
                        maxAllowed = DAILY_PLAN_MAX_DOWNLOADS
                    )
                }
            }
            // Weekly, Monthly, Yearly: Unlimited downloads
            return LimitCheckResult.Allowed(
                isUnlimited = true,
                adsEnabled = false
            )
        }

        // Free tier: Only 3 downloads total
        if (hasDownloadedMovie(userId, movieId)) {
            return LimitCheckResult.Allowed(
                isUnlimited = false,
                adsEnabled = true
            )
        }

        val downloadedCount = getDownloadedCount(userId)
        if (downloadedCount < FREE_TIER_MAX_MOVIES) {
            return LimitCheckResult.Allowed(
                isUnlimited = false,
                adsEnabled = true,
                remaining = FREE_TIER_MAX_MOVIES - downloadedCount - 1
            )
        }

        return LimitCheckResult.LimitReached(
            reason = "Umefikia kikomo cha kupakua filamu 3 za bure. Ili kupakua filamu zaidi bila kikomo, tafadhali boresha hadi NeliPlay Premium kuanzia TSh 1,000 tu!",
            limitType = LimitType.DOWNLOAD_FREE,
            currentCount = downloadedCount,
            maxAllowed = FREE_TIER_MAX_MOVIES
        )
    }

    /**
     * Resets local counters for testing or debugging if needed.
     */
    fun clearUserData(userId: String) {
        prefs.edit()
            .remove(getWatchedKey(userId))
            .remove(getDownloadsKey(userId))
            .apply()
    }
}
