package fi.attenka.VisualMessage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import fi.attenka.VisualMessage.R
import fi.attenka.VisualMessage.model.MessageLibrary
import fi.attenka.VisualMessage.model.MorseAlphabet
import fi.attenka.VisualMessage.model.MorseOutputMode
import fi.attenka.VisualMessage.model.SlideDirection
import fi.attenka.VisualMessage.model.TransitionStyle
import fi.attenka.VisualMessage.model.TransmissionMode
import fi.attenka.VisualMessage.model.TransmissionSettings
import kotlin.math.roundToInt

@Composable
fun MessageSection(
    settings: TransmissionSettings,
    library: MessageLibrary,
    onUpdate: ((TransmissionSettings) -> TransmissionSettings) -> Unit,
    onLibraryChange: (MessageLibrary) -> Unit,
) {
    GroupBox(
        titleContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.message), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                MessageTemplatesControls(
                    library = library,
                    message = settings.message,
                    onSelectMessage = { selected ->
                        onUpdate { it.copy(message = selected.withUppercaseMode(it.uppercaseEnabled)) }
                    },
                    onLibraryChange = onLibraryChange,
                )
            }
        },
    ) {
        val editorBackground = if (settings.customEditorColorsEnabled) {
            settings.editorBackground
        } else {
            MaterialTheme.colorScheme.surface
        }
        val editorForeground = when {
            settings.customEditorColorsEnabled && settings.editorForeground.isSpecified -> settings.editorForeground
            else -> MaterialTheme.colorScheme.onSurface
        }

        val editorDirection = settings.slideDirection.layoutDirectionOrNull() ?: LocalLayoutDirection.current
        CompositionLocalProvider(LocalLayoutDirection provides editorDirection) {
            BasicTextField(
                value = settings.message,
                onValueChange = { value ->
                    onUpdate { it.copy(message = value.withUppercaseMode(it.uppercaseEnabled)) }
                },
                textStyle = TextStyle(color = editorForeground, fontSize = 22.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(editorForeground),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .background(editorBackground, RoundedCornerShape(8.dp))
                    .padding(12.dp),
            )
        }

        ToggleRow(
            label = stringResource(R.string.uppercase_mode),
            checked = settings.uppercaseEnabled,
            onCheckedChange = { enabled ->
                onUpdate { it.copy(uppercaseEnabled = enabled, message = it.message.withUppercaseMode(enabled)) }
            },
        )

        Text(stringResource(R.string.slide_direction), style = MaterialTheme.typography.labelLarge)
        EnumSegmented(
            options = SlideDirection.entries,
            selected = settings.slideDirection,
            labelKey = { it.titleKey },
            onSelect = { direction -> onUpdate { it.copy(slideDirection = direction) } },
        )
    }
}

private fun String.withUppercaseMode(enabled: Boolean): String =
    if (enabled) uppercase() else this

private fun SlideDirection.layoutDirectionOrNull(): LayoutDirection? =
    when (isRightToLeft) {
        true -> LayoutDirection.Rtl
        false -> LayoutDirection.Ltr
        null -> null
    }

@Composable
fun ModeSection(
    settings: TransmissionSettings,
    onUpdate: ((TransmissionSettings) -> TransmissionSettings) -> Unit,
) {
    GroupBox(title = stringResource(R.string.mode)) {
        EnumSegmented(
            options = TransmissionMode.entries,
            selected = settings.mode,
            labelKey = { it.titleKey },
            onSelect = { mode -> onUpdate { it.copy(mode = mode) } },
        )

        if (settings.mode == TransmissionMode.VISUAL) {
            EnumSegmented(
                options = TransitionStyle.entries,
                selected = settings.transitionStyle,
                labelKey = { it.titleKey },
                onSelect = { style -> onUpdate { it.copy(transitionStyle = style) } },
            )
        }
    }
}

@Composable
fun MorseSection(
    settings: TransmissionSettings,
    onUpdate: ((TransmissionSettings) -> TransmissionSettings) -> Unit,
) {
    GroupBox(title = stringResource(R.string.morse_settings)) {
        EnumSegmented(
            options = MorseAlphabet.entries,
            selected = settings.morseAlphabet,
            labelKey = { it.titleKey },
            onSelect = { alphabet -> onUpdate { it.copy(morseAlphabet = alphabet) } },
        )

        LabeledSlider(
            title = stringResource(R.string.speed),
            value = settings.morseWordsPerMinute.toFloat(),
            valueRange = 2f..30f,
            valueText = stringResource(R.string.value_wpm, settings.morseWordsPerMinute.roundToInt()),
            onValueChange = { wpm -> onUpdate { it.withWordsPerMinute(wpm.toDouble()) } },
        )
    }
}

