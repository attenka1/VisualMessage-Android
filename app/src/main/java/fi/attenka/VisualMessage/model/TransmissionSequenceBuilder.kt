package fi.attenka.VisualMessage.model

import java.text.BreakIterator

/**
 * Turns a [TransmissionSettings] into the ordered list of frames that the player shows.
 * Mirrors the iOS TransmissionSequenceBuilder exactly so timing is identical across platforms.
 */
object TransmissionSequenceBuilder {

    fun frames(settings: TransmissionSettings): List<TransmissionFrame> {
        val visualMessage = settings.message
        val morseMessage = settings.message.trim()
        when (settings.mode) {
            TransmissionMode.VISUAL -> {
                if (visualMessage.isEmpty() && settings.messageImages.isEmpty()) return emptyList()
            }
            TransmissionMode.MORSE -> {
                if (morseMessage.isEmpty()) return emptyList()
            }
        }

        val frames = mutableListOf<TransmissionFrame>()
        // Morse uses only the configurable start delay before transmission; no logo intro.
        if (settings.mode != TransmissionMode.MORSE) {
            frames += attentionFrames(settings)
        }

        val repeats = maxOf(1, settings.repeatCount)
        for (repeatIndex in 0 until repeats) {
            when (settings.mode) {
                TransmissionMode.VISUAL -> frames += visualFrames(visualMessage, settings)
                TransmissionMode.MORSE -> frames += morseFrames(morseMessage, settings)
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
        val characters = message.graphemeClustersWithOffsets()
        if (settings.transitionStyle.isContinuousSlide) {
            return slideFramesWithImages(message, settings)
        }

        val imagesByIndex = settings.messageImages
            .filter { it.uri.isNotBlank() }
            .groupBy { it.insertionIndex.coerceIn(0, message.length) }
        frames += imageFrames(imagesByIndex[0], settings)
        characters.forEachIndexed { index, character ->
            if (character.text.isBlank()) {
                frames += TransmissionFrame(
                    FrameKind.Whitespace(character.text, settings.textColorAt(character.startIndex)),
                    settings.characterDuration * 2,
                )
            } else {
                frames += TransmissionFrame(
                    FrameKind.Character(character.text, settings.textColorAt(character.startIndex)),
                    characterDuration(character.text, settings),
                )
            }

            if (index < characters.size - 1) {
                frames += TransmissionFrame(FrameKind.Blank, settings.characterGap)
            }
            frames += imageFrames(imagesByIndex[character.endIndex], settings)
        }

        return frames
    }

    private fun imageFrames(images: List<MessageImage>?, settings: TransmissionSettings): List<TransmissionFrame> =
        images.orEmpty().map { image ->
            TransmissionFrame(FrameKind.Image(image.uri), image.durationSeconds)
        }

    private fun slideFramesWithImages(message: String, settings: TransmissionSettings): List<TransmissionFrame> {
        if (settings.transitionStyle.isContinuousSlide &&
            settings.slideImageBehavior == SlideImageBehavior.SLIDE_WITH_TEXT
        ) {
            return slideFrames(slideItems(message, settings), settings)
        }

        val frames = mutableListOf<TransmissionFrame>()
        val imagesByIndex = settings.messageImages
            .filter { it.uri.isNotBlank() }
            .groupBy { it.insertionIndex.coerceIn(0, message.length) }
            .toSortedMap()

        var cursor = 0
        imagesByIndex.forEach { (insertionIndex, images) ->
            if (insertionIndex > cursor) {
                frames += slideFrames(message.substring(cursor, insertionIndex).textSlideItems(cursor, settings), settings)
            }
            frames += imageFrames(images, settings)
            cursor = insertionIndex
        }

        if (cursor < message.length) {
            frames += slideFrames(message.substring(cursor).textSlideItems(cursor, settings), settings)
        }

        return frames
    }

    private fun slideItems(message: String, settings: TransmissionSettings): List<SlideItem> {
        val items = mutableListOf<SlideItem>()
        val imagesByIndex = settings.messageImages
            .filter { it.uri.isNotBlank() }
            .groupBy { it.insertionIndex.coerceIn(0, message.length) }
            .toSortedMap()

        var cursor = 0
        imagesByIndex.forEach { (insertionIndex, images) ->
            if (insertionIndex > cursor) {
                items += message.substring(cursor, insertionIndex).textSlideItems(cursor, settings)
            }
            images.forEach { image -> items += SlideItem.Image(image.uri, image.durationSeconds) }
            cursor = insertionIndex
        }
        if (cursor < message.length) {
            items += message.substring(cursor).textSlideItems(cursor, settings)
        }
        return items
    }

    private fun slideFrames(items: List<SlideItem>, settings: TransmissionSettings): List<TransmissionFrame> =
        if (items.isEmpty()) emptyList() else listOf(
            TransmissionFrame(
                FrameKind.SlideMessage(items, 0),
                items.sumOf { item -> itemDuration(item, settings) } + slideLeadingPaddingDuration(settings),
            ),
        )

    private fun itemDuration(item: SlideItem, settings: TransmissionSettings): Double =
        when (item) {
            is SlideItem.Text -> characterDuration(item.value, settings)
            is SlideItem.Image -> item.durationSeconds
        }

    private fun slideLeadingPaddingDuration(settings: TransmissionSettings): Double =
        settings.characterDuration * 30

    private fun characterDuration(character: String, settings: TransmissionSettings): Double =
        when {
            character.isBlank() -> settings.characterDuration * 2
            character.isEmojiGrapheme() -> settings.emojiDuration
            else -> settings.characterDuration
        }

    /** Splits a string into user-perceived characters (extended grapheme clusters). */
    private data class GraphemeCluster(val text: String, val startIndex: Int, val endIndex: Int)

    private fun String.graphemeClustersWithOffsets(): List<GraphemeCluster> {
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(this)
        val clusters = mutableListOf<GraphemeCluster>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            clusters += GraphemeCluster(substring(start, end), start, end)
            start = end
            end = iterator.next()
        }
        return clusters
    }

    private fun String.textSlideItems(baseOffset: Int, settings: TransmissionSettings): List<SlideItem> =
        graphemeClustersWithOffsets().map { cluster ->
            SlideItem.Text(
                value = cluster.text,
                color = settings.textColorAt(baseOffset + cluster.startIndex),
            )
        }

    private fun TransmissionSettings.textColorAt(index: Int): androidx.compose.ui.graphics.Color? =
        textColorSpans.lastOrNull { span -> index in span.start until span.end }?.color

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
