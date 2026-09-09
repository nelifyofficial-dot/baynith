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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

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
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d(TAG, "[contentId=$contentId] Lifecycle ON_PAUSE: pausing webview")
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
                            NeliPlayEmbedUtils.prewarmWebViewEnvironment(ctx)

                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(android.graphics.Color.BLACK)

                                // Prevent GPU memory exhaustion / OOM renderer crashes by using standard window rendering
                                setLayerType(View.LAYER_TYPE_NONE, null)

                                // Security & Settings Configuration
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
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

                                        // Prevent top-level navigation away from the player to third-party ad sites
                                        if (request.isForMainFrame) {
                                            if (url == "https://neliplay.app/" || url == "about:blank") {
                                                return false
                                            }
                                            val sanitizedHost = NeliPlayEmbedUtils.extractSanitizedHost(url)
                                            Log.d(TAG, "[contentId=$contentId] Intercepted top-level navigation to: $sanitizedHost")
                                            // If embedCode is a direct provider URL that is loading, allow it
                                            if (embedCode.trim().startsWith("http", ignoreCase = true) &&
                                                url.contains(NeliPlayEmbedUtils.extractSanitizedHost(embedCode))
                                            ) {
                                                return false
                                            }
                                            // Otherwise keep player intact and suppress external ad redirect
                                            return true
                                        }

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
                                        val didCrash = detail?.didCrash() ?: false
                                        val priority = detail?.rendererPriorityAtExit() ?: -1
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
                                val safeHtml = NeliPlayEmbedUtils.buildSafeEmbedHtml(embedCode)
                                Log.d(TAG, "[contentId=$contentId] Loading embed HTML into WebView")
                                loadDataWithBaseURL("https://neliplay.app/", safeHtml, "text/html", "UTF-8", null)
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

        // Top Overlay Bar (Controls: Back, Title, Fullscreen Toggle) - "Embedded Player" label REMOVED
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
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

                        // Movie / Episode Title (without "Embedded Player" label)
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Manual Fullscreen toggle
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
                }
            }
        }
    }
}
