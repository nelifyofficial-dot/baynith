package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.cast.NeliPlayCastButton
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.HeroBanner
import com.example.ui.components.MovieCard
import com.example.ui.components.NeliPlayLogo
import com.example.ui.components.SectionHeader
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliVoid

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMovieClick: (String) -> Unit,
    onWatchClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onTvClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeliVoid)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NeliPlayLogo(size = 36)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NeliPlayCastButton(
                        modifier = Modifier.size(36.dp)
                    )

                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.testTag("top_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onTvClick,
                        modifier = Modifier.testTag("top_tv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = "Live TV",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        containerColor = NeliVoid,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeliCyanAccent)
            }
        } else if (uiState.featuredMovie == null && uiState.trendingMovies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                val hasError = !uiState.errorMessage.isNullOrBlank()
                EmptyStateView(
                    title = if (hasError) "Unable to Load Movies" else "No Movies Available Right Now",
                    message = uiState.errorMessage ?: "Published movies from NeliPlay Studio will appear here automatically in real time.",
                    actionButtonText = if (hasError) "Watch Live TV" else "Watch Live TV",
                    onActionClick = onTvClick
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Hero Banner
                uiState.featuredMovie?.let { featured ->
                    item {
                        HeroBanner(
                            movie = featured,
                            isFavorite = uiState.favoritesIds.contains(featured.id),
                            onWatchClick = { onWatchClick(featured.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(featured) },
                            onDetailsClick = { onMovieClick(featured.id) }
                        )
                    }
                }

                // Continue Watching
                if (uiState.continueWatching.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "Continue Watching")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(uiState.continueWatching, key = { it.movieId }) { item ->
                                ContinueWatchingCard(
                                    item = item,
                                    onClick = { onWatchClick(item.movieId) }
                                )
                            }
                        }
                    }
                }

                // Trending Now
                if (uiState.trendingMovies.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader(title = "Trending Now")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.trendingMovies, key = { it.id }) { movie ->
                                MovieCard(
                                    movie = movie,
                                    onClick = { onMovieClick(movie.id) }
                                )
                            }
                        }
                    }
                }

                // Latest Movies
                if (uiState.latestMovies.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader(title = "Latest Movies")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.latestMovies, key = { it.id }) { movie ->
                                MovieCard(
                                    movie = movie,
                                    onClick = { onMovieClick(movie.id) }
                                )
                            }
                        }
                    }
                }

                // Dynamic Categories / Genres from Firestore
                uiState.genreSections.forEach { (genre, movies) ->
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader(title = "$genre Movies")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(movies, key = { it.id }) { movie ->
                                MovieCard(
                                    movie = movie,
                                    onClick = { onMovieClick(movie.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
