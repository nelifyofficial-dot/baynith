package com.example.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val gender: String? = null,
    val country: String? = null, // ISO code, e.g. "TZ"
    val language: String? = null, // e.g. "en" or "sw"
    val role: String = "user",
    val isPremium: Boolean = false,
    val createdAt: Any? = null,
    val updatedAt: Any? = null,
    val lastActiveAt: Any? = null
) {
    val isAdmin: Boolean get() = role.equals("admin", ignoreCase = true)

    companion object {
        fun fromDocument(doc: DocumentSnapshot): UserProfile {
            val data = doc.data ?: emptyMap<String, Any>()
            val premiumVal = when (val p = data["isPremium"]) {
                is Boolean -> p
                is String -> p.equals("true", ignoreCase = true)
                else -> false
            }
            return UserProfile(
                uid = doc.id.ifEmpty { data["uid"]?.toString() ?: "" },
                displayName = data["displayName"]?.toString() ?: "NeliPlay User",
                email = data["email"]?.toString() ?: "",
                photoUrl = data["photoUrl"]?.toString() ?: "",
                gender = data["gender"]?.toString(),
                country = data["country"]?.toString(),
                language = data["language"]?.toString(),
                role = data["role"]?.toString() ?: "user",
                isPremium = premiumVal,
                createdAt = data["createdAt"],
                updatedAt = data["updatedAt"],
                lastActiveAt = data["lastActiveAt"]
            )
        }
    }
}
