package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Locale

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val photoUrl: String = "",
    val gender: String? = null,
    val country: String? = null, // ISO code, e.g. "TZ"
    val language: String? = null, // e.g. "en" or "sw"
    val role: String = "user",
    val isPremium: Boolean = false,
    val premiumActive: Boolean = false,
    val premiumUntil: Timestamp? = null,
    val premiumPlan: String? = null, // "daily", "weekly", "monthly"
    val premiumUpdatedAt: Any? = null,
    val createdAt: Any? = null,
    val updatedAt: Any? = null,
    val lastActiveAt: Any? = null
) {
    val isAdmin: Boolean get() = role.equals("admin", ignoreCase = true)

    /**
     * Verifies active subscription status against trusted server/device timestamp.
     * Ensures access is revoked immediately once premiumUntil has expired.
     */
    val isSubscriptionActive: Boolean
        get() {
            if (isAdmin) return true
            val nowMs = System.currentTimeMillis()
            if (premiumUntil != null) {
                return premiumUntil.toDate().time > nowMs
            }
            return premiumActive || isPremium
        }

    val isSubscriptionExpired: Boolean
        get() {
            if (isAdmin) return false
            val nowMs = System.currentTimeMillis()
            return premiumUntil != null && premiumUntil.toDate().time <= nowMs
        }

    val planDisplayName: String
        get() = when (premiumPlan?.lowercase()) {
            "daily" -> "Daily (Siku 1)"
            "weekly" -> "Weekly (Wiki 1)"
            "monthly" -> "Monthly (Mwezi 1)"
            else -> if (isSubscriptionActive) "Premium VIP" else "Standard (Free)"
        }

    val formattedExpiryDate: String
        get() {
            val ts = premiumUntil ?: return ""
            return try {
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                sdf.format(ts.toDate())
            } catch (e: Exception) {
                ts.toDate().toString()
            }
        }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): UserProfile {
            val data = doc.data ?: emptyMap<String, Any>()
            val legacyPremiumVal = when (val p = data["isPremium"]) {
                is Boolean -> p
                is String -> p.equals("true", ignoreCase = true)
                else -> false
            }
            val premiumActiveVal = when (val p = data["premiumActive"]) {
                is Boolean -> p
                is String -> p.equals("true", ignoreCase = true)
                else -> legacyPremiumVal
            }
            val premiumUntilVal = when (val u = data["premiumUntil"]) {
                is Timestamp -> u
                is java.util.Date -> Timestamp(u)
                else -> null
            }
            return UserProfile(
                uid = doc.id.ifEmpty { data["uid"]?.toString() ?: "" },
                displayName = data["displayName"]?.toString() ?: "NeliPlay User",
                email = data["email"]?.toString() ?: "",
                phoneNumber = data["phoneNumber"]?.toString() ?: data["phone"]?.toString() ?: "",
                photoUrl = data["photoUrl"]?.toString() ?: "",
                gender = data["gender"]?.toString(),
                country = data["country"]?.toString(),
                language = data["language"]?.toString(),
                role = data["role"]?.toString() ?: "user",
                isPremium = legacyPremiumVal,
                premiumActive = premiumActiveVal,
                premiumUntil = premiumUntilVal,
                premiumPlan = data["premiumPlan"]?.toString(),
                premiumUpdatedAt = data["premiumUpdatedAt"],
                createdAt = data["createdAt"],
                updatedAt = data["updatedAt"],
                lastActiveAt = data["lastActiveAt"]
            )
        }
    }
}
