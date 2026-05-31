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
        frames += attentionFrames(settings)

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

        return frames
    }

    private fun attentionFrames(settings: TransmissionSettings): List<TransmissionFrame> {
        if (!settings.visualSignalEnabled) return emptyList()

        return listOf(
            TransmissionFrame(FrameKind.AppLogo, 0.16),
            TransmissionFrame(FrameKind.Blank, 0.08),
            TransmissionFrame(FrameKind.AppLogo, 0.16),
            TransmissionFrame(FrameKind.Blank, 0.08),
            TransmissionFrame(FrameKind.AppLogo, 0.16),
            TransmissionFrame(FrameKind.Blank, 0.35),
        )
    }

    private fun visualFrames(message: String, settings: TransmissionSettings): List<TransmissionFrame> {
        val frames = mutableListOf<TransmissionFrame>()
        // Iterate by grapheme clusters (matching Swift's Array(String)) so multi-code-unit
        // characters such as emoji stay whole instead of being split into broken surrogates.
        val characters = message.graphemeClusters()

        characters.forEachIndexed { index, character ->
            if (character.isBlank()) {
                frames += TransmissionFrame(FrameKind.Blank, settings.characterDuration * 2)
            } else {
                frames += TransmissionFrame(FrameKind.Character(character), settings.characterDuration)
            }

            if (index < characters.size - 1) {
                frames += TransmissionFrame(FrameKind.Blank, settings.characterGap)
            }
        }

        return frames
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

    private fun morseFrames(message: String, settings: TransmissionSettings): List<TransmissionFrame> {
        val frames = mutableListOf<TransmissionFrame>()
        val words = message.uppercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val unit = settings.morseUnitDuration

        words.forEachIndexed { wordIndex, word ->
            val letters = word.toList()

            letters.forEachIndexed { letterIndex, letter ->
                val code = settings.morseAlphabet.codes[letter] ?: return@forEachIndexed
                val signals = code.toList()

                signals.forEachIndexed { signalIndex, signal ->
                    frames += TransmissionFrame(FrameKind.MorseSignal, unit * (if (signal == '-') 3.0 else 1.0))

                    if (signalIndex < signals.size - 1) {
                        frames += TransmissionFrame(FrameKind.Blank, unit)
                    }
                }

                if (letterIndex < letters.size - 1) {
                    // One unit longer than the morse standard (3) to make gaps easier to read.
                    frames += TransmissionFrame(FrameKind.Blank, unit * 4)
                }
            }

            if (wordIndex < words.size - 1) {
                // One unit longer than the morse standard (7) to make word breaks clearer.
                frames += TransmissionFrame(FrameKind.Blank, unit * 8)
            }
        }

        return frames
    }
}
