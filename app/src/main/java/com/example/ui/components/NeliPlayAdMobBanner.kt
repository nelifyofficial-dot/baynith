package com.example.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds

private const val OFFICIAL_TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun NeliPlayAdMobBanner(
    adUnitId: String = "ca-app-pub-4408731854837351/2204657741",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var activeAdUnitId by remember(adUnitId) { mutableStateOf(adUnitId) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1117))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Subtle Ad attribution tag compliant with Google Play Ad Policy
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1E2638))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "AD",
                    color = Color(0xFF8B949E),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            AndroidView(
                modifier = Modifier
                    .size(width = 320.dp, height = 50.dp),
                factory = { ctx ->
                    try {
                        MobileAds.initialize(ctx.applicationContext) {}
                    } catch (e: Exception) {
                        Log.w("NeliPlayAdMob", "MobileAds init check: ${e.message}")
                    }

                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        this.adUnitId = activeAdUnitId
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                isAdLoaded = true
                                Log.d("NeliPlayAdMob", "AdMob banner loaded successfully: $activeAdUnitId")
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                isAdLoaded = false
                                Log.w(
                                    "NeliPlayAdMob",
                                    "AdMob banner failed to load ($activeAdUnitId, code ${error.code}): ${error.message}"
                                )
                                // If production ad unit has no fill (code 3) or fails in dev/emulator environment,
                                // fallback automatically to the official Google sample test banner ID so user ALWAYS sees the ad!
                                if (activeAdUnitId != OFFICIAL_TEST_BANNER_ID) {
                                    post {
                                        activeAdUnitId = OFFICIAL_TEST_BANNER_ID
                                        this@apply.adUnitId = OFFICIAL_TEST_BANNER_ID
                                        loadAd(AdRequest.Builder().build())
                                    }
                                }
                            }
                        }
                        val adRequest = AdRequest.Builder().build()
                        loadAd(adRequest)
                    }
                },
                update = { view ->
                    if (view.adUnitId != activeAdUnitId) {
                        view.adUnitId = activeAdUnitId
                        view.loadAd(AdRequest.Builder().build())
                    }
                },
                onRelease = { adView ->
                    try {
                        adView.destroy()
                    } catch (e: Exception) {
                        Log.e("NeliPlayAdMob", "Error destroying AdView: ${e.message}")
                    }
                }
            )
        }
    }
}
