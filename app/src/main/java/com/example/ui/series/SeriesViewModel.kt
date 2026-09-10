package com.example.ui.series

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Series
import com.example.data.repository.SeriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SeriesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val featuredSeries: List<Series> = emptyList(),
    val allSeries: List<Series> = emptyList(),
    val filteredSeries: List<Series> = emptyList(),
    val selectedGenre: String = "All",
    val availableGenres: List<String> = emptyList(),
    val errorMessage: String? = null
)

class SeriesViewModel(application: Application) : AndroidViewModel(application) {
    private val seriesRepository = SeriesRepository()

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    private val _isRefreshing = MutableStateFlow(false)

    init {
        loadSeries()
    }

    private fun loadSeries() {
        viewModelScope.launch {
            combine(
                seriesRepository.getPublishedSeries(),
                _selectedGenre,
                _isRefreshing,
                seriesRepository.lastError
            ) { series, genre, refreshing, errorMsg ->
                val genres = (listOf("All") + series.flatMap { it.genres }.distinct().sorted())
                val filtered = if (genre == "All") {
                    series
                } else {
                    series.filter { s -> s.genres.any { it.equals(genre, ignoreCase = true) } }
                }
                val featured = series.filter { it.featured }.ifEmpty { series.take(3) }

                SeriesUiState(
                    isLoading = false,
                    isRefreshing = refreshing,
                    featuredSeries = featured,
                    allSeries = series,
                    filteredSeries = filtered,
                    selectedGenre = genre,
                    availableGenres = genres,
                    errorMessage = errorMsg
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectGenre(genre: String) {
        _selectedGenre.value = genre
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Allow Firestore listener to deliver or reset
            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
        }
    }
}
