package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeliDarkColorScheme = darkColorScheme(
    primary = NeliBluePrimary,
    onPrimary = Color.White,
    primaryContainer = NeliSurfaceElevated,
    onPrimaryContainer = NeliCyanAccent,
    secondary = NeliCyanAccent,
    onSecondary = Color.Black,
    secondaryContainer = NeliSurfaceVariant,
    onSecondaryContainer = Color.White,
    tertiary = NeliMagentaAccent,
    onTertiary = Color.White,
    background = NeliVoid,
    onBackground = NeliTextPrimary,
    surface = NeliSurface,
    onSurface = NeliTextPrimary,
    surfaceVariant = NeliSurfaceVariant,
    onSurfaceVariant = NeliTextSecondary,
    outline = NeliBorder,
    error = NeliError,
    onError = Color.White
)

@Composable
fun NeliPlayTheme(
    content: @Composable () -> Unit
) {
    // NeliPlay is intentionally a dark cinematic streaming application by default
    MaterialTheme(
        colorScheme = NeliDarkColorScheme,
        typography = Typography,
        content = content
    )
}
