package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.FirestoreErrorParser
import com.example.data.model.Category
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CategoryRepository {
    private val TAG = "CategoryRepository"

    /**
     * Observes categories from Firestore with a realtime listener.
     */
    fun observeCategories(): Flow<List<Category>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.categoriesCollection
                .orderBy("order", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Categories listener error: ${FirestoreErrorParser.parseError(error)}")
                        // In case orderBy("order") requires an index or fails, fall back to plain collection
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val categories = snapshot.documents.mapNotNull { doc ->
                            try {
                                Category.fromDocument(doc)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        trySend(categories)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception listening to categories: ${e.message}")
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    fun getCategories(): Flow<List<Category>> = observeCategories()
}
