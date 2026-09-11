package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.NeliPlayDatabase
import com.example.data.local.entities.FavoriteEntity
import com.example.data.local.entities.WatchProgressEntity
import com.example.data.model.Movie
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private val Context.dataStore by preferencesDataStore(name = "neliplay_settings")

class UserDataRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = NeliPlayDatabase.getDatabase(appContext)
    private val watchProgressDao = db.watchProgressDao()
    private val favoriteDao = db.favoriteDao()
    private val firestore = FirebaseFirestore.getInstance()

    // Continue Watching / Watch Progress
    val continueWatchingList: Flow<List<WatchProgressEntity>> = watchProgressDao.getAllProgress()

    fun observeProgress(movieId: String): Flow<WatchProgressEntity?> =
        watchProgressDao.observeProgressForMovie(movieId)

    suspend fun getProgress(movieId: String): WatchProgressEntity? =
        watchProgressDao.getProgressForMovie(movieId)

    suspend fun saveWatchProgress(
        movieId: String,
        title: String,
        posterPath: String,
        backdropPath: String,
        positionMs: Long,
        durationMs: Long
    ) {
        if (positionMs <= 0 || durationMs <= 0) return
        // If watched more than 95%, clear progress as completed
        if (positionMs.toFloat() / durationMs.toFloat() >= 0.95f) {
            watchProgressDao.deleteProgress(movieId)
            return
        }

        watchProgressDao.saveProgress(
            WatchProgressEntity(
                movieId = movieId,
                title = title,
                posterPath = posterPath,
                backdropPath = backdropPath,
                positionMs = positionMs,
                durationMs = durationMs,
                lastWatchedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearWatchHistory() {
        watchProgressDao.clearAll()
    }

    // Favorites
    val favoritesList: Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()

    fun isFavorite(movieId: String): Flow<Boolean> = favoriteDao.isFavorite(movieId)

    suspend fun toggleFavorite(movie: Movie) {
        val exists = favoriteDao.isFavoriteSync(movie.id)
        if (exists) {
            favoriteDao.removeFavorite(movie.id)
        } else {
            favoriteDao.addFavorite(
                FavoriteEntity(
                    movieId = movie.id,
                    title = movie.title,
                    posterPath = movie.posterPath,
                    backdropPath = movie.backdropPath,
                    year = movie.year,
                    rating = movie.rating,
                    genres = movie.genres.joinToString(", "),
                    savedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun clearFavorites() {
        favoriteDao.clearAll()
    }

    // Settings Keys
    companion object {
        val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only_downloads")
        val KEY_AUTO_PLAY = booleanPreferencesKey("auto_play_next")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val KEY_VIDEO_QUALITY = stringPreferencesKey("preferred_video_quality")
        val KEY_AUTO_SKIP_INTRO = booleanPreferencesKey("auto_skip_intro_segment")
        val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches_list")
        val KEY_ACKNOWLEDGED_UPDATE = stringPreferencesKey("acknowledged_update_version")
        val KEY_USER_COUNTRY = stringPreferencesKey("user_country_iso")
        val KEY_USER_LANGUAGE = stringPreferencesKey("user_preferred_language")
    }

    val userCountry: Flow<String> = appContext.dataStore.data.map { prefs ->
        prefs[KEY_USER_COUNTRY] ?: ""
    }

    val userLanguage: Flow<String> = appContext.dataStore.data.map { prefs ->
        prefs[KEY_USER_LANGUAGE] ?: "en"
    }

    val wifiOnlyPreference: Flow<Boolean> = appContext.dataStore.data.map { prefs ->
        prefs[KEY_WIFI_ONLY] ?: false
    }

    val autoPlayPreference: Flow<Boolean> = appContext.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_PLAY] ?: true
    }

    val notificationsPreference: Flow<Boolean> = appContext.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS] ?: true
    }

    val videoQualityPreference: Flow<String> = appContext.dataStore.data.map { prefs ->
        prefs[KEY_VIDEO_QUALITY] ?: "Auto"
    }

    val autoSkipIntroPreference: Flow<Boolean> = appContext.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SKIP_INTRO] ?: false
    }

    val recentSearches: Flow<List<String>> = appContext.dataStore.data.map { prefs ->
        val raw = prefs[KEY_RECENT_SEARCHES] ?: ""
        if (raw.isBlank()) emptyList() else raw.split("|||").filter { it.isNotBlank() }
    }

    val acknowledgedUpdateVersion: Flow<String> = appContext.dataStore.data.map { prefs ->
        prefs[KEY_ACKNOWLEDGED_UPDATE] ?: ""
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        appContext.dataStore.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    suspend fun setAutoPlay(enabled: Boolean) {
        appContext.dataStore.edit { it[KEY_AUTO_PLAY] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        appContext.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    suspend fun setVideoQuality(quality: String) {
        appContext.dataStore.edit { it[KEY_VIDEO_QUALITY] = quality }
    }

    suspend fun setAutoSkipIntro(enabled: Boolean) {
        appContext.dataStore.edit { it[KEY_AUTO_SKIP_INTRO] = enabled }
    }

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        appContext.dataStore.edit { prefs ->
            val current = (prefs[KEY_RECENT_SEARCHES] ?: "")
                .split("|||")
                .filter { it.isNotBlank() && !it.equals(trimmed, ignoreCase = true) }
            val updated = (listOf(trimmed) + current).take(10)
            prefs[KEY_RECENT_SEARCHES] = updated.joinToString("|||")
        }
    }

    suspend fun removeRecentSearch(query: String) {
        appContext.dataStore.edit { prefs ->
            val current = (prefs[KEY_RECENT_SEARCHES] ?: "")
                .split("|||")
                .filter { it.isNotBlank() && !it.equals(query, ignoreCase = true) }
            prefs[KEY_RECENT_SEARCHES] = current.joinToString("|||")
        }
    }

    suspend fun clearRecentSearches() {
        appContext.dataStore.edit { prefs ->
            prefs.remove(KEY_RECENT_SEARCHES)
        }
    }

    suspend fun setAcknowledgedUpdateVersion(version: String) {
        appContext.dataStore.edit { it[KEY_ACKNOWLEDGED_UPDATE] = version }
    }

    suspend fun setUserCountry(countryCode: String) {
        val upper = countryCode.trim().uppercase()
        appContext.dataStore.edit { it[KEY_USER_COUNTRY] = upper }
    }

    suspend fun setUserLanguage(lang: String) {
        appContext.dataStore.edit { it[KEY_USER_LANGUAGE] = lang.trim().lowercase() }
    }

    /**
     * Synchronizes and merges local Room database with Firebase Firestore:
     * - Merges local Watchlist (Favorites) into `users/{uid}/watchlist`
     * - Merges local Watch History into `users/{uid}/watchHistory`
     * - Restores cloud items down into local Room cache so data is never lost across sign-ins.
     */
    suspend fun syncUserDataWithFirebase(uid: String) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        try {
            val userDocRef = firestore.collection("users").document(uid)

            // 1. Sync Watchlist / Favorites
            val localFavorites = favoriteDao.getAllFavorites().firstOrNull() ?: emptyList()
            val watchlistCollection = userDocRef.collection("watchlist")

            // Push local to cloud
            for (fav in localFavorites) {
                val favMap = mapOf(
                    "movieId" to fav.movieId,
                    "title" to fav.title,
                    "posterPath" to fav.posterPath,
                    "backdropPath" to fav.backdropPath,
                    "genres" to fav.genres,
                    "rating" to fav.rating,
                    "year" to fav.year,
                    "addedAt" to fav.savedTimestamp
                )
                watchlistCollection.document(fav.movieId).set(favMap, SetOptions.merge()).await()
            }

            // Pull cloud to local
            val cloudWatchlistSnapshot = watchlistCollection.get().await()
            for (doc in cloudWatchlistSnapshot.documents) {
                val movieId = doc.id
                val title = doc.getString("title") ?: ""
                val posterPath = doc.getString("posterPath") ?: ""
                val backdropPath = doc.getString("backdropPath") ?: ""
                val genres = doc.getString("genres") ?: ""
                val rating = doc.getDouble("rating") ?: 0.0
                val year = doc.getLong("year")?.toInt()
                val addedAt = doc.getLong("addedAt") ?: System.currentTimeMillis()

                favoriteDao.addFavorite(
                    FavoriteEntity(
                        movieId = movieId,
                        title = title,
                        posterPath = posterPath,
                        backdropPath = backdropPath,
                        genres = genres,
                        rating = rating,
                        year = year,
                        savedTimestamp = addedAt
                    )
                )
            }

            // 2. Sync Watch History / Continue Watching
            val localProgress = watchProgressDao.getAllProgress().firstOrNull() ?: emptyList()
            val historyCollection = userDocRef.collection("watchHistory")

            // Push local to cloud
            for (prog in localProgress) {
                val progMap = mapOf(
                    "movieId" to prog.movieId,
                    "title" to prog.title,
                    "posterPath" to prog.posterPath,
                    "backdropPath" to prog.backdropPath,
                    "positionMs" to prog.positionMs,
                    "durationMs" to prog.durationMs,
                    "lastWatchedTimestamp" to prog.lastWatchedTimestamp
                )
                historyCollection.document(prog.movieId).set(progMap, SetOptions.merge()).await()
            }

            // Pull cloud to local
            val cloudHistorySnapshot = historyCollection.get().await()
            for (doc in cloudHistorySnapshot.documents) {
                val movieId = doc.id
                val title = doc.getString("title") ?: ""
                val posterPath = doc.getString("posterPath") ?: ""
                val backdropPath = doc.getString("backdropPath") ?: ""
                val positionMs = doc.getLong("positionMs") ?: 0L
                val durationMs = doc.getLong("durationMs") ?: 0L
                val lastWatchedTimestamp = doc.getLong("lastWatchedTimestamp") ?: System.currentTimeMillis()

                if (positionMs > 0 && durationMs > 0) {
                    watchProgressDao.saveProgress(
                        WatchProgressEntity(
                            movieId = movieId,
                            title = title,
                            posterPath = posterPath,
                            backdropPath = backdropPath,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            lastWatchedTimestamp = lastWatchedTimestamp
                        )
                    )
                }
            }

            Log.d("UserDataRepository", "Synced local and cloud data successfully for uid: $uid")
        } catch (e: Exception) {
            Log.e("UserDataRepository", "Error syncing user data with Firebase: ${e.message}", e)
        }
    }
}
