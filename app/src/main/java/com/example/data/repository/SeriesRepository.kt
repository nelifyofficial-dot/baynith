package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.FirestoreErrorParser
import com.example.data.model.Series
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

class SeriesRepository {
    private val TAG = "SeriesRepository"

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    /**
     * Observes all published Series from the `series` Firestore collection.
     * Implements tolerant parsing and detailed diagnostics logging.
     */
    fun getPublishedSeries(): Flow<List<Series>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.seriesCollection
                .whereEqualTo("published", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val parsed = FirestoreErrorParser.parseError(error)
                        Log.e(TAG, "Series query error (where published == true): $parsed", error)
                        _lastError.value = parsed

                        // Fallback attempt: if compound index or field filter fails, query collection directly
                        fallbackFetchAll(this)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        _lastError.value = null
                        val rawCount = snapshot.size()
                        val seriesList = mutableListOf<Series>()
                        var parseFailures = 0

                        for (doc in snapshot.documents) {
                            try {
                                val series = Series.fromDocument(doc)
                                if (series.published) {
                                    seriesList.add(series)
                                }
                            } catch (e: Exception) {
                                parseFailures++
                                Log.e(TAG, "Failed parsing series doc id=${doc.id}: ${e.message} data=${doc.data}")
                            }
                        }

                        Log.i(TAG, "Series loaded: rawCount=$rawCount, parsed=${seriesList.size}, failures=$parseFailures")
                        trySend(seriesList)
                    }
                }
        } catch (e: Exception) {
            val parsed = FirestoreErrorParser.parseError(e)
            Log.e(TAG, "Exception starting series listener: $parsed", e)
            _lastError.value = parsed
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    private fun fallbackFetchAll(channel: kotlinx.coroutines.channels.ProducerScope<List<Series>>) {
        FirebaseManager.seriesCollection.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val s = Series.fromDocument(doc)
                        if (s.published) s else null
                    } catch (e: Exception) {
                        Log.e(TAG, "Fallback parsing error for doc ${doc.id}: ${e.message}")
                        null
                    }
                }
                Log.i(TAG, "Series fallback loaded ${list.size} series")
                channel.trySend(list)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Series fallback query also failed: ${e.message}")
                channel.trySend(emptyList())
            }
    }

    fun getFeaturedSeries(): Flow<List<Series>> = getPublishedSeries().map { list ->
        list.filter { it.featured }
    }

    fun getSeriesById(seriesId: String): Flow<Series?> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.seriesCollection.document(seriesId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Series $seriesId detail listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val series = Series.fromDocument(snapshot)
                            trySend(series)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing series doc $seriesId: ${e.message}")
                            trySend(null)
                        }
                    } else {
                        trySend(null)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching series $seriesId: ${e.message}")
            trySend(null)
        }

        awaitClose {
            listener?.remove()
        }
    }

    fun searchSeries(query: String): Flow<List<Series>> = getPublishedSeries().map { list ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val q = query.trim().lowercase()
            list.filter { s ->
                s.name.lowercase().contains(q) ||
                        (s.originalName?.lowercase()?.contains(q) == true) ||
                        s.genres.any { it.lowercase().contains(q) } ||
                        s.overview.lowercase().contains(q)
            }
        }
    }
}
