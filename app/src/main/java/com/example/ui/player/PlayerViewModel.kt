package com.example.ui.player

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NeliPlayDatabase
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.DownloadState
import com.example.data.model.CastMember
import com.example.data.model.CastRepository
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.Series
import com.example.data.model.TvChannel
import com.example.data.repository.DownloadRepository
import com.example.data.repository.EpisodeRepository
import com.example.data.repository.MovieRepository
import com.example.data.repository.SeriesRepository
import com.example.data.repository.TvRepository
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PlayerUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val episode: Episode? = null,
    val series: Series? = null,
    val seriesName: String? = null,
    val tvChannel: TvChannel? = null,
    val mediaUrl: String = "",
    val playbackType: String = "mp4", // "mp4", "m3u8", "embed"
    val embedCode: String = "",
    val isLive: Boolean = false,
    val isOffline: Boolean = false,
    val autoSkipIntro: Boolean = false,
    val isSwahiliNarrated: Boolean = false,
    val initialPositionMs: Long = 0L,
    val isFavorite: Boolean = false,
    val downloadEntity: DownloadEntity? = null,
    val castMembers: List<CastMember> = emptyList(),
    val moreLikeThis: List<Movie> = emptyList(),
    val seasons: List<Int> = emptyList(),
    val selectedSeason: Int = 1,
    val episodes: List<Episode> = emptyList(),
    val currentSeasonEpisodes: List<Episode> = emptyList(),
    val nextEpisode: Episode? = null,
    val isNextEpisodePreloaded: Boolean = false,
    val error: String? = null
) {
    val isEmbed: Boolean get() = playbackType.equals("embed", ignoreCase = true) && mediaUrl.isBlank()
    val displayTitle: String get() = movie?.title ?: episode?.title ?: tvChannel?.name ?: "NeliPlay"
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val movieRepo = MovieRepository()
    private val seriesRepo = SeriesRepository()
    private val episodeRepo = EpisodeRepository()
    private val tvRepo = TvRepository()
    private val userDataRepo = UserDataRepository(application)
    private val db = NeliPlayDatabase.getDatabase(application)
    private val downloadDao = db.downloadDao()
    private val downloadRepo = DownloadRepository(application, downloadDao)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        // Observe preloaded episode state from EpisodePreloadManager
        viewModelScope.launch {
            EpisodePreloadManager.preloadedEpisodeId.collect { preloadedId ->
                val currentNextId = _uiState.value.nextEpisode?.id
                if (currentNextId != null && currentNextId == preloadedId) {
                    _uiState.value = _uiState.value.copy(isNextEpisodePreloaded = true)
                }
            }
        }
    }

    fun loadMedia(contentId: String, isLiveTv: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val autoSkip = userDataRepo.autoSkipIntroPreference.firstOrNull() ?: false
            _uiState.value = PlayerUiState(isLoading = true, isLive = isLiveTv, autoSkipIntro = autoSkip)

            if (isLiveTv) {
                val channel = tvRepo.getChannelById(contentId).firstOrNull()
                if (channel != null && channel.streamUrl.isNotBlank()) {
                    val pType = if (channel.streamUrl.contains(".m3u8", ignoreCase = true)) "m3u8" else "mp4"
                    val allChannels = tvRepo.getPublishedChannels().firstOrNull() ?: emptyList()
                    val dummyMovies = allChannels.map {
                        Movie(id = it.id, title = it.name, posterPath = it.logoUrl, streamUrl = it.streamUrl)
                    }

                    _uiState.value = PlayerUiState(
                        isLoading = false,
                        tvChannel = channel,
                        mediaUrl = channel.streamUrl,
                        playbackType = pType,
                        isLive = true,
                        autoSkipIntro = false,
                        moreLikeThis = dummyMovies.filter { it.id != channel.id }.take(6)
                    )
                } else {
                    _uiState.value = PlayerUiState(
                        isLoading = false,
                        isLive = true,
                        error = "Live TV stream is currently unavailable."
                    )
                }
            } else {
                kotlinx.coroutines.coroutineScope {
                    val downloadDeferred = async { downloadDao.getDownload(contentId) }
                    val progressDeferred = async { userDataRepo.getProgress(contentId) }
                    val isFavDeferred = async { userDataRepo.isFavorite(contentId).firstOrNull() ?: false }
                    val allMoviesDeferred = async { movieRepo.getPublishedMovies().firstOrNull() ?: emptyList() }

                    val isMovieId = contentId.startsWith("mov_")
                    val isEpisodeId = contentId.startsWith("ep_") || contentId.startsWith("episode_")

                    val movieDeferred = async {
                        if (!isEpisodeId) movieRepo.getMovieById(contentId).firstOrNull() else null
                    }
                    val episodeDeferred = async {
                        if (!isMovieId) episodeRepo.getEpisodeById(contentId).firstOrNull() else null
                    }

                    val download = downloadDeferred.await()
                    val savedProgress = progressDeferred.await()
                    val isFav = isFavDeferred.await()
                    val allMovies = allMoviesDeferred.await()
                    val movie = movieDeferred.await()
                    val episode = episodeDeferred.await()

                    val localFile = download?.localFilePath?.let { File(it) }
                    val isOfflinePlayable = download?.status == DownloadState.COMPLETED && localFile != null && localFile.exists()
                    val resumePosition = savedProgress?.positionMs ?: 0L

                    var fetchedSeries: Series? = null
                    var fetchedSeriesName: String? = null
                    var seriesEpisodes: List<Episode> = emptyList()
                    var nextEp: Episode? = null

                    if (episode != null) {
                        val sId = episode.seriesId ?: episode.movieId
                        if (!sId.isNullOrBlank()) {
                            fetchedSeries = seriesRepo.getSeriesById(sId).firstOrNull()
                            fetchedSeriesName = fetchedSeries?.name
                            seriesEpisodes = episodeRepo.getEpisodesForSeries(sId).firstOrNull() ?: emptyList()
                            if (seriesEpisodes.isEmpty()) {
                                seriesEpisodes = episodeRepo.getEpisodesForMovie(sId).firstOrNull() ?: emptyList()
                            }
                        }

                        // Determine Next Episode in the series
                        val currentSeasonNum = episode.seasonNumber
                        val currentEpNum = episode.episodeNumber
                        nextEp = seriesEpisodes.firstOrNull {
                            it.seasonNumber == currentSeasonNum && it.episodeNumber == currentEpNum + 1
                        } ?: seriesEpisodes.firstOrNull {
                            it.seasonNumber == currentSeasonNum + 1 && it.episodeNumber == 1
                        }

                        // Preload Next Episode in background automatically!
                        if (nextEp != null && nextEp.streamUrl.isNotBlank()) {
                            withContext(Dispatchers.Main) {
                                EpisodePreloadManager.preloadNextEpisode(getApplication(), nextEp)
                            }
                        }
                    }

                    val titleForCast = movie?.title ?: fetchedSeriesName ?: episode?.title ?: "Movie"
                    val genresForCast = movie?.genres ?: fetchedSeries?.genres ?: emptyList()
                    val cast = CastRepository.getCastForTitle(titleForCast, genresForCast)

                    // Find similar movies
                    val similar = allMovies.filter { m ->
                        m.id != contentId && (genresForCast.isEmpty() || m.genres.any { g -> genresForCast.contains(g) })
                    }.take(8).ifEmpty {
                        allMovies.filter { it.id != contentId }.take(8)
                    }

                    val isSwahili = (movie?.isSwahiliNarrated == true) || (episode?.isSwahiliNarrated == true)

                    val availableSeasons = seriesEpisodes.map { it.seasonNumber }.distinct().sorted()
                    val activeSeason = episode?.seasonNumber ?: availableSeasons.firstOrNull() ?: 1
                    val currentSeasonEps = seriesEpisodes.filter { it.seasonNumber == activeSeason }.sortedBy { it.episodeNumber }

                    if (isOfflinePlayable && download != null) {
                        _uiState.value = PlayerUiState(
                            isLoading = false,
                            movie = movie ?: Movie(id = contentId, title = download.title, posterPath = download.posterPath, streamUrl = download.streamUrl),
                            mediaUrl = localFile!!.absolutePath,
                            playbackType = "mp4",
                            isLive = false,
                            isOffline = true,
                            autoSkipIntro = autoSkip,
                            isSwahiliNarrated = isSwahili,
                            initialPositionMs = resumePosition,
                            isFavorite = isFav,
                            downloadEntity = download,
                            castMembers = cast,
                            moreLikeThis = similar
                        )
                    } else if (movie != null) {
                        val effectiveType = movie.effectivePlaybackType
                        // If direct stream URL is available, always prioritize direct stream
                        val chosenMediaUrl = movie.streamUrl.ifBlank { "" }
                        val chosenPlaybackType = if (chosenMediaUrl.isNotBlank()) {
                            if (chosenMediaUrl.contains(".m3u8", ignoreCase = true)) "m3u8" else "mp4"
                        } else {
                            effectiveType
                        }

                        _uiState.value = PlayerUiState(
                            isLoading = false,
                            movie = movie,
                            mediaUrl = chosenMediaUrl,
                            playbackType = chosenPlaybackType,
                            embedCode = movie.embedCode,
                            isLive = false,
                            isOffline = false,
                            autoSkipIntro = autoSkip,
                            isSwahiliNarrated = isSwahili,
                            initialPositionMs = resumePosition,
                            isFavorite = isFav,
                            downloadEntity = download,
                            castMembers = cast,
                            moreLikeThis = similar
                        )
                    } else if (episode != null) {
                        val effectiveType = episode.effectivePlaybackType
                        val chosenMediaUrl = episode.streamUrl.ifBlank { "" }
                        val chosenPlaybackType = if (chosenMediaUrl.isNotBlank()) {
                            if (chosenMediaUrl.contains(".m3u8", ignoreCase = true)) "m3u8" else "mp4"
                        } else {
                            effectiveType
                        }

                        _uiState.value = PlayerUiState(
                            isLoading = false,
                            episode = episode,
                            series = fetchedSeries,
                            seriesName = fetchedSeriesName,
                            mediaUrl = chosenMediaUrl,
                            playbackType = chosenPlaybackType,
                            embedCode = episode.embedCode,
                            isLive = false,
                            isOffline = false,
                            autoSkipIntro = autoSkip,
                            isSwahiliNarrated = isSwahili,
                            initialPositionMs = resumePosition,
                            isFavorite = isFav,
                            downloadEntity = download,
                            castMembers = cast,
                            moreLikeThis = similar,
                            seasons = availableSeasons,
                            selectedSeason = activeSeason,
                            episodes = seriesEpisodes,
                            currentSeasonEpisodes = currentSeasonEps,
                            nextEpisode = nextEp,
                            isNextEpisodePreloaded = nextEp?.id != null && EpisodePreloadManager.isEpisodePreloaded(nextEp.id)
                        )
                    } else {
                        _uiState.value = PlayerUiState(
                            isLoading = false,
                            error = "Video stream could not be loaded. Please check your internet connection."
                        )
                    }
                }
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        val allEps = _uiState.value.episodes
        val seasonEps = allEps.filter { it.seasonNumber == seasonNumber }.sortedBy { it.episodeNumber }
        _uiState.value = _uiState.value.copy(
            selectedSeason = seasonNumber,
            currentSeasonEpisodes = seasonEps
        )
    }

    fun toggleFavorite() {
        val current = _uiState.value
        val movie = current.movie
        val newFav = !current.isFavorite
        _uiState.value = current.copy(isFavorite = newFav)
        viewModelScope.launch(Dispatchers.IO) {
            if (movie != null) {
                userDataRepo.toggleFavorite(movie)
            } else {
                val contentId = current.episode?.id ?: current.tvChannel?.id ?: return@launch
                val dummyMovie = Movie(
                    id = contentId,
                    title = current.displayTitle,
                    posterPath = current.episode?.stillPath ?: current.tvChannel?.logoUrl ?: ""
                )
                userDataRepo.toggleFavorite(dummyMovie)
            }
        }
    }

    fun startDownload(quality: String = "720p") {
        val current = _uiState.value
        val movie = current.movie
        if (movie != null && movie.downloadEnabled) {
            downloadRepo.startDownload(movie)
        }
    }

    fun pauseDownload() {
        val current = _uiState.value
        val id = current.movie?.id ?: current.episode?.id ?: return
        downloadRepo.pauseDownload(id)
    }

    fun preloadNextEpisodeManually(context: Context) {
        val nextEp = _uiState.value.nextEpisode ?: return
        EpisodePreloadManager.preloadNextEpisode(context, nextEp)
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val movie = _uiState.value.movie
        val episode = _uiState.value.episode
        val contentId = movie?.id ?: episode?.id ?: return
        if (_uiState.value.isLive || _uiState.value.isEmbed) return

        viewModelScope.launch(Dispatchers.IO) {
            userDataRepo.saveWatchProgress(
                movieId = contentId,
                title = movie?.title ?: episode?.title ?: "Video",
                posterPath = movie?.posterPath ?: episode?.stillPath ?: "",
                backdropPath = movie?.backdropPath ?: episode?.stillPath ?: "",
                positionMs = positionMs,
                durationMs = durationMs
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        EpisodePreloadManager.releasePreloader()
    }
}
