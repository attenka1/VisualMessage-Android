package fi.attenka.VisualMessage.receive

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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

            var sum = 0L
            var count = 0
            var y = startY
            while (y < startY + roiH) {
                val rowBase = y * rowStride
                var x = startX
                while (x < startX + roiW) {
                    val index = rowBase + x * pixelStride
                    if (index in 0 until buffer.limit()) {
                        sum += (buffer.get(index).toInt() and 0xFF)
                        count++
                    }
                    x += STEP
                }
                y += STEP
            }

            val level = if (count > 0) sum.toDouble() / count else 0.0
            onSample(level, image.imageInfo.timestamp / 1_000_000L)
        } finally {
            image.close()
        }
    }

    companion object {
        /** Fraction of the frame width/height analysed (and shown as the aim box). */
        const val ROI_FRACTION = 0.4f
        private val STEP = max(1, 4)
    }
}
