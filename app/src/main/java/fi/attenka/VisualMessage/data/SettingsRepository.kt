package fi.attenka.VisualMessage.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import fi.attenka.VisualMessage.model.MessageLibrary
import fi.attenka.VisualMessage.model.MessageImage
import fi.attenka.VisualMessage.model.MessageFontFamily
import fi.attenka.VisualMessage.model.MessageFontStyle
import fi.attenka.VisualMessage.model.MessageTextColorSpan
import fi.attenka.VisualMessage.model.SavedMessage
import fi.attenka.VisualMessage.model.MorseAlphabet
import fi.attenka.VisualMessage.model.MorseOutputMode
import fi.attenka.VisualMessage.model.SlideDirection
import fi.attenka.VisualMessage.model.TransmissionMode
import fi.attenka.VisualMessage.model.TransmissionSettings
import fi.attenka.VisualMessage.model.TransitionStyle
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists [TransmissionSettings] and [MessageLibrary] as JSON in SharedPreferences.
 * Decoding starts from defaults so missing or future keys degrade gracefully, matching
 * the iOS Codable behaviour.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): TransmissionSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return TransmissionSettings()
        return runCatching { decodeSettings(JSONObject(raw)) }.getOrDefault(TransmissionSettings())
    }

    fun saveSettings(settings: TransmissionSettings) {
        prefs.edit().putString(KEY_SETTINGS, encodeSettings(settings).toString()).apply()
    }

    fun loadLibrary(): MessageLibrary {
        val raw = prefs.getString(KEY_LIBRARY, null) ?: return MessageLibrary.starter
        return runCatching {
            val array = JSONArray(raw)
            MessageLibrary(
                buildList {
                    for (index in 0 until array.length()) {
                        decodeSavedMessage(array, index)?.let(::add)
                    }
                }
            )
        }.getOrDefault(MessageLibrary.starter)
    }

    fun saveLibrary(library: MessageLibrary) {
        val array = JSONArray()
        library.messages.forEach { array.put(encodeSavedMessage(it)) }
        prefs.edit().putString(KEY_LIBRARY, array.toString()).apply()
    }

    private fun encodeSettings(s: TransmissionSettings): JSONObject = JSONObject().apply {
        put("message", s.message)
        put("messageImages", encodeMessageImages(s.messageImages))
        put("textColorSpans", encodeTextColorSpans(s.textColorSpans))
        put("mode", s.mode.name)
        put("themeID", s.themeID)
        put("multicolorLettersEnabled", s.multicolorLettersEnabled)
        put("customEditorColorsEnabled", s.customEditorColorsEnabled)
        put("slideDirection", s.slideDirection.name)
        put("slideImageBehavior", s.slideImageBehavior.name)
        put("uppercaseEnabled", s.uppercaseEnabled)
        put("messageFontFamily", s.messageFontFamily.name)
        put("messageFontStyle", s.messageFontStyle.name)
        put("characterDuration", s.characterDuration)
        put("emojiDuration", s.emojiDuration)
        put("characterGap", s.characterGap)
        put("repeatCount", s.repeatCount)
        put("repeatForever", s.repeatForever)
        put("transitionStyle", s.transitionStyle.name)
        put("soundSignalEnabled", s.soundSignalEnabled)
        put("visualSignalEnabled", s.visualSignalEnabled)
        put("morseSoundEnabled", s.morseSoundEnabled)
        put("torchSignalEnabled", s.morseOutputMode.usesTorch)
        put("morseOutputMode", s.morseOutputMode.name)
        put("signalFrequency", s.signalFrequency)
        put("startDelaySeconds", s.startDelaySeconds)
        put("characterSizeScale", s.characterSizeScale)
        put("morseUnitDuration", s.morseUnitDuration)
        put("morseAlphabet", s.morseAlphabet.name)
        put("customForeground", encodeColor(s.customForeground))
        put("customBackground", encodeColor(s.customBackground))
        put("editorForeground", encodeColor(s.editorForeground))
        put("editorBackground", encodeColor(s.editorBackground))
    }

    private fun decodeSettings(json: JSONObject): TransmissionSettings {
        val defaults = TransmissionSettings()
        val message = json.optString("message", defaults.message)
        val characterDuration = json.optDouble("characterDuration", defaults.characterDuration)
        val emojiDuration = json.optDouble("emojiDuration", defaults.emojiDuration)
        val defaultImageDuration = emojiDuration.coerceAtLeast(characterDuration)
        val legacyTorchEnabled = json.optBoolean("torchSignalEnabled", defaults.torchSignalEnabled)
        val outputMode = json.optEnumOrNull<MorseOutputMode>("morseOutputMode")
            ?: if (legacyTorchEnabled) MorseOutputMode.SCREEN_AND_TORCH else defaults.morseOutputMode

        return defaults.copy(
            message = message,
            messageImages = json.optMessageImages(defaults.messageImages, message.length, defaultImageDuration),
            textColorSpans = json.optTextColorSpans(defaults.textColorSpans, message.length),
            mode = json.optEnum("mode", defaults.mode),
            themeID = json.optString("themeID", defaults.themeID),
            multicolorLettersEnabled = json.optBoolean("multicolorLettersEnabled", defaults.multicolorLettersEnabled),
            customEditorColorsEnabled = json.optBoolean("customEditorColorsEnabled", defaults.customEditorColorsEnabled),
            slideDirection = json.optSlideDirection(defaults.slideDirection),
            slideImageBehavior = json.optEnum("slideImageBehavior", defaults.slideImageBehavior),
            uppercaseEnabled = json.optBoolean("uppercaseEnabled", defaults.uppercaseEnabled),
            messageFontFamily = json.optEnum("messageFontFamily", defaults.messageFontFamily),
            messageFontStyle = json.optEnum("messageFontStyle", defaults.messageFontStyle),
            characterDuration = characterDuration,
            emojiDuration = emojiDuration,
            characterGap = json.optDouble("characterGap", defaults.characterGap),
            repeatCount = json.optInt("repeatCount", defaults.repeatCount),
            repeatForever = json.optBoolean("repeatForever", defaults.repeatForever),
            transitionStyle = json.optEnum("transitionStyle", defaults.transitionStyle),
            soundSignalEnabled = json.optBoolean("soundSignalEnabled", defaults.soundSignalEnabled),
            visualSignalEnabled = json.optBoolean("visualSignalEnabled", defaults.visualSignalEnabled),
            morseSoundEnabled = json.optBoolean("morseSoundEnabled", defaults.morseSoundEnabled),
            morseOutputMode = outputMode,
            signalFrequency = json.optDouble("signalFrequency", defaults.signalFrequency),
            startDelaySeconds = json.optInt("startDelaySeconds", defaults.startDelaySeconds).coerceIn(0, 10),
            characterSizeScale = json.optDouble("characterSizeScale", defaults.characterSizeScale).coerceIn(0.25, 1.0),
            morseUnitDuration = json.optDouble("morseUnitDuration", defaults.morseUnitDuration),
            morseAlphabet = json.optEnum("morseAlphabet", defaults.morseAlphabet),
            customForeground = json.optColor("customForeground", defaults.customForeground),
            customBackground = json.optColor("customBackground", defaults.customBackground),
            editorForeground = json.optColor("editorForeground", defaults.editorForeground),
            editorBackground = json.optColor("editorBackground", defaults.editorBackground),
        )
    }

    private fun encodeColor(color: Color): JSONObject = JSONObject().apply {
        if (color == Color.Unspecified) {
            put("unspecified", true)
        } else {
            put("red", color.red.toDouble())
            put("green", color.green.toDouble())
            put("blue", color.blue.toDouble())
            put("opacity", color.alpha.toDouble())
        }
    }

    private fun JSONObject.optColor(key: String, fallback: Color): Color {
        val obj = optJSONObject(key) ?: return fallback
        if (obj.optBoolean("unspecified", false)) return Color.Unspecified
        return Color(
            red = obj.optDouble("red", 1.0).toFloat(),
            green = obj.optDouble("green", 1.0).toFloat(),
            blue = obj.optDouble("blue", 1.0).toFloat(),
            alpha = obj.optDouble("opacity", 1.0).toFloat(),
        )
    }

    private fun encodeSavedMessage(saved: SavedMessage): JSONObject =
        JSONObject().apply {
            put("message", saved.message)
            put("textColorSpans", encodeTextColorSpans(saved.textColorSpans))
            put("messageFontFamily", saved.messageFontFamily.name)
            put("messageFontStyle", saved.messageFontStyle.name)
        }

    private fun decodeSavedMessage(array: JSONArray, index: Int): SavedMessage? {
        val defaults = SavedMessage("")
        val item = array.opt(index) ?: return null
        if (item is String) {
            return item.trim().takeIf { it.isNotEmpty() }?.let(::SavedMessage)
        }

        val json = item as? JSONObject ?: return null
        val message = json.optString("message", "").trim().takeIf { it.isNotEmpty() } ?: return null
        return SavedMessage(
            message = message,
            textColorSpans = json.optTextColorSpans(emptyList(), message.length),
            messageFontFamily = json.optEnum("messageFontFamily", defaults.messageFontFamily),
            messageFontStyle = json.optEnum("messageFontStyle", defaults.messageFontStyle),
        )
    }

    private fun encodeMessageImages(images: List<MessageImage>): JSONArray {
        val array = JSONArray()
        images.forEach { image ->
            array.put(
                JSONObject().apply {
                    put("id", image.id)
                    put("uri", image.uri)
                    put("insertionIndex", image.insertionIndex)
                    put("durationSeconds", image.durationSeconds)
                }
            )
        }
        return array
    }

    private fun JSONObject.optMessageImages(
        fallback: List<MessageImage>,
        messageLength: Int,
        defaultDurationSeconds: Double,
    ): List<MessageImage> {
        val array = optJSONArray("messageImages") ?: return fallback
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id", "").takeIf { it.isNotBlank() } ?: continue
                val uri = item.optString("uri", "").takeIf { it.isNotBlank() } ?: continue
                add(
                    MessageImage(
                        id = id,
                        uri = uri,
                        insertionIndex = item.optInt("insertionIndex", messageLength).coerceIn(0, messageLength),
                        durationSeconds = item.optDouble("durationSeconds", defaultDurationSeconds).coerceIn(0.15, 10.0),
                    )
                )
            }
        }
    }

    private fun encodeTextColorSpans(spans: List<MessageTextColorSpan>): JSONArray {
        val array = JSONArray()
        spans.forEach { span ->
            array.put(
                JSONObject().apply {
                    put("start", span.start)
                    put("end", span.end)
                    put("color", encodeColor(span.color))
                }
            )
        }
        return array
    }

    private fun JSONObject.optTextColorSpans(
        fallback: List<MessageTextColorSpan>,
        messageLength: Int,
    ): List<MessageTextColorSpan> {
        val array = optJSONArray("textColorSpans") ?: return fallback
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val start = item.optInt("start", 0).coerceIn(0, messageLength)
                val end = item.optInt("end", start).coerceIn(0, messageLength)
                val color = item.optColor("color", Color.Unspecified)
                if (start < end && color != Color.Unspecified) {
                    add(MessageTextColorSpan(start = start, end = end, color = color))
                }
            }
        }
    }

    private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, fallback: T): T {
        val name = optString(key, fallback.name)
        return runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
    }

    private inline fun <reified T : Enum<T>> JSONObject.optEnumOrNull(key: String): T? {
        val name = optString(key, "")
        return runCatching { enumValueOf<T>(name) }.getOrNull()
            ?: enumValues<T>().firstOrNull { value ->
                value.name.equals(name, ignoreCase = true) ||
                    value.name.replace("_", "").equals(name, ignoreCase = true)
            }
    }

    private fun JSONObject.optSlideDirection(fallback: SlideDirection): SlideDirection {
        val name = optString("slideDirection", "")
        if (name.isNotEmpty()) {
            return runCatching { SlideDirection.valueOf(name) }.getOrDefault(fallback)
        }

        return when (optString("messageLanguage", "")) {
            "LEFT_TO_RIGHT" -> SlideDirection.LTR
            "ARABIC", "URDU" -> SlideDirection.RTL
            "AUTO" -> SlideDirection.AUTO
            else -> fallback
        }
    }

    companion object {
        private const val PREFS_NAME = "visual_message_settings"
        private const val KEY_SETTINGS = "transmissionSettings"
        private const val KEY_LIBRARY = "messageLibrary"
    }
}
