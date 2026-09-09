package com.example.cast

import android.content.Context
import com.example.R
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/**
 * Official Google Cast OptionsProvider implementation for NeliPlay.
 * Configured in AndroidManifest.xml under OPTIONS_PROVIDER_CLASS_NAME meta-data.
 * Uses the receiver Application ID defined in res/values/strings.xml (cast_app_id).
 */
class NeliPlayCastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val appId = try {
            val resId = context.resources.getIdentifier("cast_app_id", "string", context.packageName)
            if (resId != 0) {
                context.getString(resId).ifBlank {
                    CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                }
            } else {
                CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
            }
        } catch (e: Exception) {
            CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
        }

        return CastOptions.Builder()
            .setReceiverApplicationId(appId)
            .setStopReceiverApplicationWhenEndingSession(true)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
