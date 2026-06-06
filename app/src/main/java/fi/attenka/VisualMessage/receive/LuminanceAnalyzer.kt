package fi.attenka.VisualMessage.receive

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.ceil
import kotlin.math.max

/**
 * Averages the luminance (Y plane) of a centre region of each camera frame and forwards it,
 * with the frame timestamp, to a sample consumer. Subsamples the region for performance.
 */
class LuminanceAnalyzer(
    private val onSample: (level: Double, timestampMs: Long) -> Unit,
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            val width = image.width
            val height = image.height
            val roiW = (width * ROI_FRACTION).toInt()
            val roiH = (height * ROI_FRACTION).toInt()
            val startX = (width - roiW) / 2
            val startY = (height - roiH) / 2

            val histogram = IntArray(LUMINANCE_BUCKETS)
            var count = 0
            var y = startY
            while (y < startY + roiH) {
                val rowBase = y * rowStride
                var x = startX
                while (x < startX + roiW) {
                    val index = rowBase + x * pixelStride
                    if (index in 0 until buffer.limit()) {
                        histogram[buffer.get(index).toInt() and 0xFF]++
                        count++
                    }
                    x += STEP
                }
                y += STEP
            }

            val level = if (count > 0) brightestAverage(histogram, count) else 0.0
            onSample(level, image.imageInfo.timestamp / 1_000_000L)
        } finally {
            image.close()
        }
    }

    private fun brightestAverage(histogram: IntArray, sampleCount: Int): Double {
        var remaining = max(MIN_BRIGHT_SAMPLES, ceil(sampleCount * BRIGHT_FRACTION).toInt())
            .coerceAtMost(sampleCount)
        var sum = 0L
        var used = 0
        for (level in histogram.indices.reversed()) {
            val take = minOf(histogram[level], remaining)
            if (take > 0) {
                sum += level.toLong() * take
                used += take
                remaining -= take
                if (remaining == 0) break
            }
        }
        return if (used > 0) sum.toDouble() / used else 0.0
    }

    companion object {
        /** Fraction of the frame width/height analysed (and shown as the aim box). */
        const val ROI_FRACTION = 0.4f
        private const val LUMINANCE_BUCKETS = 256
        private const val BRIGHT_FRACTION = 0.01
        private const val MIN_BRIGHT_SAMPLES = 8
        private val STEP = max(1, 4)
    }
}
