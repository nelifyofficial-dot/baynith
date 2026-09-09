package com.example

import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.ui.player.embed.NeliPlayEmbedUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTypeTest {

    @Test
    fun testMovieExplicitMp4() {
        val movie = Movie(
            id = "m1",
            title = "Test MP4",
            streamUrl = "https://cdn.example.com/movie.mp4",
            playbackType = "mp4"
        )
        assertEquals("mp4", movie.effectivePlaybackType)
        assertFalse(movie.isEmbed)
    }

    @Test
    fun testMovieExplicitM3u8() {
        val movie = Movie(
            id = "m2",
            title = "Test HLS",
            streamUrl = "https://cdn.example.com/master.m3u8",
            playbackType = "m3u8"
        )
        assertEquals("m3u8", movie.effectivePlaybackType)
        assertFalse(movie.isEmbed)
    }

    @Test
    fun testMovieExplicitEmbed() {
        val embedHtml = "<iframe src=\"https://provider.com/embed/123\"></iframe>"
        val movie = Movie(
            id = "m3",
            title = "Test Embed",
            playbackType = "embed",
            embedCode = embedHtml
        )
        assertEquals("embed", movie.effectivePlaybackType)
        assertTrue(movie.isEmbed)
    }

    @Test
    fun testMovieBackwardCompatibility_m3u8InUrl() {
        val movie = Movie(
            id = "m4",
            title = "Legacy HLS",
            streamUrl = "https://cdn.example.com/video/live.m3u8",
            playbackType = "" // blank / not set in old document
        )
        assertEquals("m3u8", movie.effectivePlaybackType)
        assertFalse(movie.isEmbed)
    }

    @Test
    fun testMovieBackwardCompatibility_mp4Default() {
        val movie = Movie(
            id = "m5",
            title = "Legacy MP4",
            streamUrl = "https://cdn.example.com/video/movie.mp4",
            playbackType = ""
        )
        assertEquals("mp4", movie.effectivePlaybackType)
        assertFalse(movie.isEmbed)
    }

    @Test
    fun testEpisodePlaybackType() {
        val episode = Episode(
            id = "ep1",
            title = "Episode 1",
            streamUrl = "",
            playbackType = "embed",
            embedCode = "<iframe src=\"https://test.com/embed/ep1\"></iframe>"
        )
        assertEquals("embed", episode.effectivePlaybackType)
        assertTrue(episode.isEmbed)
    }

    @Test
    fun testEmbedUtils_urlHandling() {
        val rawUrl = "https://player.vimeo.com/video/76979871"
        val html = NeliPlayEmbedUtils.buildSafeEmbedHtml(rawUrl)
        assertTrue(html.contains("<iframe"))
        assertTrue(html.contains("src=\"https://player.vimeo.com/video/76979871\""))
        assertTrue(html.contains("allowfullscreen"))
        assertTrue(html.contains("background-color: #000000;"))
    }

    @Test
    fun testEmbedUtils_iframeSnippet() {
        val snippet = "<iframe src=\"https://embed.provider.org/watch?id=456\"></iframe>"
        val html = NeliPlayEmbedUtils.buildSafeEmbedHtml(snippet)
        assertTrue(html.contains("allowfullscreen"))
        assertTrue(html.contains("encrypted-media"))
        assertTrue(html.contains("picture-in-picture"))
    }

    @Test
    fun testEmbedUtils_blankHandling() {
        val html = NeliPlayEmbedUtils.buildSafeEmbedHtml("")
        assertEquals("", html)
    }

    @Test
    fun testEmbedUtils_playsinlineAndSanitizedHost() {
        val snippet = "<iframe src=\"https://myembed.video/player/456\"></iframe>"
        val html = NeliPlayEmbedUtils.buildSafeEmbedHtml(snippet)
        assertTrue(html.contains("playsinline"))

        val host = NeliPlayEmbedUtils.extractSanitizedHost("https://stream.cdn.com/embed/123?token=secret123&pass=secret456")
        assertEquals("stream.cdn.com", host)
        assertFalse(host.contains("secret123"))
    }
}
