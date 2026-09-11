package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.FirestoreErrorParser
import com.example.data.model.Episode
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

class EpisodeRepository {
    private val TAG = "EpisodeRepository"

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    /**
     * Observes an episode document at `episodes/{episodeId}`.
     * Supports fields: streamUrl, playbackType ("mp4" | "m3u8" | "embed"), embedCode.
     */
    fun getEpisodeById(episodeId: String): Flow<Episode?> = callbackFlow {
        if (episodeId.startsWith("mov_") || episodeId.startsWith("ser_")) {
            trySend(null)
            close()
            return@callbackFlow
        }
        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.episodesCollection.document(episodeId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val parsed = FirestoreErrorParser.parseError(error)
                        Log.w(TAG, "Episode detail listener note for $episodeId: $parsed")
                        _lastError.value = parsed
                        trySend(null)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val episode = Episode.fromDocument(snapshot)
                            trySend(if (episode.published) episode else null)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing episode $episodeId: ${e.message}")
                            trySend(null)
                        }
                    } else {
                        trySend(null)
                    }
                }
        } catch (e: Exception) {
            _lastError.value = FirestoreErrorParser.parseError(e)
            trySend(null)
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Observes episodes associated with a movie or series by id.
     * Uses .whereEqualTo("published", true) to satisfy Firestore public read security rules.
     */
    fun getEpisodesForMovie(movieId: String): Flow<List<Episode>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.episodesCollection
                .whereEqualTo("published", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Query episodes for movie $movieId note: ${error.message}")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val episodes = snapshot.documents.mapNotNull { doc ->
                            try {
                                val ep = Episode.fromDocument(doc)
                                val matches = ep.movieId.equals(movieId, ignoreCase = true) ||
                                              ep.seriesId.equals(movieId, ignoreCase = true)
                                if (matches && ep.published) ep else null
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
                        trySend(episodes)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying episodes: ${e.message}")
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Observes episodes associated with a series (Item 27).
     * Filtered by seriesId or movieId and sorted by seasonNumber, episodeNumber.
     * Uses .whereEqualTo("published", true) to satisfy Firestore public read security rules.
     */
    fun getEpisodesForSeries(seriesId: String): Flow<List<Episode>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.episodesCollection
                .whereEqualTo("published", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Query episodes for series $seriesId note: ${error.message}")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val episodes = snapshot.documents.mapNotNull { doc ->
                            try {
                                val ep = Episode.fromDocument(doc)
                                val matches = ep.seriesId.equals(seriesId, ignoreCase = true) ||
                                              ep.movieId.equals(seriesId, ignoreCase = true)
                                if (matches && ep.published) ep else null
                            } catch (e: Exception) {
                                Log.e(TAG, "Episode parse error: ${e.message}")
                                null
                            }
                        }.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
                        trySend(episodes)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying episodes for series $seriesId: ${e.message}")
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    fun searchEpisodes(query: String): Flow<List<Episode>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.episodesCollection
                .whereEqualTo("published", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val q = query.trim().lowercase()
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val ep = Episode.fromDocument(doc)
                                if (ep.published && (ep.title.lowercase().contains(q) || ep.overview.lowercase().contains(q))) {
                                    ep
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        }
                        trySend(list)
                    }
                }
        } catch (e: Exception) {
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }
}
