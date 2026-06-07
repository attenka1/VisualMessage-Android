package fi.attenka.VisualMessage.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fi.attenka.VisualMessage.R

/**
 * Maps the iOS-style localization keys carried by the model enums/themes (e.g. "Visual",
 * "Black / white") to Android string resources, so a single key works on both platforms.
 */
@StringRes
fun resourceForKey(key: String): Int = when (key) {
    "Visual" -> R.string.mode_visual
    "Morse" -> R.string.mode_morse
    "Instant" -> R.string.transition_instant
    "Fade" -> R.string.transition_fade
    "Slide" -> R.string.transition_slide
    "Slide vertical" -> R.string.transition_slide_vertical
    "Scale" -> R.string.transition_scale
    "Static between text" -> R.string.slide_image_static_between_text
    "Slide with text" -> R.string.slide_image_slide_with_text
    "International" -> R.string.international
    "Continental" -> R.string.continental
    "Screen" -> R.string.morse_output_screen
    "Torch" -> R.string.morse_output_torch
    "Both" -> R.string.morse_output_both
    "Auto" -> R.string.slide_direction_auto
    "LTR" -> R.string.slide_direction_ltr
    "RTL" -> R.string.slide_direction_rtl
    "Default" -> R.string.font_default
    "Sans" -> R.string.font_sans
    "Serif" -> R.string.font_serif
    "Monospace" -> R.string.font_monospace
    "Regular" -> R.string.font_style_regular
    "Italic" -> R.string.font_style_italic
    "Bold" -> R.string.font_style_bold
    "Bold italic" -> R.string.font_style_bold_italic
    "Black / white" -> R.string.theme_black_white
    "White / black" -> R.string.theme_white_black
    "Black / orange" -> R.string.theme_black_orange
    "Orange / black" -> R.string.theme_orange_black
    "Yellow / black" -> R.string.theme_yellow_black
    "Custom" -> R.string.theme_custom
    else -> R.string.app_name
}

@Composable
fun localized(key: String): String = stringResource(resourceForKey(key))
