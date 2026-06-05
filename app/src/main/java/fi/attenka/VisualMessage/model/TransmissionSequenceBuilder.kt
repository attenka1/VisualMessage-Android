package fi.attenka.VisualMessage.model

import java.text.BreakIterator

/**
 * Turns a [TransmissionSettings] into the ordered list of frames that the player shows.
 * Mirrors the iOS TransmissionSequenceBuilder exactly so timing is identical across platforms.
 */
object TransmissionSequenceBuilder {

    fun frames(settings: TransmissionSettings): List<TransmissionFrame> {
        val message = settings.message.trim()
        if (message.isEmpty()) return emptyList()

        val frames = mutableListOf<TransmissionFrame>()
        // Morse uses only the configurable start delay before transmission; no logo intro.
        if (settings.mode != TransmissionMode.MORSE) {
            frames += attentionFrames(settings)
        }

        val repeats = maxOf(1, settings.repeatCount)
        for (repeatIndex in 0 until repeats) {
            when (settings.mode) {
                TransmissionMode.VISUAL -> frames += visualFrames(message, settings)
                TransmissionMode.MORSE -> frames += morseFrames(message, settings)
            }

            if (repeatIndex < repeats - 1) {
                frames += TransmissionFrame(FrameKind.Blank, settings.characterDuration * 5)
            }
        }

        if (settings.mode == TransmissionMode.MORSE) {
            // Hold the screen black briefly after the last morse mark.
            frames += TransmissionFrame(FrameKind.Blank, settings.morseUnitDuration * 8)
        }

        return frames
    }

    private fun attentionFrames(settings: TransmissionSettings): List<TransmissionFrame> {
        if (!settings.visualSignalEnabled) return emptyList()

        // Show the logo once for 2 seconds (no flashing), then a short blank before the message.
        return listOf(
            TransmissionFrame(FrameKind.AppLogo, 2.0),
            TransmissionFrame(FrameKind.Blank, 0.35),
        )
    }

    private fun visualFrames(message: String, settings: TransmissionSettings): List<TransmissionFrame> {
        val frames = mutableListOf<TransmissionFrame>()
        // Iterate by grapheme clusters (matching Swift's Array(String)) so multi-code-unit
        // characters such as emoji stay whole instead of being split into broken surrogates.
        val characters = message.graphemeClusters()
        if (settings.transitionStyle.isContinuousSlide) {
            return slideFrames(characters, settings)
        }

        characters.forEachIndexed { index, character ->
            if (character.isBlank()) {
                frames += TransmissionFrame(FrameKind.Blank, settings.characterDuration * 2)
            } else {
                frames += TransmissionFrame(FrameKind.Character(character), characterDuration(character, settings))
            }

            if (index < characters.size - 1) {
                frames += TransmissionFrame(FrameKind.Blank, settings.characterGap)
            }
        }

        return frames
    }

    private fun slideFrames(characters: List<String>, settings: TransmissionSettings): List<TransmissionFrame> =
        listOf(
            TransmissionFrame(
                FrameKind.SlideMessage(characters, 0),
                characters.sumOf { character -> characterDuration(character, settings) } + slideLeadingPaddingDuration(settings),
            ),
        )

    private fun slideLeadingPaddingDuration(settings: TransmissionSettings): Double =
        settings.characterDuration * 30

    private fun characterDuration(character: String, settings: TransmissionSettings): Double =
        when {
            character.isBlank() -> settings.characterDuration * 2
            character.isEmojiGrapheme() -> settings.emojiDuration
            else -> settings.characterDuration
        }

    /** Splits a string into user-perceived characters (extended grapheme clusters). */
    private fun String.graphemeClusters(): List<String> {
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(this)
        val clusters = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            clusters += substring(start, end)
            start = end
            end = iterator.next()
        }
        return clusters
    }

    private fun String.isEmojiGrapheme(): Boolean {
        if (isBlank()) return false
        var index = 0
        while (index < length) {
            val codePoint = codePointAt(index)
            if (isEmojiCodePoint(codePoint)) return true
            index += Character.charCount(codePoint)
        }
        return false
    }

    private fun isEmojiCodePoint(codePoint: Int): Boolean = when (codePoint) {
        in 0x1F300..0x1FAFF,
        in 0x1F600..0x1F64F,
        in 0x2600..0x27BF,
        in 0x1F900..0x1F9FF,
        in 0x1F1E6..0x1F1FF,
        -> true
        else -> false
    }

    private fun morseFrames(message: String, settings: TransmissionSettings): List<TransmissionFrame> {
        val frames = mutableListOf<TransmissionFrame>()
        val words = message.uppercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val unit = settings.morseUnitDuration

        words.forEachIndexed { wordIndex, word ->
            val letters = word.toList()

            letters.forEachIndexed { letterIndex, letter ->
                val code = settings.morseAlphabet.codes[letter] ?: return@forEachIndexed
                val signals = code.toList()
                val displayedLetter = letter.lowercaseChar().toString()

                signals.forEachIndexed { signalIndex, signal ->
                    frames += TransmissionFrame(FrameKind.MorseSignal(displayedLetter), unit * (if (signal == '-') 3.0 else 1.0))

                    if (signalIndex < signals.size - 1) {
                        frames += TransmissionFrame(FrameKind.MorseLetterGap(displayedLetter), unit)
                    }
                }

                if (letterIndex < letters.size - 1) {
                    frames += TransmissionFrame(FrameKind.Blank, unit * 4)
                }
            }

            if (wordIndex < words.size - 1) {
                frames += TransmissionFrame(FrameKind.Blank, unit * 8)
            }
        }

        return frames
    }
}
