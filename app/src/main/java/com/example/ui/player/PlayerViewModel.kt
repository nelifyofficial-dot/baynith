package com.example.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NeliPlayDatabase
import com.example.data.local.entities.DownloadState
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.TvChannel
import com.example.data.repository.EpisodeRepository
import com.example.data.repository.MovieRepository
import com.example.data.repository.TvRepository
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val episode: Episode? = null,
    val seriesName: String? = null,
    val tvChannel: TvChannel? = null,
    val mediaUrl: String = "",
    val playbackType: String = "mp4", // "mp4", "m3u8", "embed"
    val embedCode: String = "",
    val isLive: Boolean = false,
    val isOffline: Boolean = false,
    val autoSkipIntro: Boolean = false,
    val initialPositionMs: Long = 0L,
    val error: String? = null
) {
    val isEmbed: Boolean get() = playbackType.equals("embed", ignoreCase = true)
    val displayTitle: String get() = movie?.title ?: episode?.title ?: tvChannel?.name ?: "NeliPlay"
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val movieRepo = MovieRepository()
    private val seriesRepo = com.example.data.repository.SeriesRepository()
    private val episodeRepo = EpisodeRepository()
    private val tvRepo = TvRepository()
    private val userDataRepo = UserDataRepository(application)
    private val db = NeliPlayDatabase.getDatabase(application)
    private val downloadDao = db.downloadDao()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun loadMedia(contentId: String, isLiveTv: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val autoSkip = userDataRepo.autoSkipIntroPreference.firstOrNull() ?: false
            _uiState.value = PlayerUiState(isLoading = true, isLive = isLiveTv, autoSkipIntro = autoSkip)

            if (isLiveTv) {
                val channel = tvRepo.getChannelById(contentId).firstOrNull()
                if (channel != null && channel.streamUrl.isNotBlank()) {
                    val pType = if (channel.streamUrl.contains(".m3u8", ignoreCase = true)) "m3u8" else "mp4"
                    _uiState.value = PlayerUiState(
                        isLoading = false,
                        tvChannel = channel,
                        mediaUrl = channel.streamUrl,
                        playbackType = pType,
                        isLive = true,
                        autoSkipIntro = false
                    )
                } else {
                    _uiState.value = PlayerUiState(
                        isLoading = false,
                        isLive = true,
                        error = "Live TV stream is currently unavailable."
                    )
                }
            } else {
                // Check if movie is downloaded locally for offline playback
                val download = downloadDao.getDownload(contentId)
                val localFile = download?.localFilePath?.let { java.io.File(it) }
                val isOfflinePlayable = download?.status == DownloadState.COMPLETED && localFile != null && localFile.exists()

                // Check saved watch position for resume
                val savedProgress = userDataRepo.getProgress(contentId)
                val resumePosition = savedProgress?.positionMs ?: 0L

                val movie = movieRepo.getMovieById(contentId).firstOrNull()
                val episode = if (movie == null) episodeRepo.getEpisodeById(contentId).firstOrNull() else null
                var fetchedSeriesName: String? = null
                if (episode != null) {
                    val sId = episode.seriesId ?: episode.movieId
                    if (!sId.isNullOrBlank()) {
                        val s = seriesRepo.getSeriesById(sId).firstOrNull()
                        fetchedSeriesName = s?.name
                    }
                }

                if (isOfflinePlayable && download != null) {
                    _uiState.value = PlayerUiState(
                        isLoading = false,
                        movie = movie ?: Movie(id = contentId, title = download.title, posterPath = download.posterPath, streamUrl = download.streamUrl),
                        mediaUrl = localFile!!.absolutePath,
                        playbackType = "mp4",
                        isLive = false,
                        isOffline = true,
                        autoSkipIntro = autoSkip,
                        initialPositionMs = resumePosition
                    )
                } else if (movie != null) {
                    val effectiveType = movie.effectivePlaybackType
                    if (effectiveType == "embed") {
                        if (movie.embedCode.isBlank()) {
                            _uiState.value = PlayerUiState(
                                isLoading = false,
                                movie = movie,
                                playbackType = "embed",
                                embedCode = "",
                                error = "Unable to load this embedded player."
                            )
                        } else {
                            _uiState.value = PlayerUiState(
                                isLoading = false,
                                movie = movie,
                                playbackType = "embed",
                                embedCode = movie.embedCode,
                                isLive = false,
                                isOffline = false
                            )
                        }
                    } else if (movie.streamUrl.isNotBlank()) {
                        _uiState.value = PlayerUiState(
                            isLoading = false,
                            movie = movie,
                            mediaUrl = movie.streamUrl,
                            playbackType = effectiveType,
                            isLive = false,
                            isOffline = false,
                            autoSkipIntro = autoSkip,
                            initialPositionMs = resumePosition
                        )
                    } else {
                        _uiState.value = PlayerUiState(
                            isLoading = false,
                            movie = movie,
                            error = "Video stream could not be loaded. Please check your internet connection."
                        )
                    }
                } else if (episode != null) {
                    val effectiveType = episode.effectivePlaybackType
                    if (effectiveType == "embed") {
                        if (episode.embedCode.isBlank()) {
                            _uiState.value = PlayerUiState(
                                isLoading = false,
                                episode = episode,
                                seriesName = fetchedSeriesName,
                                playbackType = "embed",
                                embedCode = "",
                                error = "Unable to load this embedded player."
                            )
                        } else {
                            _uiState.value = PlayerUiState(
                                isLoading = false,
                                episode = episode,
                                seriesName = fetchedSeriesName,
                                playbackType = "embed",
                                embedCode = episode.embedCode,
                                isLive = false,
                                isOffline = false
                            )
                        }
                    } else if (episode.streamUrl.isNotBlank()) {
                        _uiState.value = PlayerUiState(
                            isLoading = false,
                            episode = episode,
                            seriesName = fetchedSeriesName,
                            mediaUrl = episode.streamUrl,
                            playbackType = effectiveType,
                            isLive = false,
                            isOffline = false,
                            autoSkipIntro = autoSkip,
                            initialPositionMs = resumePosition
                        )
                    } else {
                        _uiState.value = PlayerUiState(
                            isLoading = false,
                            episode = episode,
                            seriesName = fetchedSeriesName,
                            error = "Video stream could not be loaded. Please check your internet connection."
                        )
                    }
                } else {
                    _uiState.value = PlayerUiState(
                        isLoading = false,
                        error = "Video stream could not be loaded. Please check your internet connection."
                    )
                }
            }
        }
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val movie = _uiState.value.movie ?: return
        if (_uiState.value.isLive || _uiState.value.isEmbed) return

        viewModelScope.launch(Dispatchers.IO) {
            userDataRepo.saveWatchProgress(
                movieId = movie.id,
                title = movie.title,
                posterPath = movie.posterPath,
                backdropPath = movie.backdropPath,
                positionMs = positionMs,
                durationMs = durationMs
            )
        }
    }
}
