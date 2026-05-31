package fi.attenka.VisualMessage.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.attenka.VisualMessage.BuildConfig
import fi.attenka.VisualMessage.R
import fi.attenka.VisualMessage.ads.AdBannerHost
import fi.attenka.VisualMessage.model.MessageLibrary
import fi.attenka.VisualMessage.model.TransmissionMode
import fi.attenka.VisualMessage.model.TransmissionSettings
import fi.attenka.VisualMessage.player.TransmissionPlayer

@Composable
fun ContentScreen(
    settings: TransmissionSettings,
    library: MessageLibrary,
    player: TransmissionPlayer,
    onUpdateSettings: ((TransmissionSettings) -> TransmissionSettings) -> Unit,
    onLibraryChange: (MessageLibrary) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Header(
                settings = settings,
                progressText = player.progressText,
                onSend = { player.start(settings) },
            )
            HorizontalDivider()
            Controls(
                modifier = Modifier.weight(1f),
                settings = settings,
                library = library,
                onUpdateSettings = onUpdateSettings,
                onLibraryChange = onLibraryChange,
            )
            AdBannerHost()
        }

        AnimatedVisibility(
            visible = player.isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            KeepScreenOn()
            PlaybackSurface(
                frame = player.currentFrame,
                settings = settings,
                progressText = player.progressText,
                onStop = player::stop,
            )
        }
    }
}

@Composable
private fun Header(
    settings: TransmissionSettings,
    progressText: String,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.launch_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text("VisualMessage", style = MaterialTheme.typography.titleLarge)
            Text(
                "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                progressText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onSend,
            enabled = settings.message.trim().isNotEmpty(),
        ) {
            Text("\u25B6")
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.send))
        }
    }
}

@Composable
private fun Controls(
    modifier: Modifier,
    settings: TransmissionSettings,
    library: MessageLibrary,
    onUpdateSettings: ((TransmissionSettings) -> TransmissionSettings) -> Unit,
    onLibraryChange: (MessageLibrary) -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MessageSection(settings, library, onUpdateSettings, onLibraryChange)
            ModeSection(settings, onUpdateSettings)
            if (settings.mode == TransmissionMode.MORSE) {
                MorseSection(settings, onUpdateSettings)
            }
            RhythmSection(settings, onUpdateSettings)
            SignalSection(settings, onUpdateSettings)
            ColorSection(settings, onUpdateSettings)
        }
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
