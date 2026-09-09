package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.Movie
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliRatingGold
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid
import kotlinx.coroutines.delay

@Composable
fun HeroBannerSlider(
    movies: List<Movie>,
    favoritesIds: Set<String>,
    onWatchClick: (String) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onDetailsClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { movies.size })

    // Auto-advance banner every 6 seconds if not currently being touched/dragged
    LaunchedEffect(pagerState, movies.size) {
        if (movies.size > 1) {
            while (true) {
                delay(6000)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % movies.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .testTag("hero_banner_slider")
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val movie = movies[page]
            val isFav = favoritesIds.contains(movie.id)
            HeroBannerItem(
                movie = movie,
                isFavorite = isFav,
                onWatchClick = { onWatchClick(movie.id) },
                onToggleFavorite = { onToggleFavorite(movie) },
                onDetailsClick = { onDetailsClick(movie.id) }
            )
        }

        // Pager indicator dots at bottom center
        if (movies.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .testTag("hero_slider_indicators"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(movies.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) NeliCyanAccent else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

@Composable
fun HeroBanner(
    movie: Movie,
    isFavorite: Boolean,
    onWatchClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HeroBannerItem(
        movie = movie,
        isFavorite = isFavorite,
        onWatchClick = onWatchClick,
        onToggleFavorite = onToggleFavorite,
        onDetailsClick = onDetailsClick,
        modifier = modifier
    )
}

@Composable
private fun HeroBannerItem(
    movie: Movie,
    isFavorite: Boolean,
    onWatchClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .clickable(onClick = onDetailsClick)
            .testTag("hero_banner")
    ) {
        val backdropUrl = resolveBackdropUrl(movie.backdropPath.ifEmpty { movie.posterPath })

        AsyncImage(
            model = backdropUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Multiple gradient scrims for cinematic fade and contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            NeliVoid.copy(alpha = 0.5f),
                            NeliVoid
                        ),
                        startY = 100f
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Genre and Rating pill row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (movie.rating > 0.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
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
                    Text(
                        text = y.toString(),
                        color = NeliTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (movie.genres.isNotEmpty()) {
                    Text(
                        text = "•  ${movie.genres.take(2).joinToString(", ")}",
                        color = NeliTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (movie.overview.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = movie.overview,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onWatchClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("hero_watch_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeliBluePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Watch Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("hero_my_list_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = NeliSurfaceElevated.copy(alpha = 0.85f),
                        contentColor = Color.White
                    ),
                    border = null
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "My List",
                        tint = if (isFavorite) NeliBluePrimary else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFavorite) "In My List" else "+ My List",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
