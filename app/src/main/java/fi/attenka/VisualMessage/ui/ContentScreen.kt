package fi.attenka.VisualMessage.ui

import android.Manifest
import android.net.Uri
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
    onImportImage: (Uri, Int) -> Unit,
    onRemoveImage: (String) -> Unit,
    onOpenReceiver: () -> Unit = {},
) {
    val context = LocalContext.current
    var pendingTorchSend by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var pendingImageInsertionIndex by remember { mutableStateOf(0) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onImportImage(uri, pendingImageInsertionIndex)
        }
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        val shouldStart = granted && pendingTorchSend
        pendingTorchSend = false
        if (shouldStart) {
            player.start(settings)
        }
    }
    fun send() {
        val needsCameraPermission = settings.mode == TransmissionMode.MORSE &&
            settings.torchSignalEnabled &&
            !hasCameraPermission
        if (needsCameraPermission) {
            pendingTorchSend = true
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            pendingTorchSend = false
            player.start(settings)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Header(
                settings = settings,
                progressText = player.progressText,
                onSend = { send() },
                onOpenHelp = { showHelp = true },
                onOpenReceiver = onOpenReceiver,
            )
            HorizontalDivider()
            Controls(
                modifier = Modifier.weight(1f),
                settings = settings,
                library = library,
                onUpdateSettings = onUpdateSettings,
                onLibraryChange = onLibraryChange,
                onPickImage = { insertionIndex ->
                    pendingImageInsertionIndex = insertionIndex
                    imagePickerLauncher.launch("image/*")
                },
                onRemoveImage = onRemoveImage,
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

        if (showHelp) {
            HelpDialog(onDismiss = { showHelp = false })
        }
    }
}

@Composable
private fun Header(
    settings: TransmissionSettings,
    progressText: String,
    onSend: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenReceiver: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        val compact = maxWidth < 430.dp

        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HeaderIdentity(progressText = progressText)
                HeaderActions(
                    settings = settings,
                    onSend = onSend,
                    onOpenHelp = onOpenHelp,
                    onOpenReceiver = onOpenReceiver,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeaderIdentity(progressText = progressText, modifier = Modifier.weight(1f))
                HeaderActions(
                    settings = settings,
                    onSend = onSend,
                    onOpenHelp = onOpenHelp,
                    onOpenReceiver = onOpenReceiver,
                )
            }
        }
    }
}

@Composable
private fun HeaderIdentity(
    progressText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.launch_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text("VisualMessage", style = MaterialTheme.typography.titleMedium)
            Text(
                progressText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeaderActions(
    settings: TransmissionSettings,
    onSend: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenReceiver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onOpenHelp) {
            Text(stringResource(R.string.help))
        }
        TextButton(onClick = onOpenReceiver) {
            Text(stringResource(R.string.receive))
        }
        Button(
            onClick = onSend,
            enabled = settings.message.trim().isNotEmpty() ||
                (settings.mode == TransmissionMode.VISUAL && settings.messageImages.isNotEmpty()),
            modifier = Modifier.height(44.dp),
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
    onPickImage: (Int) -> Unit,
    onRemoveImage: (String) -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MessageSection(settings, library, onUpdateSettings, onLibraryChange, onPickImage, onRemoveImage)
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
