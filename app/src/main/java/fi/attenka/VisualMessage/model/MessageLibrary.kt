package fi.attenka.VisualMessage.model

/** A user-managed collection of saved message templates. Immutable. */
data class MessageLibrary(val messages: List<SavedMessage>) {

    fun contains(message: String): Boolean {
        val trimmed = normalize(message)
        return messages.any { it.message == trimmed }
    }

    fun adding(
        message: String,
        textColorSpans: List<MessageTextColorSpan>,
        messageFontFamily: MessageFontFamily,
        messageFontStyle: MessageFontStyle,
    ): MessageLibrary {
        val trimmed = normalize(message)
        if (trimmed.isEmpty() || messages.any { it.message == trimmed }) return this
        return MessageLibrary(
            messages + SavedMessage(
                message = trimmed,
                textColorSpans = textColorSpans.clippedTo(trimmed.length),
                messageFontFamily = messageFontFamily,
                messageFontStyle = messageFontStyle,
            )
        )
    }

    fun removing(message: String): MessageLibrary {
        val trimmed = normalize(message)
        return MessageLibrary(messages.filter { it.message != trimmed })
    }

    companion object {
        /** Starter templates shown on first launch. The user can freely delete or add to these. */
        val starter = MessageLibrary(
            listOf(
                SavedMessage("MEET ME HERE \uD83D\uDE42"),
                SavedMessage("ON MY WAY"),
                SavedMessage("CALL ME"),
            )
        )

        private fun normalize(message: String): String = message.trim()
    }
}

data class SavedMessage(
    val message: String,
    val textColorSpans: List<MessageTextColorSpan> = emptyList(),
    val messageFontFamily: MessageFontFamily = MessageFontFamily.DEFAULT,
    val messageFontStyle: MessageFontStyle = MessageFontStyle.BOLD,
)

private fun List<MessageTextColorSpan>.clippedTo(messageLength: Int): List<MessageTextColorSpan> =
    mapNotNull { span ->
        val start = span.start.coerceIn(0, messageLength)
        val end = span.end.coerceIn(0, messageLength)
        if (start < end) span.copy(start = start, end = end) else null
    }
