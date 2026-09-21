package com.example.ui.player.embed

object NeliPlayEmbedUtils {

    fun extractEmbedUrl(rawEmbedCode: String): String? {
        val trimmed = rawEmbedCode.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        val srcRegex = """src=["'](https?://[^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
        val match = srcRegex.find(trimmed)
        return match?.groupValues?.getOrNull(1)
    }

    /**
     * Builds a safe, responsive HTML wrapper for embedded third-party players.
     * Preserves provider's player, controls, ads, fullscreen, and playback permissions.
     * Does NOT inject any application secrets, Firebase tokens, or sensitive APIs.
     */
    fun buildSafeEmbedHtml(rawEmbedCode: String): String {
        val trimmed = rawEmbedCode.trim()
        if (trimmed.isEmpty()) {
            return ""
        }

        // If the embedCode is an URL instead of an iframe snippet, wrap it in an iframe
        val embedSnippet = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            """<iframe src="$trimmed" allowfullscreen="true" webkitallowfullscreen="true" mozallowfullscreen="true" allow="autoplay; fullscreen; encrypted-media; picture-in-picture; accelerometer; gyroscope" playsinline="true" webkit-playsinline="true" frameborder="0"></iframe>"""
        } else {
            // If it's an iframe without allowfullscreen or allow permissions, add standard playback permissions
            var code = trimmed
            if (!code.contains("allowfullscreen", ignoreCase = true)) {
                code = code.replaceFirst("<iframe", """<iframe allowfullscreen="true" webkitallowfullscreen="true" mozallowfullscreen="true"""", ignoreCase = true)
            }
            if (!code.contains("allow=", ignoreCase = true)) {
                code = code.replaceFirst(
                    "<iframe",
                    """<iframe allow="autoplay; fullscreen; encrypted-media; picture-in-picture; accelerometer; gyroscope"""",
                    ignoreCase = true
                )
            }
            if (!code.contains("playsinline", ignoreCase = true)) {
                code = code.replaceFirst(
                    "<iframe",
                    """<iframe playsinline="true" webkit-playsinline="true"""",
                    ignoreCase = true
                )
            }
            code
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
              <style>
                * {
                  box-sizing: border-box;
                }
                html, body {
                  margin: 0;
                  padding: 0;
                  width: 100%;
                  height: 100%;
                  background-color: #000000;
                  overflow: hidden;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                }
                .neliplay-embed-container {
                  position: relative;
                  width: 100%;
                  height: 100%;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  background-color: #000000;
                }
                iframe, video, object, embed {
                  position: absolute;
                  top: 0;
                  left: 0;
                  width: 100% !important;
                  height: 100% !important;
                  border: 0 !important;
                  outline: 0 !important;
                  object-fit: contain !important;
                }
                .neliplay-embed-container > div {
                  width: 100% !important;
                  height: 100% !important;
                  padding-top: 0 !important;
                  border-radius: 0 !important;
                  object-fit: contain !important;
                }
              </style>
            </head>
            <body>
              <div class="neliplay-embed-container">
                $embedSnippet
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Checks if the device is running in an emulator or virtualized container lacking physical DRI rendernodes.
     */
    fun isEmulatorEnvironment(): Boolean {
        val hardware = android.os.Build.HARDWARE.lowercase()
        val model = android.os.Build.MODEL.lowercase()
        val product = android.os.Build.PRODUCT.lowercase()
        val fingerprint = android.os.Build.FINGERPRINT.lowercase()
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        val brand = android.os.Build.BRAND.lowercase()
        val device = android.os.Build.DEVICE.lowercase()
        val board = android.os.Build.BOARD.lowercase()
        return hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                hardware.contains("cutf") ||
                hardware.contains("cuttlefish") ||
                hardware.contains("qemu") ||
                model.contains("google_sdk") ||
                model.contains("emulator") ||
                model.contains("android sdk") ||
                product.contains("sdk") ||
                product.contains("vbox") ||
                fingerprint.contains("generic") ||
                brand.startsWith("generic") ||
                device.startsWith("generic") ||
                manufacturer.contains("genymotion") ||
                board.contains("goldfish")
    }

    /**
     * Sanitizes WebView cache environment to prevent Chromium SimpleCache index reconstruction errors
     * and ENOENT file enumerator warnings. Ensures the required code cache subdirectories exist
     * so that Chromium's POSIX opendir can enumerate them cleanly without throwing ENOENT (2).
     */
    fun sanitizeWebViewEnvironment(context: android.content.Context) {
        try {
            val cache = context.cacheDir ?: return
            val webViewDir = java.io.File(cache, "WebView/Default")
            val httpCacheDir = java.io.File(webViewDir, "HTTP Cache")
            val httpCodeCacheDir = java.io.File(httpCacheDir, "Code Cache")
            val jsDir = java.io.File(httpCodeCacheDir, "js")
            val wasmDir = java.io.File(httpCodeCacheDir, "wasm")

            // Ensure js and wasm cache directories exist so Chromium's POSIX opendir does not fail with ENOENT (2)
            if (!jsDir.exists()) jsDir.mkdirs()
            if (!wasmDir.exists()) wasmDir.mkdirs()

            // Also ensure default profile Code Cache directories exist
            val defaultCodeCache = java.io.File(webViewDir, "Code Cache")
            val defaultJs = java.io.File(defaultCodeCache, "js")
            val defaultWasm = java.io.File(defaultCodeCache, "wasm")
            if (!defaultJs.exists()) defaultJs.mkdirs()
            if (!defaultWasm.exists()) defaultWasm.mkdirs()
        } catch (t: Throwable) {
            // Non-fatal, suppress if filesystem restricted
        }
    }

    @Deprecated("Use sanitizeWebViewEnvironment instead")
    fun prewarmWebViewEnvironment(context: android.content.Context) {
        sanitizeWebViewEnvironment(context)
    }

    /**
     * Extracts only the host or domain part of a URL for safe developer logging,
     * strictly excluding sensitive query parameters, auth tokens, and credentials.
     */
    fun extractSanitizedHost(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return "unknown"
        return try {
            val cleaned = if (!rawUrl.contains("://")) "https://$rawUrl" else rawUrl
            val uri = java.net.URI(cleaned)
            val host = uri.host
            if (!host.isNullOrBlank()) host else uri.scheme ?: "local"
        } catch (_: Throwable) {
            try {
                val aUri = android.net.Uri.parse(rawUrl)
                aUri.host ?: aUri.scheme ?: "unknown"
            } catch (__: Throwable) {
                "invalid_url"
            }
        }
    }
}
