package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.NeliPlayDatabase
import com.example.data.local.entities.FavoriteEntity
import com.example.data.local.entities.WatchProgressEntity
import com.example.data.model.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "neliplay_settings")

class UserDataRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = NeliPlayDatabase.getDatabase(appContext)
    private val watchProgressDao = db.watchProgressDao()
    private val favoriteDao = db.favoriteDao()

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
}
