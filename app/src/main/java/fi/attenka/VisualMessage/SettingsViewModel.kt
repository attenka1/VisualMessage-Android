package fi.attenka.VisualMessage

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fi.attenka.VisualMessage.data.SettingsRepository
import fi.attenka.VisualMessage.model.MessageImage
import fi.attenka.VisualMessage.model.MessageLibrary
import fi.attenka.VisualMessage.model.TransmissionSettings
import fi.attenka.VisualMessage.ui.evictMessageImageBitmap
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Holds the live transmission settings and message library, persisting every change. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    var settings by mutableStateOf(repository.loadSettings())
        private set

    var library by mutableStateOf(repository.loadLibrary())
        private set

    private var settingsSaveJob: Job? = null
    private var librarySaveJob: Job? = null

    fun updateSettings(transform: (TransmissionSettings) -> TransmissionSettings) {
        val updated = transform(settings)
        settings = updated
        settingsSaveJob?.cancel()
        settingsSaveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            withContext(Dispatchers.IO) { repository.saveSettings(updated) }
        }
    }

    fun updateLibrary(transform: (MessageLibrary) -> MessageLibrary) {
        val updated = transform(library)
        library = updated
        librarySaveJob?.cancel()
        librarySaveJob = viewModelScope.launch(Dispatchers.IO) {
            repository.saveLibrary(updated)
        }
    }

    fun importMessageImage(uri: Uri, insertionIndex: Int) {
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) {
                val app = getApplication<Application>()
                val imageDir = File(app.filesDir, "message-images").apply { mkdirs() }
                val id = UUID.randomUUID().toString()
                val target = File(imageDir, "$id.img")
                val copied = runCatching {
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } != null
                }.getOrDefault(false)
                if (copied) {
                    id to target
                } else {
                    target.delete()
                    null
                }
            } ?: return@launch

            updateSettings { current ->
                current.copy(
                    messageImages = current.messageImages + MessageImage(
                        id = imported.first,
                        uri = Uri.fromFile(imported.second).toString(),
                        insertionIndex = insertionIndex.coerceIn(0, current.message.length),
                        durationSeconds = current.emojiDuration.coerceAtLeast(current.characterDuration),
                    )
                )
            }
        }
    }

    fun removeMessageImage(imageId: String) {
        val removedImage = settings.messageImages.firstOrNull { it.id == imageId }
        updateSettings { current ->
            current.copy(messageImages = current.messageImages.filterNot { it.id == imageId })
        }
        removedImage?.let { image ->
            evictMessageImageBitmap(image.uri)
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { Uri.parse(image.uri).path?.let(::File)?.delete() }
            }
        }
    }

    override fun onCleared() {
        if (settingsSaveJob?.isActive == true) repository.saveSettings(settings)
        if (librarySaveJob?.isActive == true) repository.saveLibrary(library)
        super.onCleared()
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 250L
    }
}
