package com.example.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class TvChannel(
    val id: String = "",
    val name: String = "",
    val logoUrl: String = "",
    val streamUrl: String = "",
    val country: String? = null,
    val category: String? = null,
    val featured: Boolean = false,
    val published: Boolean = true,
    val isLive: Boolean = true,
    val createdAt: Any? = null,
    val updatedAt: Any? = null
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): TvChannel {
            val data = doc.data ?: emptyMap<String, Any>()

            val publishedValue: Boolean = when (val p = data["published"]) {
                is Boolean -> p
                is String -> p.equals("true", ignoreCase = true)
                null -> true
                else -> false
            }

            val isLiveValue: Boolean = when (val l = data["isLive"]) {
                is Boolean -> l
                is String -> l.equals("true", ignoreCase = true)
                null -> true
                else -> false
            }

            val featuredValue: Boolean = when (val f = data["featured"]) {
                is Boolean -> f
                is String -> f.equals("true", ignoreCase = true)
                else -> false
            }

            return TvChannel(
                id = doc.id.ifEmpty { data["id"]?.toString() ?: "" },
                name = data["name"]?.toString() ?: "Live Channel",
                logoUrl = data["logoUrl"]?.toString() ?: "",
                streamUrl = data["streamUrl"]?.toString() ?: "",
                country = data["country"]?.toString(),
                category = data["category"]?.toString() ?: "General",
                featured = featuredValue,
                published = publishedValue,
                isLive = isLiveValue,
                createdAt = data["createdAt"],
                updatedAt = data["updatedAt"]
            )
        }
    }
}
