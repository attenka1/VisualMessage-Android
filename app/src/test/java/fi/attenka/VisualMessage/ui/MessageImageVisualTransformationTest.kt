package fi.attenka.VisualMessage.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import fi.attenka.VisualMessage.model.MessageImage
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageImageVisualTransformationTest {

    @Test
    fun insertsPlaceholderAtInsertionIndex() {
        val transformation = MessageImageVisualTransformation(
            images = listOf(MessageImage(id = "1", uri = "file:///a.png", insertionIndex = 1)),
            placeholderBackground = Color.Gray,
            placeholderForeground = Color.Black,
        )

        val transformed = transformation.filter(AnnotatedString("Hi")).text.text

        assertEquals("H\u25A1i", transformed)
    }

    @Test
    fun mapsCursorAroundPlaceholder() {
        val transformation = MessageImageVisualTransformation(
            images = listOf(MessageImage(id = "1", uri = "file:///a.png", insertionIndex = 1)),
            placeholderBackground = Color.Gray,
            placeholderForeground = Color.Black,
        )

        val mapping = transformation.filter(AnnotatedString("Hi")).offsetMapping

        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(2, mapping.originalToTransformed(1))
        assertEquals(3, mapping.originalToTransformed(2))
        assertEquals(1, mapping.transformedToOriginal(2))
    }
}
