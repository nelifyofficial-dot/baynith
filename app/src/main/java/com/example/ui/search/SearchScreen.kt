package com.example.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Movie
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.TvChannelCard
import com.example.ui.components.resolveImageUrl
import com.example.ui.series.SeriesCard
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliBorder
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliLiveRed
import com.example.ui.theme.NeliRatingGold
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onMovieClick: (String) -> Unit,
    onSeriesClick: (String) -> Unit = {},
    onChannelClick: (String) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val typeFilters = listOf("All", "Movies", "TV Shows", "TV Channels")

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeliVoid)
                    .statusBarsPadding()
                    .padding(top = 8.dp, bottom = 10.dp)
            ) {
                // 1. YouTube-style Query Bar & Navigation Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("search_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = {
                            Text(
                                text = "Search movies, TV shows, channels...",
                                color = NeliTextSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = NeliCyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.clearQuery()
                                    },
                                    modifier = Modifier.testTag("search_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear query",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.commitSearch(uiState.query)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NeliSurface,
                            unfocusedContainerColor = NeliSurface,
                            focusedBorderColor = NeliCyanAccent,
                            unfocusedBorderColor = NeliBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = NeliCyanAccent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input_field")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Type Filter Pills (All, Movies, TV Shows, TV Channels)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(typeFilters) { filter ->
                        val isSelected = uiState.selectedFilter == filter ||
                                (filter == "TV Channels" && (uiState.selectedFilter == "Live Streams" || uiState.selectedFilter == "Live TV")) ||
                                (filter == "TV Shows" && uiState.selectedFilter == "Series")
                        val filterTag = "search_filter_${filter.replace(" ", "_")}"

                        val icon = when (filter) {
                            "Movies" -> Icons.Default.Movie
                            "TV Shows" -> Icons.Default.Tv
                            "TV Channels" -> Icons.Default.LiveTv
                            else -> Icons.Default.FilterList
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) NeliBluePrimary else NeliSurfaceElevated)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeliCyanAccent else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    viewModel.onFilterSelect(filter)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag(filterTag),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else NeliTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = filter,
                                color = if (isSelected) Color.White else NeliTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Genre Filter Chips Row (All Genres, Action, Comedy, News, Sports, etc.)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(uiState.availableGenres) { genre ->
                        val isSelected = uiState.selectedGenre.equals(genre, ignoreCase = true)
                        val genreLabel = if (genre.equals("All", ignoreCase = true)) "All Genres" else genre

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) NeliCyanAccent.copy(alpha = 0.22f)
                                    else NeliSurface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeliCyanAccent else NeliBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.onGenreSelect(genre)
                                }
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                                .testTag("search_genre_$genre")
                        ) {
                            Text(
                                text = genreLabel,
                                color = if (isSelected) NeliCyanAccent else Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        containerColor = NeliVoid,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Results Summary & Active Filters Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val summaryText = buildString {
                    if (uiState.query.isNotBlank()) {
                        append("Results for \"${uiState.query}\" (${uiState.totalResults})")
                    } else if (uiState.selectedGenre != "All") {
                        append("Genre: ${uiState.selectedGenre} (${uiState.totalResults})")
                    } else {
                        append("Library (${uiState.totalResults} titles)")
                    }
                }

                Text(
                    text = summaryText,
                    color = NeliTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (uiState.isFilterActive) {
                    TextButton(
                        onClick = {
                            viewModel.resetFilters()
                        },
                        modifier = Modifier.testTag("search_reset_filters_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = NeliCyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset",
                            color = NeliCyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Body content
            if (uiState.isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeliCyanAccent)
                }
            } else if (uiState.isEmpty) {
                EmptyStateView(
                    title = "No Matches Found",
                    message = if (uiState.query.isNotBlank()) {
                        "No titles found matching \"${uiState.query}\" in genre \"${uiState.selectedGenre}\". Try checking the spelling or searching another title."
                    } else {
                        "No titles available in genre \"${uiState.selectedGenre}\" for the selected filter."
                    },
                    icon = Icons.Outlined.Search,
                    actionButtonText = "Reset Filters",
                    onActionClick = { viewModel.resetFilters() },
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Recent searches strip (only when query is blank & recent searches exist)
                    if (uiState.query.isBlank() && uiState.recentSearches.isNotEmpty() && uiState.selectedGenre == "All") {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = NeliCyanAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Recent Searches",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    TextButton(onClick = { viewModel.clearRecentSearches() }) {
                                        Text(
                                            text = "Clear all",
                                            color = NeliTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(uiState.recentSearches) { term ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(NeliSurface)
                                                .border(1.dp, NeliBorder, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    viewModel.onQueryChange(term)
                                                    viewModel.commitSearch(term)
                                                }
                                                .padding(start = 12.dp, top = 5.dp, bottom = 5.dp, end = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = term,
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { viewModel.removeRecentSearch(term) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = NeliTextSecondary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }

                    // YouTube-style autocomplete suggestions as user types
                    if (uiState.query.isNotBlank() && uiState.suggestions.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NeliSurface)
                                    .border(1.dp, NeliBorder, RoundedCornerShape(14.dp))
                            ) {
                                uiState.suggestions.forEach { suggestion ->
                                    val isRecent = uiState.recentSearches.contains(suggestion)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.onQueryChange(suggestion)
                                                viewModel.commitSearch(suggestion)
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isRecent) Icons.Default.History else Icons.Default.Search,
                                            contentDescription = null,
                                            tint = if (isRecent) NeliCyanAccent else NeliTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = suggestion,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                viewModel.onQueryChange(suggestion)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Insert query",
                                                tint = NeliTextSecondary,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .rotate(135f)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // 1. MOVIES SECTION (Movies Appears First)
                    if (uiState.movies.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "🎬 Movies (${uiState.movies.size})"
                            )
                        }

                        // Display movies in pairs (2-column responsive layout)
                        items(uiState.movies.chunked(2), key = { chunk -> chunk.joinToString("-") { it.id } }) { rowMovies ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (movie in rowMovies) {
                                    SearchMovieGridCard(
                                        movie = movie,
                                        onClick = {
                                            if (uiState.query.isNotBlank()) {
                                                viewModel.commitSearch(uiState.query)
                                            }
                                            onMovieClick(movie.id)
                                        },
                                        onGenreClick = { genre ->
                                            viewModel.onGenreSelect(genre)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowMovies.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // 2. TV SHOWS & SERIES SECTION (TV Shows Appears Second)
                    if (uiState.series.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "📺 TV Shows (${uiState.series.size})"
                            )
                        }

                        items(uiState.series.chunked(2), key = { chunk -> "ser_" + chunk.joinToString("-") { it.id } }) { rowSeries ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (series in rowSeries) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        SeriesCard(
                                            series = series,
                                            onClick = {
                                                if (uiState.query.isNotBlank()) {
                                                    viewModel.commitSearch(uiState.query)
                                                }
                                                onSeriesClick(series.id)
                                            }
                                        )
                                    }
                                }
                                if (rowSeries.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // 3. TV CHANNELS SECTION (TV Channels Appears Third)
                    if (uiState.channels.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "🔴 TV Channels (${uiState.channels.size})"
                            )
                        }

                        // Display TV Channels
                        items(uiState.channels, key = { "tv_${it.id}" }) { channel ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                            ) {
                                TvChannelCard(
                                    channel = channel,
                                    onClick = {
                                        if (uiState.query.isNotBlank()) {
                                            viewModel.commitSearch(uiState.query)
                                        }
                                        onChannelClick(channel.id)
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-craft Movie card optimized for grid displays in the Search screen.
 * Displays poster art, HD badge, star rating, title, year, and primary genre.
 */
@Composable
fun SearchMovieGridCard(
    movie: Movie,
    onClick: () -> Unit,
    onGenreClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("search_movie_card_${movie.id}")
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NeliSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val imageUrl = resolveImageUrl(movie.posterPath.ifEmpty { movie.backdropPath })

                AsyncImage(
                    model = imageUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient scrim at bottom for text legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 200f
                            )
                        )
                )

                // Rating badge top-left
                if (movie.rating > 0.0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = NeliRatingGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", movie.rating),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // HD badge top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeliBluePrimary.copy(alpha = 0.85f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HD",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Primary genre tag pill at bottom-left inside image
                val primaryGenre = movie.genres.firstOrNull()
                if (!primaryGenre.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeliCyanAccent.copy(alpha = 0.85f))
                            .clickable { onGenreClick(primaryGenre) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = primaryGenre,
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = movie.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Year & runtime info
        val subtitle = buildString {
            movie.year?.let { append(it) }
            if (movie.runtime != null && movie.runtime > 0) {
                if (isNotEmpty()) append(" • ")
                append("${movie.runtime}m")
            }
        }
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                color = NeliTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
