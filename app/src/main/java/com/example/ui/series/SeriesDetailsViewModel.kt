package com.example.ui.series

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NeliPlayDatabase
import com.example.data.local.entities.WatchProgressEntity
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.Series
import com.example.data.repository.EpisodeRepository
import com.example.data.repository.SeriesRepository
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SeriesDetailsUiState(
    val isLoading: Boolean = true,
    val series: Series? = null,
    val seasons: List<Int> = emptyList(),
    val selectedSeason: Int = 1,
    val allEpisodes: List<Episode> = emptyList(),
    val currentSeasonEpisodes: List<Episode> = emptyList(),
    val resumeEpisode: Episode? = null,
    val isFavorite: Boolean = false,
    val error: String? = null
)

class SeriesDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val seriesRepo = SeriesRepository()
    private val episodeRepo = EpisodeRepository()
    private val userDataRepo = UserDataRepository(application)
    private val db = NeliPlayDatabase.getDatabase(application)
    private val watchProgressDao = db.watchProgressDao()

    private val _uiState = MutableStateFlow(SeriesDetailsUiState())
    val uiState: StateFlow<SeriesDetailsUiState> = _uiState.asStateFlow()

    private val _selectedSeason = MutableStateFlow(1)

    fun loadSeries(seriesId: String) {
        viewModelScope.launch {
            _uiState.value = SeriesDetailsUiState(isLoading = true)

            combine(
                seriesRepo.getSeriesById(seriesId),
                episodeRepo.getEpisodesForSeries(seriesId),
                _selectedSeason,
                userDataRepo.isFavorite(seriesId),
                watchProgressDao.getAllProgress()
            ) { series, episodes, selectedSeason, isFav, progressList ->
                if (series != null) {
                    val availableSeasons = episodes.map { it.seasonNumber }.distinct().sorted()
                    val seasonsList = if (availableSeasons.isNotEmpty()) availableSeasons else listOf(1)
                    val activeSeason = if (selectedSeason in seasonsList) selectedSeason else seasonsList.first()

                    val seasonEpisodes = episodes.filter { it.seasonNumber == activeSeason }
                        .sortedBy { it.episodeNumber }

                    // Determine resume episode based on watch progress
                    val watchedEpIds = progressList.map { it.movieId }.toSet()
                    val resumeCandidate = episodes.firstOrNull { it.id in watchedEpIds }
                        ?: episodes.firstOrNull()

                    SeriesDetailsUiState(
                        isLoading = false,
                        series = series,
                        seasons = seasonsList,
                        selectedSeason = activeSeason,
                        allEpisodes = episodes,
                        currentSeasonEpisodes = seasonEpisodes,
                        resumeEpisode = resumeCandidate,
                        isFavorite = isFav
                    )
                } else {
                    SeriesDetailsUiState(
                        isLoading = false,
                        error = "Series information not found."
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
        val series = _uiState.value.series ?: return
        viewModelScope.launch {
            // Adapt Series to Movie entity format for unified favorites
            val fakeMovie = Movie(
                id = series.id,
                title = series.name,
                overview = series.overview,
                posterPath = series.posterPath,
                backdropPath = series.backdropPath,
                rating = series.rating,
                genres = series.genres
            )
            userDataRepo.toggleFavorite(fakeMovie)
        }
    }
}
