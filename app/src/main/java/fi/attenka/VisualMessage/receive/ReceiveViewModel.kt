package fi.attenka.VisualMessage.receive

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

/**
 * Holds the live decoding state for the morse receiver. The analyzer runs on the camera's
 * main executor, so receiver callbacks already arrive on the main thread.
 */
class ReceiveViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = ReceivePreferences(application)

    var state by mutableStateOf(MorseReceiverState())
        private set

    var preferHighFrameRate by mutableStateOf(preferences.preferHighFrameRate)
        private set

    private val receiver = MorseReceiver { state = it }

    val analyzer = LuminanceAnalyzer { level, timestampMs ->
        receiver.onSample(level, timestampMs)
    }

    fun clear() = receiver.reset()

    fun updatePreferHighFrameRate(enabled: Boolean) {
        preferHighFrameRate = enabled
        preferences.preferHighFrameRate = enabled
    }
}
