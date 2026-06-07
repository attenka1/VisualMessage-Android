package fi.attenka.VisualMessage.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import fi.attenka.VisualMessage.model.MessageFontFamily
import fi.attenka.VisualMessage.model.MessageFontStyle

fun MessageFontFamily.composeFontFamily(): FontFamily = when (this) {
    MessageFontFamily.DEFAULT -> FontFamily.Default
    MessageFontFamily.SANS -> FontFamily.SansSerif
    MessageFontFamily.SERIF -> FontFamily.Serif
    MessageFontFamily.MONOSPACE -> FontFamily.Monospace
}

fun MessageFontStyle.composeFontStyle(): FontStyle = when (this) {
    MessageFontStyle.ITALIC,
    MessageFontStyle.BOLD_ITALIC,
    -> FontStyle.Italic
    MessageFontStyle.REGULAR,
    MessageFontStyle.BOLD,
    -> FontStyle.Normal
}

fun MessageFontStyle.composeFontWeight(): FontWeight = when (this) {
    MessageFontStyle.BOLD,
    MessageFontStyle.BOLD_ITALIC,
    -> FontWeight.Black
    MessageFontStyle.REGULAR,
    MessageFontStyle.ITALIC,
    -> FontWeight.Normal
}

fun MessageFontStyle.composeEditorFontWeight(): FontWeight = when (this) {
    MessageFontStyle.BOLD,
    MessageFontStyle.BOLD_ITALIC,
    -> FontWeight.Bold
    MessageFontStyle.REGULAR,
    MessageFontStyle.ITALIC,
    -> FontWeight.Medium
}
