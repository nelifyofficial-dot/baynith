package com.example.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class Episode(
    val id: String = "",
    val movieId: String? = null,
    val seriesId: String? = null,
    val title: String = "",
    val episodeNumber: Int = 1,
    val seasonNumber: Int = 1,
    val overview: String = "",
    val stillPath: String = "",
    val streamUrl: String = "",
    val playbackType: String = "",
    val embedCode: String = "",
    val runtime: Int? = null,
    val downloadEnabled: Boolean = false,
    val published: Boolean = true,
    val createdAt: Any? = null,
    val updatedAt: Any? = null
) {
    /**
     * Determines the effective playback type: "mp4", "m3u8", or "embed".
     * Follows backward-compatibility rules:
     * If playbackType is missing or blank:
     * - if streamUrl contains ".m3u8" -> "m3u8"
     * - otherwise -> "mp4"
     */
    val effectivePlaybackType: String
        get() = when {
            playbackType.equals("embed", ignoreCase = true) -> "embed"
            playbackType.equals("m3u8", ignoreCase = true) -> "m3u8"
            playbackType.equals("mp4", ignoreCase = true) -> "mp4"
            streamUrl.contains(".m3u8", ignoreCase = true) || streamUrl.contains("m3u8", ignoreCase = true) -> "m3u8"
            else -> "mp4"
        }

    val isEmbed: Boolean get() = effectivePlaybackType == "embed"

    companion object {
        fun fromDocument(doc: DocumentSnapshot): Episode {
            val data = doc.data ?: emptyMap<String, Any>()
            val downloadEnabledVal = when (val de = data["downloadEnabled"]) {
                is Boolean -> de
                is String -> de.equals("true", ignoreCase = true)
                else -> false
            }
            val publishedVal = when (val p = data["published"]) {
                is Boolean -> p
                is String -> p.equals("true", ignoreCase = true)
                null -> true
                else -> false
            }
            return Episode(
                id = doc.id.ifEmpty { data["id"]?.toString() ?: "" },
                movieId = data["movieId"]?.toString(),
                seriesId = data["seriesId"]?.toString(),
                title = data["title"]?.toString() ?: "",
                episodeNumber = (data["episodeNumber"] as? Number)?.toInt() ?: 1,
                seasonNumber = (data["seasonNumber"] as? Number)?.toInt() ?: 1,
                overview = data["overview"]?.toString() ?: "",
                stillPath = data["stillPath"]?.toString() ?: "",
                streamUrl = data["streamUrl"]?.toString() ?: "",
                playbackType = data["playbackType"]?.toString()?.trim()?.lowercase() ?: "",
                embedCode = data["embedCode"]?.toString() ?: "",
                runtime = (data["runtime"] as? Number)?.toInt(),
                downloadEnabled = downloadEnabledVal,
                published = publishedVal,
                createdAt = data["createdAt"],
                updatedAt = data["updatedAt"]
            )
        }
    }
}
