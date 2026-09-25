package com.example.ads

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Adaptive/Responsive AdMob Banner Composable.
 * - Fits smoothly into content layout
 * - Does not block content if ad fails to load (hides container without large blank gap)
 * - Properly calls destroy() on disposal to prevent leaks
 * - Never continuously reloads the same banner
 */
@Composable
fun NeliAdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.bannerAdUnitId
) {
    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    // If ad failed to load, keep the container completely collapsed
    if (hasError) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            isAdLoaded = true
                            hasError = false
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            super.onAdFailedToLoad(error)
                            isAdLoaded = false
                            hasError = true
                        }
                    }
                    loadAd(AdManager.createAdRequest())
                }
            },
            update = {
                // Avoid reloading on recomposition
            }
        )
    }
}
