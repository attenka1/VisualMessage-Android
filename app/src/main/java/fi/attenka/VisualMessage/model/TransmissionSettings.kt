package fi.attenka.VisualMessage.model

import androidx.compose.ui.graphics.Color

/**
 * All user-configurable transmission options. Immutable so Compose state updates are
 * done with [copy]; persistence is handled separately.
 */
data class TransmissionSettings(
    val message: String = "MEET ME HERE \uD83D\uDE42",
    val mode: TransmissionMode = TransmissionMode.VISUAL,
    val themeID: String = ColorTheme.presets[1].id,
    val customForeground: Color = Color.White,
    val customBackground: Color = Color.Black,
    val customEditorColorsEnabled: Boolean = false,
    val editorForeground: Color = DefaultEditorForeground,
    val editorBackground: Color = DefaultEditorBackground,
    val characterDuration: Double = 0.7,
    val characterGap: Double = 0.18,
    val repeatCount: Int = 1,
    val transitionStyle: TransitionStyle = TransitionStyle.FADE,
    val soundSignalEnabled: Boolean = true,
    val visualSignalEnabled: Boolean = true,
    val signalFrequency: Double = 880.0,
    val startDelaySeconds: Int = 3,
    val morseUnitDuration: Double = 0.3, // 4 WPM (unit = 1.2 / WPM)
    val morseAlphabet: MorseAlphabet = MorseAlphabet.INTERNATIONAL,
) {
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

    companion object {
        // Sentinel values that mean "use the theme default" so they survive persistence.
        val DefaultEditorForeground = Color.Unspecified
        val DefaultEditorBackground = Color(0xFF2C2C2E)
    }
}
