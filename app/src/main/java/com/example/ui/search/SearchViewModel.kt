package com.example.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Movie
import com.example.data.model.Series
import com.example.data.model.TvChannel
import com.example.data.repository.MovieRepository
import com.example.data.repository.SeriesRepository
import com.example.data.repository.TvRepository
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedFilter: String = "All", // "All", "Movies", "Live Streams", "Series"
    val selectedGenre: String = "All",
    val availableGenres: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val channels: List<TvChannel> = emptyList(),
    val popularMovies: List<Movie> = emptyList(),
    val totalMovieCount: Int = 0,
    val totalChannelCount: Int = 0,
    val isSearching: Boolean = false
) {
    val totalResults: Int get() = movies.size + series.size + channels.size
    val isFilterActive: Boolean get() = query.isNotBlank() || selectedGenre != "All" || selectedFilter != "All"
    val isEmpty: Boolean get() = !isSearching && totalResults == 0
}

private data class SearchParams(
    val query: String,
    val filter: String,
    val genre: String
)

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val movieRepository = MovieRepository()
    private val seriesRepository = SeriesRepository()
    private val tvRepository = TvRepository()
    private val userDataRepo = UserDataRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private val _isSearching = MutableStateFlow(false)

    val popularMovies: StateFlow<List<Movie>> = movieRepository.getTrendingMovies()
        .map { it.take(8) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentSearches: StateFlow<List<String>> = userDataRepo.recentSearches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val allMoviesFlow = movieRepository.getPublishedMovies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allChannelsFlow = tvRepository.getPublishedChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allSeriesFlow = seriesRepository.getPublishedSeries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic combined genres extracted from movies, live TV channels, and series
    private val defaultGenres = listOf(
        "All", "Action", "Comedy", "Drama", "News", "Sports",
        "Horror", "Sci-Fi", "Romance", "Thriller", "Animation",
        "Documentary", "Music", "Family"
    )

    private val availableGenresFlow = combine(allMoviesFlow, allChannelsFlow, allSeriesFlow) { movies, channels, series ->
        val movieGenres = movies.flatMap { it.genres }
        val channelCategories = channels.mapNotNull { it.category }
        val seriesGenres = series.flatMap { it.genres }

        val dynamicSet = (movieGenres + channelCategories + seriesGenres)
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
            .toSet()

        val unionList = mutableListOf("All")
        defaultGenres.filter { it != "All" }.forEach { genre ->
            if (unionList.none { it.equals(genre, ignoreCase = true) }) {
                unionList.add(genre)
            }
        }
        dynamicSet.sorted().forEach { genre ->
            if (unionList.none { it.equals(genre, ignoreCase = true) }) {
                unionList.add(genre)
            }
        }
        unionList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultGenres)

    private val debouncedQuery = _searchQuery
        .debounce(150L)
        .distinctUntilChanged()

    private val searchParamsFlow = combine(debouncedQuery, _selectedFilter, _selectedGenre) { query, filter, genre ->
        SearchParams(query, filter, genre)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        allMoviesFlow,
        allChannelsFlow,
        allSeriesFlow,
        searchParamsFlow
    ) { allMovies, allChannels, allSeries, params ->
        val cleanQuery = params.query.trim().lowercase()
        val isGenreFilterActive = params.genre != "All" && params.genre.isNotBlank()

        // Filter Movies by title, original title, overview, or genres
        val filteredMovies = allMovies.filter { movie ->
            val matchesGenre = !isGenreFilterActive || movie.genres.any { it.equals(params.genre, ignoreCase = true) }
            val matchesQuery = cleanQuery.isEmpty() ||
                    movie.title.lowercase().contains(cleanQuery) ||
                    (movie.originalTitle?.lowercase()?.contains(cleanQuery) == true) ||
                    movie.genres.any { it.lowercase().contains(cleanQuery) } ||
                    movie.overview.lowercase().contains(cleanQuery)
            matchesGenre && matchesQuery
        }

        // Filter Live Streams (TV Channels) by title (channel name), category/genre, or country
        val filteredChannels = allChannels.filter { ch ->
            val matchesGenre = !isGenreFilterActive ||
                    (ch.category?.equals(params.genre, ignoreCase = true) == true) ||
                    ch.name.contains(params.genre, ignoreCase = true)
            val matchesQuery = cleanQuery.isEmpty() ||
                    ch.name.lowercase().contains(cleanQuery) ||
                    (ch.category?.lowercase()?.contains(cleanQuery) == true) ||
                    (ch.country?.lowercase()?.contains(cleanQuery) == true) ||
                    (ch.description?.lowercase()?.contains(cleanQuery) == true)
            matchesGenre && matchesQuery
        }

        // Filter Series by title or genres
        val filteredSeries = allSeries.filter { s ->
            val matchesGenre = !isGenreFilterActive || s.genres.any { it.equals(params.genre, ignoreCase = true) }
            val matchesQuery = cleanQuery.isEmpty() ||
                    s.name.lowercase().contains(cleanQuery) ||
                    s.genres.any { it.lowercase().contains(cleanQuery) } ||
                    s.overview.lowercase().contains(cleanQuery)
            matchesGenre && matchesQuery
        }

        // Apply type filter: "All", "Movies", "Live Streams" (or "Live TV"), "Series"
        val showMovies = params.filter == "All" || params.filter == "Movies"
        val showChannels = params.filter == "All" || params.filter == "Live Streams" || params.filter == "Live TV"
        val showSeries = params.filter == "All" || params.filter == "Series"

        SearchUiState(
            query = params.query,
            selectedFilter = params.filter,
            selectedGenre = params.genre,
            availableGenres = availableGenresFlow.value,
            recentSearches = recentSearches.value,
            movies = if (showMovies) filteredMovies else emptyList(),
            channels = if (showChannels) filteredChannels else emptyList(),
            series = if (showSeries) filteredSeries else emptyList(),
            popularMovies = popularMovies.value,
            totalMovieCount = allMovies.size,
            totalChannelCount = allChannels.size,
            isSearching = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState(availableGenres = defaultGenres)
    )

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelect(filter: String) {
        _selectedFilter.value = filter
    }

    fun onGenreSelect(genre: String) {
        _selectedGenre.value = genre
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedFilter.value = "All"
        _selectedGenre.value = "All"
    }

    fun commitSearch(query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch {
                userDataRepo.addRecentSearch(query.trim())
            }
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            userDataRepo.removeRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            userDataRepo.clearRecentSearches()
        }
    }

    fun clearQuery() {
        _searchQuery.value = ""
    }
}
