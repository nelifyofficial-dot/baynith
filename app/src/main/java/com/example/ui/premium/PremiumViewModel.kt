package com.example.ui.premium

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Payment
import com.example.data.model.PaymentStatus
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.CreatePaymentResult
import com.example.data.repository.PremiumRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PremiumPlanItem(
    val id: String, // "daily", "weekly", "monthly"
    val title: String,
    val durationLabel: String,
    val priceTzs: Long,
    val formattedPrice: String,
    val badge: String? = null,
    val description: String
)

data class PremiumUiState(
    val currentUser: FirebaseUser? = null,
    val profile: UserProfile? = null,
    val selectedPlan: String = "monthly",
    val phoneNumber: String = "",
    val paymentStatus: PaymentStatus = PaymentStatus.IDLE,
    val currentPayment: Payment? = null,
    val createdPaymentResult: CreatePaymentResult? = null,
    val showLoginPrompt: Boolean = false,
    val message: String? = null,
    val error: String? = null
) {
    val isLoggedIn: Boolean get() = currentUser != null && !currentUser.isAnonymous
    val isSubscriptionActive: Boolean get() = profile?.isSubscriptionActive == true
}

class PremiumViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = AuthRepository()
    private val premiumRepo = PremiumRepository()

    val availablePlans = listOf(
        PremiumPlanItem(
            id = "daily",
            title = "SIKU 1",
            durationLabel = "Siku 1 (Masaa 24)",
            priceTzs = 1000L,
            formattedPrice = "TSh 1,000",
            badge = "Rahisi",
            description = "Tazama maudhui yote ya NeliPlay kwa siku moja kamili bila kikomo."
        ),
        PremiumPlanItem(
            id = "weekly",
            title = "WIKI 1",
            durationLabel = "Siku 7 (Wiki Nzima)",
            priceTzs = 3000L,
            formattedPrice = "TSh 3,000",
            badge = "Maarufu Zaidi",
            description = "Furahia filamu na vipindi kwa wiki 1 nzima kwa gharama nafuu."
        ),
        PremiumPlanItem(
            id = "monthly",
            title = "MWEZI 1",
            durationLabel = "Siku 30 (Mwezi Mzima)",
            priceTzs = 10000L,
            formattedPrice = "TSh 10,000",
            badge = "Thamani Bora",
            description = "Pata uhuru kamili wa Premium kwa mwezi mzima bila matangazo."
        )
    )

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private var paymentObservationJob: Job? = null
    private var autoVerifyJob: Job? = null

    init {
        // Observe Auth & User Profile
        viewModelScope.launch {
            authRepo.currentUserFlow
                .flatMapLatest { user ->
                    if (user != null) {
                        combine(
                            flowOf(user),
                            authRepo.observeUserProfile(user.uid)
                        ) { u, p -> Pair(u, p) }
                    } else {
                        flowOf(Pair(null, null))
                    }
                }
                .collectLatest { (user, profile) ->
                    _uiState.update { current ->
                        current.copy(
                            currentUser = user,
                            profile = profile,
                            // Prefill phone if available and current input is empty
                            phoneNumber = if (current.phoneNumber.isBlank() && !profile?.phoneNumber.isNullOrBlank()) {
                                profile!!.phoneNumber
                            } else {
                                current.phoneNumber
                            }
                        )
                    }
                }
        }
    }

    fun selectPlan(planId: String) {
        _uiState.update { it.copy(selectedPlan = planId.lowercase().trim(), error = null) }
    }

    fun onPhoneNumberChanged(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone, error = null) }
    }

    fun dismissLoginPrompt() {
        _uiState.update { it.copy(showLoginPrompt = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun resetPaymentState() {
        paymentObservationJob?.cancel()
        paymentObservationJob = null
        autoVerifyJob?.cancel()
        autoVerifyJob = null
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.IDLE,
                currentPayment = null,
                createdPaymentResult = null,
                error = null,
                message = null
            )
        }
    }

    /**
     * Sanitizes phone number to Tanzanian standard format:
     */
    private fun sanitizePhone(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        if (digits.startsWith("0") && (digits.startsWith("06") || digits.startsWith("07"))) {
            return "255" + digits.substring(1)
        }
        if (digits.startsWith("255") && digits.length == 12) {
            return digits
        }
        return digits
    }

    private fun isValidTzPhone(phone: String): Boolean {
        val sanitized = sanitizePhone(phone)
        return Regex("^255(6|7)\\d{8}$").matches(sanitized)
    }

    /**
     * Initiates PalmPesa payment flow via Firebase Cloud Functions
     */
    fun startPayment() {
        val state = _uiState.value
        val user = state.currentUser

        // User MUST be logged in and not an anonymous user
        if (user == null || user.isAnonymous) {
            _uiState.update { it.copy(showLoginPrompt = true) }
            return
        }

        val rawPhone = state.phoneNumber.trim()
        val sanitizedPhone = sanitizePhone(rawPhone)

        if (!isValidTzPhone(sanitizedPhone)) {
            _uiState.update {
                it.copy(
                    error = "Weka nambari sahihi ya simu ya Tanzania (mfano 0712345678 au 0612345678)."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.CREATING_PAYMENT,
                error = null,
                message = "Inatayarisha malipo ya PalmPesa..."
            )
        }

        viewModelScope.launch {
            val result = premiumRepo.createPayment(
                plan = state.selectedPlan,
                phone = sanitizedPhone,
                buyerName = state.profile?.displayName ?: user.displayName ?: "NeliPlay User",
                buyerEmail = state.profile?.email ?: user.email ?: ""
            )

            result.onSuccess { paymentResult ->
                _uiState.update {
                    it.copy(
                        paymentStatus = PaymentStatus.WAITING_FOR_PAYMENT,
                        createdPaymentResult = paymentResult,
                        message = "Ombi limetumwa kwenye simu yako ($sanitizedPhone). Weka PIN yako kukamilisha malipo."
                    )
                }

                // Start observing payment record from Firestore
                observePaymentRecord(paymentResult.paymentId)

                // Start automatic verification after 15 seconds as fallback
                startAutoVerification(paymentResult.paymentId, paymentResult.orderId)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        paymentStatus = PaymentStatus.PAYMENT_FAILED,
                        error = err.message ?: "Imeshindwa kuanzisha malipo. Tafadhali jaribu tena."
                    )
                }
            }
        }
    }

    private fun observePaymentRecord(paymentId: String) {
        paymentObservationJob?.cancel()
        paymentObservationJob = viewModelScope.launch {
            premiumRepo.observePayment(paymentId).collectLatest { payment ->
                if (payment != null) {
                    _uiState.update { it.copy(currentPayment = payment) }

                    if (payment.isCompleted) {
                        autoVerifyJob?.cancel()
                        _uiState.update {
                            it.copy(
                                paymentStatus = PaymentStatus.PAYMENT_SUCCESS,
                                message = "Hongera! Malipo yamethibitishwa na NeliPlay Premium imewashwa kikamilifu!"
                            )
                        }
                    } else if (payment.isFailed) {
                        autoVerifyJob?.cancel()
                        _uiState.update {
                            it.copy(
                                paymentStatus = PaymentStatus.PAYMENT_FAILED,
                                error = "Malipo hayakukamilika au yamekataliwa. Tafadhali jaribu tena."
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startAutoVerification(paymentId: String, orderId: String) {
        autoVerifyJob?.cancel()
        autoVerifyJob = viewModelScope.launch {
            delay(12_000L) // Wait 12 seconds before first automated poll
            if (_uiState.value.paymentStatus == PaymentStatus.WAITING_FOR_PAYMENT) {
                verifyPaymentSilently(paymentId, orderId)
            }
        }
    }

    private suspend fun verifyPaymentSilently(paymentId: String, orderId: String) {
        val result = premiumRepo.verifyPayment(paymentId, orderId)
        result.onSuccess { verifyRes ->
            if (verifyRes.isCompleted) {
                _uiState.update {
                    it.copy(
                        paymentStatus = PaymentStatus.PAYMENT_SUCCESS,
                        message = "Hongera! NeliPlay Premium imethibitishwa na kuwashwa!"
                    )
                }
            }
        }
    }

    /**
     * User clicks "Nimeshalipa / Thibitisha Malipo" button
     */
    fun verifyPaymentManually() {
        val state = _uiState.value
        val paymentId = state.createdPaymentResult?.paymentId ?: state.currentPayment?.paymentId ?: ""
        val orderId = state.createdPaymentResult?.orderId ?: state.currentPayment?.orderId ?: ""

        if (paymentId.isBlank() && orderId.isBlank()) {
            _uiState.update { it.copy(error = "Hakuna oda inayothibitishwa.") }
            return
        }

        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.VERIFYING_PAYMENT,
                message = "Inathibitisha malipo na PalmPesa..."
            )
        }

        viewModelScope.launch {
            val result = premiumRepo.verifyPayment(paymentId, orderId)
            result.onSuccess { verifyRes ->
                if (verifyRes.isCompleted) {
                    _uiState.update {
                        it.copy(
                            paymentStatus = PaymentStatus.PAYMENT_SUCCESS,
                            message = "Hongera! Malipo yamethibitishwa. NeliPlay Premium imewashwa!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            paymentStatus = PaymentStatus.PAYMENT_PENDING,
                            message = "Malipo bado yanathibitishwa na mtandao wa simu. Subiri sekunde chache kisha ugonge 'Thibitisha Tena'."
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        paymentStatus = PaymentStatus.PAYMENT_PENDING,
                        error = err.message ?: "Mtandao unachelewa kuthibitisha. Tafadhali subiri kidogo kisha ujaribu tena."
                    )
                }
            }
        }
    }

    fun cancelPayment() {
        paymentObservationJob?.cancel()
        paymentObservationJob = null
        autoVerifyJob?.cancel()
        autoVerifyJob = null
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.PAYMENT_CANCELLED,
                message = "Umeghairi mchakato wa malipo."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        paymentObservationJob?.cancel()
        autoVerifyJob?.cancel()
    }
}
