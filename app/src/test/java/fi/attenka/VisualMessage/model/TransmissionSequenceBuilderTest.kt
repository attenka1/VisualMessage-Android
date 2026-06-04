package fi.attenka.VisualMessage.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TransmissionSequenceBuilderTest {

    @Test
    fun visualModeKeepsEmojiAsSingleFrame() {
        val settings = TransmissionSettings(
            message = "A🙂B",
            visualSignalEnabled = false,
            characterDuration = 1.0,
            emojiDuration = 1.0,
            characterGap = 0.0,
        )

        val frames = TransmissionSequenceBuilder.frames(settings)

        assertEquals(
            listOf(
                FrameKind.Character("A"),
                FrameKind.Blank,
                FrameKind.Character("🙂"),
                FrameKind.Blank,
                FrameKind.Character("B"),
            ),
            frames.map { it.kind },
        )
    }

    @Test
    fun emojiUsesEmojiDuration() {
        val settings = TransmissionSettings(
            message = "A🙂",
            visualSignalEnabled = false,
            characterDuration = 0.5,
            emojiDuration = 2.0,
            characterGap = 0.0,
        )

        val frames = TransmissionSequenceBuilder.frames(settings)

        assertEquals(0.5, frames[0].durationSeconds, 0.000_001)
        assertEquals(2.0, frames[2].durationSeconds, 0.000_001)
    }

    @Test
    fun slideModeUsesSingleMessageFrame() {
        val settings = TransmissionSettings(
            message = "AB",
            visualSignalEnabled = false,
            characterDuration = 0.5,
            transitionStyle = TransitionStyle.SLIDE,
        )

        val frames = TransmissionSequenceBuilder.frames(settings)

        assertEquals(
            listOf(
                FrameKind.SlideMessage(listOf("A", "B"), 0),
            ),
            frames.map { it.kind },
        )
        assertEquals(16.0, frames.single().durationSeconds, 0.000_001)
    }

    @Test
    fun verticalSlideModeUsesSingleMessageFrame() {
        val settings = TransmissionSettings(
            message = "AB CD",
            visualSignalEnabled = false,
            characterDuration = 0.5,
            characterGap = 0.0,
            transitionStyle = TransitionStyle.SLIDE_VERTICAL,
        )

        val frames = TransmissionSequenceBuilder.frames(settings)

        assertEquals(
            listOf(
                FrameKind.SlideMessage(listOf("A", "B", " ", "C", "D"), 0),
            ),
            frames.map { it.kind },
        )
        assertEquals(18.0, frames.single().durationSeconds, 0.000_001)
    }
}
