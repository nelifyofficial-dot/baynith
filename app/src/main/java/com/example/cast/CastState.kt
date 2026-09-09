package com.example.cast

sealed class CastPlaybackState {
    object Disconnected : CastPlaybackState()
    object Connecting : CastPlaybackState()
    object Connected : CastPlaybackState()
    object Playing : CastPlaybackState()
    object Paused : CastPlaybackState()
    object Buffering : CastPlaybackState()
    data class Error(val message: String) : CastPlaybackState()
}
