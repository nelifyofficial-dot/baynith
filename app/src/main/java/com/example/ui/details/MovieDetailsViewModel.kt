package com.example.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NeliPlayDatabase
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.DownloadState
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.repository.DownloadRepository
import com.example.data.repository.EpisodeRepository
import com.example.data.repository.MovieRepository
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MovieDetailsUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val isFavorite: Boolean = false,
    val downloadEntity: DownloadEntity? = null,
    val similarMovies: List<Movie> = emptyList(),
    val seasons: List<Int> = emptyList(),
    val selectedSeason: Int = 1,
    val episodes: List<Episode> = emptyList(),
    val currentSeasonEpisodes: List<Episode> = emptyList(),
    val hasEpisodes: Boolean = false,
    val error: String? = null
)

class MovieDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val movieRepo = MovieRepository()
    private val episodeRepo = EpisodeRepository()
    private val userDataRepo = UserDataRepository(application)
    private val db = NeliPlayDatabase.getDatabase(application)
    private val downloadRepo = DownloadRepository(application, db.downloadDao())

    private val _selectedSeason = MutableStateFlow<Int?>(null)
    private val _uiState = MutableStateFlow(MovieDetailsUiState())
    val uiState: StateFlow<MovieDetailsUiState> = _uiState.asStateFlow()

    fun loadMovie(movieId: String) {
        viewModelScope.launch {
            _uiState.value = MovieDetailsUiState(isLoading = true)

            combine(
                combine(
                    movieRepo.getMovieById(movieId),
                    episodeRepo.getEpisodesForMovie(movieId),
                    _selectedSeason
                ) { movie, episodes, selectedSeason ->
                    Triple(movie, episodes, selectedSeason)
                },
                userDataRepo.isFavorite(movieId),
                downloadRepo.observeDownload(movieId),
                movieRepo.getPublishedMovies()
            ) { (movie, episodes, selectedSeason), isFav, download, allMovies ->
                if (movie != null) {
                    val similar = allMovies
                        .filter { it.id != movie.id && it.genres.any { g -> movie.genres.contains(g) } }
                        .take(8)

                    val availableSeasons = episodes.map { it.seasonNumber }.distinct().sorted()
                    val seasonsList = if (availableSeasons.isNotEmpty()) availableSeasons else emptyList()
                    val activeSeason = if (selectedSeason != null && selectedSeason in seasonsList) {
                        selectedSeason
                    } else {
                        seasonsList.firstOrNull() ?: 1
                    }

                    val seasonEpisodes = episodes
                        .filter { it.seasonNumber == activeSeason }
                        .sortedBy { it.episodeNumber }

                    MovieDetailsUiState(
                        isLoading = false,
                        movie = movie,
                        isFavorite = isFav,
                        downloadEntity = download,
                        similarMovies = similar,
                        seasons = seasonsList,
                        selectedSeason = activeSeason,
                        episodes = episodes,
                        currentSeasonEpisodes = seasonEpisodes,
                        hasEpisodes = episodes.isNotEmpty()
                    )
                } else {
                    val err = movieRepo.lastError.value ?: "Movie details could not be found."
                    MovieDetailsUiState(
                        isLoading = false,
                        error = err
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectSeason(season: Int) {
        _selectedSeason.value = season
    }

    fun toggleFavorite() {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            userDataRepo.toggleFavorite(movie)
        }
    }

    fun startDownload(quality: String = "Auto") {
        val movie = _uiState.value.movie ?: return
        val wifiOnly = false
        downloadRepo.startDownload(movie, wifiOnly)
    }

    fun pauseDownload() {
        val movie = _uiState.value.movie ?: return
        downloadRepo.pauseDownload(movie.id)
    }

    fun cancelDownload() {
        val movie = _uiState.value.movie ?: return
        downloadRepo.cancelDownload(movie.id)
    }

    fun deleteDownload() {
        val movie = _uiState.value.movie ?: return
        downloadRepo.deleteDownload(movie.id)
    }
}
