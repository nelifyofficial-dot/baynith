package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Payment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class CreatePaymentResult(
    val success: Boolean,
    val paymentId: String,
    val orderId: String,
    val plan: String,
    val amount: Long,
    val currency: String,
    val buyerPhone: String,
    val checkoutUrl: String?,
    val transactionId: String?,
    val message: String
)

data class VerifyPaymentResult(
    val status: String, // COMPLETED, PENDING, FAILED, CANCELLED
    val orderId: String,
    val plan: String,
    val amount: Long,
    val message: String,
    val isCompleted: Boolean = status.equals("COMPLETED", ignoreCase = true)
)

class PremiumRepository {
    private val TAG = "PremiumRepository"
    private val functions: FirebaseFunctions get() = FirebaseFunctions.getInstance()
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseManager.firestore

    /**
     * Calls Cloud Function createPalmPesaPremiumPayment
     */
    suspend fun createPayment(
        plan: String,
        phone: String,
        buyerName: String = "",
        buyerEmail: String = ""
    ): Result<CreatePaymentResult> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.failure(IllegalStateException("Ingia au fungua akaunti ili kuendelea (Login required)."))
        }

        return try {
            val apiKey = com.example.BuildConfig.PALMPESA_API_KEY
            val payload = hashMapOf<String, Any>(
                "plan" to plan.lowercase().trim(),
                "phone" to phone.trim(),
                "buyerName" to buyerName.ifBlank { currentUser.displayName ?: "NeliPlay User" },
                "buyerEmail" to buyerEmail.ifBlank { currentUser.email ?: "" }
            )
            if (apiKey.isNotBlank() && apiKey != "YOUR_PALMPESA_API_KEY") {
                payload["apiKey"] = apiKey
            }

            val callTask = functions
                .getHttpsCallable("createPalmPesaPremiumPayment")
                .call(payload)

            // 30 seconds timeout
            val result = withTimeoutOrNull(30_000L) {
                callTask.await()
            } ?: return Result.failure(IllegalStateException("Muda wa kuwasiliana na seva ya malipo umeisha. Tafadhali jaribu tena."))

            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any?>
                ?: return Result.failure(IllegalStateException("Jibu lisilotegemewa kutoka kwa seva ya malipo."))

            val success = data["success"] as? Boolean ?: false
            val paymentId = data["paymentId"]?.toString() ?: ""
            val orderId = data["orderId"]?.toString() ?: ""
            val planReturned = data["plan"]?.toString() ?: plan
            val amount = when (val a = data["amount"]) {
                is Number -> a.toLong()
                is String -> a.toLongOrNull() ?: 0L
                else -> 0L
            }
            val currency = data["currency"]?.toString() ?: "TZS"
            val buyerPhone = data["buyerPhone"]?.toString() ?: phone
            val checkoutUrl = data["checkoutUrl"]?.toString()
            val transactionId = data["transactionId"]?.toString()
            val message = data["message"]?.toString() ?: "Ombi la malipo limepokelewa."

            Result.success(
                CreatePaymentResult(
                    success = success,
                    paymentId = paymentId,
                    orderId = orderId,
                    plan = planReturned,
                    amount = amount,
                    currency = currency,
                    buyerPhone = buyerPhone,
                    checkoutUrl = checkoutUrl,
                    transactionId = transactionId,
                    message = message
                )
            )
        } catch (e: FirebaseFunctionsException) {
            Log.e(TAG, "FirebaseFunctionsException: ${e.code} - ${e.message}", e)
            val friendlyMsg = when (e.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Ingia au fungua akaunti ili kuendelea."
                FirebaseFunctionsException.Code.INVALID_ARGUMENT -> e.message ?: "Tafadhali kagua taarifa ulizoingiza."
                FirebaseFunctionsException.Code.UNAVAILABLE -> "Huduma ya malipo haipatikani kwa sasa. Tafadhali jaribu baadaye."
                else -> e.message ?: "Hitilafu imetokea wakati wa kuanzisha malipo."
            }
            Result.failure(Exception(friendlyMsg))
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling createPalmPesaPremiumPayment: ${e.message}", e)
            Result.failure(Exception(e.localizedMessage ?: "Hitilafu ya mtandao. Tafadhali jaribu tena."))
        }
    }

    /**
     * Calls Cloud Function verifyPalmPesaPayment
     */
    suspend fun verifyPayment(
        paymentId: String,
        orderId: String
    ): Result<VerifyPaymentResult> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.failure(IllegalStateException("User is not authenticated."))
        }

        return try {
            val apiKey = com.example.BuildConfig.PALMPESA_API_KEY
            val payload = hashMapOf<String, Any>(
                "paymentId" to paymentId,
                "orderId" to orderId
            )
            if (apiKey.isNotBlank() && apiKey != "YOUR_PALMPESA_API_KEY") {
                payload["apiKey"] = apiKey
            }

            val callTask = functions
                .getHttpsCallable("verifyPalmPesaPayment")
                .call(payload)

            val result = withTimeoutOrNull(20_000L) {
                callTask.await()
            } ?: return Result.failure(IllegalStateException("Muda wa kuthibitisha malipo umeisha. Tafadhali gonga Thibitisha tena."))

            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any?>
                ?: return Result.failure(IllegalStateException("Jibu lisilotegemewa kutoka kwa mfumo."))

            val status = data["status"]?.toString() ?: "PENDING"
            val orderIdReturned = data["orderId"]?.toString() ?: orderId
            val plan = data["plan"]?.toString() ?: ""
            val amount = when (val a = data["amount"]) {
                is Number -> a.toLong()
                is String -> a.toLongOrNull() ?: 0L
                else -> 0L
            }
            val message = data["message"]?.toString() ?: ""

            Result.success(
                VerifyPaymentResult(
                    status = status,
                    orderId = orderIdReturned,
                    plan = plan,
                    amount = amount,
                    message = message
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "verifyPayment error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Observes real-time updates of a payment document from Firestore.
     */
    fun observePayment(paymentId: String): Flow<Payment?> = callbackFlow {
        if (paymentId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        var listener: ListenerRegistration? = null
        try {
            listener = db.collection("payments").document(paymentId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Payment listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        trySend(Payment.fromDocument(snapshot))
                    } else {
                        trySend(null)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error observing payment: ${e.message}")
            trySend(null)
        }

        awaitClose {
            listener?.remove()
        }
    }
}
