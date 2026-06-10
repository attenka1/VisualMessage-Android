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
import kotlinx.coroutines.currentCoroutineContext
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

        val playbackSettings = repeatPlaybackSettings(settings)
        val firstFrames = TransmissionSequenceBuilder.frames(playbackSettings)
        if (firstFrames.isEmpty()) return

        isPlaying = true

        playbackJob = viewModelScope.launch {
            try {
                flashlightController.turnOff()

                // Give the user time to turn the phone before anything is shown or heard.
                if (settings.startDelaySeconds > 0) {
                    for (seconds in settings.startDelaySeconds downTo 1) {
                        if (!isActive) return@launch
                        progressText = seconds.toString()
                        delay(1000)
                    }
                }

                progressText = getApplication<Application>().getString(R.string.sending)

                if (settings.soundSignalEnabled) {
                    tonePlayer.playSignal(settings.signalFrequency)
                }

                if (settings.repeatForever) {
                    var loopIndex = 1
                    playFrames(firstFrames, settings, loopIndex)
                    val loopSettings = playbackSettings.copy(
                        visualSignalEnabled = false,
                    )
                    val loopFrames = TransmissionSequenceBuilder.frames(loopSettings)
                    while (isActive && loopFrames.isNotEmpty()) {
                        loopIndex += 1
                        playFrames(loopFrames, settings, loopIndex)
                    }
                } else {
                    playFrames(firstFrames, settings)
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

    private fun repeatPlaybackSettings(settings: TransmissionSettings): TransmissionSettings {
        if (!settings.repeatForever) return settings
        return settings.copy(repeatCount = 1)
    }

    private suspend fun playFrames(
        frames: List<TransmissionFrame>,
        settings: TransmissionSettings,
        loopIndex: Int? = null,
    ) {
        frames.forEachIndexed { index, frame ->
            if (!currentCoroutineContext().isActive) return
            currentFrame = frame.withPlaybackLoopIndex(loopIndex)
            val isMorseSignal = frame.kind is FrameKind.MorseSignal
            flashlightController.setEnabled(frame.shouldUseTorch(settings))
            tonePlayer.setContinuousToneEnabled(
                enabled = settings.mode == TransmissionMode.MORSE &&
                    settings.morseSoundEnabled &&
                    isMorseSignal,
                frequency = settings.signalFrequency,
            )
            progressText = if (settings.repeatForever) {
                getApplication<Application>().getString(R.string.sending_infinite, loopIndex ?: 1, index + 1, frames.size)
            } else {
                "${index + 1} / ${frames.size}"
            }
            delay((frame.durationSeconds * 1000).toLong())
        }
    }

    private fun TransmissionFrame.shouldUseTorch(settings: TransmissionSettings): Boolean =
        settings.mode == TransmissionMode.MORSE &&
            settings.morseOutputMode.usesTorch &&
            kind is FrameKind.MorseSignal

    private fun TransmissionFrame.withPlaybackLoopIndex(loopIndex: Int?): TransmissionFrame {
        val slideMessage = kind as? FrameKind.SlideMessage ?: return this
        return copy(kind = slideMessage.copy(index = loopIndex ?: slideMessage.index))
    }
}
