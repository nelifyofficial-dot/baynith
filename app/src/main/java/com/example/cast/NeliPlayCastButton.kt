package com.example.cast

import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * NeliPlay Cast Button Composable.
 * Shows real Google Cast connection state (connected cyan glow vs standby white),
 * and launches the system Cast dialog on click without triggering background
 * hardware scanner overhead or video encoder interface queries.
 */
@Composable
fun NeliPlayCastButton(
    modifier: Modifier = Modifier,
    sizeDp: Int = 36
) {
    val context = LocalContext.current
    val castManager = remember { CastManager.getInstance(context) }
    val isCasting by castManager.isCasting.collectAsStateWithLifecycle()
    val isSupported = remember { castManager.isCastSupported() }

    IconButton(
        onClick = {
            if (isSupported) {
                castManager.showCastDialog(context)
            } else {
                Toast.makeText(
                    context,
                    "Google Cast connects to smart TVs on Google Play devices.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        modifier = modifier
            .size(sizeDp.dp)
            .testTag("cast_button")
    ) {
        Icon(
            imageVector = if (isCasting) Icons.Default.CastConnected else Icons.Default.Cast,
            contentDescription = if (isCasting) "Casting to TV" else "Cast to TV",
            tint = if (isCasting) Color(0xFF00D4FF) else Color.White
        )
    }
}