@Composable
fun RhythmSection(
    settings: TransmissionSettings,
    onUpdate: ((TransmissionSettings) -> TransmissionSettings) -> Unit,
) {
    GroupBox(title = stringResource(R.string.rhythm)) {
        LabeledSlider(
            title = stringResource(R.string.start_delay),
            value = settings.startDelaySeconds.toFloat(),
            valueRange = 1f..10f,
            valueText = stringResource(R.string.value_start_delay, settings.startDelaySeconds),
            onValueChange = { v -> onUpdate { it.copy(startDelaySeconds = v.roundToInt().coerceIn(1, 10)) } },
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.repeats), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalIconButton(
                    onClick = { onUpdate { it.copy(repeatCount = (it.repeatCount - 1).coerceAtLeast(1)) } },
                ) { Text("\u2212", fontSize = 20.sp) }
                Text("${settings.repeatCount}", style = MaterialTheme.typography.titleMedium)
                FilledTonalIconButton(
                    onClick = { onUpdate { it.copy(repeatCount = (it.repeatCount + 1).coerceAtMost(20)) } },
                ) { Text("+", fontSize = 20.sp) }
            }
        }

        if (settings.mode == TransmissionMode.VISUAL) {
            LabeledSlider(
                title = stringResource(R.string.character_duration),
                value = settings.characterDuration.toFloat(),
                valueRange = 0.15f..3.0f,
                valueText = stringResource(R.string.value_seconds, settings.characterDuration.toFloat()),
                onValueChange = { v -> onUpdate { it.copy(characterDuration = v.toDouble()) } },
            )
            LabeledSlider(
                title = stringResource(R.string.emoji_duration),
                value = settings.emojiDuration.toFloat(),
                valueRange = 0.15f..3.0f,
                valueText = stringResource(R.string.value_seconds, settings.emojiDuration.toFloat()),
                onValueChange = { v -> onUpdate { it.copy(emojiDuration = v.toDouble()) } },
            )
            LabeledSlider(
                title = stringResource(R.string.character_gap),
                value = settings.characterGap.toFloat(),
                valueRange = 0.0f..1.5f,
                valueText = stringResource(R.string.value_seconds, settings.characterGap.toFloat()),
                onValueChange = { v -> onUpdate { it.copy(characterGap = v.toDouble()) } },
            )
        }
    }
}

@Composable
fun SignalSection(
    settings: TransmissionSettings,
    onUpdate: ((TransmissionSettings) -> TransmissionSettings) -> Unit,
) {
    GroupBox(title = stringResource(R.string.attention_signal)) {
        ToggleRow(
            label = stringResource(R.string.sound),
            checked = settings.soundSignalEnabled,
            onCheckedChange = { on -> onUpdate { it.copy(soundSignalEnabled = on) } },
        )
        ToggleRow(
            label = stringResource(R.string.start_flash),
            checked = settings.visualSignalEnabled,
            onCheckedChange = { on -> onUpdate { it.copy(visualSignalEnabled = on) } },
        )
        if (settings.mode == TransmissionMode.MORSE) {
            ToggleRow(
                label = stringResource(R.string.morse_beep),
                checked = settings.morseSoundEnabled,
                onCheckedChange = { on -> onUpdate { it.copy(morseSoundEnabled = on) } },
            )
            Text(stringResource(R.string.morse_output), style = MaterialTheme.typography.labelLarge)
            EnumSegmented(
                options = MorseOutputMode.entries,
                selected = settings.morseOutputMode,
                labelKey = { it.titleKey },
                onSelect = { outputMode -> onUpdate { it.copy(morseOutputMode = outputMode) } },
            )
        }
        LabeledSlider(
            title = stringResource(R.string.frequency),
            value = settings.signalFrequency.toFloat(),
            valueRange = 220f..1760f,
            valueText = stringResource(R.string.value_hz, settings.signalFrequency.roundToInt()),
            onValueChange = { v -> onUpdate { it.copy(signalFrequency = v.toDouble()) } },
            enabled = settings.soundSignalEnabled,
        )
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
