package com.example.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class Series(
    val id: String = "",
    val tmdbId: Long? = null,
    val name: String = "",
    val originalName: String? = null,
    val overview: String = "",
    val posterPath: String = "",
    val backdropPath: String = "",
    val firstAirDate: String? = null,
    val lastAirDate: String? = null,
    val year: Int? = null,
    val genres: List<String> = emptyList(),
    val rating: Double = 0.0,
    val voteCount: Long? = null,
    val originalLanguage: String? = null,
    val numberOfSeasons: Int = 1,
    val numberOfEpisodes: Int = 0,
    val featured: Boolean = false,
    val published: Boolean = true,
    val createdAt: Any? = null,
    val updatedAt: Any? = null
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): Series {
            val data = doc.data ?: emptyMap<String, Any>()

            val nameValue = (data["name"] ?: data["title"] ?: data["seriesName"])?.toString() ?: "NeliPlay Series"

            val posterValue = (data["posterPath"] ?: data["poster_path"] ?: data["poster"])?.toString() ?: ""
            val backdropValue = (data["backdropPath"] ?: data["backdrop_path"] ?: data["backdrop"])?.toString() ?: ""

            val genresRaw = data["genres"]
            val genresList: List<String> = when (genresRaw) {
                is List<*> -> genresRaw.mapNotNull { it?.toString() }
                is String -> genresRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                else -> emptyList()
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
                    val date = (data["firstAirDate"] ?: data["releaseDate"]) as? String
                    date?.take(4)?.toIntOrNull()
                }
            }

            val rawPublished = data["published"] ?: data["enabled"]
            val publishedValue: Boolean = when (rawPublished) {
                is Boolean -> rawPublished
                is String -> rawPublished.equals("true", ignoreCase = true)
                null -> true
                else -> false
            }

            val featuredValue: Boolean = when (val f = data["featured"]) {
                is Boolean -> f
                is String -> f.equals("true", ignoreCase = true)
                else -> false
            }

            val tmdbIdVal: Long? = when (val t = data["tmdbId"]) {
                is Number -> t.toLong()
                is String -> t.toLongOrNull()
                else -> null
            }

            val voteCountVal: Long? = when (val v = data["voteCount"]) {
                is Number -> v.toLong()
                is String -> v.toLongOrNull()
                else -> null
            }

            val seasonsCount = when (val s = data["numberOfSeasons"] ?: data["seasonsCount"]) {
                is Number -> s.toInt()
                is String -> s.toIntOrNull() ?: 1
                else -> 1
            }

            val episodesCount = when (val e = data["numberOfEpisodes"] ?: data["episodesCount"]) {
                is Number -> e.toInt()
                is String -> e.toIntOrNull() ?: 0
                else -> 0
            }

            return Series(
                id = doc.id.ifEmpty { data["id"]?.toString() ?: "" },
                tmdbId = tmdbIdVal,
                name = nameValue,
                originalName = data["originalName"]?.toString(),
                overview = data["overview"]?.toString() ?: "",
                posterPath = posterValue,
                backdropPath = backdropValue,
                firstAirDate = data["firstAirDate"]?.toString(),
                lastAirDate = data["lastAirDate"]?.toString(),
                year = yearValue,
                genres = genresList,
                rating = ratingValue,
                voteCount = voteCountVal,
                originalLanguage = data["originalLanguage"]?.toString(),
                numberOfSeasons = seasonsCount.coerceAtLeast(1),
                numberOfEpisodes = episodesCount,
                featured = featuredValue,
                published = publishedValue,
                createdAt = data["createdAt"],
                updatedAt = data["updatedAt"]
            )
        }
    }
}
