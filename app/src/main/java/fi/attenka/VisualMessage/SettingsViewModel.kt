package fi.attenka.VisualMessage

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import fi.attenka.VisualMessage.data.SettingsRepository
import fi.attenka.VisualMessage.model.MessageImage
import fi.attenka.VisualMessage.model.MessageLibrary
import fi.attenka.VisualMessage.model.TransmissionSettings
import java.io.File
import java.util.UUID

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

    fun importMessageImage(uri: Uri, insertionIndex: Int) {
        val app = getApplication<Application>()
        val imageDir = File(app.filesDir, "message-images").apply { mkdirs() }
        val id = UUID.randomUUID().toString()
        val target = File(imageDir, "$id.img")

        app.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return

        updateSettings { current ->
            current.copy(
                messageImages = current.messageImages + MessageImage(
                    id = id,
                    uri = Uri.fromFile(target).toString(),
                    insertionIndex = insertionIndex.coerceIn(0, current.message.length),
                    durationSeconds = current.emojiDuration.coerceAtLeast(current.characterDuration),
                )
            )
        }
    }

    fun removeMessageImage(imageId: String) {
        settings.messageImages.firstOrNull { it.id == imageId }?.let { image ->
            runCatching { Uri.parse(image.uri).path?.let(::File)?.delete() }
        }
        updateSettings { current ->
            current.copy(messageImages = current.messageImages.filterNot { it.id == imageId })
        }
    }
}
