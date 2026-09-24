package com.example.data.payment.harakapay

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * HarakaPayService integrates with the HarakaPay API (https://harakapay.net/)
 * using Retrofit and Moshi.
 *
 * It manages:
 * - collectPayment: Initiates mobile money USSD push payment to customer's phone
 * - checkStatus: Verifies payment status using orderId
 * - getBalance: Queries current wallet & float balance
 *
 * Phone number normalization ensures local Tanzanian numbers (07XXXXXXXX, 06XXXXXXXX,
 * 7XXXXXXXX, etc.) are processed smoothly without requiring users to input country codes.
 */
class HarakaPayService(
    private val apiKey: String = API_KEY,
    private val baseUrl: String = BASE_URL,
    private val api: HarakaPayApi = createDefaultApi(baseUrl)
) {
    companion object {
        private const val TAG = "HarakaPayService"
        const val BASE_URL = "https://harakapay.net/"
        const val API_KEY = "hpk_93b63ba05db51f1963b174570c71762a73541195bdbc5538"

        @Volatile
        private var defaultInstance: HarakaPayService? = null

        fun getInstance(): HarakaPayService {
            return defaultInstance ?: synchronized(this) {
                defaultInstance ?: HarakaPayService().also { defaultInstance = it }
            }
        }

        /**
         * Normalizes a phone number to standard Tanzania format (e.g. 0712345678 or 0687123456).
         * Supports user entering local numbers without country code (07XXXXXXXX, 06XXXXXXXX, 7XXXXXXXX, 6XXXXXXXX)
         * as well as international formats (+255..., 255..., +2550...).
         */
        fun normalizePhoneNumber(rawPhone: String): String {
            var clean = rawPhone.replace(Regex("[^0-9+]"), "")
            if (clean.startsWith("+2550")) {
                clean = "0" + clean.substring(5)
            } else if (clean.startsWith("+255")) {
                clean = "0" + clean.substring(4)
            } else if (clean.startsWith("2550") && clean.length > 10) {
                clean = "0" + clean.substring(4)
            } else if (clean.startsWith("255") && clean.length > 9) {
                clean = "0" + clean.substring(3)
            }
            if (!clean.startsWith("0") && clean.length == 9) {
                clean = "0$clean"
            }
            return clean
        }

        private fun createDefaultApi(baseUrl: String): HarakaPayApi {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(HarakaPayApi::class.java)
        }
    }

    /**
     * Normalizes phone number using the companion formatter.
     */
    fun normalizePhoneNumber(rawPhone: String): String = HarakaPayService.normalizePhoneNumber(rawPhone)

    /**
     * POST /api/v1/collect
     * Tuma ombi la malipo kwa simu ya mteja kupitia USSD push ya mtandao husika.
     */
    suspend fun collectPayment(
        phone: String,
        amount: Long,
        description: String = "NeliPlay Swahili Subscription",
        webhookUrl: String? = null
    ): Result<HarakaPayCollectResponse> = withContext(Dispatchers.IO) {
        try {
            val normalizedPhone = normalizePhoneNumber(phone)
            Log.d(TAG, "Initiating collectPayment for $normalizedPhone, amount=$amount, desc=$description")

            val request = CollectPaymentRequest(
                phone = normalizedPhone,
                amount = amount,
                description = description,
                webhookUrl = webhookUrl
            )

            val response = api.collectPayment(apiKey, request)
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext Result.failure(Exception("Hakuna jibu lililopokelewa kutoka HarakaPay."))
                if (body.success) {
                    Result.success(body)
                } else {
                    val errMsg = body.error ?: body.message ?: "Hitilafu katika mfumo wa malipo wa HarakaPay."
                    Result.failure(Exception(errMsg))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = parseErrorMessage(errorBody) ?: "Hitilafu katika mfumo wa malipo wa HarakaPay (${response.code()})."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in collectPayment: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * GET /api/v1/status/{order_id}
     * Angalia hali ya malipo kwa kutumia order_id.
     */
    suspend fun checkStatus(orderId: String): Result<HarakaPayStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val cleanOrderId = orderId.trim()
            if (cleanOrderId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Order ID ni lazima."))
            }

            Log.d(TAG, "Checking payment status for order: $cleanOrderId")
            val response = api.checkStatus(apiKey, cleanOrderId)
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext Result.failure(Exception("Hakuna taarifa zilizopatikana."))
                val payment = body.payment
                val finalStatus = payment?.status ?: body.status ?: "pending"
                val finalAmount = payment?.amount ?: body.amount ?: 0L

                val statusResult = HarakaPayStatusResponse(
                    success = body.success,
                    orderId = cleanOrderId,
                    status = finalStatus,
                    amount = finalAmount,
                    netAmount = payment?.netAmount ?: 0L,
                    feeAmount = payment?.feeAmount ?: 0L,
                    createdAt = payment?.createdAt,
                    completedAt = payment?.completedAt,
                    error = body.error
                )
                Result.success(statusResult)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = parseErrorMessage(errorBody) ?: "Hitilafu wakati wa kukagua hali ya malipo (${response.code()})."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkStatus: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * GET /api/v1/balance
     * Angalia salio la akaunti ya HarakaPay (wallet na float).
     */
    suspend fun getBalance(): Result<HarakaPayBalanceResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching HarakaPay balance")
            val response = api.getBalance(apiKey)
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext Result.failure(Exception("Hakuna jibu la salio lililopatikana."))
                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = parseErrorMessage(errorBody) ?: "Hitilafu wakati wa kuangalia salio (${response.code()})."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getBalance: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val json = JSONObject(errorBody)
            json.optString("error", null) ?: json.optString("message", null)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Singleton client object for easy application-wide access and backward compatibility.
 */
object HarakaPayClient {
    val service: HarakaPayService get() = HarakaPayService.getInstance()
    const val BASE_URL = HarakaPayService.BASE_URL
    const val API_KEY = HarakaPayService.API_KEY

    fun normalizePhoneNumber(rawPhone: String): String = service.normalizePhoneNumber(rawPhone)

    suspend fun collectPayment(
        phone: String,
        amount: Long,
        description: String = "NeliPlay Swahili Subscription",
        webhookUrl: String? = null
    ): Result<HarakaPayCollectResponse> = service.collectPayment(phone, amount, description, webhookUrl)

    suspend fun checkStatus(orderId: String): Result<HarakaPayStatusResponse> = service.checkStatus(orderId)

    suspend fun getBalance(): Result<HarakaPayBalanceResponse> = service.getBalance()
}
