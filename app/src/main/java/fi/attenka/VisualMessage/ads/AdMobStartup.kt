package fi.attenka.VisualMessage.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Prepares AdMob after the Google UMP consent flow completes.
 * Mirrors the iOS AdMobStartup coordinator that initializes MobileAds once.
 */
object AdMobStartup {
    private var isPrepared = false
    private var isPreparing = false
    private val completions = mutableListOf<() -> Unit>()
    private val lock = Any()

    fun canRequestAds(context: Context): Boolean =
        runCatching { UserMessagingPlatform.getConsentInformation(context).canRequestAds() }
            .getOrDefault(false)

    fun prepare(activity: Activity, onReady: () -> Unit) {
        synchronized(lock) {
            if (isPrepared) {
                onReady()
                return
            }
            completions += onReady
            if (isPreparing) return
            isPreparing = true
        }

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    completePrepare(activity)
                }
            },
            { _ ->
                completePrepare(activity)
            },
        )
    }

    private fun completePrepare(activity: Activity) {
        if (!canRequestAds(activity)) {
            synchronized(lock) {
                isPreparing = false
                val pending = completions.toList()
                completions.clear()
                pending.forEach { it() }
            }
            return
        }

        MobileAds.initialize(activity) {
            synchronized(lock) {
                isPrepared = true
                isPreparing = false
                val pending = completions.toList()
                completions.clear()
                pending.forEach { it() }
            }
        }
    }
}
