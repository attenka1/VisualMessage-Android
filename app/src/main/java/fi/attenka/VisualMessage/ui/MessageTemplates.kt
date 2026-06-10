package fi.attenka.VisualMessage.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import fi.attenka.VisualMessage.R
import fi.attenka.VisualMessage.model.MessageLibrary
import fi.attenka.VisualMessage.model.MessageFontFamily
import fi.attenka.VisualMessage.model.MessageFontStyle
import fi.attenka.VisualMessage.model.MessageTextColorSpan
import fi.attenka.VisualMessage.model.SavedMessage

/** Saved-messages menu and save/remove toggle, mirroring the iOS MessageTemplatesControls. */
@Composable
fun MessageTemplatesControls(
    library: MessageLibrary,
    message: String,
    textColorSpans: List<MessageTextColorSpan>,
    messageFontFamily: MessageFontFamily,
    messageFontStyle: MessageFontStyle,
    onSelectMessage: (SavedMessage) -> Unit,
    onLibraryChange: (MessageLibrary) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isCurrentSaved = library.contains(message)
    val trimmedEmpty = message.trim().isEmpty()

    Row {
        IconButton(
            onClick = { menuExpanded = true },
            enabled = library.messages.isNotEmpty(),
        ) {
            Text("\u2630", fontSize = 20.sp)
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            if (library.messages.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.no_saved_messages)) },
                    onClick = {},
                    enabled = false,
                )
            } else {
                library.messages.forEach { saved ->
                    DropdownMenuItem(
                        text = { Text(saved.message) },
                        onClick = {
                            onSelectMessage(saved)
                            menuExpanded = false
                        },
                    )
                }
            }
        }

        IconButton(
            onClick = {
                onLibraryChange(
                    if (isCurrentSaved) {
                        library.removing(message)
                    } else {
                        library.adding(
                            message = message,
                            textColorSpans = textColorSpans,
                            messageFontFamily = messageFontFamily,
                            messageFontStyle = messageFontStyle,
                        )
                    }
                )
            },
            enabled = !trimmedEmpty,
        ) {
            Text(if (isCurrentSaved) "\u2605" else "\u2606", fontSize = 20.sp)
        }
    }
}
