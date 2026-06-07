package fi.attenka.VisualMessage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import fi.attenka.VisualMessage.R
import fi.attenka.VisualMessage.model.ColorTheme
import fi.attenka.VisualMessage.model.TransmissionSettings

@Composable
fun ColorSection(
    settings: TransmissionSettings,
    onUpdate: ((TransmissionSettings) -> TransmissionSettings) -> Unit,
) {
    GroupBox(title = stringResource(R.string.colors)) {
        PresetPicker(
            selectedId = settings.themeID,
            onSelect = { id -> onUpdate { it.copy(themeID = id) } },
        )

        if (settings.themeID == "custom") {
            ColorPickerField(
                label = stringResource(R.string.signal_color),
                color = settings.customForeground,
                onColorChange = { c -> onUpdate { it.copy(customForeground = c) } },
            )
            ColorPickerField(
                label = stringResource(R.string.background_color),
                color = settings.customBackground,
                onColorChange = { c -> onUpdate { it.copy(customBackground = c) } },
            )
        }

        ThemePreview(settings)

        ToggleRow(
            label = stringResource(R.string.multicolor_letters),
            checked = settings.multicolorLettersEnabled,
            onCheckedChange = { on -> onUpdate { it.copy(multicolorLettersEnabled = on) } },
        )

        HorizontalDivider()

        ToggleRow(
            label = stringResource(R.string.editor_colors),
            checked = settings.customEditorColorsEnabled,
            onCheckedChange = { on -> onUpdate { it.copy(customEditorColorsEnabled = on) } },
        )

        if (settings.customEditorColorsEnabled) {
            ColorPickerField(
                label = stringResource(R.string.text_color),
                color = settings.editorForeground.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSurface,
                onColorChange = { c -> onUpdate { it.copy(editorForeground = c) } },
            )
            ColorPickerField(
                label = stringResource(R.string.background_color),
                color = settings.editorBackground,
                onColorChange = { c -> onUpdate { it.copy(editorBackground = c) } },
            )
        }
    }
}

@Composable
private fun PresetPicker(selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = ColorTheme.presets.firstOrNull { it.id == selectedId } ?: ColorTheme.presets[1]

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.preset), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            ThemeSwatch(selected)
            Text(localized(selected.nameKey), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ColorTheme.presets.forEach { theme ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ThemeSwatch(theme)
                            Text(localized(theme.nameKey))
                        }
                    },
                    onClick = {
                        onSelect(theme.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatch(theme: ColorTheme) {
    Row(
        modifier = Modifier
            .size(width = 30.dp, height = 16.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
    ) {
        Box(Modifier.weight(1f).fillMaxHeight().background(theme.foreground))
        Box(Modifier.weight(1f).fillMaxHeight().background(theme.background))
    }
}

@Composable
private fun ThemePreview(settings: TransmissionSettings) {
    val theme = settings.activeTheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(theme.background)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "A",
            color = theme.foreground,
            fontSize = 54.sp,
            fontFamily = settings.messageFontFamily.composeFontFamily(),
            fontStyle = settings.messageFontStyle.composeFontStyle(),
            fontWeight = settings.messageFontStyle.composeFontWeight(),
        )
    }
}
