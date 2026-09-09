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
            """<iframe src="$trimmed" allowfullscreen allow="autoplay; fullscreen; encrypted-media; picture-in-picture" frameborder="0"></iframe>"""
        } else {
            // If it's an iframe without allowfullscreen or allow permissions, add standard playback permissions
            var code = trimmed
            if (!code.contains("allowfullscreen", ignoreCase = true)) {
                code = code.replaceFirst("<iframe", "<iframe allowfullscreen", ignoreCase = true)
            }
            if (!code.contains("allow=", ignoreCase = true)) {
                code = code.replaceFirst(
                    "<iframe",
                    """<iframe allow="autoplay; fullscreen; encrypted-media; picture-in-picture"""",
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
}
