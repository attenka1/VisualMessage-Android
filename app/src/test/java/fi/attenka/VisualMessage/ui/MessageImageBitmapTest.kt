package fi.attenka.VisualMessage.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageImageBitmapTest {
    @Test
    fun keepsImageAtFullSizeWhenItAlreadyFits() {
        assertEquals(1, calculateInSampleSize(1080, 1920, 1080, 1920))
    }

    @Test
    fun samplesLargeImageToNearDisplaySize() {
        assertEquals(2, calculateInSampleSize(4000, 3000, 1080, 1920))
        assertEquals(4, calculateInSampleSize(8000, 6000, 1080, 1920))
    }

    @Test
    fun handlesMissingOrInvalidDimensions() {
        assertEquals(1, calculateInSampleSize(0, 0, 1080, 1920))
        assertEquals(1, calculateInSampleSize(4000, 3000, 0, 0))
    }
}
