package com.example.ui.details

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entities.DownloadState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MovieCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.resolveBackdropUrl
import com.example.ui.series.EpisodeItemRow
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliGreenSuccess
import com.example.ui.theme.NeliRatingGold
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid

fun formatRuntime(minutes: Int?): String {
    if (minutes == null || minutes <= 0) return ""
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

@Composable
fun MovieDetailsScreen(
    movieId: String,
    viewModel: MovieDetailsViewModel,
    onWatchClick: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(movieId) {
        viewModel.loadMovie(movieId)
    }

    val uiState by viewModel.uiState.collectAsState()
    var showQualityDialog by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf("1080p") }

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
        } else if (uiState.movie == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    title = "Movie Not Found",
                    message = "This movie could not be loaded from Firestore.",
                    actionButtonText = "Go Back",
                    onActionClick = onBack
                )
            }
        } else {
            val movie = uiState.movie!!
            val download = uiState.downloadEntity
            val targetPlayId = if (uiState.hasEpisodes && uiState.currentSeasonEpisodes.isNotEmpty()) {
                uiState.currentSeasonEpisodes.first().id
            } else {
                movie.id
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Backdrop with Play button overlay & Back button
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        val backdropUrl = resolveBackdropUrl(movie.backdropPath.ifEmpty { movie.posterPath })

                        AsyncImage(
                            model = backdropUrl,
                            contentDescription = movie.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Top & bottom gradient scrims
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.7f),
                                            Color.Transparent,
                                            NeliVoid
                                        )
                                    )
                                )
                        )

                        // Top Back Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.testTag("details_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Center Play Button Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NeliBluePrimary)
                                .clickable { onWatchClick(targetPlayId) }
                                .testTag("details_center_play_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Watch Movie",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Title & Badges
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = movie.title,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )

                        if (!movie.originalTitle.isNullOrBlank() && movie.originalTitle != movie.title) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = movie.originalTitle,
                                color = NeliTextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metadata pills: Rating, Year, Runtime, HD, Narrated
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (movie.rating > 0.0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeliSurfaceElevated)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = NeliRatingGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", movie.rating),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            movie.year?.let { y ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeliSurfaceElevated)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = y.toString(),
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            val runtimeFormatted = formatRuntime(movie.runtime)
                            if (runtimeFormatted.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeliSurfaceElevated)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = runtimeFormatted,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeliCyanAccent.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (movie.isEmbed) "Embed" else "HD",
                                    color = NeliCyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (movie.narrated == true) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeliBluePrimary.copy(alpha = 0.25f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Narrated ${movie.narrationLanguage ?: ""}".trim(),
                                        color = NeliCyanAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ACTION BUTTONS: Watch, Download, My List
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Watch Button
                            Button(
                                onClick = { onWatchClick(targetPlayId) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeliBluePrimary),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(46.dp)
                                    .testTag("details_watch_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Watch",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Watch", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            // Download Button
                            if (movie.isEmbed) {
                                // Embedded content: do NOT attempt to download embedCode HTML
                                OutlinedButton(
                                    onClick = { /* Embedded media direct download unavailable */ },
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = false,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        disabledContainerColor = NeliSurfaceElevated.copy(alpha = 0.5f),
                                        disabledContentColor = NeliTextSecondary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("details_download_unavailable_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Download unavailable",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Download unavailable", fontSize = 10.sp, maxLines = 1)
                                }
                            } else if (movie.downloadEnabled) {
                                when (download?.status) {
                                    DownloadState.COMPLETED -> {
                                        Button(
                                            onClick = { /* already downloaded */ },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeliGreenSuccess),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                                .testTag("details_downloaded_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FileDownloadDone,
                                                contentDescription = "Downloaded",
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "Ready", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                    DownloadState.DOWNLOADING -> {
                                        Button(
                                            onClick = { viewModel.pauseDownload() },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeliSurfaceElevated),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Pause,
                                                contentDescription = "Pause",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${(download.progress * 100).toInt()}%",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                    else -> {
                                        Button(
                                            onClick = { showQualityDialog = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeliSurfaceElevated),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                                .testTag("details_download_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download",
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "Download", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else {
                                // Download disabled by publisher
                                OutlinedButton(
                                    onClick = { /* Info tooltip */ },
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = false,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        disabledContainerColor = NeliSurfaceElevated.copy(alpha = 0.5f),
                                        disabledContentColor = NeliTextSecondary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "No Download",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Stream Only", fontSize = 12.sp)
                                }
                            }

                            // My List / Favorite Button
                            IconButton(
                                onClick = { viewModel.toggleFavorite() },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeliSurfaceElevated)
                                    .testTag("details_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isFavorite) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = "My List",
                                    tint = if (uiState.isFavorite) NeliCyanAccent else Color.White
                                )
                            }
                        }

                        // Download Progress bar if currently downloading
                        if (download?.status == DownloadState.DOWNLOADING) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { download.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = NeliCyanAccent,
                                trackColor = NeliSurfaceElevated
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Genres chips
                        if (movie.genres.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(movie.genres) { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NeliSurface)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            color = NeliTextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Storyline / Overview
                        if (movie.overview.isNotBlank()) {
                            Text(
                                text = "Storyline",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = movie.overview,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Production countries & language info
                        val extraDetails = buildList {
                            if (!movie.originalLanguage.isNullOrBlank()) add("Language: ${movie.originalLanguage.uppercase()}")
                            if (!movie.productionCountries.isNullOrEmpty()) add("Country: ${movie.productionCountries.joinToString(", ")}")
                            if (movie.voteCount != null && movie.voteCount > 0) add("Votes: ${movie.voteCount}")
                        }

                        if (extraDetails.isNotEmpty()) {
                            Text(
                                text = extraDetails.joinToString("  •  "),
                                color = NeliTextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }

                // Episodes Section (for series or episodic movies like Squid Game)
                if (uiState.hasEpisodes) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Episodes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (uiState.seasons.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(uiState.seasons) { seasonNum ->
                                        val isSelected = uiState.selectedSeason == seasonNum
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) NeliBluePrimary else NeliSurfaceElevated)
                                                .clickable { viewModel.selectSeason(seasonNum) }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                                .testTag("details_season_$seasonNum")
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
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    if (uiState.currentSeasonEpisodes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
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
                        items(uiState.currentSeasonEpisodes, key = { it.id }) { ep ->
                            EpisodeItemRow(
                                episode = ep,
                                onClick = { onWatchClick(ep.id) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Similar Movies / More Like This
                if (uiState.similarMovies.isNotEmpty()) {
                    item {
                        SectionHeader(title = "More Like This")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.similarMovies, key = { it.id }) { similar ->
                                MovieCard(
                                    movie = similar,
                                    onClick = { onMovieClick(similar.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQualityDialog) {
        val qualities = listOf(
            "1080p" to "High Quality (1080p - Best visual experience)",
            "720p" to "Standard (720p - Balanced storage & quality)",
            "480p" to "Data Saver (480p - Faster download, less space)"
        )
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            containerColor = NeliSurfaceElevated,
            title = {
                Text(
                    text = "Download Quality",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    qualities.forEach { (qKey, qLabel) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedQuality = qKey }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = (selectedQuality == qKey),
                                onClick = { selectedQuality = qKey },
                                colors = RadioButtonDefaults.colors(selectedColor = NeliCyanAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = qLabel,
                                color = if (selectedQuality == qKey) Color.White else NeliTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQualityDialog = false
                        viewModel.startDownload(selectedQuality)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeliBluePrimary)
                ) {
                    Text("Start Download", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Cancel", color = NeliTextSecondary)
                }
            }
        )
    }
}
