package fi.attenka.VisualMessage

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import fi.attenka.VisualMessage.data.SettingsRepository
import fi.attenka.VisualMessage.model.MessageLibrary
import fi.attenka.VisualMessage.model.TransmissionSettings

/** Holds the live transmission settings and message library, persisting every change. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    var settings by mutableStateOf(repository.loadSettings())
        private set

    var library by mutableStateOf(repository.loadLibrary())
        private set

    fun updateSettings(transform: (TransmissionSettings) -> TransmissionSettings) {
        settings = transform(settings).also(repository::saveSettings)
    }

    fun updateLibrary(transform: (MessageLibrary) -> MessageLibrary) {
        library = transform(library).also(repository::saveLibrary)
    }
}
