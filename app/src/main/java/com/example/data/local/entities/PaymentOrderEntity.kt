package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store payment orders history locally.
 * Ensures orders are saved with date/time, package info, amount,
 * and tracked so they can only be confirmed once.
 */
@Entity(tableName = "payment_orders")
data class PaymentOrderEntity(
    @PrimaryKey val orderId: String,
    val packageId: String,
    val packageName: String,
    val amountTzs: Long,
    val phoneNumber: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, COMPLETED, FAILED
    val isConfirmed: Boolean = false, // Must be confirmed ONCE only
    val confirmedAt: Long? = null,
    val movieId: String? = null
)
