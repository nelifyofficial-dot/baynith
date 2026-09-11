package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.firebase.FirebaseManager
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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

    private fun getActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * Authenticates with Google using modern Android Credential Manager.
     */
    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val activity = getActivity(context)
            val launchContext = activity ?: context
            val credentialManager = CredentialManager.create(launchContext)

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(GOOGLE_WEB_CLIENT_ID)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val response = credentialManager.getCredential(context = launchContext, request = request)
            val credential = response.credential

            when {
                credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val user = authResult.user ?: throw Exception("Firebase user is null after sign in.")

                    // Sync/Create user profile in Firestore
                    syncUserProfile(user)

                    Result.success(user)
                }
                else -> {
                    Result.failure(Exception("Unrecognized credential type: ${credential.type}"))
                }
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User canceled Google sign-in.")
            Result.failure(Exception("Sign-in canceled"))
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts found: ${e.message}")
            Result.failure(Exception("No Google accounts found on this device. Please add a Google account in Android Settings or try Guest Mode."))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.message}", e)
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in error: ${e.message}", e)
            Result.failure(e)
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
     * Guarantees that client cannot overwrite server-controlled admin or premium permissions.
     */
    suspend fun syncUserProfile(
        user: FirebaseUser,
        country: String? = null,
        language: String? = null,
        gender: String? = null
    ) {
        try {
            val userDocRef = FirebaseManager.usersCollection.document(user.uid)
            val existing = userDocRef.get().await()
            val now = System.currentTimeMillis()

            val updates = mutableMapOf<String, Any>(
                "uid" to user.uid,
                "displayName" to (user.displayName ?: "NeliPlay User"),
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

            if (!existing.exists()) {
                updates["role"] = "user"
                updates["isPremium"] = false
                updates["createdAt"] = now
                if (!updates.containsKey("country")) {
                    updates["country"] = "TZ" // Default country
                }
                if (!updates.containsKey("language")) {
                    updates["language"] = "en"
                }
            } else {
                // If existing doc already has country and we didn't pass one, retain it
                val existingCountry = existing.getString("country")
                if (!existingCountry.isNullOrBlank() && !updates.containsKey("country")) {
                    updates["country"] = existingCountry
                }
            }

            userDocRef.set(updates, SetOptions.merge()).await()
            Log.i(TAG, "User profile synchronized in Firestore for uid: ${user.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user profile: ${e.message}", e)
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
     * Signs out user and clears credential state.
     */
    suspend fun signOut(context: Context) {
        try {
            auth.signOut()
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
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
     * If recent authentication is required, attempts Google re-authentication.
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
                if (context != null) {
                    Log.i(TAG, "Re-authenticating user before account deletion...")
                    val reauthResult = signInWithGoogle(context)
                    if (reauthResult.isSuccess) {
                        auth.currentUser?.delete()?.await()
                    } else {
                        throw Exception("Re-authentication required. Please sign in with Google again to confirm account deletion.")
                    }
                } else {
                    throw Exception("Security policy requires recent authentication. Please sign in again and retry.")
                }
            }

            if (context != null) {
                signOut(context)
            } else {
                auth.signOut()
            }

            Log.i(TAG, "User account successfully deleted for uid: $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting account: ${e.message}", e)
            Result.failure(e)
        }
    }
}
