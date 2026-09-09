package com.example.ui.player.embed

object NeliPlayEmbedUtils {

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
        return hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                hardware.contains("cutf") ||
                hardware.contains("cuttlefish") ||
                model.contains("google_sdk") ||
                model.contains("emulator") ||
                model.contains("android sdk") ||
                product.contains("sdk") ||
                product.contains("vbox") ||
                fingerprint.contains("generic")
    }

    /**
     * Prepares WebView disk cache directories and code cache structures.
     * Prevents Chromium simple_file_enumerator POSIX ENOENT errors on fresh installs:
     * - opendir /cache/WebView/Default/HTTP Cache/Code Cache/wasm
     * - opendir /cache/WebView/Default/HTTP Cache/Code Cache/js
     */
    fun prewarmWebViewEnvironment(context: android.content.Context) {
        try {
            val cache = context.cacheDir ?: return
            val webViewDir = java.io.File(cache, "WebView/Default")
            val httpCacheDir = java.io.File(webViewDir, "HTTP Cache")
            val codeCacheDir = java.io.File(httpCacheDir, "Code Cache")
            java.io.File(codeCacheDir, "js").mkdirs()
            java.io.File(codeCacheDir, "wasm").mkdirs()
            java.io.File(httpCacheDir, "index-dir").mkdirs()

            val defaultCodeCache = java.io.File(webViewDir, "Code Cache")
            java.io.File(defaultCodeCache, "js").mkdirs()
            java.io.File(defaultCodeCache, "wasm").mkdirs()
            java.io.File(webViewDir, "GPUCache").mkdirs()
        } catch (t: Throwable) {
            // Non-fatal, suppress if filesystem restricted
        }
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
