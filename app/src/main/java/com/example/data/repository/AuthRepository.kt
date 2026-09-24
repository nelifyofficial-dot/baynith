package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val TAG = "AuthRepository"

    companion object {
        // Web Client ID from google-services.json
        const val GOOGLE_WEB_CLIENT_ID = "39702563643-ffg3f9g17s23vjngij5umvtvujrd7g3m.apps.googleusercontent.com"
    }

    /**
     * Observes Firebase Auth state changes.
     */
    val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { fbAuth ->
            trySend(fbAuth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        trySend(auth.currentUser)

        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    /**
     * Observes current user's profile document from Firestore (`users/{uid}`).
     */
    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.usersCollection.document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "User profile listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val profile = UserProfile.fromDocument(snapshot)
                            trySend(profile)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing user profile: ${e.message}")
                            trySend(null)
                        }
                    } else {
                        trySend(null)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception observing profile: ${e.message}")
            trySend(null)
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Signs up a new user with email, password, username, and optional phone.
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        username: String,
        phoneNumber: String = ""
    ): Result<FirebaseUser> {
        return try {
            val trimmedEmail = email.trim()
            val trimmedUsername = username.trim()
            val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val user = authResult.user ?: throw Exception("Failed to create account.")

            // Update Firebase User display name
            try {
                val profileChange = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = trimmedUsername
                }
                user.updateProfile(profileChange).await()
            } catch (e: Exception) {
                Log.w(TAG, "Profile update warning: ${e.message}")
            }

            // Sync User Profile to Firestore (omit role and isPremium so server security rules are respected)
            try {
                val now = System.currentTimeMillis()
                val data = mutableMapOf<String, Any>(
                    "uid" to user.uid,
                    "displayName" to trimmedUsername,
                    "username" to trimmedUsername,
                    "email" to trimmedEmail,
                    "phoneNumber" to phoneNumber.trim(),
                    "photoUrl" to "",
                    "createdAt" to now,
                    "updatedAt" to now,
                    "lastActiveAt" to now,
                    "country" to "TZ",
                    "language" to "sw"
                )
                FirebaseManager.usersCollection.document(user.uid).set(data, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Profile Firestore creation warning: ${e.message}")
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error: ${e.message}", e)
            val userMsg = when {
                e.message?.contains("email address is already in use", ignoreCase = true) == true ->
                    "Barua pepe hii tayari inatumika. Tafadhali ingia."
                e.message?.contains("weak password", ignoreCase = true) == true ->
                    "Nenosiri ni dhaifu. Tafadhali weka angalau herufi 6."
                e.message?.contains("badly formatted", ignoreCase = true) == true ->
                    "Tafadhali weka barua pepe sahihi."
                else -> e.message ?: "Usajili haukufanikiwa."
            }
            Result.failure(Exception(userMsg))
        }
    }

    /**
     * Signs in an existing user with email (or username) and password.
     */
    suspend fun signInWithEmail(emailOrUsername: String, password: String): Result<FirebaseUser> {
        return try {
            var targetEmail = emailOrUsername.trim()
            if (!targetEmail.contains("@")) {
                // Look up by username in Firestore
                try {
                    val query = FirebaseManager.usersCollection
                        .whereEqualTo("username", targetEmail)
                        .limit(1)
                        .get()
                        .await()
                    val foundDoc = query.documents.firstOrNull()
                    val foundEmail = foundDoc?.getString("email")
                    if (!foundEmail.isNullOrBlank()) {
                        targetEmail = foundEmail
                    } else {
                        targetEmail = "$targetEmail@neliplay.app"
                    }
                } catch (e: Exception) {
                    targetEmail = "$targetEmail@neliplay.app"
                }
            }

            val authResult = auth.signInWithEmailAndPassword(targetEmail, password).await()
            val user = authResult.user ?: throw Exception("Sign-in failed.")
            syncUserProfile(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error: ${e.message}", e)
            val userMsg = when {
                e.message?.contains("no user record", ignoreCase = true) == true ||
                e.message?.contains("user-not-found", ignoreCase = true) == true ->
                    "Akaunti haijapatikana. Tafadhali jisajili kwanza."
                e.message?.contains("invalid-credential", ignoreCase = true) == true ||
                e.message?.contains("wrong-password", ignoreCase = true) == true ->
                    "Nenosiri au barua pepe si sahihi."
                else -> e.message ?: "Kuingia kumeshindwa."
            }
            Result.failure(Exception(userMsg))
        }
    }

    /**
     * Fallback / Instant guest sign-in using Firebase Anonymous Auth.
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInAnonymously().await()
            val user = authResult.user ?: throw Exception("Failed to sign in as guest.")
            syncUserProfile(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign-in error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Synchronizes authenticated user to Firestore `users/{uid}`.
     * Respects security rules by omitting sensitive server fields ('role', 'isPremium', 'premiumActive').
     */
    suspend fun syncUserProfile(
        user: FirebaseUser,
        country: String? = null,
        language: String? = null,
        gender: String? = null
    ) {
        val currentAuth = auth.currentUser
        if (currentAuth == null || currentAuth.uid != user.uid) {
            Log.d(TAG, "Skipping profile sync: user not actively authenticated.")
            return
        }

        // Anonymous guest sessions are maintained locally
        if (user.isAnonymous) {
            Log.d(TAG, "Anonymous guest session - maintaining local state.")
            return
        }

        try {
            val userDocRef = FirebaseManager.usersCollection.document(user.uid)
            val now = System.currentTimeMillis()

            val updates = mutableMapOf<String, Any>(
                "uid" to user.uid,
                "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "NeliPlay User"),
                "email" to (user.email ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "lastActiveAt" to now,
                "updatedAt" to now
            )

            if (!country.isNullOrBlank()) {
                updates["country"] = country.trim().uppercase()
            }
            if (!language.isNullOrBlank()) {
                updates["language"] = language.trim().lowercase()
            }
            if (!gender.isNullOrBlank()) {
                updates["gender"] = gender
            }

            try {
                val existing = userDocRef.get().await()
                if (!existing.exists()) {
                    updates["createdAt"] = now
                    if (!updates.containsKey("country")) {
                        updates["country"] = "TZ" // Default country
                    }
                    if (!updates.containsKey("language")) {
                        updates["language"] = "en"
                    }
                } else {
                    val existingCountry = existing.getString("country")
                    if (!existingCountry.isNullOrBlank() && !updates.containsKey("country")) {
                        updates["country"] = existingCountry
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Safe read check on profile: ${e.message}")
            }

            userDocRef.set(updates, SetOptions.merge()).await()
            Log.i(TAG, "User profile synchronized in Firestore for uid: ${user.uid}")
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            Log.w(TAG, "Firestore sync note (${e.code}): ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Notice syncing user profile: ${e.message}")
        }
    }

    suspend fun updateUserCountry(uid: String, countryCode: String): Result<Unit> {
        return try {
            val upper = countryCode.trim().uppercase()
            val now = System.currentTimeMillis()
            FirebaseManager.usersCollection.document(uid).set(
                mapOf(
                    "country" to upper,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating country: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs out user.
     */
    suspend fun signOut(context: Context? = null) {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error during signOut: ${e.message}")
        }
    }

    /**
     * Permanently deletes user account, Firestore profile, and subcollections:
     * - users/{uid}/watchlist
     * - users/{uid}/watchHistory
     * - users/{uid}
     * Then deletes the Firebase Authentication account.
     */
    suspend fun deleteAccount(context: Context? = null): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("No user currently logged in."))
        val uid = user.uid

        return try {
            val userDocRef = FirebaseManager.usersCollection.document(uid)

            // 1. Delete users/{uid}/watchlist
            try {
                val watchlistDocs = userDocRef.collection("watchlist").get().await()
                for (doc in watchlistDocs.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting watchlist: ${e.message}")
            }

            // 2. Delete users/{uid}/watchHistory
            try {
                val historyDocs = userDocRef.collection("watchHistory").get().await()
                for (doc in historyDocs.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting watchHistory: ${e.message}")
            }

            // 3. Delete users/{uid} profile document
            try {
                userDocRef.delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete user profile doc: ${e.message}")
            }

            // 4. Delete Firebase Auth account
            try {
                user.delete().await()
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                throw Exception("Tafadhali ingia tena upya kisha ufute akaunti kwa ajili ya usalama.")
            }

            auth.signOut()

            Log.i(TAG, "User account successfully deleted for uid: $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting account: ${e.message}", e)
            Result.failure(e)
        }
    }
}
