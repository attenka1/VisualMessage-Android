package fi.attenka.VisualMessage.ads

import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import fi.attenka.VisualMessage.BuildConfig

object AdMobConfiguration {
    /** Set admob.banner.id in local.properties for release builds. */
    val bannerAdUnitID: String
        get() = BuildConfig.ADMOB_BANNER_ID

    /** Non-personalized ad request, mirroring the iOS AdMobConfiguration.nonPersonalizedRequest(). */
    fun createAdRequest(): AdRequest {
        val extras = Bundle().apply { putString("npa", "1") }
        return AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            .build()
    }
}
