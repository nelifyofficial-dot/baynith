package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.FirestoreErrorParser
import com.example.data.model.TvChannel
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

class TvRepository {
    private val TAG = "TvRepository"

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    /**
     * Observes all published Live TV channels from Firestore with a realtime listener.
     * Uses .whereEqualTo("published", true) in compliance with security rules.
     */
    fun getPublishedChannels(): Flow<List<TvChannel>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.tvChannelsCollection
                .whereEqualTo("published", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val parsed = FirestoreErrorParser.parseError(error)
                        Log.e(TAG, "TV channels query error: $parsed", error)
                        _lastError.value = parsed
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        _lastError.value = null
                        val channels = snapshot.documents.mapNotNull { doc ->
                            try {
                                val channel = TvChannel.fromDocument(doc)
                                if (channel.published) channel else null
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing channel ${doc.id}: ${e.message}")
                                null
                            }
                        }
                        trySend(channels)
                    }
                }
        } catch (e: Exception) {
            val parsed = FirestoreErrorParser.parseError(e)
            _lastError.value = parsed
            Log.e(TAG, "Exception getting TV channels: $parsed")
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    fun observePublishedChannels(): Flow<List<TvChannel>> = getPublishedChannels()

    fun getFeaturedChannels(): Flow<List<TvChannel>> = getPublishedChannels().map { list ->
        list.filter { it.featured }
    }

    fun getChannel(channelId: String): Flow<TvChannel?> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.tvChannelsCollection.document(channelId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "TV channel $channelId detail listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val ch = TvChannel.fromDocument(snapshot)
                            trySend(if (ch.published) ch else null)
                        } catch (e: Exception) {
                            trySend(null)
                        }
                    } else {
                        trySend(null)
                    }
                }
        } catch (e: Exception) {
            trySend(null)
        }

        awaitClose {
            listener?.remove()
        }
    }

    fun getChannelById(channelId: String): Flow<TvChannel?> = getChannel(channelId)
}
