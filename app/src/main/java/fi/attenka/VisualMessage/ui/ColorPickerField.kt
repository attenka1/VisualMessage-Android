package fi.attenka.VisualMessage.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** A labelled color well that mirrors the compact SwiftUI ColorPicker rows in the iOS app. */
@Composable
fun ColorPickerField(
    label: String,
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Spacer(
            Modifier
                .size(36.dp, 24.dp)
                .background(color, RoundedCornerShape(6.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
        )
    }

    if (showDialog) {
        ColorPickerDialog(
            initial = color,
            onDismiss = { showDialog = false },
            onConfirm = {
                onColorChange(it)
                showDialog = false
            },
        )
    }
}

@Composable
private fun ColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val initialHsv = remember(initial) { initial.toHsv() }
    var hue by remember(initial) { mutableStateOf(initialHsv[0]) }
    var saturation by remember(initial) { mutableStateOf(initialHsv[1]) }
    var value by remember(initial) { mutableStateOf(initialHsv[2]) }
    val preview = Color.hsv(hue, saturation, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(preview) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            androidx.compose.foundation.layout.Column {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .size(width = 0.dp, height = 48.dp)
                        .background(preview, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SaturationValuePlane(
                        hue = hue,
                        saturation = saturation,
                        value = value,
                        onChange = { nextSaturation, nextValue ->
                            saturation = nextSaturation
                            value = nextValue
                        },
                        modifier = Modifier.weight(1f),
                    )
                    HueBar(
                        hue = hue,
                        onChange = { hue = it },
                    )
                }
            }
        },
    )
}

@Composable
private fun SaturationValuePlane(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val updateColor = { position: Offset ->
        if (size.width > 0 && size.height > 0) {
            onChange(
                (position.x / size.width).coerceIn(0f, 1f),
                (1f - position.y / size.height).coerceIn(0f, 1f),
            )
        }
    }

    Canvas(
        modifier = modifier
            .height(220.dp)
            .onSizeChanged { size = it }
            .colorPickerPointerInput(updateColor),
    ) {
        val hueColor = Color.hsv(hue, 1f, 1f)
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        val marker = Offset(saturation * size.width, (1f - value) * size.height)
        drawCircle(Color.White, radius = 8.dp.toPx(), center = marker, style = Stroke(2.dp.toPx()))
        drawCircle(Color.Black.copy(alpha = 0.55f), radius = 10.dp.toPx(), center = marker, style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun HueBar(
    hue: Float,
    onChange: (Float) -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val updateHue = { position: Offset ->
        if (size.height > 0) {
            onChange(((position.y / size.height).coerceIn(0f, 1f) * 360f).coerceIn(0f, 360f))
        }
    }

    Canvas(
        modifier = Modifier
            .width(36.dp)
            .height(220.dp)
            .onSizeChanged { size = it }
            .colorPickerPointerInput(updateHue),
    ) {
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color.hsv(0f, 1f, 1f),
                    Color.hsv(60f, 1f, 1f),
                    Color.hsv(120f, 1f, 1f),
                    Color.hsv(180f, 1f, 1f),
                    Color.hsv(240f, 1f, 1f),
                    Color.hsv(300f, 1f, 1f),
                    Color.hsv(360f, 1f, 1f),
                ),
            ),
            size = Size(size.width.toFloat(), size.height.toFloat()),
        )
        val y = (hue / 360f).coerceIn(0f, 1f) * size.height
        drawLine(
            color = Color.White,
            start = Offset(0f, y),
            end = Offset(size.width.toFloat(), y),
            strokeWidth = 3.dp.toPx(),
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.55f),
            start = Offset(0f, y),
            end = Offset(size.width.toFloat(), y),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

private fun Modifier.colorPickerPointerInput(onPosition: (Offset) -> Unit): Modifier =
    pointerInput(onPosition) {
        detectTapGestures { onPosition(it) }
    }.pointerInput(onPosition) {
        detectDragGestures(
            onDragStart = { onPosition(it) },
            onDrag = { change, _ ->
                if (change.positionChange() != Offset.Zero) {
                    onPosition(change.position)
                }
            },
        )
    }

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    return hsv
}
