package com.example.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class TvChannel(
    val id: String = "",
    val name: String = "",
    val logoUrl: String = "",
    val streamUrl: String = "",
    val country: String? = null,
    val category: String? = null,
    val description: String? = null,
    val featured: Boolean = false,
    val published: Boolean = true,
    val isLive: Boolean = true,
    val createdAt: Any? = null,
    val updatedAt: Any? = null
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): TvChannel {
            val data = doc.data ?: emptyMap<String, Any>()

            val rawPublished = data["published"] ?: data["enabled"]
            val publishedValue: Boolean = when (rawPublished) {
                is Boolean -> rawPublished
                is String -> rawPublished.equals("true", ignoreCase = true)
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

            val logoString = (data["logoUrl"] ?: data["logo"])?.toString() ?: ""

            return TvChannel(
                id = doc.id.ifEmpty { data["id"]?.toString() ?: "" },
                name = data["name"]?.toString() ?: "Live Channel",
                logoUrl = logoString,
                streamUrl = data["streamUrl"]?.toString() ?: "",
                country = data["country"]?.toString(),
                category = data["category"]?.toString() ?: "General",
                description = data["overview"]?.toString() ?: data["description"]?.toString(),
                featured = featuredValue,
                published = publishedValue,
                isLive = isLiveValue,
                createdAt = data["createdAt"],
                updatedAt = data["updatedAt"]
            )
        }
    }
}
