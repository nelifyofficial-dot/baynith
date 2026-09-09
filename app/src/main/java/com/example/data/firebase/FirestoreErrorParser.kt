package com.example.data.firebase

import com.google.firebase.firestore.FirebaseFirestoreException

object FirestoreErrorParser {
    fun parseError(e: Throwable?): String {
        if (e == null) return "An unexpected error occurred."
        if (e is FirebaseFirestoreException) {
            return when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "You don't have permission to access this content."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Unable to load movies. Please check your internet connection."
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "The requested content could not be found."
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "The request timed out. Please check your internet connection."
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                    "Service quota exceeded. Please try again later."
                FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                    "Query requirement error: ${e.message}"
                else -> e.localizedMessage ?: "Failed to retrieve data from Firestore."
            }
        }
        val msg = e.localizedMessage?.lowercase() ?: ""
        return if (msg.contains("network") || msg.contains("connection") || msg.contains("offline") || msg.contains("host")) {
            "Unable to connect to NeliPlay. Please check your internet connection."
        } else {
            e.localizedMessage ?: "Unable to load content."
        }
    }
}
