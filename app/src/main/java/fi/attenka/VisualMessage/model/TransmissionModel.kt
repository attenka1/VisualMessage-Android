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
    SLIDE_VERTICAL("Slide vertical"),
    SCALE("Scale");

    val isContinuousSlide: Boolean
        get() = this == SLIDE || this == SLIDE_VERTICAL
}

enum class SlideImageBehavior(val titleKey: String) {
    STATIC_BETWEEN_TEXT("Static between text"),
    SLIDE_WITH_TEXT("Slide with text");
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

/** Where Morse marks are emitted during playback. */
enum class MorseOutputMode(val titleKey: String) {
    SCREEN("Screen"),
    TORCH("Torch"),
    SCREEN_AND_TORCH("Both");

    val usesScreen: Boolean
        get() = this != TORCH

    val usesTorch: Boolean
        get() = this != SCREEN
}

/** Direction used for slide/text playback, independently of the app UI language. */
enum class SlideDirection(val titleKey: String, val isRightToLeft: Boolean?) {
    AUTO("Auto", null),
    LTR("LTR", false),
    RTL("RTL", true);
}

enum class MessageFontFamily(val titleKey: String) {
    DEFAULT("Default"),
    SANS("Sans"),
    SERIF("Serif"),
    MONOSPACE("Monospace");
}

enum class MessageFontStyle(val titleKey: String) {
    REGULAR("Regular"),
    ITALIC("Italic"),
    BOLD("Bold"),
    BOLD_ITALIC("Bold italic");
}

/** A single thing shown on the playback surface for a given duration. */
sealed interface FrameKind {
    data class Character(val value: String, val color: Color? = null) : FrameKind
    data class Whitespace(val value: String, val color: Color? = null) : FrameKind
    data class SlideMessage(val items: List<SlideItem>, val index: Int) : FrameKind
    data class Image(val uri: String) : FrameKind
    data class MorseSignal(val letter: String) : FrameKind
    data class MorseLetterGap(val letter: String) : FrameKind
    data object AppLogo : FrameKind
    data object Blank : FrameKind
}

sealed interface SlideItem {
    data class Text(val value: String, val color: Color? = null) : SlideItem
    data class Image(val uri: String, val durationSeconds: Double = 0.7) : SlideItem
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

data class MessageImage(
    val id: String,
    val uri: String,
    val insertionIndex: Int,
    val durationSeconds: Double = 0.7,
)

data class MessageTextColorSpan(
    val start: Int,
    val end: Int,
    val color: Color,
)
