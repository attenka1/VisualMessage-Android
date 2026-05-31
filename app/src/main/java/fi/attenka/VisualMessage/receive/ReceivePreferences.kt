package fi.attenka.VisualMessage.receive

import android.content.Context

/** Receiver-specific preferences (camera options), separate from transmission settings. */
class ReceivePreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var preferHighFrameRate: Boolean
        get() = prefs.getBoolean(KEY_HIGH_FPS, true)
        set(value) = prefs.edit().putBoolean(KEY_HIGH_FPS, value).apply()

    companion object {
        private const val PREFS_NAME = "visual_message_receive"
        private const val KEY_HIGH_FPS = "preferHighFrameRate"
    }
}
