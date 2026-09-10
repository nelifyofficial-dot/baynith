package com.example.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class Movie(
    val id: String = "",
    val tmdbId: Long? = null,
    val title: String = "",
    val originalTitle: String? = null,
    val overview: String = "",
    val posterPath: String = "",
    val backdropPath: String = "",
    val releaseDate: String? = null,
    val year: Int? = null,
    val runtime: Int? = null,
    val genres: List<String> = emptyList(),
    val rating: Double = 0.0,
    val voteCount: Long? = null,
    val originalLanguage: String? = null,
    val productionCountries: List<String>? = null,
    val streamUrl: String = "",
    val playbackType: String = "",
    val embedCode: String = "",
    val downloadEnabled: Boolean = false,
    val narrated: Boolean? = null,
    val narrationLanguage: String? = null,
    val contentGroupId: String? = null,
    val versionType: String? = null, // "original", "dubbed", "narrated"
    val audioLanguage: String? = null, // e.g. "English", "Swahili"
    val regionAvailability: String? = null, // "WORLDWIDE", "EAST_AFRICA", "SELECTED_COUNTRIES"
    val availableCountries: List<String>? = null,
    val featured: Boolean = false,
    val published: Boolean = true,
    val createdAt: Any? = null,
    val updatedAt: Any? = null
) {
    /**
     * Identifies if this movie is a Swahili dubbed version.
     */
    val isSwahiliDubbed: Boolean
        get() = versionType.equals("dubbed", ignoreCase = true) &&
                audioLanguage.equals("Swahili", ignoreCase = true)

    /**
     * Identifies if this movie is a Swahili narrated version.
     * Preserves backward compatibility with legacy `narrated` and `narrationLanguage` fields.
     */
    val isSwahiliNarrated: Boolean
        get() = (narrated == true && narrationLanguage.equals("Swahili", ignoreCase = true)) ||
                (versionType.equals("narrated", ignoreCase = true) && audioLanguage.equals("Swahili", ignoreCase = true))

    /**
     * Identifies whether this movie is Swahili content (dubbed, narrated, or native).
     */
    val isSwahili: Boolean
        get() = isSwahiliDubbed || isSwahiliNarrated ||
                audioLanguage.equals("Swahili", ignoreCase = true) ||
                narrationLanguage.equals("Swahili", ignoreCase = true)

    /**
     * Elegant badge label according to Requirements 15 & 16:
     * - "Swahili Narrated" if narrated == true && narrationLanguage == "Swahili"
     * - "Swahili Dubbed" if versionType == "dubbed" && audioLanguage == "Swahili"
     */
    val versionBadgeLabel: String?
        get() = when {
            isSwahiliDubbed -> "Swahili Dubbed"
            isSwahiliNarrated -> "Swahili Narrated"
            versionType.equals("dubbed", ignoreCase = true) -> "${audioLanguage ?: ""} Dubbed".trim()
            versionType.equals("narrated", ignoreCase = true) -> "${audioLanguage ?: ""} Narrated".trim()
            versionType.equals("original", ignoreCase = true) -> "Original"
            else -> null
        }

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
        fun fromDocument(doc: DocumentSnapshot): Movie {
            val data = doc.data ?: emptyMap<String, Any>()
            val genresRaw = data["genres"]
            val genresList: List<String> = when (genresRaw) {
                is List<*> -> genresRaw.mapNotNull { it?.toString() }
                is String -> genresRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                else -> emptyList()
            }

            val prodCountriesRaw = data["productionCountries"]
            val prodCountriesList: List<String>? = when (prodCountriesRaw) {
                is List<*> -> prodCountriesRaw.mapNotNull { it?.toString() }
                is String -> prodCountriesRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                else -> null
            }

            val ratingValue: Double = when (val r = data["rating"]) {
                is Number -> r.toDouble()
                is String -> r.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val yearValue: Int? = when (val y = data["year"]) {
                is Number -> y.toInt()
                is String -> y.toIntOrNull()
                else -> {
                    val rel = data["releaseDate"] as? String
                    rel?.take(4)?.toIntOrNull()
                }
            }

            val runtimeValue: Int? = when (val rt = data["runtime"]) {
                is Number -> rt.toInt()
                is String -> rt.toIntOrNull()
                else -> null
            }

            val tmdbIdValue: Long? = when (val t = data["tmdbId"]) {
                is Number -> t.toLong()
                is String -> t.toLongOrNull()
                else -> null
            }

            val voteCountValue: Long? = when (val v = data["voteCount"]) {
                is Number -> v.toLong()
                is String -> v.toLongOrNull()
                else -> null
            }

            val downloadEnabledValue: Boolean = when (val de = data["downloadEnabled"]) {
                is Boolean -> de
                is String -> de.equals("true", ignoreCase = true)
                else -> false
            }

            val publishedValue: Boolean = when (val p = data["published"]) {
                is Boolean -> p
                is String -> p.equals("true", ignoreCase = true)
                null -> true // Default to true if missing in existing docs
                else -> false
            }

            val featuredValue: Boolean = when (val f = data["featured"]) {
                is Boolean -> f
                is String -> f.equals("true", ignoreCase = true)
                else -> false
            }

            val narratedValue: Boolean? = when (val n = data["narrated"]) {
                is Boolean -> n
                is String -> n.equals("true", ignoreCase = true)
                else -> null
            }

            val availableCountriesRaw = data["availableCountries"]
            val availableCountriesList: List<String>? = when (availableCountriesRaw) {
                is List<*> -> availableCountriesRaw.mapNotNull { it?.toString() }
                is String -> availableCountriesRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                else -> null
            }

            return Movie(
                id = doc.id.ifEmpty { data["id"]?.toString() ?: "" },
                tmdbId = tmdbIdValue,
                title = data["title"]?.toString() ?: "",
                originalTitle = data["originalTitle"]?.toString(),
                overview = data["overview"]?.toString() ?: "",
                posterPath = data["posterPath"]?.toString() ?: "",
                backdropPath = data["backdropPath"]?.toString() ?: "",
                releaseDate = data["releaseDate"]?.toString(),
                year = yearValue,
                runtime = runtimeValue,
                genres = genresList,
                rating = ratingValue,
                voteCount = voteCountValue,
                originalLanguage = data["originalLanguage"]?.toString(),
                productionCountries = prodCountriesList,
                streamUrl = data["streamUrl"]?.toString() ?: "",
                playbackType = data["playbackType"]?.toString()?.trim()?.lowercase() ?: "",
                embedCode = data["embedCode"]?.toString() ?: "",
                downloadEnabled = downloadEnabledValue,
                narrated = narratedValue,
                narrationLanguage = data["narrationLanguage"]?.toString(),
                contentGroupId = data["contentGroupId"]?.toString(),
                versionType = data["versionType"]?.toString(),
                audioLanguage = data["audioLanguage"]?.toString(),
                regionAvailability = data["regionAvailability"]?.toString(),
                availableCountries = availableCountriesList,
                featured = featuredValue,
                published = publishedValue,
                createdAt = data["createdAt"],
                updatedAt = data["updatedAt"]
            )
        }
    }
}
