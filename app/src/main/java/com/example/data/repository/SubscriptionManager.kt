package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class SubscriptionState(
    val isVip: Boolean = false,
    val planName: String = "Bure (Free)",
    val expiresAtMillis: Long = 0L,
    val unlockedMovieIds: Set<String> = emptySet(),
    val recentOrderIds: List<String> = emptyList()
)

object SubscriptionManager {
    private const val TAG = "SubscriptionManager"
    private const val PREFS_NAME = "neliplay_subscription_prefs"
    private const val KEY_EXPIRY_MILLIS = "sub_expiry_millis"
    private const val KEY_PLAN_NAME = "sub_plan_name"
    private const val KEY_UNLOCKED_MOVIES = "unlocked_movie_ids"
    private const val KEY_ORDER_IDS = "recent_order_ids"

    private lateinit var prefs: SharedPreferences
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadState()
    }

    private fun loadState() {
        val expiry = prefs.getLong(KEY_EXPIRY_MILLIS, 0L)
        val plan = prefs.getString(KEY_PLAN_NAME, "Bure (Free)") ?: "Bure (Free)"
        val unlocked = prefs.getStringSet(KEY_UNLOCKED_MOVIES, emptySet()) ?: emptySet()
        val ordersRaw = prefs.getString(KEY_ORDER_IDS, "") ?: ""
        val orders = if (ordersRaw.isNotBlank()) ordersRaw.split(",").filter { it.isNotBlank() } else emptyList()

        val isVipActive = expiry > System.currentTimeMillis()

        _state.value = SubscriptionState(
            isVip = isVipActive,
            planName = if (isVipActive) plan else "Bure (Free)",
            expiresAtMillis = expiry,
            unlockedMovieIds = unlocked,
            recentOrderIds = orders
        )
    }

    fun isMovieUnlocked(movieId: String): Boolean {
        if (movieId.isBlank()) return true
        val current = _state.value
        if (current.isVip) return true // VIP unlocks everything!
        return current.unlockedMovieIds.contains(movieId)
    }

    fun isVipActive(): Boolean {
        return _state.value.isVip && _state.value.expiresAtMillis > System.currentTimeMillis()
    }

    /**
     * Unlock a single movie for TSh 100
     */
    fun unlockMovie(movieId: String, orderId: String? = null) {
        if (movieId.isBlank()) return
        val current = _state.value
        val updatedSet = current.unlockedMovieIds + movieId
        val updatedOrders = if (!orderId.isNullOrBlank()) (listOf(orderId) + current.recentOrderIds).distinct().take(20) else current.recentOrderIds

        prefs.edit()
            .putStringSet(KEY_UNLOCKED_MOVIES, updatedSet)
            .putString(KEY_ORDER_IDS, updatedOrders.joinToString(","))
            .apply()

        _state.value = current.copy(
            unlockedMovieIds = updatedSet,
            recentOrderIds = updatedOrders
        )

        syncToCloud(movieId = movieId)
    }

    /**
     * Activate a membership plan:
     * - "single_movie": TSh 100
     * - "single_episode": TSh 200
     * - "daily": TSh 500 (24 hrs)
     * - "weekly": TSh 3,000 (7 days)
     * - "monthly": TSh 10,000 (30 days)
     * - "yearly": TSh 100,000 (365 days)
     */
    fun activatePlan(planId: String, orderId: String? = null, targetMovieId: String? = null) {
        val now = System.currentTimeMillis()
        val currentExpiry = prefs.getLong(KEY_EXPIRY_MILLIS, 0L)
        val baseTime = if (currentExpiry > now) currentExpiry else now

        var planTitle = "VIP Member"
        var newExpiry = baseTime

        when (planId.lowercase().trim()) {
            "movie", "single_movie" -> {
                if (!targetMovieId.isNullOrBlank()) {
                    unlockMovie(targetMovieId, orderId)
                    return
                }
            }
            "episode", "single_episode" -> {
                if (!targetMovieId.isNullOrBlank()) {
                    unlockMovie(targetMovieId, orderId)
                    return
                }
            }
            "daily", "siku" -> {
                newExpiry = baseTime + TimeUnit.DAYS.toMillis(1)
                planTitle = "Premium Siku (Masaa 24)"
            }
            "weekly", "wiki" -> {
                newExpiry = baseTime + TimeUnit.DAYS.toMillis(7)
                planTitle = "Premium Wiki (Siku 7)"
            }
            "monthly", "mwezi" -> {
                newExpiry = baseTime + TimeUnit.DAYS.toMillis(30)
                planTitle = "Premium Mwezi (Siku 30)"
            }
            "yearly", "mwaka" -> {
                newExpiry = baseTime + TimeUnit.DAYS.toMillis(365)
                planTitle = "Premium Mwaka (Siku 365)"
            }
            else -> {
                newExpiry = baseTime + TimeUnit.DAYS.toMillis(30)
                planTitle = "Premium Mwezi"
            }
        }

        val currentOrders = _state.value.recentOrderIds
        val updatedOrders = if (!orderId.isNullOrBlank()) (listOf(orderId) + currentOrders).distinct().take(20) else currentOrders

        prefs.edit()
            .putLong(KEY_EXPIRY_MILLIS, newExpiry)
            .putString(KEY_PLAN_NAME, planTitle)
            .putString(KEY_ORDER_IDS, updatedOrders.joinToString(","))
            .apply()

        _state.value = _state.value.copy(
            isVip = true,
            planName = planTitle,
            expiresAtMillis = newExpiry,
            recentOrderIds = updatedOrders
        )

        syncToCloud(planTitle = planTitle, newExpiry = newExpiry)
    }

    private fun syncToCloud(movieId: String? = null, planTitle: String? = null, newExpiry: Long? = null) {
        scope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val userDoc = FirebaseManager.firestore.collection("users").document(uid)
                val updates = mutableMapOf<String, Any>()
                if (planTitle != null && newExpiry != null) {
                    updates["isSubscriptionActive"] = true
                    updates["subscriptionStatus"] = "ACTIVE"
                    updates["plan"] = planTitle
                    updates["subscriptionExpiresAt"] = com.google.firebase.Timestamp(newExpiry / 1000, 0)
                }
                if (movieId != null) {
                    updates["unlockedMovies"] = com.google.firebase.firestore.FieldValue.arrayUnion(movieId)
                }
                if (updates.isNotEmpty()) {
                    userDoc.set(updates, SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync error: ${e.message}")
            }
        }
    }
}
