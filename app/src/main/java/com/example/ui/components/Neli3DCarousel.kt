package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Movie
import com.example.ui.theme.NeliBlueAccent
import com.example.ui.theme.NeliBorder
import com.example.ui.theme.NeliGreenGlow
import com.example.ui.theme.NeliGreenPrimary
import com.example.ui.theme.NeliPillDark
import com.example.ui.theme.NeliRatingGold
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextPrimary
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

/**
 * 3D Fanned Card Deck Carousel combining the styles from the reference images:
 * - 3D angled perspective rotation for flanking cards (rotationY, rotationZ, scale, depth)
 * - Elevated center active card with glowing edge
 * - Floating translucent heart favorite button
 * - Genre chips (Cobalt Blue #345CFF & Emerald Green #0FA226)
 * - Star rating pill (7.8 ★)
 * - Smooth animated pagination pill indicators
 */
@Composable
fun Neli3DCardCarousel(
    movies: List<Movie>,
    favoritesIds: Set<String>,
    onMovieClick: (String) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { movies.size })
    val density = LocalDensity.current

    // Auto-advance banner smoothly if user is not dragging
    LaunchedEffect(pagerState, movies.size) {
        if (movies.size > 1) {
            while (true) {
                delay(5500)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % movies.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("neli_3d_carousel"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ambient background glow behind the deck
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp),
            contentAlignment = Alignment.Center
        ) {
            // Subtle ambient backdrop glow
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer { alpha = 0.35f }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeliBlueAccent.copy(alpha = 0.4f),
                                NeliGreenPrimary.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 68.dp),
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 0.dp
            ) { page ->
                val movie = movies[page]
                val isFav = favoritesIds.contains(movie.id)

                // Calculate signed offset from current page
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val absOffset = pageOffset.absoluteValue.coerceIn(0f, 2f)

                // 3D Matrix Transformations
                val rotY = (-pageOffset * 22f).coerceIn(-28f, 28f)
                val rotZ = (-pageOffset * 3.2f).coerceIn(-8f, 8f)
                val scale = (1f - (absOffset * 0.12f)).coerceIn(0.85f, 1f)
                val transX = (pageOffset * with(density) { 20.dp.toPx() })
                val cardAlpha = (1f - (absOffset * 0.3f)).coerceIn(0.6f, 1f)
                val elev = if (absOffset < 0.5f) 20.dp else 4.dp

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            cameraDistance = 16f * density.density
                            rotationY = rotY
                            rotationZ = rotZ
                            scaleX = scale
                            scaleY = scale
                            translationX = transX
                            alpha = cardAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CarouselCardItem(
                        movie = movie,
                        isFavorite = isFav,
                        isActive = absOffset < 0.5f,
                        onClick = { onMovieClick(movie.id) },
                        onToggleFavorite = { onToggleFavorite(movie) },
                        elevation = elev
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pagination Dots / Pill Indicator
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            val totalDots = movies.size.coerceAtMost(6)
            val activeIndex = pagerState.currentPage % totalDots

            for (i in 0 until totalDots) {
                val isSelected = i == activeIndex
                val width by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 6.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = "dot_width"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isSelected) NeliGreenPrimary else NeliPillDark
                        )
                )
            }
        }
    }
}

@Composable
private fun CarouselCardItem(
    movie: Movie,
    isFavorite: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    elevation: androidx.compose.ui.unit.Dp
) {
    val cornerRadius = 20.dp

    Box(
        modifier = Modifier
            .width(230.dp)
            .height(330.dp)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = if (isActive) NeliGreenGlow.copy(alpha = 0.35f) else Color.Black,
                spotColor = if (isActive) NeliGreenPrimary.copy(alpha = 0.5f) else Color.Black
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(NeliSurfaceVariant)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                brush = if (isActive) {
                    Brush.verticalGradient(
                        listOf(
                            NeliGreenPrimary.copy(alpha = 0.8f),
                            NeliBlueAccent.copy(alpha = 0.5f),
                            NeliBorder
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(NeliBorder, NeliBorder.copy(alpha = 0.3f))
                    )
                },
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(onClick = onClick)
    ) {
        val imageUrl = movie.posterPath.ifEmpty { movie.backdropPath }

        AsyncImage(
            model = imageUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Vignette gradient overlay from dark to transparent to bottom fade
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f),
                            NeliVoid.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = 900f
                    )
                )
        )

        // Floating Favorite Heart Button at Top Right
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) Color(0xFFFF2E63) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Bottom Info Card (Title, Rating, Genre Badges)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Title + Rating Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = movie.title,
                    color = NeliTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (movie.rating > 0.0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = String.format("%.1f", movie.rating),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = NeliRatingGold,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Genre Badges Row
            val genres = movie.genres.map { it.trim() }.filter { it.isNotBlank() }
            if (genres.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val firstGenre = genres.getOrNull(0)
                    val secondGenre = genres.getOrNull(1)

                    if (!firstGenre.isNullOrBlank()) {
                        GenreBadge(title = firstGenre, backgroundColor = NeliBlueAccent)
                    }
                    if (!secondGenre.isNullOrBlank()) {
                        GenreBadge(title = secondGenre, backgroundColor = NeliGreenPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreBadge(
    title: String,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
