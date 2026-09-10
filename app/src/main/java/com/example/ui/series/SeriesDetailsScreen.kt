package com.example.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Episode
import com.example.ui.components.EmptyStateView
import com.example.ui.components.resolveBackdropUrl
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliRatingGold
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliTextPrimary
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid
import com.example.util.NeliPlayShareUtils

@Composable
fun SeriesDetailsScreen(
    seriesId: String,
    viewModel: SeriesDetailsViewModel,
    onPlayEpisode: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(seriesId) {
        viewModel.loadSeries(seriesId)
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
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
        } else if (uiState.series == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                EmptyStateView(
                    title = "Series Not Found",
                    message = uiState.error ?: "Unable to load series details.",
                    actionButtonText = "Go Back",
                    onActionClick = onBack
                )
            }
        } else {
            val series = uiState.series!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // Hero Backdrop
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        val heroUrl = resolveBackdropUrl(series.backdropPath.ifBlank { series.posterPath })
                        AsyncImage(
                            model = heroUrl,
                            contentDescription = series.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Vignette Gradients
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.6f),
                                            Color.Transparent,
                                            NeliVoid
                                        )
                                    )
                                )
                        )

                        // Top Back and Share Action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .testTag("series_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { NeliPlayShareUtils.shareSeries(context, series) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .testTag("series_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Series",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Title, Metadata, and Action Buttons
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            text = series.name,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeliTextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (series.rating > 0.0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = NeliRatingGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", series.rating),
                                        color = NeliRatingGold,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            val seasonsCount = uiState.seasons.size.coerceAtLeast(series.numberOfSeasons)
                            Text(
                                text = "$seasonsCount Season${if (seasonsCount > 1) "s" else ""}",
                                color = NeliTextSecondary,
                                fontSize = 13.sp
                            )

                            if (series.firstAirDate != null) {
                                Text(
                                    text = series.firstAirDate.take(4),
                                    color = NeliTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Genres
                        if (series.genres.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(series.genres) { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NeliSurfaceElevated)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            color = NeliCyanAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Play / Resume Button
                        val targetEp = uiState.resumeEpisode ?: uiState.allEpisodes.firstOrNull()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { targetEp?.let { onPlayEpisode(it.id) } },
                                enabled = targetEp != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeliCyanAccent,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("series_play_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val label = if (targetEp != null) {
                                    "Watch S${targetEp.seasonNumber}:E${targetEp.episodeNumber}"
                                } else {
                                    "Watch Series"
                                }
                                Text(
                                    text = label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Favorite Bookmark Button
                            OutlinedButton(
                                onClick = { viewModel.toggleFavorite() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("series_favorite_button"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Favorite",
                                    tint = if (uiState.isFavorite) NeliCyanAccent else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Overview
                        if (series.overview.isNotBlank()) {
                            Text(
                                text = series.overview,
                                color = NeliTextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Season Selector
                        Text(
                            text = "Episodes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.seasons) { seasonNum ->
                                val isSelected = uiState.selectedSeason == seasonNum
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NeliBluePrimary else NeliSurfaceElevated)
                                        .clickable { viewModel.selectSeason(seasonNum) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("season_tab_$seasonNum")
                                ) {
                                    Text(
                                        text = "Season $seasonNum",
                                        color = if (isSelected) Color.White else NeliTextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Episodes list for current selected season
                if (uiState.currentSeasonEpisodes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No episodes available for Season ${uiState.selectedSeason}.",
                                color = NeliTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(uiState.currentSeasonEpisodes, key = { it.id }) { episode ->
                        EpisodeItemRow(
                            episode = episode,
                            onClick = { onPlayEpisode(episode.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
fun EpisodeItemRow(
    episode: Episode,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Episode Still / Thumbnail
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 66.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NeliSurface)
        ) {
            AsyncImage(
                model = episode.stillPath,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Play overlay icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Episode",
                        tint = NeliCyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Episode Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNumber}. ${episode.title.ifBlank { "Episode ${episode.episodeNumber}" }}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (episode.runtime != null && episode.runtime > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${episode.runtime}m",
                    color = NeliTextSecondary,
                    fontSize = 12.sp
                )
            }

            if (episode.overview.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.overview,
                    color = NeliTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
