package fi.attenka.VisualMessage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fi.attenka.VisualMessage.player.TransmissionPlayer
import fi.attenka.VisualMessage.receive.ReceiveScreen
import fi.attenka.VisualMessage.ui.ContentScreen
import fi.attenka.VisualMessage.ui.theme.VisualMessageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            VisualMessageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val settingsViewModel: SettingsViewModel = viewModel()
                    val player: TransmissionPlayer = viewModel()
                    val lifecycleOwner = LocalLifecycleOwner.current
                    var showReceiver by remember { mutableStateOf(false) }

                    DisposableEffect(lifecycleOwner, player) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_STOP) player.stop()
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    if (showReceiver) {
                        ReceiveScreen(onClose = { showReceiver = false })
                    } else {
                        ContentScreen(
                            settings = settingsViewModel.settings,
                            library = settingsViewModel.library,
                            player = player,
                            onUpdateSettings = settingsViewModel::updateSettings,
                            onLibraryChange = { newLibrary -> settingsViewModel.updateLibrary { newLibrary } },
                            onImportImage = settingsViewModel::importMessageImage,
                            onRemoveImage = settingsViewModel::removeMessageImage,
                            onOpenReceiver = { showReceiver = true },
                        )
                    }
                }
            }
        }
    }
}
