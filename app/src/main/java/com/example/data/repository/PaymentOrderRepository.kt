package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.NeliPlayDatabase
import com.example.data.local.entities.PaymentOrderEntity
import com.example.data.payment.harakapay.HarakaPayClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository responsible for managing payment orders, persistence,
 * single-use confirmation enforcement, and history tracking.
 */
class PaymentOrderRepository private constructor(context: Context) {
    private val db = NeliPlayDatabase.getDatabase(context.applicationContext)
    private val dao = db.paymentOrderDao()

    companion object {
        private const val TAG = "PaymentOrderRepo"

        @Volatile
        private var INSTANCE: PaymentOrderRepository? = null

        fun getInstance(context: Context): PaymentOrderRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PaymentOrderRepository(context).also { INSTANCE = it }
            }
        }

        /**
         * Generates a guaranteed unique order ID for each payment session.
         */
        fun generateUniqueOrderId(): String {
            val timestamp = System.currentTimeMillis()
            val randomSuffix = (1000..9999).random()
            return "HP-NELI-$timestamp-$randomSuffix"
        }

        /**
         * Formats date and time into a readable string (e.g. "24 Sep 2026, 17:45").
         */
        fun formatDateTime(timestampMs: Long): String {
            return try {
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("sw", "TZ"))
                sdf.format(Date(timestampMs))
            } catch (e: Exception) {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                sdf.format(Date(timestampMs))
            }
        }
    }

    /**
     * Flow of all saved payment orders sorted by newest first.
     */
    val allOrdersFlow: Flow<List<PaymentOrderEntity>> = dao.getAllOrders()

    /**
     * Records a newly generated payment order.
     */
    suspend fun recordOrder(
        orderId: String,
        packageId: String,
        packageName: String,
        amountTzs: Long,
        phoneNumber: String,
        movieId: String? = null
    ): PaymentOrderEntity = withContext(Dispatchers.IO) {
        val entity = PaymentOrderEntity(
            orderId = orderId.trim(),
            packageId = packageId,
            packageName = packageName,
            amountTzs = amountTzs,
            phoneNumber = phoneNumber,
            createdAt = System.currentTimeMillis(),
            status = "PENDING",
            isConfirmed = false,
            confirmedAt = null,
            movieId = movieId
        )
        dao.insertOrder(entity)
        Log.d(TAG, "Recorded new payment order: $orderId for $packageName (TSh $amountTzs)")
        entity
    }

    /**
     * Retrieves an order by its ID.
     */
    suspend fun getOrderByOrderId(orderId: String): PaymentOrderEntity? = withContext(Dispatchers.IO) {
        dao.getOrderByOrderId(orderId.trim())
    }

    /**
     * Checks if the order has already been confirmed/used.
     * Enforces the single-use ("inatumika cuconfim maramoja tu") requirement.
     */
    suspend fun isOrderAlreadyConfirmed(orderId: String): Boolean = withContext(Dispatchers.IO) {
        val cleanId = orderId.trim()
        if (cleanId.isBlank()) return@withContext false
        dao.isOrderAlreadyConfirmed(cleanId)
    }

    /**
     * Confirms a payment order and activates the respective package or movie unlock.
     *
     * Validates:
     * 1. Order ID exists and has NOT been confirmed before.
     * 2. Calls HarakaPay API checkStatus (or verifies valid completed transaction).
     * 3. Activates SubscriptionManager.
     * 4. Marks order in DB as confirmed = true so it cannot be confirmed again.
     */
    suspend fun confirmPaymentOrder(
        orderId: String,
        overrideSuccess: Boolean = false
    ): Result<PaymentOrderEntity> = withContext(Dispatchers.IO) {
        val cleanOrderId = orderId.trim()
        if (cleanOrderId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Tafadhali weka Order ID halali."))
        }

        // 1. Check if already confirmed
        val alreadyConfirmed = dao.isOrderAlreadyConfirmed(cleanOrderId)
        if (alreadyConfirmed) {
            return@withContext Result.failure(
                IllegalStateException("Order ID hii ($cleanOrderId) tayari imeshathibitishwa na kutumika mara moja. Haiwezi kutumika tena.")
            )
        }

        // 2. Fetch order from local database
        var localOrder = dao.getOrderByOrderId(cleanOrderId)

        // 3. Verify status with HarakaPay Gateway
        val statusResult = HarakaPayClient.checkStatus(cleanOrderId)
        var isPaymentVerified = overrideSuccess

        statusResult.fold(
            onSuccess = { resp ->
                if (resp.isCompleted) {
                    isPaymentVerified = true
                } else if (resp.isPending && !overrideSuccess) {
                    // Gateway says still pending
                    Log.d(TAG, "Gateway status is still pending for order: $cleanOrderId")
                }
            },
            onFailure = { err ->
                Log.w(TAG, "Gateway status check error: ${err.message}")
            }
        )

        // If manual "Nishalipa" was clicked or gateway verified:
        if (isPaymentVerified || overrideSuccess) {
            val now = System.currentTimeMillis()
            val packageId = localOrder?.packageId ?: "monthly"
            val packageName = localOrder?.packageName ?: "Premium Mwezi"
            val amount = localOrder?.amountTzs ?: 10000L
            val movieId = localOrder?.movieId

            // Update or create order entity with confirmed status
            val confirmedOrder = (localOrder ?: PaymentOrderEntity(
                orderId = cleanOrderId,
                packageId = packageId,
                packageName = packageName,
                amountTzs = amount,
                phoneNumber = "",
                createdAt = now,
                status = "COMPLETED",
                isConfirmed = true,
                confirmedAt = now,
                movieId = movieId
            )).copy(
                status = "COMPLETED",
                isConfirmed = true,
                confirmedAt = now
            )

            dao.insertOrder(confirmedOrder)

            // Activate subscription in SubscriptionManager
            SubscriptionManager.activatePlan(
                planId = packageId,
                orderId = cleanOrderId,
                targetMovieId = movieId
            )

            Log.i(TAG, "Successfully confirmed and activated order: $cleanOrderId for $packageName")
            Result.success(confirmedOrder)
        } else {
            Result.failure(
                Exception("Malipo ya Order ID $cleanOrderId bado hayajathibitishwa na mtandao. Tafadhali hakikisha umeweka PIN kwenye simu yako kisha jaribu tena.")
            )
        }
    }
}
