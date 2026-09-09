package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.DownloadState
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY timestamp DESC")
    fun getDownloadsByStatus(status: DownloadState): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE movieId = :movieId LIMIT 1")
    fun observeDownload(movieId: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE movieId = :movieId LIMIT 1")
    suspend fun getDownload(movieId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, progress = :progress, bytesDownloaded = :bytes, totalBytes = :total WHERE movieId = :movieId")
    suspend fun updateProgress(movieId: String, status: DownloadState, progress: Float, bytes: Long, total: Long)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE movieId = :movieId")
    suspend fun updateStatus(movieId: String, status: DownloadState, error: String? = null)

    @Query("DELETE FROM downloads WHERE movieId = :movieId")
    suspend fun deleteDownload(movieId: String)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
