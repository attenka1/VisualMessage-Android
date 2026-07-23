package fi.attenka.VisualMessage.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import fi.attenka.VisualMessage.R
import fi.attenka.VisualMessage.model.MessageFontFamily
import fi.attenka.VisualMessage.model.MessageFontStyle
import fi.attenka.VisualMessage.model.MessageImage
import fi.attenka.VisualMessage.model.MessageLibrary
import fi.attenka.VisualMessage.model.MessageTextColorSpan
import fi.attenka.VisualMessage.model.MorseAlphabet
import fi.attenka.VisualMessage.model.MorseOutputMode
import fi.attenka.VisualMessage.model.SlideDirection
import fi.attenka.VisualMessage.model.SlideImageBehavior
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
    onPickImage: (Int) -> Unit,
    onRemoveImage: (String) -> Unit,
) {
    var editorValue by remember {
        mutableStateOf(TextFieldValue(settings.message.withImagePlaceholders(settings.messageImages)))
    }
    var selectedTextColor by remember { mutableStateOf(Color(0xFFFF3B30)) }
    LaunchedEffect(settings.message, settings.messageImages) {
        val nextEditorText = settings.message.withImagePlaceholders(settings.messageImages)
        if (nextEditorText != editorValue.text) {
            editorValue = TextFieldValue(
                text = nextEditorText,
                selection = TextRange(nextEditorText.length),
            )
        }
    }

    GroupBox(
        titleContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.message), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(
                    enabled = editorValue.text.isNotEmpty(),
                    onClick = {
                        editorValue = TextFieldValue("")
                        onUpdate {
                            it.copy(
                                message = "",
                                messageImages = emptyList(),
                                textColorSpans = emptyList(),
                            )
                        }
                    },
                ) {
                    Text(stringResource(R.string.clear))
                }
                TextButton(onClick = { onPickImage(editorValue.selection.start.toMessageOffset(editorValue.text)) }) {
                    Text(stringResource(R.string.insert_image))
                }
                MessageTemplatesControls(
                    library = library,
                    message = settings.message,
                    textColorSpans = settings.textColorSpans,
                    messageFontFamily = settings.messageFontFamily,
                    messageFontStyle = settings.messageFontStyle,
                    onSelectMessage = { saved ->
                        onUpdate {
                            val selectedMessage = saved.message.withUppercaseMode(it.uppercaseEnabled)
                            it.copy(
                                message = selectedMessage,
                                messageImages = emptyList(),
                                textColorSpans = saved.textColorSpans.adjustTextColorSpans(
                                    oldText = saved.message,
                                    newText = selectedMessage,
                                ),
                                messageFontFamily = saved.messageFontFamily,
                                messageFontStyle = saved.messageFontStyle,
                            )
                        }
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
            val styledEditorValue = TextFieldValue(
                annotatedString = editorValue.text.annotatedEditorTextWithTextColors(settings.textColorSpans),
                selection = editorValue.selection,
                composition = editorValue.composition,
            )

            BasicTextField(
                value = styledEditorValue,
                onValueChange = { value ->
                    val update = value.text.toMessageEditorUpdate(
                        previousImages = settings.messageImages,
                        uppercaseEnabled = settings.uppercaseEnabled,
                    )
                    editorValue = TextFieldValue(
                        text = update.editorText,
                        selection = value.selection.coerceIn(update.editorText.length),
                        composition = value.composition?.coerceIn(update.editorText.length),
                    )
                    onUpdate {
                        it.copy(
                            message = update.message,
                            messageImages = update.images,
                            textColorSpans = it.textColorSpans.adjustTextColorSpans(
                                oldText = settings.message,
                                newText = update.message,
                            ),
                        )
                    }
                },
                textStyle = TextStyle(
                    color = editorForeground,
                    fontSize = 22.sp,
                    fontFamily = settings.messageFontFamily.composeFontFamily(),
                    fontStyle = settings.messageFontStyle.composeFontStyle(),
                    fontWeight = settings.messageFontStyle.composeEditorFontWeight(),
                ),
                cursorBrush = SolidColor(editorForeground),
                singleLine = false,
                maxLines = Int.MAX_VALUE,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 88.dp)
                    .background(editorBackground, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            )
        }

        val selectionStart = minOf(editorValue.selection.start, editorValue.selection.end)
            .toMessageOffset(editorValue.text)
        val selectionEnd = maxOf(editorValue.selection.start, editorValue.selection.end)
            .toMessageOffset(editorValue.text)
        val hasSelection = selectionStart < selectionEnd

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColorPickerField(
                label = stringResource(R.string.selected_text_color),
                color = selectedTextColor,
                onColorChange = { selectedTextColor = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    enabled = hasSelection,
                    onClick = {
                        onUpdate {
                            it.copy(
                                textColorSpans = it.textColorSpans.applyTextColor(
                                    start = selectionStart,
                                    end = selectionEnd,
                                    color = selectedTextColor,
                                ),
                            )
                        }
                    },
                ) {
                    Text(stringResource(R.string.apply_text_color))
                }
                TextButton(
                    enabled = hasSelection,
                    onClick = {
                        onUpdate {
                            it.copy(
                                textColorSpans = it.textColorSpans.removeTextColor(
                                    start = selectionStart,
                                    end = selectionEnd,
                                ),
                            )
                        }
                    },
                ) {
                    Text(stringResource(R.string.clear_text_color))
                }
            }
        }

        settings.messageImages
            .sortedBy { it.insertionIndex }
            .forEach { image ->
                MessageImageRow(
                    imageUri = image.uri,
                    insertionIndex = image.insertionIndex,
                    durationSeconds = image.durationSeconds,
                    onDurationChange = { duration ->
                        onUpdate {
                            it.copy(
                                messageImages = it.messageImages.map { existing ->
                                    if (existing.id == image.id) {
                                        existing.copy(durationSeconds = duration.coerceImageDuration())
                                    } else {
                                        existing
                                    }
                                },
                            )
                        }
                    },
                    onRemove = { onRemoveImage(image.id) },
                )
            }

        ToggleRow(
            label = stringResource(R.string.uppercase_mode),
            checked = settings.uppercaseEnabled,
            onCheckedChange = { enabled ->
                onUpdate { it.copy(uppercaseEnabled = enabled, message = it.message.withUppercaseMode(enabled)) }
            },
        )

        Text(stringResource(R.string.font), style = MaterialTheme.typography.labelLarge)
        EnumSegmented(
            options = MessageFontFamily.entries,
            selected = settings.messageFontFamily,
            labelKey = { it.titleKey },
            onSelect = { family -> onUpdate { it.copy(messageFontFamily = family) } },
        )

        Text(stringResource(R.string.font_style), style = MaterialTheme.typography.labelLarge)
        EnumSegmented(
            options = MessageFontStyle.entries,
            selected = settings.messageFontStyle,
            labelKey = { it.titleKey },
            onSelect = { style -> onUpdate { it.copy(messageFontStyle = style) } },
        )
    }
}

