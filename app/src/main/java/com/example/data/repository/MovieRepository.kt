package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.FirestoreErrorParser
import com.example.data.model.Category
import com.example.data.model.Movie
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

class MovieRepository {
    private val TAG = "MovieRepository"

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    /**
     * Observes all published movies from Cloud Firestore using real snapshot listener.
     * Crucial: Uses .whereEqualTo("published", true) so that Firestore security rules
     * allow public read access without permission-denied errors.
     */
    fun getPublishedMovies(): Flow<List<Movie>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.moviesCollection
                .whereEqualTo("published", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val parsed = FirestoreErrorParser.parseError(error)
                        Log.e(TAG, "Firestore published movies query error: $parsed", error)
                        _lastError.value = parsed
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        _lastError.value = null
                        val movies = snapshot.documents.mapNotNull { doc ->
                            try {
                                Movie.fromDocument(doc)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing movie ${doc.id}: ${e.message}")
                                null
                            }
                        }
                        trySend(movies)
                    }
                }
        } catch (e: Exception) {
            val parsed = FirestoreErrorParser.parseError(e)
            _lastError.value = parsed
            Log.e(TAG, "Exception attaching movies listener: $parsed")
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Realtime alias for getPublishedMovies()
     */
    fun observePublishedMovies(): Flow<List<Movie>> = getPublishedMovies()

    /**
     * Featured movies queried directly from Firestore where published == true and featured == true
     */
    fun getFeaturedMovies(): Flow<List<Movie>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.moviesCollection
                .whereEqualTo("published", true)
                .whereEqualTo("featured", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore featured movies query error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val movies = snapshot.documents.mapNotNull { doc ->
                            try {
                                Movie.fromDocument(doc)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        trySend(movies)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception attaching featured movies listener: ${e.message}")
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    fun observeFeaturedMovies(): Flow<List<Movie>> = getFeaturedMovies()

    /**
     * Latest movies: ordered by releaseDate or year descending from published movies
     */
    fun getLatestMovies(): Flow<List<Movie>> = getPublishedMovies().map { list ->
        list.sortedByDescending { it.releaseDate ?: it.year?.toString() ?: "" }
    }

    /**
     * Trending movies sorted by rating and voteCount
     */
    fun getTrendingMovies(): Flow<List<Movie>> = getPublishedMovies().map { list ->
        list.sortedByDescending { it.rating }
    }

    /**
     * Filter movies by genre
     */
    fun getMoviesByGenre(genre: String): Flow<List<Movie>> = getPublishedMovies().map { list ->
        list.filter { movie ->
            movie.genres.any { it.equals(genre, ignoreCase = true) }
        }
    }

    /**
     * Observes a single movie from Firestore at `movies/{movieId}`
     */
    fun getMovie(movieId: String): Flow<Movie?> = callbackFlow {
        if (movieId.startsWith("ep_") || movieId.startsWith("episode_") || movieId.startsWith("ser_")) {
            trySend(null)
            close()
            return@callbackFlow
        }
        var listener: ListenerRegistration? = null
        try {
            listener = FirebaseManager.moviesCollection.document(movieId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val parsed = FirestoreErrorParser.parseError(error)
                        Log.w(TAG, "Movie detail listener note for $movieId: $parsed")
                        _lastError.value = parsed
                        trySend(null)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val movie = Movie.fromDocument(snapshot)
                            trySend(if (movie.published) movie else null)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing movie $movieId: ${e.message}")
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

    fun getMovieById(movieId: String): Flow<Movie?> = getMovie(movieId)

    /**
     * Search movies against Firestore-backed movie data with client-side token matching
     */
    fun searchMovies(query: String): Flow<List<Movie>> = getPublishedMovies().map { list ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val q = query.trim().lowercase()
            list.filter { movie ->
                movie.title.lowercase().contains(q) ||
                (movie.originalTitle?.lowercase()?.contains(q) == true) ||
                movie.genres.any { it.lowercase().contains(q) } ||
                movie.overview.lowercase().contains(q)
            }
        }
    }

    /**
     * Categories delegation
     */
    private val categoryRepository = CategoryRepository()
    fun getCategories(): Flow<List<Category>> = categoryRepository.getCategories()
    fun observeCategories(): Flow<List<Category>> = categoryRepository.observeCategories()
}
