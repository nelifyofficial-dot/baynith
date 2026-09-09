package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress ORDER BY lastWatchedTimestamp DESC")
    fun getAllProgress(): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE movieId = :movieId LIMIT 1")
    suspend fun getProgressForMovie(movieId: String): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress WHERE movieId = :movieId LIMIT 1")
    fun observeProgressForMovie(movieId: String): Flow<WatchProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE movieId = :movieId")
    suspend fun deleteProgress(movieId: String)

    @Query("DELETE FROM watch_progress")
    suspend fun clearAll()
}
