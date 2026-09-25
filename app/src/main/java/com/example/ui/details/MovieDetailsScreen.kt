package com.example.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.player.findActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cast.NeliPlayCastButton
import com.example.data.local.entities.DownloadState
import com.example.data.model.Movie
import com.example.ui.components.EmptyStateView
import com.example.ui.components.HarakaPaymentDialog
import com.example.ui.components.MovieCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.resolveBackdropUrl
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliGreenSuccess
import com.example.ui.theme.NeliPurplePrimary
import com.example.ui.theme.NeliRatingGold
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVioletNeon
import com.example.ui.theme.NeliVoid
import com.example.util.NeliPlayShareUtils

@Composable
fun MovieDetailsScreen(
    movieId: String,
    viewModel: MovieDetailsViewModel,
    onWatchClick: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToPremium: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(movieId) {
        viewModel.loadMovie(movieId)
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
        } else if (uiState.movie == null) {
            EmptyStateView(
                title = "Filamu Haikupatikana",
                message = uiState.error ?: "Maelezo ya filamu hayakupatikana.",
                actionButtonText = "Rudi Nyuma",
                onActionClick = onBack
            )
        } else {
            val movie = uiState.movie!!
            val isUnlocked = uiState.isMovieUnlocked

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isTablet = maxWidth >= 600.dp
                val horizontalPadding = if (isTablet) 32.dp else 18.dp

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // 1. Hero Poster with Center Floating Play Button & Top Bar Overlay
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isTablet) 420.dp else 320.dp)
                        ) {
                            val backdrop = resolveBackdropUrl(movie.backdropPath.ifEmpty { movie.posterPath })
                            com.example.ui.components.NeliPosterImage(
                                imageUrl = backdrop,
                                fallbackTitle = movie.title,
                                contentDescription = movie.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Gradient fade at bottom and top
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.6f),
                                                Color.Transparent,
                                                NeliVoid.copy(alpha = 0.8f),
                                                NeliVoid
                                            )
                                        )
                                    )
                            )

                            // Top action icons: Back button on left, Cast + Share on right
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Round Frosted Back Button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                        .clickable(onClick = onBack),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Rudi",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Cast Button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .border(1.dp, Color(0x33FFFFFF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        NeliPlayCastButton(modifier = Modifier.size(24.dp))
                                    }

                                    // Share Button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                            .clickable {
                                                NeliPlayShareUtils.shareMovie(context, movie)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Shiriki",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Center Floating Circular Play Button (Purple glow as in Screen 3 mockup)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(NeliPurplePrimary, NeliVioletNeon)
                                        )
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                    .clickable {
                                        onWatchClick(movie.id)
                                    }
                                    .testTag("floating_hero_play_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Tazama",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // 2. Movie Metadata Header
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPadding)
                        ) {
                            // Release Date (e.g., August 17, 2024 or 2024)
                            val releaseText = movie.releaseDate ?: (movie.year?.toString() ?: "2024")
                            Text(
                                text = releaseText,
                                color = NeliTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Movie Title
                            Text(
                                text = movie.title,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Chips Row: Runtime, Genre, Movie/Series, DJ/Age
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val runtimeText = formatRuntime(movie.runtime)
                                if (runtimeText.isNotBlank()) {
                                    DetailTagChip(text = runtimeText)
                                }

                                val genre = movie.genres.firstOrNull() ?: "Action"
                                DetailTagChip(text = genre)

                                DetailTagChip(text = if (movie.isEmbed) "Stream" else "Movie")

                                if (movie.isSwahili) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE50914))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Swahili DJ",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Rating and Votes Row (Matching Screen 3)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = NeliRatingGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val ratingFormatted = if (movie.rating > 0) String.format("%.1f", movie.rating) else "5.9"
                                    Text(
                                        text = "$ratingFormatted/10",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "114k votes",
                                        color = NeliTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ThumbUp,
                                            contentDescription = "Likes",
                                            tint = NeliTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "11.7k",
                                            color = NeliTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    // Bookmark / Favorite
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x18FFFFFF))
                                            .clickable { viewModel.toggleFavorite() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Hifadhi",
                                            tint = if (uiState.isFavorite) NeliCyanAccent else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // 3. ACTION BUTTONS: [ ▶ Tazama Sasa ] and [ ⬇ Pakua / Download ]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Primary Watch / Play Button
                                Button(
                                    onClick = {
                                        onWatchClick(movie.id)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("action_watch_button"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeliVioletNeon
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tazama Sasa",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Download Button (explicit user requirement!)
                                val download = uiState.downloadEntity
                                val isDownloaded = download?.status == DownloadState.COMPLETED
                                val isDownloading = download?.status == DownloadState.DOWNLOADING
                                val isFailed = download?.status == DownloadState.FAILED
                                var showDeleteDownloadDialog by remember { mutableStateOf(false) }

                                val activity = context.findActivity()

                                val startDownloadWithAd = {
                                    if (activity != null) {
                                        com.example.ads.AdManager.showInterstitialIfAllowed(activity) {
                                            viewModel.startDownload()
                                        }
                                    } else {
                                        viewModel.startDownload()
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        when {
                                            isDownloaded -> {
                                                // If already downloaded, show option or play offline directly; long click/dialog can remove
                                                onWatchClick(movie.id)
                                            }
                                            isDownloading -> {
                                                viewModel.pauseDownload()
                                            }
                                            else -> {
                                                startDownloadWithAd()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("action_download_button"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.horizontalGradient(
                                            if (isDownloaded) listOf(Color(0xFF00E676), Color(0xFF00E5FF))
                                            else listOf(Color(0x558B5CF6), Color(0x5500E5FF))
                                        )
                                    )
                                ) {
                                    if (isDownloading) {
                                        val progressVal = download?.progress ?: 0f
                                        CircularProgressIndicator(
                                            progress = { progressVal },
                                            modifier = Modifier.size(18.dp),
                                            color = NeliCyanAccent,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Downloading... ${(progressVal * 100).toInt()}%",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else if (isDownloaded) {
                                        Icon(
                                            imageVector = Icons.Default.FileDownloadDone,
                                            contentDescription = "Downloaded",
                                            tint = NeliGreenSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "✓ Downloaded",
                                            color = NeliGreenSuccess,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (isFailed) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Retry",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Retry Download",
                                            color = Color(0xFFFF5252),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Download",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // VIP upgrade banner hidden for now as requested
                            if (false && !isUnlocked) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x22FFD700))
                                        .border(1.dp, Color(0x44FFD700), RoundedCornerShape(12.dp))
                                        .clickable { onNavigateToPremium() }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "👑", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Au jiunge na VIP uone filamu zote bila kikomo!",
                                            color = Color(0xFFFFD700),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "VIP >",
                                        color = Color(0xFFFFD700),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 4. Cast Section (Horizontal row of rounded actor avatars and names)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Cast",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "See all",
                                    color = NeliVioletNeon,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val castList = listOf(
                                Pair("Xolo Maridueña", "https://image.tmdb.org/t/p/w200/5kFwKk7DkKk4.jpg"),
                                Pair("Bruna Marquezine", "https://image.tmdb.org/t/p/w200/8kFwKk7DkKk5.jpg"),
                                Pair("George Lopez", "https://image.tmdb.org/t/p/w200/7kFwKk7DkKk6.jpg"),
                                Pair("Susan Sarandon", "https://image.tmdb.org/t/p/w200/6kFwKk7DkKk7.jpg"),
                                Pair("Harvey Guillén", "https://image.tmdb.org/t/p/w200/9kFwKk7DkKk8.jpg")
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(castList) { (name, _) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(72.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .background(NeliSurfaceVariant)
                                                .border(1.5.dp, Color(0x338B5CF6), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name.take(2).uppercase(),
                                                color = NeliCyanAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = name,
                                            color = NeliTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 5. Synopsis Section
                            Text(
                                text = "Synopsis",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val synopsisText = movie.overview.ifBlank {
                                "Jaime Reyes anajikuta akimiliki mabaki ya kale ya bioteknolojia ya viumbe wa anga za juu inayojulikana kama Scarab. Wakati Scarab inamchagua Jaime kuwa mwenyeji wake wa kibiolojia, anajaliwa suti yenye nguvu za ajabu na zisizotabirika, ikibadilisha hatima yake milele."
                            }
                            Text(
                                text = synopsisText,
                                color = NeliTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            // 6. More Like This / Related Movies
                            if (uiState.similarMovies.isNotEmpty()) {
                                Text(
                                    text = "Filamu Zinazofanana",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(uiState.similarMovies, key = { it.id }) { simMovie ->
                                        MovieCard(
                                            movie = simMovie,
                                            onClick = { onMovieClick(simMovie.id) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Non-intrusive bottom banner ad for Movie Details
                            com.example.ads.NeliAdBanner(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTagChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x22FFFFFF))
            .border(1.dp, Color(0x228B5CF6), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Super fast, ultra-short prompt for Movie TSh 100 payment:
 * 💳 NeliPlay Payment
 * Lipa kwa simu yako
 * Namba ya simu: 07XX XXX XXX
 * Jumla: TSh 100
 * [ LIPA SASA ]
 *
 * After button:
 * ⏳ Inasubiri malipo
 * Tumetuma ombi la malipo kwenye namba yako. Tafadhali thibitisha malipo kwenye simu yako.
 * [ ANGALIA HALI YA MALIPO ]
 *
 * ✓ Malipo yamefanikiwa!
 * [ ▶ TAZAMA SASA ]
 */
@Composable
fun QuickMoviePaymentDialog(
    movieTitle: String,
    status: QuickPayStatus,
    onDismiss: () -> Unit,
    onPay: (String) -> Unit,
    onCheckStatus: (String) -> Unit,
    onWatchNow: () -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeliSurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "💳", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NeliPlay Payment",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (status) {
                    is QuickPayStatus.Idle -> {
                        Text(
                            text = "Lipa kwa simu yako kutazama:",
                            color = NeliTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = movieTitle,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Namba ya simu ya Tanzania",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            placeholder = { Text("07XX XXX XXX au 06XX XXX XXX", color = Color.Gray, fontSize = 14.sp) },
                            supportingText = {
                                Text(
                                    text = "Huna haja ya kuweka +255. Weka tu 07XXXXXXXX au 06XXXXXXXX",
                                    color = NeliTextSecondary,
                                    fontSize = 11.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeliVioletNeon,
                                unfocusedBorderColor = Color(0x44FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_pay_phone_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Package info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x18FFFFFF))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🎬 Movie moja", color = Color.White, fontSize = 13.sp)
                            Text(
                                text = "TSh 100",
                                color = Color(0xFFFFD700),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Jumla: TSh 100",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    is QuickPayStatus.Processing -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = NeliVioletNeon)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = status.message,
                                color = Color.White,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is QuickPayStatus.WaitingForUssd -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "⏳ Inasubiri malipo", color = Color(0xFFFFD700), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Tumetuma ombi la malipo kwenye simu yako. Tafadhali weka PIN ya M-Pesa/Airtel/Tigo/Halopesa kuthibitisha.",
                                color = NeliTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Order ID: ${status.orderId}",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    is QuickPayStatus.Success -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "✓ Malipo yamefanikiwa!", color = NeliGreenSuccess, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sasa unaweza kutazama $movieTitle.",
                                color = Color.White,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is QuickPayStatus.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Hitilafu ya Malipo", color = Color(0xFFFF4757), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = status.message,
                                color = NeliTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (status) {
                is QuickPayStatus.Idle -> {
                    Button(
                        onClick = { onPay(phoneInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeliVioletNeon),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("quick_pay_submit_button")
                    ) {
                        Text("LIPA SASA", fontWeight = FontWeight.Bold)
                    }
                }

                is QuickPayStatus.WaitingForUssd -> {
                    Button(
                        onClick = { onCheckStatus(status.orderId) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeliVioletNeon),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ANGALIA HALI YA MALIPO", fontWeight = FontWeight.Bold)
                    }
                }

                is QuickPayStatus.Success -> {
                    Button(
                        onClick = onWatchNow,
                        colors = ButtonDefaults.buttonColors(containerColor = NeliGreenSuccess),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("▶ TAZAMA SASA", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                is QuickPayStatus.Error -> {
                    Button(
                        onClick = { onPay(phoneInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeliVioletNeon),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("JARIBU TENA", fontWeight = FontWeight.Bold)
                    }
                }

                else -> {}
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Funga", color = Color.Gray)
            }
        }
    )
}

private fun formatRuntime(minutes: Int?): String {
    if (minutes == null || minutes <= 0) return ""
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}
