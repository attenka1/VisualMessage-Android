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
    /**
     * In debug builds we use Google's official sample banner unit, which always serves
     * test ads. The release id below is a placeholder and must be replaced with the real
     * Android banner ad unit id before publishing.
     */
    val bannerAdUnitID: String
        get() = if (BuildConfig.DEBUG) {
            "ca-app-pub-3940256099942544/6300978111"
        } else {
            "ca-app-pub-3940256099942544/6300978111"
        }
}
