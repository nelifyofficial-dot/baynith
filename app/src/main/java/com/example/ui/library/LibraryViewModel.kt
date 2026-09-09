package com.example.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.FavoriteEntity
import com.example.data.local.entities.WatchProgressEntity
import com.example.data.model.Movie
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val favorites: List<FavoriteEntity> = emptyList(),
    val watchHistory: List<WatchProgressEntity> = emptyList()
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val userDataRepo = UserDataRepository(application)

    val uiState: StateFlow<LibraryUiState> = combine(
        userDataRepo.favoritesList,
        userDataRepo.continueWatchingList
    ) { favs, history ->
        LibraryUiState(
            favorites = favs,
            watchHistory = history
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun removeFavorite(movieId: String) {
        viewModelScope.launch {
            userDataRepo.toggleFavorite(Movie(id = movieId))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            userDataRepo.clearWatchHistory()
        }
    }
}
