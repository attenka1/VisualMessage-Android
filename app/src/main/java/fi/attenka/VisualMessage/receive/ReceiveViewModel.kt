package fi.attenka.VisualMessage.receive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Holds the live decoding state for the morse receiver. The analyzer runs on the camera's
 * main executor, so receiver callbacks already arrive on the main thread.
 */
class ReceiveViewModel : ViewModel() {

    var state by mutableStateOf(MorseReceiverState())
        private set

    private val receiver = MorseReceiver { state = it }

    val analyzer = LuminanceAnalyzer { level, timestampMs ->
        receiver.onSample(level, timestampMs)
    }

    fun clear() = receiver.reset()
}
