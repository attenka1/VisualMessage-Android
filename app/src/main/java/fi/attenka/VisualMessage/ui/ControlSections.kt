package fi.attenka.VisualMessage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.attenka.VisualMessage.R
import fi.attenka.VisualMessage.model.MessageLibrary
import fi.attenka.VisualMessage.model.MorseAlphabet
import fi.attenka.VisualMessage.model.TransitionStyle
import fi.attenka.VisualMessage.model.TransmissionMode
import fi.attenka.VisualMessage.model.TransmissionSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
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
                    onSelectMessage = { selected -> onUpdate { it.copy(message = selected) } },
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

        BasicTextField(
            value = settings.message,
            onValueChange = { value -> onUpdate { it.copy(message = value) } },
            textStyle = TextStyle(color = editorForeground, fontSize = 22.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(editorForeground),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .background(editorBackground, RoundedCornerShape(8.dp))
                .padding(12.dp),
        )
    }
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
