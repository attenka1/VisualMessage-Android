package fi.attenka.VisualMessage.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import fi.attenka.VisualMessage.BuildConfig

/** Bottom banner ad, mirroring the iOS AdBannerHost. */
@Composable
fun AdBannerHost(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdMobConfiguration.bannerAdUnitID
                    loadAd(AdRequest.Builder().build())
                }
            },
        )
    }
}

object AdMobConfiguration {
    /** Set admob.banner.id in local.properties for release builds. */
    val bannerAdUnitID: String
        get() = BuildConfig.ADMOB_BANNER_ID
}
