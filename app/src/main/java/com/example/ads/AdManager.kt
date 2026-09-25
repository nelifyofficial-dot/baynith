package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Centralized AdMob manager for NeliPlay.
 * AdMob App ID: ca-app-pub-4408731854837351~4416275156
 *
 * Ad Units:
 * App Open: ca-app-pub-4408731854837351/1013469770
 * Banner: ca-app-pub-4408731854837351/2204657741
 * Interstitial: ca-app-pub-4408731854837351/1021208631
 * Rewarded Interstitial: ca-app-pub-4408731854837351/8365260460
 * Rewarded: ca-app-pub-4408731854837351/5914442660
 * Native Advanced: ca-app-pub-4408731854837351/4952714780
 *
 * During development (DEBUG builds), standard Google sample test ad unit IDs are used
 * to comply with Google AdMob policies and prevent account flagging.
 */
object AdManager {
    private const val TAG = "NeliPlay_AdManager"

    // Production Ad Unit IDs
    private const val PROD_BANNER = "ca-app-pub-4408731854837351/2204657741"
    private const val PROD_INTERSTITIAL = "ca-app-pub-4408731854837351/1021208631"
    private const val PROD_REWARDED = "ca-app-pub-4408731854837351/5914442660"
    private const val PROD_NATIVE = "ca-app-pub-4408731854837351/4952714780"

    // Google Official Test Ad Unit IDs
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"

    val bannerAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER else PROD_BANNER

    val interstitialAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else PROD_INTERSTITIAL

    val rewardedAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED else PROD_REWARDED

    val nativeAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_NATIVE else PROD_NATIVE

    private val isInitialized = AtomicBoolean(false)

    // Preloaded Interstitial Ad
    private var preloadedInterstitial: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var lastInterstitialShownTimestamp: Long = 0L

    // Minimum cooldown between interstitial ads: 45 seconds to prevent spam
    private const val INTERSTITIAL_COOLDOWN_MS = 45_000L

    /**
     * Initializes MobileAds safely once at application startup.
     */
    fun init(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                MobileAds.initialize(context.applicationContext) { status ->
                    Log.d(TAG, "AdMob MobileAds initialized successfully: $status")
                }
                // Preload first interstitial ad in the background
                preloadInterstitial(context.applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize AdMob: ${e.message}", e)
            }
        }
    }

    /**
     * Builds a standard AdRequest
     */
    fun createAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    /**
     * Preloads an interstitial ad in background
     */
    fun preloadInterstitial(context: Context) {
        if (preloadedInterstitial != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val request = createAdRequest()
        InterstitialAd.load(
            context,
            interstitialAdUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully.")
                    preloadedInterstitial = ad
                    isInterstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message} (code ${error.code})")
                    preloadedInterstitial = null
                    isInterstitialLoading = false
                }
            }
        )
    }

    /**
     * Shows an interstitial ad if available and cooldown has elapsed.
     * After ad closes or if not available, invokes onAdDismissed() automatically so the user flow
     * is NEVER blocked!
     */
    fun showInterstitialIfAllowed(activity: Activity, onAdDismissed: () -> Unit) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastInterstitialShownTimestamp
        val ad = preloadedInterstitial

        if (ad != null && elapsed >= INTERSTITIAL_COOLDOWN_MS) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed by user.")
                    preloadedInterstitial = null
                    lastInterstitialShownTimestamp = System.currentTimeMillis()
                    onAdDismissed()
                    // Preload next one
                    preloadInterstitial(activity.applicationContext)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.w(TAG, "Interstitial ad failed to show: ${error.message}")
                    preloadedInterstitial = null
                    onAdDismissed()
                    preloadInterstitial(activity.applicationContext)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad displayed full screen.")
                }
            }
            ad.show(activity)
        } else {
            // Either no ad ready or on cooldown: continue immediately without blocking
            onAdDismissed()
            if (ad == null && !isInterstitialLoading) {
                preloadInterstitial(activity.applicationContext)
            }
        }
    }
}
