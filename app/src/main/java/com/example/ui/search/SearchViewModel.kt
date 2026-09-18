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
    val selectedFilter: String = "All", // "All", "Movies", "Series", "TV Channels"
    val selectedGenre: String = "All",
    val availableGenres: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
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
        .debounce(40L)
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
        val queryTokens = cleanQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // --- SMART RELEVANCE RANKING ENGINE ---

        // 1. Movie matching & scoring
        val scoredMovies = allMovies.mapNotNull { movie ->
            val matchesGenre = !isGenreFilterActive || movie.genres.any { it.equals(params.genre, ignoreCase = true) }
            if (!matchesGenre) return@mapNotNull null

            if (cleanQuery.isEmpty()) {
                return@mapNotNull Pair(movie, movie.rating.toFloat())
            }

            val titleLower = movie.title.lowercase()
            val origLower = (movie.originalTitle ?: "").lowercase()
            val overviewLower = movie.overview.lowercase()
            val genresLower = movie.genres.map { it.lowercase() }

            var score = 0f

            // Exact match
            if (titleLower == cleanQuery) {
                score += 120f
            } else if (titleLower.startsWith(cleanQuery)) {
                score += 80f
            } else if (titleLower.contains(cleanQuery)) {
                score += 50f
            }

            // Token based match
            val matchingTokens = queryTokens.count { token ->
                titleLower.contains(token) || origLower.contains(token) ||
                        genresLower.any { it.contains(token) } || overviewLower.contains(token)
            }

            if (matchingTokens == queryTokens.size) {
                score += 40f
            } else if (matchingTokens > 0) {
                score += matchingTokens * 15f
            }

            if (genresLower.any { it.contains(cleanQuery) }) {
                score += 25f
            }

            if (origLower.contains(cleanQuery)) {
                score += 30f
            }

            if (overviewLower.contains(cleanQuery)) {
                score += 10f
            }

            if (score > 0) {
                Pair(movie, score + movie.rating.toFloat())
            } else {
                null
            }
        }.sortedByDescending { it.second }.map { it.first }

        // 2. TV Channel matching & scoring
        val scoredChannels = allChannels.mapNotNull { ch ->
            val matchesGenre = !isGenreFilterActive ||
                    (ch.category?.equals(params.genre, ignoreCase = true) == true) ||
                    ch.name.contains(params.genre, ignoreCase = true)
            if (!matchesGenre) return@mapNotNull null

            if (cleanQuery.isEmpty()) {
                return@mapNotNull Pair(ch, 10f)
            }

            val nameLower = ch.name.lowercase()
            val catLower = (ch.category ?: "").lowercase()
            val countryLower = (ch.country ?: "").lowercase()
            val descLower = (ch.description ?: "").lowercase()

            var score = 0f

            if (nameLower == cleanQuery) {
                score += 100f
            } else if (nameLower.startsWith(cleanQuery)) {
                score += 70f
            } else if (nameLower.contains(cleanQuery)) {
                score += 40f
            }

            val matchingTokens = queryTokens.count { token ->
                nameLower.contains(token) || catLower.contains(token) ||
                        countryLower.contains(token) || descLower.contains(token)
            }

            if (matchingTokens == queryTokens.size) {
                score += 35f
            } else if (matchingTokens > 0) {
                score += matchingTokens * 12f
            }

            if (catLower.contains(cleanQuery)) {
                score += 20f
            }

            if (score > 0) {
                Pair(ch, score)
            } else {
                null
            }
        }.sortedByDescending { it.second }.map { it.first }

        // 3. Series matching & scoring
        val scoredSeries = allSeries.mapNotNull { series ->
            val matchesGenre = !isGenreFilterActive || series.genres.any { it.equals(params.genre, ignoreCase = true) }
            if (!matchesGenre) return@mapNotNull null

            if (cleanQuery.isEmpty()) {
                return@mapNotNull Pair(series, series.rating.toFloat())
            }

            val nameLower = series.name.lowercase()
            val overviewLower = series.overview.lowercase()
            val genresLower = series.genres.map { it.lowercase() }

            var score = 0f

            if (nameLower == cleanQuery) {
                score += 120f
            } else if (nameLower.startsWith(cleanQuery)) {
                score += 80f
            } else if (nameLower.contains(cleanQuery)) {
                score += 50f
            }

            val matchingTokens = queryTokens.count { token ->
                nameLower.contains(token) || genresLower.any { it.contains(token) } || overviewLower.contains(token)
            }

            if (matchingTokens == queryTokens.size) {
                score += 40f
            } else if (matchingTokens > 0) {
                score += matchingTokens * 15f
            }

            if (genresLower.any { it.contains(cleanQuery) }) {
                score += 25f
            }

            if (score > 0) {
                Pair(series, score + series.rating.toFloat())
            } else {
                null
            }
        }.sortedByDescending { it.second }.map { it.first }

        // Filter types: "All", "Movies", "Series", "TV Channels"
        val showMovies = params.filter == "All" || params.filter == "Movies"
        val showChannels = params.filter == "All" || params.filter == "TV Channels" || params.filter == "Live Streams" || params.filter == "Live TV"
        val showSeries = params.filter == "All" || params.filter == "TV Shows" || params.filter == "Series"

        // Autocomplete suggestions as user types
        val suggestions = if (cleanQuery.isNotEmpty()) {
            val fromRecent = recentSearches.value.filter { it.lowercase().contains(cleanQuery) }
            val fromMovies = allMovies.filter { it.title.lowercase().contains(cleanQuery) }.map { it.title }
            val fromSeries = allSeries.filter { it.name.lowercase().contains(cleanQuery) }.map { it.name }
            val fromChannels = allChannels.filter { it.name.lowercase().contains(cleanQuery) }.map { it.name }
            (fromRecent + fromMovies + fromSeries + fromChannels).distinct().take(8)
        } else {
            emptyList()
        }

        SearchUiState(
            query = _searchQuery.value,
            selectedFilter = params.filter,
            selectedGenre = params.genre,
            availableGenres = availableGenresFlow.value,
            recentSearches = recentSearches.value,
            suggestions = suggestions,
            movies = if (showMovies) scoredMovies else emptyList(),
            channels = if (showChannels) scoredChannels else emptyList(),
            series = if (showSeries) scoredSeries else emptyList(),
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
            val trimmed = query.trim()
            _searchQuery.value = trimmed
            viewModelScope.launch {
                userDataRepo.addRecentSearch(trimmed)
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
