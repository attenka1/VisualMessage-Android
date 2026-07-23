package fi.attenka.VisualMessage.receive

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Holds the live decoding state for the morse receiver. Camera samples are processed on a
 * dedicated executor and completed snapshots are posted back to the main thread for Compose.
 */
class ReceiveViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = ReceivePreferences(application)

    var state by mutableStateOf(MorseReceiverState())
        private set

    var preferHighFrameRate by mutableStateOf(preferences.preferHighFrameRate)
        private set

    private val receiverLock = Any()
    private val receiver = MorseReceiver { nextState ->
        viewModelScope.launch(Dispatchers.Main.immediate) {
            state = nextState
        }
    }

    val analyzer = LuminanceAnalyzer { level, timestampMs ->
        synchronized(receiverLock) {
            receiver.onSample(level, timestampMs)
        }
    }

    fun clear() {
        synchronized(receiverLock) {
            receiver.reset()
        }
    }

    fun updatePreferHighFrameRate(enabled: Boolean) {
        preferHighFrameRate = enabled
        preferences.preferHighFrameRate = enabled
    }
}
