package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.PaymentOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentOrderDao {
    @Query("SELECT * FROM payment_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<PaymentOrderEntity>>

    @Query("SELECT * FROM payment_orders ORDER BY createdAt DESC")
    suspend fun getAllOrdersSync(): List<PaymentOrderEntity>

    @Query("SELECT * FROM payment_orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderByOrderId(orderId: String): PaymentOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: PaymentOrderEntity)

    @Update
    suspend fun updateOrder(order: PaymentOrderEntity)

    @Query("UPDATE payment_orders SET status = :status, isConfirmed = :isConfirmed, confirmedAt = :confirmedAt WHERE orderId = :orderId")
    suspend fun markConfirmed(
        orderId: String,
        status: String = "COMPLETED",
        isConfirmed: Boolean = true,
        confirmedAt: Long = System.currentTimeMillis()
    ): Int

    @Query("SELECT EXISTS(SELECT 1 FROM payment_orders WHERE orderId = :orderId AND isConfirmed = 1)")
    suspend fun isOrderAlreadyConfirmed(orderId: String): Boolean
}
