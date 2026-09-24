package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Movie
import com.example.ui.theme.NeliPurplePrimary
import com.example.ui.theme.NeliRatingGold
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVioletNeon
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

    // Auto-advance banner every 6 seconds if not currently being touched
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_banner_slider"),
        contentAlignment = Alignment.TopCenter
    ) {
        val screenWidth = maxWidth
        // Responsive card width and height
        val cardWidth = if (screenWidth < 600.dp) {
            (screenWidth * 0.82f).coerceAtMost(360.dp)
        } else {
            420.dp
        }
        val cardHeight = if (screenWidth < 600.dp) 380.dp else 440.dp

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight),
                pageSpacing = 16.dp
            ) { page ->
                val movie = movies[page]
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    HeroCard(
                        movie = movie,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        onClick = { onDetailsClick(movie.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Information below the card: Year, Title, Badges, and Dot indicators
            val currentMovie = movies[pagerState.currentPage]

            // Release year
            val yearText = currentMovie.year?.toString()
                ?: currentMovie.releaseDate?.take(4)
                ?: "2024"
            Text(
                text = yearText,
                color = NeliTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Movie Title
            Text(
                text = currentMovie.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clickable { onDetailsClick(currentMovie.id) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sleek translucent badge chips: Genre, Runtime, Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                val genre = currentMovie.genres.firstOrNull() ?: "Fantasy"
                HeroBadgeChip(text = genre)

                val runtimeText = formatRuntime(currentMovie.runtime)
                if (runtimeText.isNotBlank()) {
                    HeroBadgeChip(text = runtimeText)
                }

                // Rating Chip with Gold Star
                val ratingScore = if (currentMovie.rating > 0) String.format("%.1f", currentMovie.rating) else "5.9"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x337C5CF7))
                        .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = NeliRatingGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ratingScore,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (currentMovie.isSwahili) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE50914))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Swahili",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Carousel pagination dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(movies.size.coerceAtMost(8)) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(if (isSelected) 18.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) NeliVioletNeon else Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    movie: Movie,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val backdropUrl = resolveBackdropUrl(movie)

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .shadow(20.dp, shape = RoundedCornerShape(24.dp), ambientColor = NeliPurplePrimary, spotColor = NeliVioletNeon)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(24.dp))
            .background(NeliSurfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = backdropUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Subtle gradient overlay at top and bottom for contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun HeroBadgeChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x337C5CF7))
            .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatRuntime(minutes: Int?): String {
    if (minutes == null || minutes <= 0) return ""
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

private fun resolveBackdropUrl(movie: Movie): String {
    return if (movie.posterPath.isNotBlank()) movie.posterPath else movie.backdropPath
}
