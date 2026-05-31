package fi.attenka.VisualMessage.model

import androidx.compose.ui.graphics.Color

/** How a message is transmitted on screen. */
enum class TransmissionMode(val titleKey: String) {
    VISUAL("Visual"),
    MORSE("Morse");
}

/** Animation used between characters in visual mode. */
enum class TransitionStyle(val titleKey: String) {
    INSTANT("Instant"),
    FADE("Fade"),
    SLIDE("Slide"),
    SCALE("Scale");
}

/** Morse code variant. */
enum class MorseAlphabet(val titleKey: String) {
    INTERNATIONAL("International"),
    CONTINENTAL("Continental");

    val codes: Map<Char, String>
        get() = when (this) {
            INTERNATIONAL -> MorseCode.international
            CONTINENTAL -> MorseCode.continental
        }
}

/** A single thing shown on the playback surface for a given duration. */
sealed interface FrameKind {
    data class Character(val value: String) : FrameKind
    data object MorseSignal : FrameKind
    data object AppLogo : FrameKind
    data object Blank : FrameKind
}

data class TransmissionFrame(
    val kind: FrameKind,
    val durationSeconds: Double,
)

data class ColorTheme(
    val id: String,
    val nameKey: String,
    val foreground: Color,
    val background: Color,
) {
    companion object {
        // Apple-equivalent system colors so the Android port matches the iOS look.
        private val systemOrange = Color(0xFFFF9500)
        private val systemYellow = Color(0xFFFFCC00)

        val presets = listOf(
            ColorTheme("blackWhite", "Black / white", Color.Black, Color.White),
            ColorTheme("whiteBlack", "White / black", Color.White, Color.Black),
            ColorTheme("blackOrange", "Black / orange", Color.Black, systemOrange),
            ColorTheme("orangeBlack", "Orange / black", systemOrange, Color.Black),
            ColorTheme("yellowBlack", "Yellow / black", systemYellow, Color.Black),
            ColorTheme("custom", "Custom", Color.White, Color.Black),
        )
    }
}
