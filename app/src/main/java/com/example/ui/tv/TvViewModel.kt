package com.example.ui.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TvChannel
import com.example.data.repository.TvRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TvUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedCategory: String = "All",
    val channels: List<TvChannel> = emptyList(),
    val filteredChannels: List<TvChannel> = emptyList(),
    val categories: List<String> = emptyList(),
    val errorMessage: String? = null
)

class TvViewModel : ViewModel() {
    private val tvRepository = TvRepository()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<TvUiState> = combine(
        tvRepository.getPublishedChannels(),
        _selectedCategory,
        _isRefreshing,
        tvRepository.lastError
    ) { channels, category, refreshing, errorMsg ->
        val cats = listOf("All") + channels.mapNotNull { it.category }.distinct()
        val filtered = if (category == "All") {
            channels
        } else {
            channels.filter { it.category.equals(category, ignoreCase = true) }
        }

        TvUiState(
            isLoading = false,
            isRefreshing = refreshing,
            selectedCategory = category,
            channels = channels,
            filteredChannels = filtered,
            categories = cats,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TvUiState()
    )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
        }
    }
}
