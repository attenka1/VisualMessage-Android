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

    fun stop() {
        track?.run {
            runCatching {
                if (state == AudioTrack.STATE_INITIALIZED) stop()
                release()
            }
        }
        track = null
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
}
