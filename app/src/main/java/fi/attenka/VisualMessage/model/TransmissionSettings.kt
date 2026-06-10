package fi.attenka.VisualMessage.model

import androidx.compose.ui.graphics.Color

/**
 * All user-configurable transmission options. Immutable so Compose state updates are
 * done with [copy]; persistence is handled separately.
 */
data class TransmissionSettings(
    val message: String = "MEET ME HERE \uD83D\uDE42",
    val messageImages: List<MessageImage> = emptyList(),
    val textColorSpans: List<MessageTextColorSpan> = emptyList(),
    val mode: TransmissionMode = TransmissionMode.VISUAL,
    val themeID: String = ColorTheme.presets[1].id,
    val customForeground: Color = Color.White,
    val customBackground: Color = Color.Black,
    val multicolorLettersEnabled: Boolean = false,
    val customEditorColorsEnabled: Boolean = false,
    val slideDirection: SlideDirection = SlideDirection.AUTO,
    val slideImageBehavior: SlideImageBehavior = SlideImageBehavior.STATIC_BETWEEN_TEXT,
    val uppercaseEnabled: Boolean = false,
    val messageFontFamily: MessageFontFamily = MessageFontFamily.DEFAULT,
    val messageFontStyle: MessageFontStyle = MessageFontStyle.BOLD,
    val editorForeground: Color = DefaultEditorForeground,
    val editorBackground: Color = DefaultEditorBackground,
    val characterDuration: Double = 0.15,
    val emojiDuration: Double = 0.7,
    val characterGap: Double = 0.20,
    val repeatCount: Int = 2,
    val repeatForever: Boolean = false,
    val transitionStyle: TransitionStyle = TransitionStyle.SLIDE,
    val soundSignalEnabled: Boolean = true,
    val visualSignalEnabled: Boolean = true,
    val morseSoundEnabled: Boolean = false,
    val morseOutputMode: MorseOutputMode = MorseOutputMode.SCREEN,
    val signalFrequency: Double = 880.0,
    val startDelaySeconds: Int = 2,
    val morseUnitDuration: Double = 0.24, // 5 WPM (unit = 1.2 / WPM)
    val morseAlphabet: MorseAlphabet = MorseAlphabet.INTERNATIONAL,
    /** Playback text scale relative to the auto-fit maximum. 1.0 fills the screen. */
    val characterSizeScale: Double = 1.0,
) {
    val torchSignalEnabled: Boolean
        get() = morseOutputMode.usesTorch

    val activeTheme: ColorTheme
        get() {
            if (themeID == "custom") {
                return ColorTheme("custom", "Custom", customForeground, customBackground)
            }
            return ColorTheme.presets.firstOrNull { it.id == themeID } ?: ColorTheme.presets[1]
        }

    /**
     * Morse speed in words per minute using the PARIS standard (one word == 50 units),
     * so unit duration in seconds == 1.2 / wpm. All mark, character and word gaps scale
     * from [morseUnitDuration], keeping their relative lengths intact.
     */
    val morseWordsPerMinute: Double
        get() = 1.2 / morseUnitDuration

    fun withWordsPerMinute(wpm: Double): TransmissionSettings =
        copy(morseUnitDuration = 1.2 / wpm.coerceAtLeast(1.0))

    fun playbackSingleCharacterFontSizeSp(widthDp: Float, heightDp: Float): Float =
        minOf(widthDp * 0.98f, heightDp * 0.98f) * 1.05f * characterSizeScale.toFloat()

    fun playbackSlideFontSizeSp(sideDp: Float, baseFraction: Float): Float =
        sideDp * baseFraction * characterSizeScale.toFloat()

    fun playbackImageFillFraction(): Float = 0.88f * characterSizeScale.toFloat()

    companion object {
        // Sentinel values that mean "use the theme default" so they survive persistence.
        val DefaultEditorForeground = Color.Unspecified
        val DefaultEditorBackground = Color(0xFF2C2C2E)
    }
}
