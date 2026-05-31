package fi.attenka.VisualMessage.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import fi.attenka.VisualMessage.model.MessageLibrary
import fi.attenka.VisualMessage.model.MorseAlphabet
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
            MessageLibrary((0 until array.length()).map { array.getString(it) })
        }.getOrDefault(MessageLibrary.starter)
    }

    fun saveLibrary(library: MessageLibrary) {
        val array = JSONArray()
        library.messages.forEach { array.put(it) }
        prefs.edit().putString(KEY_LIBRARY, array.toString()).apply()
    }

    private fun encodeSettings(s: TransmissionSettings): JSONObject = JSONObject().apply {
        put("message", s.message)
        put("mode", s.mode.name)
        put("themeID", s.themeID)
        put("customEditorColorsEnabled", s.customEditorColorsEnabled)
        put("characterDuration", s.characterDuration)
        put("characterGap", s.characterGap)
        put("repeatCount", s.repeatCount)
        put("transitionStyle", s.transitionStyle.name)
        put("soundSignalEnabled", s.soundSignalEnabled)
        put("visualSignalEnabled", s.visualSignalEnabled)
        put("signalFrequency", s.signalFrequency)
        put("startDelaySeconds", s.startDelaySeconds)
        put("morseUnitDuration", s.morseUnitDuration)
        put("morseAlphabet", s.morseAlphabet.name)
        put("customForeground", encodeColor(s.customForeground))
        put("customBackground", encodeColor(s.customBackground))
        put("editorForeground", encodeColor(s.editorForeground))
        put("editorBackground", encodeColor(s.editorBackground))
    }

    private fun decodeSettings(json: JSONObject): TransmissionSettings {
        val defaults = TransmissionSettings()
        return defaults.copy(
            message = json.optString("message", defaults.message),
            mode = json.optEnum("mode", defaults.mode),
            themeID = json.optString("themeID", defaults.themeID),
            customEditorColorsEnabled = json.optBoolean("customEditorColorsEnabled", defaults.customEditorColorsEnabled),
            characterDuration = json.optDouble("characterDuration", defaults.characterDuration),
            characterGap = json.optDouble("characterGap", defaults.characterGap),
            repeatCount = json.optInt("repeatCount", defaults.repeatCount),
            transitionStyle = json.optEnum("transitionStyle", defaults.transitionStyle),
            soundSignalEnabled = json.optBoolean("soundSignalEnabled", defaults.soundSignalEnabled),
            visualSignalEnabled = json.optBoolean("visualSignalEnabled", defaults.visualSignalEnabled),
            signalFrequency = json.optDouble("signalFrequency", defaults.signalFrequency),
            startDelaySeconds = json.optInt("startDelaySeconds", defaults.startDelaySeconds).coerceIn(1, 10),
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

    private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, fallback: T): T {
        val name = optString(key, fallback.name)
        return runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
    }

    companion object {
        private const val PREFS_NAME = "visual_message_settings"
        private const val KEY_SETTINGS = "transmissionSettings"
        private const val KEY_LIBRARY = "messageLibrary"
    }
}
