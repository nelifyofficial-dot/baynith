package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.Series
import com.example.data.model.TvChannel

object NeliPlayShareUtils {

    const val BASE_URL = "https://neliplay.app"

    fun shareMovie(context: Context, movie: Movie) {
        val url = "$BASE_URL/movie/${movie.id}"
        val text = "Watch ${movie.title} on NeliPlay 🎬\n\n$url"
        launchShareSheet(context, text, "Share ${movie.title}")
    }

    fun shareSeries(context: Context, series: Series) {
        val url = "$BASE_URL/series/${series.id}"
        val text = "Watch ${series.name} on NeliPlay 🍿\n\n$url"
        launchShareSheet(context, text, "Share ${series.name}")
    }

    fun shareEpisode(context: Context, seriesName: String, episode: Episode) {
        val url = "$BASE_URL/episode/${episode.id}"
        val epTitle = "S${String.format("%02d", episode.seasonNumber)} E${String.format("%02d", episode.episodeNumber)}"
        val text = "Watch $seriesName ($epTitle) on NeliPlay 🎬\n\n$url"
        launchShareSheet(context, text, "Share $seriesName - $epTitle")
    }

    fun shareTvChannel(context: Context, channel: TvChannel) {
        val url = "$BASE_URL/tv/${channel.id}"
        val text = "Watch ${channel.name} live on NeliPlay 📺\n\n$url"
        launchShareSheet(context, text, "Share ${channel.name}")
    }

    private fun launchShareSheet(context: Context, text: String, title: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }
}
