package com.example.data.payment.harakapay

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Data models for HarakaPay API integration.
 */

@JsonClass(generateAdapter = true)
data class CollectPaymentRequest(
    @Json(name = "phone") val phone: String,
    @Json(name = "amount") val amount: Long,
    @Json(name = "description") val description: String = "NeliPlay Swahili Subscription",
    @Json(name = "webhook_url") val webhookUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class HarakaPayCollectResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "order_id") val orderId: String? = null,
    @Json(name = "amount") val amount: Long = 0L,
    @Json(name = "net_amount") val netAmount: Long = 0L,
    @Json(name = "fee") val fee: Long = 0L,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class HarakaPayPaymentDetail(
    @Json(name = "order_id") val orderId: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "amount") val amount: Long = 0L,
    @Json(name = "net_amount") val netAmount: Long = 0L,
    @Json(name = "fee_amount") val feeAmount: Long = 0L,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "completed_at") val completedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class HarakaPayStatusRawResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "order_id") val orderId: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "payment") val payment: HarakaPayPaymentDetail? = null,
    @Json(name = "amount") val amount: Long? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class HarakaPayStatusResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "order_id") val orderId: String = "",
    @Json(name = "status") val status: String = "", // "completed", "pending", "failed", "processing"
    @Json(name = "amount") val amount: Long = 0L,
    @Json(name = "net_amount") val netAmount: Long = 0L,
    @Json(name = "fee_amount") val feeAmount: Long = 0L,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "completed_at") val completedAt: String? = null,
    @Json(name = "error") val error: String? = null
) {
    val isCompleted: Boolean get() = status.equals("completed", ignoreCase = true) || status.equals("success", ignoreCase = true)
    val isPending: Boolean get() = status.equals("pending", ignoreCase = true) || status.equals("processing", ignoreCase = true)
    val isFailed: Boolean get() = status.equals("failed", ignoreCase = true) || status.equals("cancelled", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class HarakaPayBalanceResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "wallet_balance") val walletBalance: Long = 0L,
    @Json(name = "float_balance") val floatBalance: Long = 0L,
    @Json(name = "error") val error: String? = null
)

/**
 * Retrofit interface definition for HarakaPay API endpoints.
 */
interface HarakaPayApi {
    @POST("api/v1/collect")
    suspend fun collectPayment(
        @Header("X-API-Key") apiKey: String,
        @Body request: CollectPaymentRequest
    ): Response<HarakaPayCollectResponse>

    @GET("api/v1/status/{order_id}")
    suspend fun checkStatus(
        @Header("X-API-Key") apiKey: String,
        @Path("order_id") orderId: String
    ): Response<HarakaPayStatusRawResponse>

    @GET("api/v1/balance")
    suspend fun getBalance(
        @Header("X-API-Key") apiKey: String
    ): Response<HarakaPayBalanceResponse>
}
