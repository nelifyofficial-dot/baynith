package com.example.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NeliPlayDatabase
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.DownloadState
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.payment.harakapay.HarakaPayClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.DownloadRepository
import com.example.data.repository.EpisodeRepository
import com.example.data.repository.MovieRepository
import com.example.data.repository.SubscriptionManager
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class QuickPayStatus {
    object Idle : QuickPayStatus()
    data class Processing(val message: String) : QuickPayStatus()
    data class WaitingForUssd(val orderId: String, val message: String) : QuickPayStatus()
    data class Success(val orderId: String, val message: String) : QuickPayStatus()
    data class Error(val message: String) : QuickPayStatus()
}

data class MovieDetailsUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val isFavorite: Boolean = false,
    val isUserPremium: Boolean = false,
    val isMovieUnlocked: Boolean = true,
    val downloadEntity: DownloadEntity? = null,
    val similarMovies: List<Movie> = emptyList(),
    val seasons: List<Int> = emptyList(),
    val selectedSeason: Int = 1,
    val episodes: List<Episode> = emptyList(),
    val currentSeasonEpisodes: List<Episode> = emptyList(),
    val hasEpisodes: Boolean = false,
    val error: String? = null,
    val quickPayStatus: QuickPayStatus = QuickPayStatus.Idle
)

class MovieDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepo = AuthRepository()
    private val movieRepo = MovieRepository()
    private val episodeRepo = EpisodeRepository()
    private val userDataRepo = UserDataRepository(application)
    private val db = NeliPlayDatabase.getDatabase(application)
    private val downloadRepo = DownloadRepository(application, db.downloadDao())

    private val userProfileFlow = authRepo.currentUserFlow.flatMapLatest { user ->
        if (user != null) {
            authRepo.observeUserProfile(user.uid)
        } else {
            flowOf(null)
        }
    }

    private val _selectedSeason = MutableStateFlow<Int?>(null)
    private val _quickPayStatus = MutableStateFlow<QuickPayStatus>(QuickPayStatus.Idle)
    private val _uiState = MutableStateFlow(MovieDetailsUiState())
    val uiState: StateFlow<MovieDetailsUiState> = _uiState.asStateFlow()

    private var statusPollJob: Job? = null

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
                combine(
                    userDataRepo.isFavorite(movieId),
                    downloadRepo.observeDownload(movieId),
                    movieRepo.getPublishedMovies()
                ) { isFav, download, allMovies ->
                    Triple(isFav, download, allMovies)
                },
                combine(
                    userProfileFlow,
                    SubscriptionManager.state,
                    _quickPayStatus
                ) { userProfile, subState, quickPay ->
                    Triple(userProfile, subState, quickPay)
                }
            ) { (movie, episodes, selectedSeason), (isFav, download, allMovies), (userProfile, subState, quickPay) ->
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

                    // VIP and plans hidden for now, all movies are unlocked like Netflix
                    val isPremiumUser = true
                    val isUnlocked = true

                    MovieDetailsUiState(
                        isLoading = false,
                        movie = movie,
                        isFavorite = isFav,
                        isUserPremium = isPremiumUser,
                        isMovieUnlocked = isUnlocked,
                        downloadEntity = download,
                        similarMovies = similar,
                        seasons = seasonsList,
                        selectedSeason = activeSeason,
                        episodes = episodes,
                        currentSeasonEpisodes = seasonEpisodes,
                        hasEpisodes = episodes.isNotEmpty(),
                        quickPayStatus = quickPay
                    )
                } else {
                    val err = movieRepo.lastError.value ?: "Maelezo ya filamu hayakupatikana."
                    MovieDetailsUiState(
                        isLoading = false,
                        error = err,
                        quickPayStatus = quickPay
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

    /**
     * Fast HarakaPay Payment for TSh 100 single movie unlock
     */
    fun payForMovie(phone: String) {
        val movie = _uiState.value.movie ?: return
        val rawPhone = phone.trim()
        val digits = rawPhone.replace(Regex("[^0-9+]"), "")
        if (digits.length < 9) {
            _quickPayStatus.value = QuickPayStatus.Error("Tafadhali weka namba sahihi ya simu ya Tanzania (mfano: 07XXXXXXXX au 06XXXXXXXX).")
            return
        }
        val normalizedPhone = HarakaPayClient.normalizePhoneNumber(rawPhone)

        viewModelScope.launch {
            _quickPayStatus.value = QuickPayStatus.Processing("Inatuma ombi la malipo...")
            val result = HarakaPayClient.collectPayment(
                phone = normalizedPhone,
                amount = 100,
                description = "NeliPlay Movie: ${movie.title}"
            )

            result.fold(
                onSuccess = { resp ->
                    val orderId = resp.orderId ?: "HP${System.currentTimeMillis()}"
                    _quickPayStatus.value = QuickPayStatus.WaitingForUssd(
                        orderId = orderId,
                        message = resp.message ?: "Tumetuma ombi la malipo kwenye simu yako ($normalizedPhone). Tafadhali thibitisha kwenye simu yako kwa kuweka PIN."
                    )
                    // Auto-poll status every 4 seconds for up to 60 seconds
                    startPollingPaymentStatus(orderId, movie.id)
                },
                onFailure = { err ->
                    _quickPayStatus.value = QuickPayStatus.Error(err.message ?: "Hitilafu katika mfumo wa HarakaPay. Jaribu tena.")
                }
            )
        }
    }

    fun checkPaymentStatus(orderId: String) {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            val res = HarakaPayClient.checkStatus(orderId)
            res.fold(
                onSuccess = { statusResp ->
                    if (statusResp.isCompleted) {
                        SubscriptionManager.unlockMovie(movie.id, orderId)
                        _quickPayStatus.value = QuickPayStatus.Success(
                            orderId = orderId,
                            message = "Malipo yamefanikiwa! Sasa unaweza kutazama ${movie.title}."
                        )
                        statusPollJob?.cancel()
                    } else if (statusResp.isFailed) {
                        _quickPayStatus.value = QuickPayStatus.Error("Malipo yamekataliwa au hayakukamilika.")
                        statusPollJob?.cancel()
                    }
                },
                onFailure = {
                    // Check failed
                }
            )
        }
    }

    private fun startPollingPaymentStatus(orderId: String, movieId: String) {
        statusPollJob?.cancel()
        statusPollJob = viewModelScope.launch {
            repeat(15) { // 15 times x 4s = 60s
                delay(4000)
                val res = HarakaPayClient.checkStatus(orderId)
                val status = res.getOrNull()
                if (status?.isCompleted == true) {
                    SubscriptionManager.unlockMovie(movieId, orderId)
                    _quickPayStatus.value = QuickPayStatus.Success(
                        orderId = orderId,
                        message = "Malipo yamefanikiwa! Sasa unaweza kutazama filamu hii."
                    )
                    return@launch
                }
            }
        }
    }

    fun unlockMovie() {
        val currentMovie = _uiState.value.movie
        if (currentMovie != null) {
            SubscriptionManager.unlockMovie(currentMovie.id)
            _uiState.update { it.copy(isMovieUnlocked = true) }
        }
    }

    fun resetQuickPay() {
        statusPollJob?.cancel()
        _quickPayStatus.value = QuickPayStatus.Idle
    }
}
