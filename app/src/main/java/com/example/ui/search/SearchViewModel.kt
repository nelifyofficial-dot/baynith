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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedFilter: String = "All", // "All", "Movies", "Series", "Live TV"
    val recentSearches: List<String> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val channels: List<TvChannel> = emptyList(),
    val popularMovies: List<Movie> = emptyList(),
    val isSearching: Boolean = false
) {
    val totalResults: Int get() = movies.size + series.size + channels.size
    val isEmpty: Boolean get() = query.isNotBlank() && !isSearching && totalResults == 0
}

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

    private val searchResultsFlow = _searchQuery
        .debounce(250L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                _isSearching.value = false
                flowOf(Triple(emptyList<Movie>(), emptyList<Series>(), emptyList<TvChannel>()))
            } else {
                _isSearching.value = true
                combine(
                    movieRepository.searchMovies(query),
                    seriesRepository.searchSeries(query),
                    tvRepository.searchChannels(query)
                ) { m, s, c ->
                    _isSearching.value = false
                    Triple(m, s, c)
                }
            }
        }

    private val searchParamsFlow = combine(_searchQuery, _selectedFilter, _isSearching) { q, filter, searching ->
        Triple(q, filter, searching)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        searchParamsFlow,
        recentSearches,
        popularMovies,
        searchResultsFlow
    ) { (q, filter, searching), recents, popular, (m, s, c) ->
        val filteredMovies = if (filter == "All" || filter == "Movies") m else emptyList()
        val filteredSeries = if (filter == "All" || filter == "Series") s else emptyList()
        val filteredChannels = if (filter == "All" || filter == "Live TV") c else emptyList()

        SearchUiState(
            query = q,
            selectedFilter = filter,
            recentSearches = recents,
            movies = filteredMovies,
            series = filteredSeries,
            channels = filteredChannels,
            popularMovies = popular,
            isSearching = searching
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelect(filter: String) {
        _selectedFilter.value = filter
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
