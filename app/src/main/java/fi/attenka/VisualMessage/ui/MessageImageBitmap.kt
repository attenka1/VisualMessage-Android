package fi.attenka.VisualMessage.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

fun loadMessageImageBitmap(context: Context, uriString: String): ImageBitmap? {
    val uri = Uri.parse(uriString)
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input)?.asImageBitmap()?.let { return it }
    }
    return uri.path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
}
