package fi.attenka.VisualMessage.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays the short three-beep attention signal used at the start of a transmission.
 * Generates a sine-wave PCM buffer on the fly, mirroring the iOS TonePlayer.
 */
class TonePlayer {

    private val sampleRate = 44_100
    private var track: AudioTrack? = null
    private var continuousToneEnabled = false
    private var continuousToneFrequency: Double? = null

    fun playSignal(frequency: Double) {
        stop()

        val samples = makeSignalSamples(frequency)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        track = audioTrack
    }

    fun setContinuousToneEnabled(enabled: Boolean, frequency: Double = 880.0) {
        if (enabled) {
            if (continuousToneEnabled && continuousToneFrequency == frequency) {
                return
            }
            playContinuousTone(frequency)
            return
        }

        if (!continuousToneEnabled) {
            return
        }

        stop()
    }

    fun stop() {
        continuousToneEnabled = false
        continuousToneFrequency = null
        track?.run {
            runCatching {
                if (state == AudioTrack.STATE_INITIALIZED) stop()
                release()
            }
        }
        track = null
    }

    private fun playContinuousTone(frequency: Double) {
        stop()

        val samples = makeToneSamples(frequency, minimumDurationSeconds = 4.0)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.setLoopPoints(0, samples.size, -1)
        audioTrack.play()
        track = audioTrack
        continuousToneEnabled = true
        continuousToneFrequency = frequency
    }

    private fun makeSignalSamples(frequency: Double): ShortArray {
        val beepLength = (sampleRate * 0.09).toInt()
        val gapLength = (sampleRate * 0.07).toInt()
        val totalLength = (beepLength * 3) + (gapLength * 2)
        val samples = ShortArray(totalLength)

        var cursor = 0
        for (beepIndex in 0 until 3) {
            for (frame in 0 until beepLength) {
                val phase = 2.0 * PI * frequency * frame / sampleRate
                samples[cursor++] = (sin(phase) * 0.35 * Short.MAX_VALUE).toInt().toShort()
            }
            if (beepIndex < 2) {
                repeat(gapLength) { samples[cursor++] = 0 }
            }
        }

        return samples
    }

    private fun makeToneSamples(frequency: Double, minimumDurationSeconds: Double): ShortArray {
        val totalLength = (sampleRate * minimumDurationSeconds).toInt()
        val samples = ShortArray(totalLength)

        for (frame in samples.indices) {
            val phase = 2.0 * PI * frequency * frame / sampleRate
            samples[frame] = (sin(phase) * 0.35 * Short.MAX_VALUE).toInt().toShort()
        }

        return samples
    }
}
