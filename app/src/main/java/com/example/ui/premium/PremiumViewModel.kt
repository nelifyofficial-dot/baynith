package com.example.ui.premium

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.payment.harakapay.HarakaPayClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.SubscriptionManager
import com.example.data.repository.SubscriptionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NeliPlayPackage(
    val id: String,
    val icon: String,
    val name: String,
    val amountTzs: Long,
    val formattedPrice: String,
    val description: String,
    val isPopular: Boolean = false
)

enum class PaymentScreenState {
    INPUT_AND_SELECT,
    PROCESSING,
    WAITING_FOR_PAYMENT,
    SUCCESS,
    ERROR
}

data class PremiumUiState(
    val packages: List<NeliPlayPackage> = emptyList(),
    val selectedPackageId: String = "movie",
    val phoneNumber: String = "",
    val state: PaymentScreenState = PaymentScreenState.INPUT_AND_SELECT,
    val activeOrderId: String? = null,
    val waitingMessage: String = "",
    val successMessage: String = "",
    val errorMessage: String = "",
    val subscription: SubscriptionState = SubscriptionState()
) {
    val selectedPackage: NeliPlayPackage?
        get() = packages.find { it.id == selectedPackageId } ?: packages.firstOrNull()
}

class PremiumViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = AuthRepository()

    val packageList = listOf(
        NeliPlayPackage(
            id = "movie",
            icon = "🎬",
            name = "Movie moja",
            amountTzs = 100L,
            formattedPrice = "TSh 100",
            description = "Tazama filamu 1 unayoichagua"
        ),
        NeliPlayPackage(
            id = "episode",
            icon = "📺",
            name = "Series / Episode moja",
            amountTzs = 200L,
            formattedPrice = "TSh 200",
            description = "Tazama sehemu 1 ya tamthilia"
        ),
        NeliPlayPackage(
            id = "daily",
            icon = "⭐",
            name = "Premium Siku",
            amountTzs = 500L,
            formattedPrice = "TSh 500",
            description = "Masaa 24 bila kikomo",
            isPopular = false
        ),
        NeliPlayPackage(
            id = "weekly",
            icon = "⭐",
            name = "Premium Wiki",
            amountTzs = 3000L,
            formattedPrice = "TSh 3,000",
            description = "Siku 7 za burudani ya VIP",
            isPopular = true
        ),
        NeliPlayPackage(
            id = "monthly",
            icon = "⭐",
            name = "Premium Mwezi",
            amountTzs = 10000L,
            formattedPrice = "TSh 10,000",
            description = "Siku 30 bila matangazo",
            isPopular = true
        ),
        NeliPlayPackage(
            id = "yearly",
            icon = "👑",
            name = "Premium Mwaka",
            amountTzs = 100000L,
            formattedPrice = "TSh 100,000",
            description = "Mwaka mzima wa VIP Unlimited"
        )
    )

    private val _uiState = MutableStateFlow(
        PremiumUiState(
            packages = packageList,
            selectedPackageId = "movie"
        )
    )
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            SubscriptionManager.state.collect { subState ->
                _uiState.update { it.copy(subscription = subState) }
            }
        }
    }

    fun selectPackage(packageId: String) {
        _uiState.update { it.copy(selectedPackageId = packageId) }
    }

    fun onPhoneNumberChange(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone) }
    }

    fun initiatePayment() {
        val current = _uiState.value
        val rawPhone = current.phoneNumber.trim()
        val digits = rawPhone.replace(Regex("[^0-9+]"), "")
        val pkg = current.selectedPackage ?: return

        if (digits.length < 9) {
            _uiState.update {
                it.copy(
                    state = PaymentScreenState.ERROR,
                    errorMessage = "Tafadhali weka namba sahihi ya simu ya Tanzania (mfano: 07XXXXXXXX au 06XXXXXXXX)."
                )
            }
            return
        }

        val normalizedPhone = HarakaPayClient.normalizePhoneNumber(rawPhone)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    state = PaymentScreenState.PROCESSING,
                    errorMessage = ""
                )
            }

            val desc = "NeliPlay: ${pkg.name}"
            val res = HarakaPayClient.collectPayment(
                phone = normalizedPhone,
                amount = pkg.amountTzs,
                description = desc
            )

            res.fold(
                onSuccess = { collectResp ->
                    val orderId = collectResp.orderId ?: "HP${System.currentTimeMillis()}"
                    _uiState.update {
                        it.copy(
                            state = PaymentScreenState.WAITING_FOR_PAYMENT,
                            activeOrderId = orderId,
                            waitingMessage = "Tumetuma ombi la malipo kwenye simu yako ($normalizedPhone). Tafadhali thibitisha malipo kwenye simu yako kwa kuweka PIN."
                        )
                    }
                    startAutoStatusPolling(orderId, pkg.id)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            state = PaymentScreenState.ERROR,
                            errorMessage = err.message ?: "Hitilafu imetokea. Tafadhali jaribu tena."
                        )
                    }
                }
            )
        }
    }

    fun checkOrderStatus(orderId: String? = null) {
        val oid = orderId ?: _uiState.value.activeOrderId ?: return
        viewModelScope.launch {
            val res = HarakaPayClient.checkStatus(oid)
            res.fold(
                onSuccess = { statusResp ->
                    if (statusResp.isCompleted) {
                        handlePaymentSuccess(oid)
                    } else if (statusResp.isFailed) {
                        pollingJob?.cancel()
                        _uiState.update {
                            it.copy(
                                state = PaymentScreenState.ERROR,
                                errorMessage = "Malipo yameshindikana au yamekataliwa kwenye simu yako."
                            )
                        }
                    }
                },
                onFailure = {
                    // status check temporary glitch, will retry
                }
            )
        }
    }

    private fun startAutoStatusPolling(orderId: String, packageId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            // Poll up to 20 times every 4 seconds (80 seconds)
            repeat(20) {
                delay(4000)
                val res = HarakaPayClient.checkStatus(orderId)
                val statusResp = res.getOrNull()
                if (statusResp != null && statusResp.isCompleted) {
                    handlePaymentSuccess(orderId)
                    return@launch
                } else if (statusResp != null && statusResp.isFailed) {
                    _uiState.update {
                        it.copy(
                            state = PaymentScreenState.ERROR,
                            errorMessage = "Malipo yamekataliwa au yameghairiwa kwenye simu yako."
                        )
                    }
                    return@launch
                }
            }
        }
    }

    private fun handlePaymentSuccess(orderId: String) {
        pollingJob?.cancel()
        val pkg = _uiState.value.selectedPackage
        SubscriptionManager.activatePlan(pkg?.id ?: "monthly", orderId)

        _uiState.update {
            it.copy(
                state = PaymentScreenState.SUCCESS,
                successMessage = "Malipo yamefanikiwa!\nSasa unaweza kutazama filamu na vipindi vyote vya NeliPlay."
            )
        }
    }

    fun resetState() {
        pollingJob?.cancel()
        _uiState.update {
            it.copy(
                state = PaymentScreenState.INPUT_AND_SELECT,
                errorMessage = "",
                waitingMessage = "",
                successMessage = ""
            )
        }
    }
}
