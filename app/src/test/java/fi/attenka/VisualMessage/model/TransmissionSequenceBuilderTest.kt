package fi.attenka.VisualMessage.model

import androidx.compose.ui.graphics.Color
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
    fun visualModeShowsWhitespaceAsVisibleFrame() {
        val settings = TransmissionSettings(
            message = "A B",
            visualSignalEnabled = false,
            characterDuration = 0.75,
            characterGap = 0.0,
        )

        val frames = TransmissionSequenceBuilder.frames(settings)

        assertEquals(
            listOf(
                FrameKind.Character("A"),
                FrameKind.Blank,
                FrameKind.Whitespace(" "),
                FrameKind.Blank,
                FrameKind.Character("B"),
            ),
            frames.map { it.kind },
        )
        assertEquals(1.5, frames[2].durationSeconds, 0.000_001)
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
                FrameKind.SlideMessage(listOf(SlideItem.Text("A"), SlideItem.Text("B")), 0),
            ),
            frames.map { it.kind },
        )
        assertEquals(16.0, frames.single().durationSeconds, 0.000_001)
    }

    @Test
    fun manualTextColorIsAttachedToCharacterFrames() {
        val color = Color(0xFFFF3B30)
        val settings = TransmissionSettings(
            message = "AB",
            textColorSpans = listOf(MessageTextColorSpan(start = 0, end = 1, color = color)),
            visualSignalEnabled = false,
            characterGap = 0.0,
        )

        val frames = TransmissionSequenceBuilder.frames(settings)

        assertEquals(
            listOf(
                FrameKind.Character("A", color),
                FrameKind.Blank,
                FrameKind.Character("B"),
            ),
            frames.map { it.kind },
        )
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
                FrameKind.SlideMessage(
                    listOf(
                        SlideItem.Text("A"),
                        SlideItem.Text("B"),
                        SlideItem.Text(" "),
                        SlideItem.Text("C"),
                        SlideItem.Text("D"),
                    ),
                    0,
                ),
            ),
            frames.map { it.kind },
        )
        assertEquals(18.0, frames.single().durationSeconds, 0.000_001)
    }

    @Test
    fun morseFramesCarryDisplayedLetter() {
        val settings = TransmissionSettings(
            message = "A",
            mode = TransmissionMode.MORSE,
            morseUnitDuration = 0.2,
        )

        val frames = TransmissionSequenceBuilder.frames(settings)

        assertEquals(
            listOf(
                FrameKind.MorseSignal("a"),
                FrameKind.MorseLetterGap("a"),
                FrameKind.MorseSignal("a"),
                FrameKind.Blank,
            ),
            frames.map { it.kind },
        )
        assertEquals(0.2, frames[0].durationSeconds, 0.000_001)
        assertEquals(0.2, frames[1].durationSeconds, 0.000_001)
        assertEquals(0.6, frames[2].durationSeconds, 0.000_001)
    }

    @Test
    fun visualModeInsertsImageAtMessageIndex() {
        val settings = TransmissionSettings(
            message = "AB",
            messageImages = listOf(
                MessageImage(
                    id = "image-1",
                    uri = "file:///tmp/image.png",
                    insertionIndex = 1,
                    durationSeconds = 1.25,
                )
            ),
            visualSignalEnabled = false,
            characterDuration = 0.5,
            emojiDuration = 1.25,
            characterGap = 0.0,
        )

        val frames = TransmissionSequenceBuilder.frames(settings)

        assertEquals(
            listOf(
                FrameKind.Character("A"),
                FrameKind.Blank,
                FrameKind.Image("file:///tmp/image.png"),
                FrameKind.Character("B"),
            ),
            frames.map { it.kind },
        )
        assertEquals(1.25, frames[2].durationSeconds, 0.000_001)
    }

    @Test
    fun slideModeWithImageKeepsTextAsSlideFrames() {
        val settings = TransmissionSettings(
            message = "HELLO WORLD",
            messageImages = listOf(
                MessageImage(
                    id = "image-1",
                    uri = "file:///tmp/image.png",
                    insertionIndex = 6,
                    durationSeconds = 2.0,
                )
            ),
            visualSignalEnabled = false,
            characterDuration = 0.5,
            emojiDuration = 1.25,
            transitionStyle = TransitionStyle.SLIDE,
        )

        val frames = TransmissionSequenceBuilder.frames(settings)

        assertEquals(
            listOf(
                FrameKind.SlideMessage(
                    listOf(
                        SlideItem.Text("H"),
                        SlideItem.Text("E"),
                        SlideItem.Text("L"),
                        SlideItem.Text("L"),
                        SlideItem.Text("O"),
                        SlideItem.Text(" "),
                    ),
                    0,
                ),
                FrameKind.Image("file:///tmp/image.png"),
                FrameKind.SlideMessage(
                    listOf(
                        SlideItem.Text("W"),
                        SlideItem.Text("O"),
                        SlideItem.Text("R"),
                        SlideItem.Text("L"),
                        SlideItem.Text("D"),
                    ),
                    0,
                ),
            ),
            frames.map { it.kind },
        )
    }

    @Test
    fun slideWithTextImageBehaviorKeepsImageInSlideFrame() {
        val settings = TransmissionSettings(
            message = "HELLO WORLD",
            messageImages = listOf(
                MessageImage(
                    id = "image-1",
                    uri = "file:///tmp/image.png",
                    insertionIndex = 6,
                    durationSeconds = 2.0,
                )
            ),
            visualSignalEnabled = false,
            characterDuration = 0.5,
            emojiDuration = 1.25,
            transitionStyle = TransitionStyle.SLIDE,
            slideImageBehavior = SlideImageBehavior.SLIDE_WITH_TEXT,
        )

        val frames = TransmissionSequenceBuilder.frames(settings)

        assertEquals(
            listOf(
                FrameKind.SlideMessage(
                    listOf(
                        SlideItem.Text("H"),
                        SlideItem.Text("E"),
                        SlideItem.Text("L"),
                        SlideItem.Text("L"),
                        SlideItem.Text("O"),
                        SlideItem.Text(" "),
                        SlideItem.Image("file:///tmp/image.png", 2.0),
                        SlideItem.Text("W"),
                        SlideItem.Text("O"),
                        SlideItem.Text("R"),
                        SlideItem.Text("L"),
                        SlideItem.Text("D"),
                    ),
                    0,
                ),
            ),
            frames.map { it.kind },
        )
        assertEquals(23.0, frames.single().durationSeconds, 0.000_001)
    }

    @Test
    fun staticImageAfterFullMessageIsSeparateCenteredFrame() {
        val settings = TransmissionSettings(
            message = "HELLO",
            messageImages = listOf(MessageImage(id = "image-1", uri = "file:///tmp/image.png", insertionIndex = 5)),
            visualSignalEnabled = false,
            characterDuration = 0.5,
            transitionStyle = TransitionStyle.SLIDE,
            slideImageBehavior = SlideImageBehavior.STATIC_BETWEEN_TEXT,
        )

        val frames = TransmissionSequenceBuilder.frames(settings)

        assertEquals(
            listOf(
                FrameKind.SlideMessage(
                    listOf(
                        SlideItem.Text("H"),
                        SlideItem.Text("E"),
                        SlideItem.Text("L"),
                        SlideItem.Text("L"),
                        SlideItem.Text("O"),
                    ),
                    0,
                ),
                FrameKind.Image("file:///tmp/image.png"),
            ),
            frames.map { it.kind },
        )
    }

    @Test
    fun slideWithTextImageBehaviorPreservesSpacesBeforeImage() {
        val settings = TransmissionSettings(
            message = "A   ",
            messageImages = listOf(MessageImage(id = "image-1", uri = "file:///tmp/image.png", insertionIndex = 4)),
            visualSignalEnabled = false,
            characterDuration = 0.5,
            transitionStyle = TransitionStyle.SLIDE,
            slideImageBehavior = SlideImageBehavior.SLIDE_WITH_TEXT,
        )

        val frames = TransmissionSequenceBuilder.frames(settings)

        assertEquals(
            listOf(
                FrameKind.SlideMessage(
                    listOf(
                        SlideItem.Text("A"),
                        SlideItem.Text(" "),
                        SlideItem.Text(" "),
                        SlideItem.Text(" "),
                        SlideItem.Image("file:///tmp/image.png"),
                    ),
                    0,
                ),
            ),
            frames.map { it.kind },
        )
    }

    @Test
    fun repeatForeverWithStaticBetweenTextKeepsSeparateImageFrame() {
        val settings = TransmissionSettings(
            message = "HELLO",
            messageImages = listOf(MessageImage(id = "image-1", uri = "file:///tmp/image.png", insertionIndex = 3)),
            visualSignalEnabled = false,
            characterDuration = 0.5,
            transitionStyle = TransitionStyle.SLIDE,
            slideImageBehavior = SlideImageBehavior.STATIC_BETWEEN_TEXT,
            repeatForever = true,
        )

        val frames = TransmissionSequenceBuilder.frames(settings.copy(repeatCount = 1))

        assertEquals(
            listOf(
                FrameKind.SlideMessage(
                    listOf(
                        SlideItem.Text("H"),
                        SlideItem.Text("E"),
                        SlideItem.Text("L"),
                    ),
                    0,
                ),
                FrameKind.Image("file:///tmp/image.png"),
                FrameKind.SlideMessage(
                    listOf(
                        SlideItem.Text("L"),
                        SlideItem.Text("O"),
                    ),
                    0,
                ),
            ),
            frames.map { it.kind },
        )
    }

    @Test
    fun repeatForeverDoesNotExpandFrameListInBuilder() {
        val finite = TransmissionSettings(
            message = "AB",
            visualSignalEnabled = false,
            repeatCount = 1,
            repeatForever = false,
        )
        val infinite = finite.copy(repeatForever = true)

        assertEquals(
            TransmissionSequenceBuilder.frames(finite).map { it.kind },
            TransmissionSequenceBuilder.frames(infinite).map { it.kind },
        )
    }
}
