package fi.attenka.VisualMessage.receive

import fi.attenka.VisualMessage.model.MorseCode

/** Snapshot of the receiver, exposed to the UI. */
data class MorseReceiverState(
    val text: String = "",
    val partial: String = "",
    val lightOn: Boolean = false,
    val level: Float = 0f,
)

/**
 * Converts a stream of luminance samples into decoded morse text.
 *
 * Pipeline: adaptive threshold + hysteresis -> debounced on/off transitions ->
 * duration classification (dot/dash, letter/word gaps) -> reverse morse lookup.
 *
 * Pure Kotlin (no Android dependencies) so it can be unit-tested with synthetic samples.
 * The dot length is auto-calibrated from the shortest marks, so it adapts to the sender's
 * speed without prior knowledge.
 */
class MorseReceiver(private val onResult: (MorseReceiverState) -> Unit) {

    private val reverse = MorseCode.reverseInternational

    private var minLevel = 0.0
    private var maxLevel = 0.0
    private var initialized = false

    private var lightOn = false
    private var pendingOn = false
    private var pendingSinceMs = 0L
    private var segmentStartMs = 0L

    private var dotMs = 250.0
    private val symbol = StringBuilder()
    private val text = StringBuilder()
    private var letterFlushed = true
    private var spaceAdded = true

    fun onSample(level: Double, tsMs: Long) {
        if (!initialized) {
            minLevel = level
            maxLevel = level
            segmentStartMs = tsMs
            initialized = true
        }

        // Fast attack, slow release so the bounds track the current lighting.
        maxLevel = if (level > maxLevel) level else maxLevel * 0.99 + level * 0.01
        minLevel = if (level < minLevel) level else minLevel * 0.99 + level * 0.01
        val range = maxLevel - minLevel

        val rawOn = when {
            range < NOISE_FLOOR -> false
            lightOn -> level >= minLevel + range * LOW_RATIO
            else -> level > minLevel + range * HIGH_RATIO
        }

        if (rawOn != lightOn) {
            if (rawOn != pendingOn) {
                pendingOn = rawOn
                pendingSinceMs = tsMs
            } else if (tsMs - pendingSinceMs >= DEBOUNCE_MS) {
                commitTransition(rawOn, tsMs)
            }
        } else {
            pendingOn = lightOn
        }

        if (!lightOn) {
            val off = tsMs - segmentStartMs
            if (!letterFlushed && symbol.isNotEmpty() && off > dotMs * LETTER_GAP_RATIO) {
                flushLetter()
            }
            if (!spaceAdded && off > dotMs * WORD_GAP_RATIO) {
                if (text.isNotEmpty() && !text.endsWith(" ")) text.append(' ')
                spaceAdded = true
            }
        }

        emit(level, range)
    }

    fun reset() {
        symbol.clear()
        text.clear()
        letterFlushed = true
        spaceAdded = true
        lightOn = false
        pendingOn = false
        initialized = false
        emit(0.0, 0.0)
    }

    private fun commitTransition(on: Boolean, tsMs: Long) {
        val duration = tsMs - segmentStartMs
        if (lightOn && !on) {
            classifyMark(duration)
        }
        lightOn = on
        segmentStartMs = tsMs
        if (on) {
            letterFlushed = false
            spaceAdded = false
        }
    }

    private fun classifyMark(durationMs: Long) {
        if (durationMs < dotMs * DOT_DASH_RATIO) {
            // Blend short marks into the dot-length estimate.
            dotMs = dotMs * 0.6 + durationMs * 0.4
            symbol.append('.')
        } else {
            symbol.append('-')
        }
    }

    private fun flushLetter() {
        if (symbol.isEmpty()) return
        text.append(reverse[symbol.toString()] ?: UNKNOWN)
        symbol.clear()
        letterFlushed = true
    }

    private fun emit(level: Double, range: Double) {
        val normalized = if (range > NOISE_FLOOR) {
            ((level - minLevel) / range).coerceIn(0.0, 1.0).toFloat()
        } else {
            0f
        }
        onResult(
            MorseReceiverState(
                text = text.toString(),
                partial = symbol.toString(),
                lightOn = lightOn,
                level = normalized,
            )
        )
    }

    companion object {
        private const val NOISE_FLOOR = 12.0
        private const val DEBOUNCE_MS = 45L
        private const val HIGH_RATIO = 0.6
        private const val LOW_RATIO = 0.4
        private const val DOT_DASH_RATIO = 2.0
        private const val LETTER_GAP_RATIO = 2.5
        private const val WORD_GAP_RATIO = 6.0
        private const val UNKNOWN = '\u00B7' // middle dot for an unrecognized code
    }
}
