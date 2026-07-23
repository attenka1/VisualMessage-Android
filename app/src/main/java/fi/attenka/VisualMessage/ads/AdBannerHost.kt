package fi.attenka.VisualMessage.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnPreDraw
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Bottom banner ad, mirroring the iOS AdBannerHost. */
@Composable
fun AdBannerHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    var canShowAds by remember { mutableStateOf(false) }

    LaunchedEffect(activity, view) {
        val hostActivity = activity ?: return@LaunchedEffect
        awaitFirstDraw(view)
        AdMobStartup.prepare(hostActivity) {
            view.post {
                canShowAds = AdMobStartup.canRequestAds(hostActivity)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (canShowAds) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(),
                factory = { hostContext ->
                    AdView(hostContext).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = AdMobConfiguration.bannerAdUnitID
                        loadAd(AdMobConfiguration.createAdRequest())
                    }
                },
            )
        }
    }
}

private suspend fun awaitFirstDraw(view: android.view.View) {
    suspendCancellableCoroutine { continuation ->
        view.doOnPreDraw {
            view.post {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }
}
