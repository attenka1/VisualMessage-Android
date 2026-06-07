package fi.attenka.VisualMessage.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import fi.attenka.VisualMessage.model.MessageImage

/**
 * Inserts a visible square placeholder at each [MessageImage.insertionIndex] in the editor
 * without changing the stored message text.
 */
class MessageImageVisualTransformation(
    private val images: List<MessageImage>,
    private val placeholderBackground: Color,
    private val placeholderForeground: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val byIndex = images
            .groupBy { it.insertionIndex.coerceIn(0, text.length) }
            .toSortedMap()

        if (byIndex.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder()
        val transformedToOriginal = mutableListOf<Int>()
        var pos = 0

        for (index in 0..text.length) {
            if (index > pos) {
                val segment = text.subSequence(pos, index)
                builder.append(segment)
                for (charIndex in pos until index) {
                    transformedToOriginal.add(charIndex)
                }
            }

            val imagesAtIndex = byIndex[index].orEmpty()
            repeat(imagesAtIndex.size) {
                builder.pushStyle(
                    SpanStyle(
                        background = placeholderBackground,
                        color = placeholderForeground,
                    ),
                )
                builder.append(PLACEHOLDER)
                builder.pop()
                transformedToOriginal.add(index)
            }

            pos = index
        }

        if (pos < text.length) {
            val segment = text.subSequence(pos, text.length)
            builder.append(segment)
            for (charIndex in pos until text.length) {
                transformedToOriginal.add(charIndex)
            }
        }
        transformedToOriginal.add(text.length)

        return TransformedText(
            builder.toAnnotatedString(),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    val safeOffset = offset.coerceIn(0, text.length)
                    var placeholdersBefore = 0
                    for ((index, list) in byIndex) {
                        if (index <= safeOffset) placeholdersBefore += list.size
                    }
                    return safeOffset + placeholdersBefore
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (transformedToOriginal.isEmpty()) return 0
                    val safeOffset = offset.coerceIn(0, transformedToOriginal.lastIndex)
                    return transformedToOriginal[safeOffset]
                }
            },
        )
    }

    override fun equals(other: Any?): Boolean =
        other is MessageImageVisualTransformation &&
            images == other.images &&
            placeholderBackground == other.placeholderBackground &&
            placeholderForeground == other.placeholderForeground

    override fun hashCode(): Int =
        images.hashCode() * 31 + placeholderBackground.hashCode()

    private companion object {
        private const val PLACEHOLDER = "\u25A1"
    }
}
