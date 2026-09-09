package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private const val PROJECT_ID = "neliplay"
    private const val API_KEY = "AIzaSyCbaB7Z0k6pHlDMorY_jezi8G9ItoaWw0s"
    private const val APP_ID = "1:39702563643:android:219dd01461b9eb6183051e"
    private const val STORAGE_BUCKET = "neliplay.firebasestorage.app"

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setProjectId(PROJECT_ID)
                        .setApplicationId(APP_ID)
                        .setApiKey(API_KEY)
                        .setStorageBucket(STORAGE_BUCKET)
                        .build()
                    FirebaseApp.initializeApp(context.applicationContext, options)
                    Log.i(TAG, "Firebase initialized successfully with project: $PROJECT_ID")
                } else {
                    Log.i(TAG, "Firebase initialized via Google Services Provider: $PROJECT_ID")
                }

                try {
                    val firestoreInstance = FirebaseFirestore.getInstance()
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                    firestoreInstance.firestoreSettings = settings
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore persistence settings note: ${e.message}")
                }

                initialized = true

                // Debug verification mechanism: test Firestore connection
                testFirestoreConnection()
            } catch (e: Exception) {
                Log.e(TAG, "Firebase initialization failed: ${e.message}", e)
            }
        }
    }

    private fun testFirestoreConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                moviesCollection
                    .whereEqualTo("published", true)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        Log.i(TAG, "Firestore connection successful. Found ${snapshot.size()} initial document(s).")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firestore connection check notice: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore initial test call caught: ${e.message}")
            }
        }
    }

    val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    val moviesCollection get() = firestore.collection("movies")
    val episodesCollection get() = firestore.collection("episodes")
    val tvChannelsCollection get() = firestore.collection("tvChannels")
    val categoriesCollection get() = firestore.collection("categories")
    val notificationsCollection get() = firestore.collection("notifications")
    val settingsCollection get() = firestore.collection("settings")
}
