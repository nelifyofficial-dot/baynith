package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.theme.NeliPurplePrimary
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVioletNeon

/**
 * Robust image URL resolver that checks TMDB paths, full HTTP/HTTPS urls,
 * and strips corrupted whitespace or formatting.
 */
fun resolveSafeImageUrl(path: String?, alternativePath: String? = null): String {
    val primary = path?.trim().orEmpty()
    val secondary = alternativePath?.trim().orEmpty()

    val chosen = when {
        primary.isNotBlank() -> primary
        secondary.isNotBlank() -> secondary
        else -> ""
    }

    if (chosen.isBlank()) return ""
    return if (chosen.startsWith("http://") || chosen.startsWith("https://")) {
        chosen
    } else {
        "https://image.tmdb.org/t/p/w500${if (chosen.startsWith("/")) "" else "/"}$chosen"
    }
}

/**
 * Standard NeliPlay Poster Component with:
 * 1. Primary poster check
 * 2. Alternative image fallback
 * 3. Loading placeholder
 * 4. Error / Missing poster fallback with title and cinematic gradient
 * 5. Never a broken icon or large empty black box
 */
@Composable
fun NeliPosterImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackTitle: String = "",
    contentScale: ContentScale = ContentScale.Crop
) {
    val resolvedUrl = resolveSafeImageUrl(imageUrl)

    if (resolvedUrl.isBlank()) {
        PosterFallbackBox(
            title = fallbackTitle,
            modifier = modifier
        )
    } else {
        SubcomposeAsyncImage(
            model = resolvedUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NeliSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = NeliVioletNeon,
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                PosterFallbackBox(
                    title = fallbackTitle,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}

@Composable
fun PosterFallbackBox(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        NeliSurfaceElevated,
                        NeliSurfaceVariant,
                        Color(0xFF0F0B24)
                    )
                )
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0x228B5CF6), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = NeliVioletNeon,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (title.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
