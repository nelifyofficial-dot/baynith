package com.example.ui.player.embed

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliVoid

private const val TAG = "NeliPlayEmbed"

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NeliPlayEmbeddedPlayer(
    embedCode: String,
    title: String,
    onBack: () -> Unit,
    contentId: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var isManualFullscreen by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }

    var showControls by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionSeconds by remember { mutableFloatStateOf(0f) }
    var durationSeconds by remember { mutableFloatStateOf(0f) }
    var isSubtitlesEnabled by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentQuality by remember { mutableStateOf("Auto") }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Auto-hide controls overlay after 3.8s of inactivity while playing
    LaunchedEffect(showControls, isPlaying, isControlsLocked, showSpeedMenu, showQualityMenu) {
        if (showControls && isPlaying && !isControlsLocked && !showSpeedMenu && !showQualityMenu) {
            kotlinx.coroutines.delay(3800)
            showControls = false
        }
    }

    // Intercept hardware Back button: exit fullscreen first, else pop back stack
    BackHandler {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else if (isManualFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            isManualFullscreen = false
        } else {
            onBack()
        }
    }

    // Lifecycle handling: pause and resume timers and webview without reload
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    Log.d(TAG, "[contentId=$contentId] Lifecycle $event: pausing webview")
                    webViewInstance?.evaluateJavascript(
                        "(function() { var v = document.querySelector('video'); if (v && !v.paused) v.pause(); })();",
                        null
                    )
                    webViewInstance?.onPause()
                    webViewInstance?.pauseTimers()
                }
                Lifecycle.Event.ON_RESUME -> {
                    Log.d(TAG, "[contentId=$contentId] Lifecycle ON_RESUME: resuming webview")
                    webViewInstance?.onResume()
                    webViewInstance?.resumeTimers()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                Log.d(TAG, "[contentId=$contentId] Destroying embedded WebView")
                webViewInstance?.let { wv ->
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.destroy()
                }
                webViewInstance = null
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } catch (e: Throwable) {
                Log.w(TAG, "[contentId=$contentId] WebView cleanup exception: ${e.message}")
            }
        }
    }

    // Fullscreen Custom View overlay (provider triggered, e.g. via iframe fullscreen button)
    if (customView != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("embed_custom_view_fullscreen")
        ) {
            AndroidView(
                factory = {
                    FrameLayout(it).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        customView?.let { cv ->
                            (cv.parent as? ViewGroup)?.removeView(cv)
                            addView(cv)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    // Main Layout
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
            .testTag("neliplay_embedded_player_container")
    ) {
        if (embedCode.isBlank() || hasError) {
            // Friendly Player Error Screen with Retry System
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeliVoid)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Playback Error",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Playback Error",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorMessage ?: "Unable to continue playback. Please check your connection and try again.",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            Log.d(TAG, "[contentId=$contentId] User tapped Retry: recreating fresh WebView")
                            try {
                                webViewInstance?.let { wv ->
                                    (wv.parent as? ViewGroup)?.removeView(wv)
                                    wv.destroy()
                                }
                            } catch (e: Throwable) {
                                Log.w(TAG, "[contentId=$contentId] Error during retry cleanup: ${e.message}")
                            }
                            webViewInstance = null
                            hasError = false
                            errorMessage = null
                            isLoading = true
                            retryKey++
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeliBluePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("embed_retry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onBack,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = NeliSurfaceElevated,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = null,
                        modifier = Modifier.testTag("embed_error_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Go Back",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            // Embedded WebView Container with persistent state & fresh retry instantiation
            key(retryKey) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            Log.d(TAG, "[contentId=$contentId] Creating fresh WebView instance (retryKey=$retryKey)")
                            NeliPlayEmbedUtils.sanitizeWebViewEnvironment(ctx)

                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(android.graphics.Color.BLACK)

                                // Hardware composited layer for HTML5 video decoders and surfaces
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                                // Security & Settings Configuration
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    setSupportZoom(false)
                                    setSupportMultipleWindows(false)
                                    javaScriptCanOpenWindowsAutomatically = false
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                }

                                // Cookie Support for legitimate third-party embeds
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webChromeClient = object : WebChromeClient() {
                                    override fun getDefaultVideoPoster(): Bitmap? {
                                        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                    }

                                    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                                        Log.d(TAG, "[contentId=$contentId] WebChromeClient: onShowCustomView")
                                        customView = view
                                        customViewCallback = callback
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    }

                                    override fun onHideCustomView() {
                                        Log.d(TAG, "[contentId=$contentId] WebChromeClient: onHideCustomView")
                                        customView = null
                                        customViewCallback = null
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        val scheme = request.url?.scheme?.lowercase() ?: ""

                                        // Block non-http schemes (intent, market, etc.) safely without throwing unknown scheme errors
                                        if (scheme != "http" && scheme != "https") {
                                            Log.d(TAG, "[contentId=$contentId] Blocked custom scheme navigation: $scheme")
                                            return true
                                        }

                                        // Allow normal playback redirects and iframe navigations
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        val host = NeliPlayEmbedUtils.extractSanitizedHost(url)
                                        Log.d(TAG, "[contentId=$contentId] onPageStarted (host=$host)")
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        val host = NeliPlayEmbedUtils.extractSanitizedHost(url)
                                        Log.d(TAG, "[contentId=$contentId] onPageFinished (host=$host)")
                                        isLoading = false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        val isMainFrame = request?.isForMainFrame == true
                                        val errorCode = error?.errorCode ?: 0
                                        val desc = error?.description?.toString() ?: "unknown"
                                        val host = NeliPlayEmbedUtils.extractSanitizedHost(request?.url?.toString())

                                        Log.w(TAG, "[contentId=$contentId] onReceivedError (mainFrame=$isMainFrame, code=$errorCode, desc=$desc, host=$host)")

                                        // Only trigger error if the main frame fails with a fatal network/connection error
                                        if (isMainFrame) {
                                            // Ignore non-fatal aborts (-1) which happen during normal stream switching
                                            if (errorCode != WebViewClient.ERROR_UNKNOWN && errorCode != -1) {
                                                isLoading = false
                                                hasError = true
                                                errorMessage = "Unable to continue playback. Please check your connection and try again."
                                            }
                                        }
                                    }

                                    override fun onReceivedHttpError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        errorResponse: WebResourceResponse?
                                    ) {
                                        val isMainFrame = request?.isForMainFrame == true
                                        val status = errorResponse?.statusCode ?: 0
                                        val host = NeliPlayEmbedUtils.extractSanitizedHost(request?.url?.toString())

                                        Log.w(TAG, "[contentId=$contentId] onReceivedHttpError (mainFrame=$isMainFrame, status=$status, host=$host)")

                                        // Only handle severe main frame 5xx server failures
                                        if (isMainFrame && status in 500..599) {
                                            isLoading = false
                                            hasError = true
                                            errorMessage = "Provider server is currently unavailable. Please try again later."
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        val didCrash = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            detail?.didCrash() ?: false
                                        } else {
                                            false
                                        }
                                        val priority = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            detail?.rendererPriorityAtExit() ?: -1
                                        } else {
                                            -1
                                        }
                                        Log.e(TAG, "[contentId=$contentId] onRenderProcessGone (didCrash=$didCrash, priority=$priority)")

                                        // Promptly detach and destroy dead WebView so the OS browser terminator does not crash the app
                                        try {
                                            (view?.parent as? ViewGroup)?.removeView(view)
                                            view?.destroy()
                                        } catch (e: Throwable) {
                                            Log.w(TAG, "[contentId=$contentId] Error destroying terminated WebView: ${e.message}")
                                        }
                                        webViewInstance = null

                                        isLoading = false
                                        hasError = true
                                        errorMessage = "Playback was interrupted. Tap Retry to reload the player."
                                        // Return true to signal that the app has handled the termination and prevent application crash
                                        return true
                                    }
                                }

                                webViewInstance = this
                                val directUrl = NeliPlayEmbedUtils.extractEmbedUrl(embedCode)
                                if (directUrl != null && !embedCode.contains("<script", ignoreCase = true)) {
                                    Log.d(TAG, "[contentId=$contentId] Loading direct embed URL into WebView: $directUrl")
                                    loadUrl(directUrl)
                                } else {
                                    val safeHtml = NeliPlayEmbedUtils.buildSafeEmbedHtml(embedCode)
                                    Log.d(TAG, "[contentId=$contentId] Loading embed HTML into WebView")
                                    loadDataWithBaseURL("https://neliplay.app/", safeHtml, "text/html", "UTF-8", null)
                                }
                            }
                        },
                        update = {
                            // Intentionally no-op to prevent reload on normal recompositions
                        },
                        onRelease = { webView ->
                            try {
                                (webView.parent as? ViewGroup)?.removeView(webView)
                                webView.destroy()
                            } catch (e: Throwable) {
                                Log.w(TAG, "[contentId=$contentId] Error in onRelease: ${e.message}")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Loading Indicator
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = NeliCyanAccent,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
        }

        // Periodic position updater from HTML5 video element
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                webViewInstance?.evaluateJavascript(
                    """
                    (function() {
                        var v = document.querySelector('video');
                        if (v) {
                            return JSON.stringify({
                                currentTime: v.currentTime || 0,
                                duration: v.duration || 0,
                                paused: v.paused,
                                playbackRate: v.playbackRate || 1.0
                            });
                        }
                        return null;
                    })();
                    """.trimIndent()
                ) { result ->
                    if (result != null && result != "null" && result != "\"null\"") {
                        try {
                            val cleanJson = if (result.startsWith("\"") && result.endsWith("\"")) {
                                result.substring(1, result.length - 1).replace("\\\"", "\"")
                            } else {
                                result
                            }
                            val obj = org.json.JSONObject(cleanJson)
                            currentPositionSeconds = obj.optDouble("currentTime", 0.0).toFloat()
                            val dur = obj.optDouble("duration", 0.0).toFloat()
                            if (dur > 0) durationSeconds = dur
                            isPlaying = !obj.optBoolean("paused", false)
                        } catch (e: Throwable) {
                            // Ignore json parse error
                        }
                    }
                }
            }
        }

        // Helper functions for playback control via JavaScript injection
        val togglePlayPause = {
            webViewInstance?.evaluateJavascript(
                """
                (function() {
                    var v = document.querySelector('video');
                    if (v) {
                        if (v.paused) {
                            v.play();
                            return 'playing';
                        } else {
                            v.pause();
                            return 'paused';
                        }
                    }
                    return 'none';
                })();
                """.trimIndent()
            ) { res ->
                if (res?.contains("playing") == true) {
                    isPlaying = true
                } else if (res?.contains("paused") == true) {
                    isPlaying = false
                } else {
                    isPlaying = !isPlaying
                }
            }
        }

        val seekRelative = { offsetSeconds: Float ->
            val target = (currentPositionSeconds + offsetSeconds).coerceAtLeast(0f)
            currentPositionSeconds = target
            webViewInstance?.evaluateJavascript(
                """
                (function() {
                    var v = document.querySelector('video');
                    if (v) {
                        v.currentTime = Math.max(0, Math.min(v.duration || 999999, (v.currentTime || 0) + ($offsetSeconds)));
                        return v.currentTime;
                    }
                    return 0;
                })();
                """.trimIndent(),
                null
            )
        }

        val seekTo = { targetSeconds: Float ->
            currentPositionSeconds = targetSeconds
            webViewInstance?.evaluateJavascript(
                """
                (function() {
                    var v = document.querySelector('video');
                    if (v) {
                        v.currentTime = $targetSeconds;
                    }
                })();
                """.trimIndent(),
                null
            )
        }

        val setSpeed = { speed: Float ->
            currentSpeed = speed
            webViewInstance?.evaluateJavascript(
                """
                (function() {
                    var v = document.querySelector('video');
                    if (v) {
                        v.playbackRate = $speed;
                    }
                })();
                """.trimIndent(),
                null
            )
        }

        val toggleSubtitles = {
            isSubtitlesEnabled = !isSubtitlesEnabled
            webViewInstance?.evaluateJavascript(
                """
                (function() {
                    var v = document.querySelector('video');
                    if (v && v.textTracks) {
                        for (var i = 0; i < v.textTracks.length; i++) {
                            v.textTracks[i].mode = '$isSubtitlesEnabled' === 'true' ? 'showing' : 'hidden';
                        }
                    }
                })();
                """.trimIndent(),
                null
            )
        }

        val formatTime = { seconds: Float ->
            val totalSeconds = seconds.toInt().coerceAtLeast(0)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val secs = totalSeconds % 60
            if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, secs)
            } else {
                String.format("%02d:%02d", minutes, secs)
            }
        }

        // Lock Screen Button (visible when locked or when controls are toggled)
        AnimatedVisibility(
            visible = showControls || isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp)
        ) {
            IconButton(
                onClick = { isControlsLocked = !isControlsLocked },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .testTag("embed_lock_toggle")
            ) {
                Icon(
                    imageVector = if (isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isControlsLocked) "Unlock Controls" else "Lock Controls",
                    tint = if (isControlsLocked) NeliCyanAccent else Color.White
                )
            }
        }

        // Cinematic Dark Controls Overlay
        AnimatedVisibility(
            visible = showControls && !isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Overlay Bar (Controls: Back, Title, CC, Speed, Quality, Fullscreen)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.70f))
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("embed_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Top Action Icons: CC, Speed, Quality, Fullscreen
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Subtitles (CC) Toggle
                            IconButton(
                                onClick = { toggleSubtitles() },
                                modifier = Modifier.testTag("embed_cc_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClosedCaption,
                                    contentDescription = "Closed Captions",
                                    tint = if (isSubtitlesEnabled) NeliCyanAccent else Color.White.copy(alpha = 0.8f)
                                )
                            }

                            // Speed Selector
                            Box {
                                IconButton(
                                    onClick = { showSpeedMenu = true },
                                    modifier = Modifier.testTag("embed_speed_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "Playback Speed",
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false }
                                ) {
                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                        DropdownMenuItem(
                                            text = { Text("${spd}x" + if (currentSpeed == spd) " ✓" else "") },
                                            onClick = {
                                                setSpeed(spd)
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Quality Selector
                            Box {
                                IconButton(
                                    onClick = { showQualityMenu = true },
                                    modifier = Modifier.testTag("embed_quality_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = "Video Quality",
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showQualityMenu,
                                    onDismissRequest = { showQualityMenu = false }
                                ) {
                                    listOf("Auto", "1080p", "720p", "480p", "360p").forEach { q ->
                                        DropdownMenuItem(
                                            text = { Text(q + if (currentQuality == q) " ✓" else "") },
                                            onClick = {
                                                currentQuality = q
                                                showQualityMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Fullscreen toggle
                            IconButton(
                                onClick = {
                                    if (isManualFullscreen) {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                        isManualFullscreen = false
                                    } else {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        isManualFullscreen = true
                                    }
                                },
                                modifier = Modifier.testTag("embed_fullscreen_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isManualFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Toggle Fullscreen",
                                    tint = Color.White
                                )
                            }

                            // Spacer for the top-right Lock button
                            Spacer(modifier = Modifier.width(42.dp))
                        }
                    }
                }

                // Center Playback Controls: Replay 10s, Play/Pause, Forward 10s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 10s Back
                    IconButton(
                        onClick = { seekRelative(-10f) },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                            .testTag("embed_rewind_10s")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play / Pause
                    IconButton(
                        onClick = { togglePlayPause() },
                        modifier = Modifier
                            .size(68.dp)
                            .background(NeliBluePrimary.copy(alpha = 0.85f), RoundedCornerShape(34.dp))
                            .testTag("embed_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // 10s Forward
                    IconButton(
                        onClick = { seekRelative(10f) },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                            .testTag("embed_forward_10s")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Overlay Bar (Progress Bar with Current Time & Duration)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.70f))
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = if (durationSeconds > 0f) currentPositionSeconds.coerceIn(0f, durationSeconds) else 0f,
                            onValueChange = { newPos ->
                                seekTo(newPos)
                            },
                            valueRange = 0f..(if (durationSeconds > 0f) durationSeconds else 100f),
                            colors = SliderDefaults.colors(
                                thumbColor = NeliCyanAccent,
                                activeTrackColor = NeliCyanAccent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .testTag("embed_playback_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentPositionSeconds),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = if (durationSeconds > 0f) formatTime(durationSeconds) else "--:--",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
