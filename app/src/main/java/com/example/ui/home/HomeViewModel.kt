package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.FavoriteEntity
import com.example.data.local.entities.WatchProgressEntity
import com.example.data.model.Movie
import com.example.data.model.Series
import com.example.data.repository.MovieRepository
import com.example.data.repository.SeriesRepository
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val featuredMovie: Movie? = null,
    val featuredMovies: List<Movie> = emptyList(),
    val continueWatching: List<WatchProgressEntity> = emptyList(),
    val trendingMovies: List<Movie> = emptyList(),
    val trendingSeries: List<Series> = emptyList(),
    val latestMovies: List<Movie> = emptyList(),
    val genreSections: Map<String, List<Movie>> = emptyMap(),
    val favoritesIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val movieRepository = MovieRepository()
    private val seriesRepository = SeriesRepository()
    private val userDataRepository = UserDataRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        val contentFlow = combine(
            movieRepository.getPublishedMovies(),
            seriesRepository.getPublishedSeries()
        ) { movies, series -> movies to series }

        val userFlow = combine(
            userDataRepository.continueWatchingList,
            userDataRepository.favoritesList
        ) { cw, favs -> cw to favs }

        viewModelScope.launch {
            combine(
                contentFlow,
                userFlow,
                _isRefreshing,
                movieRepository.lastError
            ) { (movies, series), (continueWatching, favorites), refreshing, errorMsg ->
                val favIds = favorites.map { it.movieId }.toSet()

                if (movies.isEmpty() && series.isEmpty()) {
                    HomeUiState(
                        isLoading = false,
                        isRefreshing = refreshing,
                        continueWatching = continueWatching,
                        favoritesIds = favIds,
                        errorMessage = errorMsg
                    )
                } else {
                    val explicitFeatured = movies.filter { it.featured }
                    val sliderMovies = if (explicitFeatured.size >= 3) {
                        explicitFeatured.take(5)
                    } else {
                        (explicitFeatured + movies.sortedByDescending { it.rating })
                            .distinctBy { it.id }
                            .take(5)
                    }
                    val featured = sliderMovies.firstOrNull()
                    val trending = movies.sortedByDescending { it.rating }
                    val latest = movies.sortedByDescending { it.year ?: 0 }
                    val sortedSeries = series.sortedByDescending { it.rating }

                    // Group dynamic genres
                    val defaultGenres = listOf("Action", "Comedy", "Drama", "African", "Gospel", "Animation", "Romance", "Horror", "Family")
                    val existingGenres = movies.flatMap { it.genres }.distinct()
                    val genresToDisplay = (defaultGenres + existingGenres).distinct()

                    val genreMap = mutableMapOf<String, List<Movie>>()
                    for (genre in genresToDisplay) {
                        val matching = movies.filter { m ->
                            m.genres.any { it.equals(genre, ignoreCase = true) }
                        }
                        if (matching.isNotEmpty()) {
                            genreMap[genre] = matching
                        }
                    }

                    HomeUiState(
                        isLoading = false,
                        isRefreshing = refreshing,
                        featuredMovie = featured,
                        featuredMovies = sliderMovies,
                        continueWatching = continueWatching,
                        trendingMovies = trending,
                        trendingSeries = sortedSeries,
                        latestMovies = latest,
                        genreSections = genreMap,
                        favoritesIds = favIds,
                        errorMessage = errorMsg
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
        }
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            userDataRepository.toggleFavorite(movie)
        }
    }
}
