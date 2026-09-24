package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// NeliPlay Luxury Futuristic Dark Purple / Violet Cinematic Palette
// Aligned with the streaming UI mockups
val NeliVoid = Color(0xFF0B0818)           // Ultra-deep night purple background
val NeliSurface = Color(0xFF140F2D)        // Dark purple container surface
val NeliSurfaceVariant = Color(0xFF1E1742) // Medium purple card surface
val NeliSurfaceElevated = Color(0xFF282054)// Elevated card surface with soft tint

// Brand Accents
val NeliPurplePrimary = Color(0xFF6C5CE7)  // Electric royal purple
val NeliVioletNeon = Color(0xFF8B5CF6)     // Vibrant neon violet
val NeliBluePrimary = Color(0xFF5352ED)    // Rich indigo blue
val NeliCyanAccent = Color(0xFF00E5FF)     // Glowing futuristic cyan
val NeliMagentaAccent = Color(0xFFFF2A6D)  // Vivid magenta
val NeliOrangeAccent = Color(0xFFFF6D00)   // Energetic orange
val NeliLiveRed = Color(0xFFFF2D55)        // Live stream red
val NeliRatingGold = Color(0xFFFFB800)     // Shimmering star gold
val NeliVipGold = Color(0xFFFFD700)        // Gold badge accent

// Text & Neutral Colors
val NeliTextPrimary = Color(0xFFFFFFFF)
val NeliTextSecondary = Color(0xFFA5A3C4)   // Soft readable lavender-gray
val NeliTextTertiary = Color(0xFF716D96)    // Muted deep lavender
val NeliBorder = Color(0x2E8B5CF6)         // Subtle glowing purple stroke
val NeliBorderLight = Color(0x1FFFFFFF)    // Subtle white sheen stroke
val NeliDivider = Color(0xFF1B153D)

// Success & Status
val NeliGreenSuccess = Color(0xFF00E676)
val NeliGreenPrimary = Color(0xFF00E676)
val NeliGreenGlow = Color(0x3300E676)
val NeliPillDark = Color(0x660B0818)
val NeliBlueAccent = Color(0xFF00E5FF)
val NeliError = Color(0xFFFF4757)

// Dynamic Gradients
val NeliPurpleGradient = Brush.horizontalGradient(
    listOf(Color(0xFF6C5CE7), Color(0xFF8B5CF6))
)

val NeliGoldGradient = Brush.horizontalGradient(
    listOf(Color(0xFFFFD700), Color(0xFFFFA500))
)

val NeliHeroFadeGradient = Brush.verticalGradient(
    listOf(
        Color.Transparent,
        Color(0x990B0818),
        Color(0xFF0B0818)
    )
)
