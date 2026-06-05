package fi.attenka.VisualMessage.player

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fi.attenka.VisualMessage.R
import fi.attenka.VisualMessage.model.FrameKind
import fi.attenka.VisualMessage.model.TransmissionFrame
import fi.attenka.VisualMessage.model.TransmissionMode
import fi.attenka.VisualMessage.model.TransmissionSequenceBuilder
import fi.attenka.VisualMessage.model.TransmissionSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drives playback of a transmission: steps through the frame list on the main thread,
 * exposing the current frame and progress text as Compose state. Mirrors the iOS
 * TransmissionPlayer.
 */
class TransmissionPlayer(application: Application) : AndroidViewModel(application) {

    private val readyText = application.getString(R.string.ready)

    var currentFrame by mutableStateOf(TransmissionFrame(FrameKind.Blank, 0.0))
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var progressText by mutableStateOf(readyText)
        private set

    private val tonePlayer = TonePlayer()
    private val flashlightController = FlashlightController(application)
    private var playbackJob: Job? = null

    fun start(settings: TransmissionSettings) {
        stop()

        val frames = TransmissionSequenceBuilder.frames(settings)
        if (frames.isEmpty()) return

        isPlaying = true

        playbackJob = viewModelScope.launch {
            try {
                flashlightController.turnOff()

                // Give the user time to turn the phone before anything is shown or heard.
                for (seconds in settings.startDelaySeconds downTo 1) {
                    if (!isActive) return@launch
                    progressText = seconds.toString()
                    delay(1000)
                }

                progressText = getApplication<Application>().getString(R.string.sending)

                if (settings.soundSignalEnabled) {
                    tonePlayer.playSignal(settings.signalFrequency)
                }

                frames.forEachIndexed { index, frame ->
                    if (!isActive) return@launch
                    currentFrame = frame
                    val isMorseSignal = frame.kind is FrameKind.MorseSignal
                    flashlightController.setEnabled(frame.shouldUseTorch(settings))
                    tonePlayer.setContinuousToneEnabled(
                        enabled = settings.mode == TransmissionMode.MORSE &&
                            settings.morseSoundEnabled &&
                            isMorseSignal,
                        frequency = settings.signalFrequency,
                    )
                    progressText = "${index + 1} / ${frames.size}"
                    delay((frame.durationSeconds * 1000).toLong())
                }
            } finally {
                flashlightController.turnOff()
                tonePlayer.setContinuousToneEnabled(false)
            }
            finish()
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        tonePlayer.setContinuousToneEnabled(false)
        tonePlayer.stop()
        flashlightController.turnOff()
        finish()
    }

    private fun finish() {
        currentFrame = TransmissionFrame(FrameKind.Blank, 0.0)
        progressText = readyText
        isPlaying = false
    }

    override fun onCleared() {
        super.onCleared()
        tonePlayer.stop()
        flashlightController.turnOff()
    }

    private fun TransmissionFrame.shouldUseTorch(settings: TransmissionSettings): Boolean =
        settings.mode == TransmissionMode.MORSE &&
            settings.morseOutputMode.usesTorch &&
            kind is FrameKind.MorseSignal
}
