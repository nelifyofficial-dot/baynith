package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.example.data.local.dao.DownloadDao
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.DownloadState
import com.example.data.model.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val TAG = "DownloadRepository"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun observeDownload(movieId: String): Flow<DownloadEntity?> = downloadDao.observeDownload(movieId)

    suspend fun getDownload(movieId: String): DownloadEntity? = downloadDao.getDownload(movieId)

    /**
     * Initiates or resumes downloading a movie.
     * Enforces:
     * 1. movie.downloadEnabled == true
     * 2. Sufficient storage space
     * 3. Network availability
     * 4. No duplicate simultaneous downloads
     */
    fun startDownload(movie: Movie, wifiOnly: Boolean = false) {
        if (!movie.downloadEnabled) {
            Log.w(TAG, "Download rejected: downloadEnabled is false for movie ${movie.title}")
            return
        }

        // Embedded media cannot be downloaded directly; do NOT attempt to download embedCode HTML!
        if (movie.isEmbed) {
            Log.w(TAG, "Download rejected: movie ${movie.title} uses embedded player; direct download unavailable.")
            return
        }

        if (movie.streamUrl.isBlank()) {
            Log.w(TAG, "Download rejected: streamUrl is empty for movie ${movie.title}")
            return
        }

        // Check network
        if (!isNetworkAvailable(wifiOnly)) {
            scope.launch {
                val errorMsg = if (wifiOnly) "Wi-Fi required for downloads" else "No internet connection"
                downloadDao.insertOrUpdate(
                    DownloadEntity(
                        movieId = movie.id,
                        title = movie.title,
                        posterPath = movie.posterPath,
                        streamUrl = movie.streamUrl,
                        status = DownloadState.FAILED,
                        errorMessage = errorMsg
                    )
                )
            }
            return
        }

        // Cancel existing job if running
        activeJobs[movie.id]?.cancel()

        val job = scope.launch {
            downloadMovieInternal(movie)
        }
        activeJobs[movie.id] = job
    }

    private suspend fun downloadMovieInternal(movie: Movie) = withContext(Dispatchers.IO) {
        val moviesDir = getDownloadsDirectory()
        if (!moviesDir.exists()) moviesDir.mkdirs()

        val safeFileName = "movie_${movie.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.mp4"
        val destinationFile = File(moviesDir, safeFileName)

        val existingDownloaded = destinationFile.length()

        val existingEntity = downloadDao.getDownload(movie.id)
        val initialEntity = existingEntity?.copy(
            status = DownloadState.DOWNLOADING,
            localFilePath = destinationFile.absolutePath,
            errorMessage = null
        ) ?: DownloadEntity(
            movieId = movie.id,
            title = movie.title,
            posterPath = movie.posterPath,
            streamUrl = movie.streamUrl,
            localFilePath = destinationFile.absolutePath,
            status = DownloadState.DOWNLOADING,
            bytesDownloaded = existingDownloaded,
            progress = 0f
        )
        downloadDao.insertOrUpdate(initialEntity)

        try {
            val requestBuilder = Request.Builder().url(movie.streamUrl)
            if (existingDownloaded > 0) {
                requestBuilder.header("Range", "bytes=$existingDownloaded-")
            }
            val request = requestBuilder.build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful && response.code != 206) {
                // If Range request is not supported (code 416 or error), retry from 0
                if (existingDownloaded > 0 && (response.code == 416 || response.code == 400)) {
                    destinationFile.delete()
                    val retryRequest = Request.Builder().url(movie.streamUrl).build()
                    val retryResponse = okHttpClient.newCall(retryRequest).execute()
                    if (!retryResponse.isSuccessful) {
                        downloadDao.updateStatus(movie.id, DownloadState.FAILED, "Server returned error: ${retryResponse.code}")
                        return@withContext
                    }
                    processResponseBody(movie.id, retryResponse, destinationFile, 0L)
                    return@withContext
                }

                downloadDao.updateStatus(movie.id, DownloadState.FAILED, "Server returned error: ${response.code}")
                return@withContext
            }

            processResponseBody(movie.id, response, destinationFile, existingDownloaded)

        } catch (e: Exception) {
            if (activeJobs[movie.id]?.isCancelled == true) {
                Log.d(TAG, "Download cancelled or paused by user: ${movie.title}")
            } else {
                Log.e(TAG, "Download error for ${movie.title}: ${e.message}", e)
                downloadDao.updateStatus(movie.id, DownloadState.FAILED, e.localizedMessage ?: "Download failed")
            }
        } finally {
            activeJobs.remove(movie.id)
        }
    }

    private suspend fun processResponseBody(
        movieId: String,
        response: okhttp3.Response,
        file: File,
        alreadyDownloaded: Long
    ) = withContext(Dispatchers.IO) {
        val body = response.body ?: run {
            downloadDao.updateStatus(movieId, DownloadState.FAILED, "Empty response body")
            return@withContext
        }

        val contentLength = body.contentLength()
        val totalLength = if (contentLength > 0) alreadyDownloaded + contentLength else 0L

        // Check storage space
        if (totalLength > 0 && !hasEnoughStorage(totalLength - alreadyDownloaded)) {
            downloadDao.updateStatus(movieId, DownloadState.FAILED, "Insufficient storage space")
            return@withContext
        }

        val raf = RandomAccessFile(file, "rw")
        raf.seek(alreadyDownloaded)

        val buffer = ByteArray(32 * 1024)
        val inputStream = body.byteStream()
        var currentBytes = alreadyDownloaded
        var lastDbUpdate = System.currentTimeMillis()

        try {
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                raf.write(buffer, 0, read)
                currentBytes += read

                val now = System.currentTimeMillis()
                // Update DB progress every 500ms
                if (now - lastDbUpdate > 500L || currentBytes == totalLength) {
                    val progress = if (totalLength > 0) currentBytes.toFloat() / totalLength else 0f
                    downloadDao.updateProgress(movieId, DownloadState.DOWNLOADING, progress, currentBytes, totalLength)
                    lastDbUpdate = now
                }
            }

            // Download completed successfully
            downloadDao.updateProgress(movieId, DownloadState.COMPLETED, 1.0f, currentBytes, currentBytes)
            Log.d(TAG, "Download completed: $movieId at ${file.absolutePath}")

        } finally {
            try {
                inputStream.close()
                raf.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing stream: ${e.message}")
            }
        }
    }

    fun pauseDownload(movieId: String) {
        activeJobs[movieId]?.cancel()
        activeJobs.remove(movieId)
        scope.launch {
            downloadDao.updateStatus(movieId, DownloadState.PAUSED)
        }
    }

    fun cancelDownload(movieId: String) {
        activeJobs[movieId]?.cancel()
        activeJobs.remove(movieId)
        deleteDownload(movieId)
    }

    fun deleteDownload(movieId: String) {
        scope.launch(Dispatchers.IO) {
            val entity = downloadDao.getDownload(movieId)
            if (entity != null) {
                if (entity.localFilePath.isNotEmpty()) {
                    val file = File(entity.localFilePath)
                    if (file.exists()) file.delete()
                }
                downloadDao.deleteDownload(movieId)
            }
        }
    }

    private fun getDownloadsDirectory(): File {
        val ext = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return ext ?: File(context.filesDir, "downloads")
    }

    private fun hasEnoughStorage(requiredBytes: Long): Boolean {
        return try {
            val path = getDownloadsDirectory()
            val stat = StatFs(path.path)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            available > (requiredBytes + 50 * 1024 * 1024) // Keep 50MB buffer
        } catch (e: Exception) {
            true
        }
    }

    private fun isNetworkAvailable(wifiOnly: Boolean): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) return false

        if (wifiOnly) {
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                   caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
        return true
    }
}
