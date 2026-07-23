package fi.attenka.VisualMessage.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.attenka.VisualMessage.BuildConfig
import fi.attenka.VisualMessage.R
import fi.attenka.VisualMessage.model.MorseCode

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val shareMessage = stringResource(R.string.share_app_message, APP_WEBSITE_URL)
    val shareChooserTitle = stringResource(R.string.share_app_chooser_title)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.help_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "${stringResource(R.string.app_name)} v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.help_summary))
                HelpSection(
                    title = stringResource(R.string.help_message_title),
                    body = stringResource(R.string.help_message_body),
                )
                HelpSection(
                    title = stringResource(R.string.help_visual_title),
                    body = stringResource(R.string.help_visual_body),
                )
                HelpSection(
                    title = stringResource(R.string.help_rhythm_title),
                    body = stringResource(R.string.help_rhythm_body),
                )
                HelpSection(
                    title = stringResource(R.string.help_morse_title),
                    body = stringResource(R.string.help_morse_body),
                )
                HelpSection(
                    title = stringResource(R.string.help_receiver_title),
                    body = stringResource(R.string.help_receiver_body),
                )
                MorseCodeSection(
                    title = stringResource(R.string.international),
                    entries = MorseCode.international,
                )
                MorseCodeSection(
                    title = stringResource(R.string.continental),
                    entries = MorseCode.continental.filterKeys { it == 'Ü' || it == 'Ñ' },
                    note = stringResource(R.string.help_continental_note),
                )
                HelpSection(
                    title = stringResource(R.string.help_share_title),
                    body = stringResource(R.string.help_share_body),
                )
                TextButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
                    },
                ) {
                    Text(stringResource(R.string.share_app))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun HelpSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MorseCodeSection(
    title: String,
    entries: Map<Char, String>,
    note: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (note != null) {
            Text(note, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = formatMorseEntries(entries),
            modifier = Modifier.padding(start = 2.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

private fun formatMorseEntries(entries: Map<Char, String>): String =
    entries.entries
        .sortedWith(compareBy({ entrySortGroup(it.key) }, { it.key }))
        .chunked(3)
        .joinToString(separator = "\n") { row ->
            row.joinToString(separator = "   ") { (character, code) ->
                "$character $code"
            }
        }

private fun entrySortGroup(character: Char): Int = when {
    character in 'A'..'Z' -> 0
    character in '0'..'9' -> 1
    else -> 2
}

private const val APP_WEBSITE_URL = "https://visualmessage.netlify.app/"