@Composable
private fun MessageImageRow(
    imageUri: String,
    insertionIndex: Int,
    durationSeconds: Double,
    onDurationChange: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap = androidx.compose.runtime.remember(imageUri) {
        loadMessageImageBitmap(context, imageUri)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.image))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.image), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.image_insert_position, insertionIndex),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilledTonalIconButton(
                onClick = { onDurationChange(durationSeconds - 0.1) },
                modifier = Modifier.size(36.dp),
            ) { Text("\u2212", fontSize = 18.sp) }
            Text(
                stringResource(R.string.value_seconds, durationSeconds.toFloat()),
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalIconButton(
                onClick = { onDurationChange(durationSeconds + 0.1) },
                modifier = Modifier.size(36.dp),
            ) { Text("+", fontSize = 18.sp) }
        }
        TextButton(onClick = onRemove) {
            Text(stringResource(R.string.remove))
        }
    }
}

private fun String.withUppercaseMode(enabled: Boolean): String =
    if (enabled) uppercase() else this

private fun TextRange.coerceIn(textLength: Int): TextRange =
    TextRange(
        start = start.coerceIn(0, textLength),
        end = end.coerceIn(0, textLength),
    )

private data class MessageEditorUpdate(
    val editorText: String,
    val message: String,
    val images: List<MessageImage>,
)

