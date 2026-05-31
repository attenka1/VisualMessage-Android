package fi.attenka.VisualMessage.receive

import org.junit.Assert.assertEquals
import org.junit.Test

class MorseReceiverTest {

    private data class Segment(val on: Boolean, val durationMs: Long)

    /** Drives the receiver with a synthetic light timeline sampled at ~30 fps. */
    private fun decode(segments: List<Segment>, sampleIntervalMs: Long = 33): String {
        var last = MorseReceiverState()
        val receiver = MorseReceiver { last = it }

        var clock = 0L
        for (segment in segments) {
            val end = clock + segment.durationMs
            while (clock < end) {
                receiver.onSample(if (segment.on) 255.0 else 0.0, clock)
                clock += sampleIntervalMs
            }
        }
        return last.text
    }

    private fun pattern(unit: Long): List<Segment> {
        val segments = mutableListOf<Segment>()
        // Leading dark period to establish the baseline.
        segments += Segment(on = false, durationMs = unit * 4)

        fun mark(units: Long) = segments.add(Segment(on = true, durationMs = unit * units))
        fun gap(units: Long) = segments.add(Segment(on = false, durationMs = unit * units))

        // S O S with standard intra (1u) gaps and 4u letter gaps.
        // S = ...
        mark(1); gap(1); mark(1); gap(1); mark(1)
        gap(4)
        // O = ---
        mark(3); gap(1); mark(3); gap(1); mark(3)
        gap(4)
        // S = ...
        mark(1); gap(1); mark(1); gap(1); mark(1)
        // Trailing dark period so the final letter flushes.
        gap(8)
        return segments
    }

    @Test
    fun decodesSos() {
        assertEquals("SOS", decode(pattern(unit = 300)).trim())
    }

    @Test
    fun decodesSosAtFasterSpeed() {
        assertEquals("SOS", decode(pattern(unit = 150)).trim())
    }
}
