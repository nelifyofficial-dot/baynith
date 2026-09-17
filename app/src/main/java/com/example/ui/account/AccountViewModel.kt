package com.example.ui.account

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserDataRepository
import com.example.util.RegionService
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountUiState(
    val currentUser: FirebaseUser? = null,
    val profile: UserProfile? = null,
    val userCountry: String = "",
    val isSigningIn: Boolean = false,
    val isSigningUp: Boolean = false,
    val isGuestSigningIn: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val favoritesCount: Int = 0,
    val continueWatchingCount: Int = 0,
    val message: String? = null,
    val error: String? = null
) {
    val effectiveCountryCode: String
        get() = when {
            !profile?.country.isNullOrBlank() -> profile!!.country!!
            userCountry.isNotBlank() -> userCountry
            else -> RegionService.DEFAULT_COUNTRY_CODE
        }

    val countryObj get() = RegionService.getCountry(effectiveCountryCode)
}

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = AuthRepository()
    private val userDataRepo = UserDataRepository(application)

    private val _isSigningIn = MutableStateFlow(false)
    private val _isSigningUp = MutableStateFlow(false)
    private val _isGuestSigningIn = MutableStateFlow(false)
    private val _isDeleting = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    private val statsFlow = combine(
        userDataRepo.favoritesList,
        userDataRepo.continueWatchingList
    ) { favs, cw -> favs.size to cw.size }

    private val loadingStateFlow = combine(
        _isSigningIn,
        _isSigningUp,
        _isGuestSigningIn,
        _isDeleting
    ) { signin, signup, guest, deleting ->
        listOf(signin, signup, guest, deleting)
    }

    private val messageFlow = combine(_message, _error) { msg, err ->
        msg to err
    }

    val uiState: StateFlow<AccountUiState> = authRepo.currentUserFlow
        .flatMapLatest { user ->
            val profileFlow = if (user != null) authRepo.observeUserProfile(user.uid) else flowOf(null)
            combine(
                profileFlow,
                userDataRepo.userCountry,
                statsFlow,
                loadingStateFlow,
                messageFlow
            ) { profile, localCountry, (favCount, cwCount), loadingList, (msg, err) ->
                AccountUiState(
                    currentUser = user,
                    profile = profile,
                    userCountry = localCountry,
                    isSigningIn = loadingList[0],
                    isSigningUp = loadingList[1],
                    isGuestSigningIn = loadingList[2],
                    isDeletingAccount = loadingList[3],
                    favoritesCount = favCount,
                    continueWatchingCount = cwCount,
                    message = msg,
                    error = err
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountUiState(currentUser = authRepo.currentUser)
        )

    init {
        // If logged in initially, sync user data
        viewModelScope.launch {
            val user = authRepo.currentUser
            if (user != null) {
                userDataRepo.syncUserDataWithFirebase(user.uid)
            }
        }
    }

    fun signIn(emailOrUsername: String, pass: String) {
        if (emailOrUsername.isBlank() || pass.isBlank()) {
            _error.value = "Tafadhali jaza barua pepe/jina na nenosiri."
            return
        }
        viewModelScope.launch {
            _isSigningIn.value = true
            _error.value = null
            _message.value = null

            val result = authRepo.signInWithEmail(emailOrUsername, pass)
            _isSigningIn.value = false

            result.onSuccess { user ->
                val currentCountry = userDataRepo.userCountry.firstOrNull() ?: RegionService.DEFAULT_COUNTRY_CODE
                authRepo.syncUserProfile(user, country = currentCountry)
                userDataRepo.syncUserDataWithFirebase(user.uid)
                _message.value = "Karibu tena, ${user.displayName ?: "Mtumiaji"}!"
            }.onFailure { err ->
                _error.value = err.message ?: "Kuingia kumeshindwa."
            }
        }
    }

    fun signUp(email: String, pass: String, username: String) {
        if (email.isBlank() || pass.isBlank() || username.isBlank()) {
            _error.value = "Tafadhali jaza jina la mtumiaji, barua pepe, na nenosiri."
            return
        }
        if (pass.length < 6) {
            _error.value = "Nenosiri linapaswa kuwa na angalau herufi 6."
            return
        }
        viewModelScope.launch {
            _isSigningUp.value = true
            _error.value = null
            _message.value = null

            val result = authRepo.signUpWithEmail(email, pass, username)
            _isSigningUp.value = false

            result.onSuccess { user ->
                val currentCountry = userDataRepo.userCountry.firstOrNull() ?: RegionService.DEFAULT_COUNTRY_CODE
                authRepo.syncUserProfile(user, country = currentCountry)
                userDataRepo.syncUserDataWithFirebase(user.uid)
                _message.value = "Akaunti yako imeundwa vizuri! Karibu, ${user.displayName ?: username}!"
            }.onFailure { err ->
                _error.value = err.message ?: "Usajili umeshindwa."
            }
        }
    }

    fun signInAsGuest() {
        viewModelScope.launch {
            _isGuestSigningIn.value = true
            _error.value = null
            _message.value = null

            val result = authRepo.signInAnonymously()
            _isGuestSigningIn.value = false

            result.onSuccess { user ->
                val currentCountry = userDataRepo.userCountry.firstOrNull() ?: RegionService.DEFAULT_COUNTRY_CODE
                authRepo.syncUserProfile(user, country = currentCountry)
                userDataRepo.syncUserDataWithFirebase(user.uid)
                _message.value = "Karibu kama Mgeni!"
            }.onFailure { err ->
                _error.value = err.message ?: "Kuingia kama mgeni kumeshindwa."
            }
        }
    }

    fun selectCountry(countryCode: String) {
        viewModelScope.launch {
            val upper = countryCode.trim().uppercase()
            userDataRepo.setUserCountry(upper)
            val user = authRepo.currentUser
            if (user != null) {
                authRepo.updateUserCountry(user.uid, upper)
            }
            val countryName = RegionService.getCountryName(upper)
            _message.value = "Nchi imebadilishwa kuwa $countryName"
        }
    }

    fun signOut(context: Context? = null) {
        viewModelScope.launch {
            authRepo.signOut(context)
            _message.value = "Umetoka kwenye akaunti kikamilifu."
        }
    }

    fun deleteAccount(context: Context? = null) {
        viewModelScope.launch {
            _isDeleting.value = true
            _error.value = null

            val result = authRepo.deleteAccount(context)
            _isDeleting.value = false

            result.onSuccess {
                _message.value = "Akaunti yako imefutwa kikamilifu."
            }.onFailure { err ->
                _error.value = err.message ?: "Kufuta akaunti kumeshindwa."
            }
        }
    }

    fun clearMessage() {
        _message.value = null
        _error.value = null
    }
}