private fun String.withImagePlaceholders(images: List<MessageImage>): String {
    if (images.isEmpty()) return this
    val imagesByIndex = images
        .groupBy { it.insertionIndex.coerceIn(0, length) }
        .toSortedMap()
    return buildString {
        for (index in 0..this@withImagePlaceholders.length) {
            repeat(imagesByIndex[index].orEmpty().size) {
                append(ImagePlaceholder)
            }
            if (index < this@withImagePlaceholders.length) {
                append(this@withImagePlaceholders[index])
            }
        }
    }
}

private fun String.toMessageEditorUpdate(
    previousImages: List<MessageImage>,
    uppercaseEnabled: Boolean,
): MessageEditorUpdate {
    val previousImagesByOrder = previousImages.sortedWith(
        compareBy<MessageImage> { it.insertionIndex }.thenBy { it.id }
    )
    var imageIndex = 0
    val nextImages = mutableListOf<MessageImage>()
    val message = buildString {
        this@toMessageEditorUpdate.forEach { char ->
            if (char == ImagePlaceholder) {
                previousImagesByOrder.getOrNull(imageIndex)?.let { image ->
                    nextImages += image.copy(insertionIndex = length)
                }
                imageIndex += 1
            } else {
                append(char)
            }
        }
    }.withUppercaseMode(uppercaseEnabled)

    return MessageEditorUpdate(
        editorText = message.withImagePlaceholders(nextImages),
        message = message,
        images = nextImages,
    )
}

private fun Int.toMessageOffset(editorText: String): Int {
    val safeOffset = coerceIn(0, editorText.length)
    var messageOffset = 0
    for (index in 0 until safeOffset) {
        if (editorText[index] != ImagePlaceholder) {
            messageOffset += 1
        }
    }
    return messageOffset
}

private fun String.annotatedEditorTextWithTextColors(spans: List<MessageTextColorSpan>): AnnotatedString =
    buildAnnotatedString {
        var messageIndex = 0
        this@annotatedEditorTextWithTextColors.forEach { char ->
            val span = if (char == ImagePlaceholder) {
                null
            } else {
                spans.lastOrNull { messageIndex in it.start until it.end }
            }
            if (span != null) {
                pushStyle(SpanStyle(color = span.color))
                append(char)
                pop()
            } else {
                append(char)
            }
            if (char != ImagePlaceholder) {
                messageIndex += 1
            }
        }
    }

private const val ImagePlaceholder = '\u25A1'

private fun List<MessageImage>.adjustAnchors(
    oldText: String,
    newText: String,
): List<MessageImage> {
    if (oldText == newText) return map { it.copy(insertionIndex = it.insertionIndex.coerceIn(0, newText.length)) }

    var prefix = 0
    while (prefix < oldText.length && prefix < newText.length && oldText[prefix] == newText[prefix]) {
        prefix += 1
    }

    var suffix = 0
    while (
        suffix < oldText.length - prefix &&
        suffix < newText.length - prefix &&
        oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
    ) {
        suffix += 1
    }

    val removed = oldText.length - prefix - suffix
    val inserted = newText.length - prefix - suffix
    val delta = inserted - removed
    val replacedEnd = prefix + removed

    return map { image ->
        val nextIndex = when {
            image.insertionIndex < prefix -> image.insertionIndex
            image.insertionIndex >= replacedEnd -> image.insertionIndex + delta
            else -> prefix + inserted
        }.coerceIn(0, newText.length)
        image.copy(insertionIndex = nextIndex)
    }
}

private fun Double.coerceImageDuration(): Double =
    coerceIn(0.15, 10.0)

private fun List<MessageTextColorSpan>.adjustTextColorSpans(
    oldText: String,
    newText: String,
): List<MessageTextColorSpan> {
    if (oldText == newText) {
        return mapNotNull { span ->
            val start = span.start.coerceIn(0, newText.length)
            val end = span.end.coerceIn(0, newText.length)
            if (start < end) span.copy(start = start, end = end) else null
        }
    }

    var prefix = 0
    while (prefix < oldText.length && prefix < newText.length && oldText[prefix] == newText[prefix]) {
        prefix += 1
    }

    var suffix = 0
    while (
        suffix < oldText.length - prefix &&
        suffix < newText.length - prefix &&
        oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
    ) {
        suffix += 1
    }

    val removed = oldText.length - prefix - suffix
    val inserted = newText.length - prefix - suffix
    val delta = inserted - removed
    val replacedEnd = prefix + removed

    fun adjustIndex(index: Int): Int = when {
        index < prefix -> index
        index >= replacedEnd -> index + delta
        else -> prefix + inserted
    }.coerceIn(0, newText.length)

    return mapNotNull { span ->
        val start = adjustIndex(span.start)
        val end = adjustIndex(span.end)
        if (start < end) span.copy(start = start, end = end) else null
    }
}

