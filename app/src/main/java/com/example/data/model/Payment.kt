package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

enum class PaymentStatus {
    IDLE,
    CREATING_PAYMENT,
    WAITING_FOR_PAYMENT,
    VERIFYING_PAYMENT,
    PAYMENT_SUCCESS,
    PAYMENT_PENDING,
    PAYMENT_FAILED,
    PAYMENT_CANCELLED,
    NETWORK_ERROR,
    SERVER_ERROR
}

data class Payment(
    val paymentId: String = "",
    val orderId: String = "",
    val firebaseUid: String = "",
    val plan: String = "", // "daily", "weekly", "monthly"
    val amount: Long = 0L,
    val currency: String = "TZS",
    val status: String = "PENDING", // PENDING, COMPLETED, FAILED, CANCELLED
    val provider: String = "PALMPESA",
    val buyerName: String = "",
    val buyerEmail: String = "",
    val buyerPhone: String = "",
    val checkoutUrl: String? = null,
    val providerReference: String? = null,
    val transactionId: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val completedAt: Timestamp? = null
) {
    val isCompleted: Boolean get() = status.equals("COMPLETED", ignoreCase = true)
    val isFailed: Boolean get() = status.equals("FAILED", ignoreCase = true)
    val isCancelled: Boolean get() = status.equals("CANCELLED", ignoreCase = true)
    val isPending: Boolean get() = status.equals("PENDING", ignoreCase = true)

    companion object {
        fun fromDocument(doc: DocumentSnapshot): Payment {
            val data = doc.data ?: emptyMap<String, Any>()
            val amountNum = when (val a = data["amount"]) {
                is Number -> a.toLong()
                is String -> a.toLongOrNull() ?: 0L
                else -> 0L
            }
            return Payment(
                paymentId = doc.id,
                orderId = data["orderId"]?.toString() ?: "",
                firebaseUid = data["firebaseUid"]?.toString() ?: "",
                plan = data["plan"]?.toString() ?: "",
                amount = amountNum,
                currency = data["currency"]?.toString() ?: "TZS",
                status = data["status"]?.toString() ?: "PENDING",
                provider = data["provider"]?.toString() ?: "PALMPESA",
                buyerName = data["buyerName"]?.toString() ?: "",
                buyerEmail = data["buyerEmail"]?.toString() ?: "",
                buyerPhone = data["buyerPhone"]?.toString() ?: "",
                checkoutUrl = data["checkoutUrl"]?.toString(),
                providerReference = data["providerReference"]?.toString(),
                transactionId = data["transactionId"]?.toString(),
                createdAt = data["createdAt"] as? Timestamp,
                updatedAt = data["updatedAt"] as? Timestamp,
                completedAt = data["completedAt"] as? Timestamp
            )
        }
    }
}
