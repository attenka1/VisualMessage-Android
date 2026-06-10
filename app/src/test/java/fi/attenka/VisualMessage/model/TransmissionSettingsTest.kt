package fi.attenka.VisualMessage.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TransmissionSettingsTest {

    @Test
    fun defaultTransmissionSettingsMatchIos() {
        val settings = TransmissionSettings()

        assertEquals(2, settings.repeatCount)
        assertEquals(0.15, settings.characterDuration, 0.001)
        assertEquals(0.20, settings.characterGap, 0.001)
        assertEquals(TransitionStyle.SLIDE, settings.transitionStyle)
        assertEquals(2, settings.startDelaySeconds)
        assertEquals(1.0, settings.characterSizeScale, 0.001)
    }

    @Test
    fun defaultCharacterSizeScaleIsFullScreen() {
        assertEquals(1.0, TransmissionSettings().characterSizeScale, 0.001)
    }

    @Test
    fun playbackSingleCharacterFontSizeScalesWithCharacterSizeScale() {
        val full = TransmissionSettings(characterSizeScale = 1.0)
        val half = TransmissionSettings(characterSizeScale = 0.5)

        val fullSize = full.playbackSingleCharacterFontSizeSp(widthDp = 400f, heightDp = 800f)
        val halfSize = half.playbackSingleCharacterFontSizeSp(widthDp = 400f, heightDp = 800f)

        assertEquals(fullSize * 0.5f, halfSize, 0.001f)
    }

    @Test
    fun playbackSlideFontSizeScalesWithCharacterSizeScale() {
        val full = TransmissionSettings(characterSizeScale = 1.0)
        val quarter = TransmissionSettings(characterSizeScale = 0.25)

        val fullSize = full.playbackSlideFontSizeSp(sideDp = 400f, baseFraction = 0.62f)
        val quarterSize = quarter.playbackSlideFontSizeSp(sideDp = 400f, baseFraction = 0.62f)

        assertEquals(fullSize * 0.25f, quarterSize, 0.001f)
    }

    @Test
    fun playbackImageFillFractionScalesWithCharacterSizeScale() {
        val settings = TransmissionSettings(characterSizeScale = 0.5)
        assertEquals(0.44f, settings.playbackImageFillFraction(), 0.001f)
    }
}