private fun List<MessageTextColorSpan>.applyTextColor(
    start: Int,
    end: Int,
    color: Color,
): List<MessageTextColorSpan> =
    removeTextColor(start, end) + MessageTextColorSpan(start = start, end = end, color = color)

private fun List<MessageTextColorSpan>.removeTextColor(
    start: Int,
    end: Int,
): List<MessageTextColorSpan> =
    flatMap { span ->
        when {
            end <= span.start || start >= span.end -> listOf(span)
            start <= span.start && end >= span.end -> emptyList()
            start <= span.start -> listOf(span.copy(start = end.coerceAtMost(span.end)))
            end >= span.end -> listOf(span.copy(end = start.coerceAtLeast(span.start)))
            else -> listOf(
                span.copy(end = start),
                span.copy(start = end),
            )
        }
    }.filter { it.start < it.end }

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
            Text(stringResource(R.string.slide_direction), style = MaterialTheme.typography.labelLarge)
            EnumSegmented(
                options = SlideDirection.entries,
                selected = settings.slideDirection,
                labelKey = { it.titleKey },
                onSelect = { direction -> onUpdate { it.copy(slideDirection = direction) } },
            )
            if (settings.transitionStyle.isContinuousSlide) {
                Text(stringResource(R.string.slide_image_behavior), style = MaterialTheme.typography.labelLarge)
                EnumSegmented(
                    options = SlideImageBehavior.entries,
                    selected = settings.slideImageBehavior,
                    labelKey = { it.titleKey },
                    onSelect = { behavior -> onUpdate { it.copy(slideImageBehavior = behavior) } },
                )
            }
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
            valueRange = 0f..10f,
            valueText = stringResource(R.string.value_start_delay, settings.startDelaySeconds),
            onValueChange = { v -> onUpdate { it.copy(startDelaySeconds = v.roundToInt().coerceIn(0, 10)) } },
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.repeats), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(
                    onClick = { onUpdate { it.copy(repeatCount = (it.repeatCount - 1).coerceAtLeast(1)) } },
                    modifier = Modifier.size(40.dp),
                ) { Text("\u2212", fontSize = 20.sp) }
                Text("${settings.repeatCount}", style = MaterialTheme.typography.titleMedium)
                FilledTonalIconButton(
                    onClick = { onUpdate { it.copy(repeatCount = (it.repeatCount + 1).coerceAtMost(20)) } },
                    modifier = Modifier.size(40.dp),
                ) { Text("+", fontSize = 20.sp) }
            }
        }

        ToggleRow(
            label = stringResource(R.string.repeat_forever),
            checked = settings.repeatForever,
            onCheckedChange = { enabled -> onUpdate { it.copy(repeatForever = enabled) } },
        )

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
            LabeledSlider(
                title = stringResource(R.string.character_size),
                value = settings.characterSizeScale.toFloat(),
                valueRange = 0.25f..1.0f,
                valueText = stringResource(
                    R.string.value_percent,
                    (settings.characterSizeScale * 100).roundToInt(),
                ),
                onValueChange = { v ->
                    onUpdate { it.copy(characterSizeScale = v.toDouble().coerceIn(0.25, 1.0)) }
                },
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToggleRow(
                label = stringResource(R.string.sound),
                checked = settings.soundSignalEnabled,
                onCheckedChange = { on -> onUpdate { it.copy(soundSignalEnabled = on) } },
                modifier = Modifier.weight(1f),
            )
            ToggleRow(
                label = stringResource(R.string.start_flash),
                checked = settings.visualSignalEnabled,
                onCheckedChange = { on -> onUpdate { it.copy(visualSignalEnabled = on) } },
                modifier = Modifier.weight(1f),
            )
        }
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
fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
