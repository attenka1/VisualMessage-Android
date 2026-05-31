package fi.attenka.VisualMessage.model

/** A user-managed collection of saved message templates. Immutable. */
data class MessageLibrary(val messages: List<String>) {

    fun contains(message: String): Boolean = messages.contains(normalize(message))

    fun adding(message: String): MessageLibrary {
        val trimmed = normalize(message)
        if (trimmed.isEmpty() || messages.contains(trimmed)) return this
        return MessageLibrary(messages + trimmed)
    }

    fun removing(message: String): MessageLibrary {
        val trimmed = normalize(message)
        return MessageLibrary(messages.filter { it != trimmed })
    }

    companion object {
        /** Starter templates shown on first launch. The user can freely delete or add to these. */
        val starter = MessageLibrary(listOf("MEET ME HERE \uD83D\uDE42", "ON MY WAY", "CALL ME"))

        private fun normalize(message: String): String = message.trim()
    }
}
