package fi.attenka.VisualMessage.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.InputStream

fun loadMessageImageBitmap(context: Context, uriString: String): ImageBitmap? {
    val uri = Uri.parse(uriString)
    val displayMetrics = context.resources.displayMetrics
    val requestedWidth = displayMetrics.widthPixels.coerceAtLeast(1)
    val requestedHeight = displayMetrics.heightPixels.coerceAtLeast(1)
    val cacheKey = "$uriString@$requestedWidth:$requestedHeight"
    messageImageCache.get(cacheKey)?.let { return it }

    val bitmap = runCatching {
        decodeSampledBitmap(
            openInputStream = { context.contentResolver.openInputStream(uri) },
            requestedWidth = requestedWidth,
            requestedHeight = requestedHeight,
        )
    }.getOrNull()?.asImageBitmap() ?: uri.path?.let { path ->
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(
                    sourceWidth = bounds.outWidth,
                    sourceHeight = bounds.outHeight,
                    requestedWidth = requestedWidth,
                    requestedHeight = requestedHeight,
                )
            }
            BitmapFactory.decodeFile(path, options)?.asImageBitmap()
        }.getOrNull()
    }

    if (bitmap != null) {
        messageImageCache.put(cacheKey, bitmap)
    }
    return bitmap
}

fun evictMessageImageBitmap(uriString: String) {
    messageImageCache.snapshot().keys
        .filter { it.startsWith("$uriString@") }
        .forEach(messageImageCache::remove)
}

private fun decodeSampledBitmap(
    openInputStream: () -> InputStream?,
    requestedWidth: Int,
    requestedHeight: Int,
) = BitmapFactory.Options().run {
    inJustDecodeBounds = true
    openInputStream()?.use { BitmapFactory.decodeStream(it, null, this) }
    inSampleSize = calculateInSampleSize(
        sourceWidth = outWidth,
        sourceHeight = outHeight,
        requestedWidth = requestedWidth,
        requestedHeight = requestedHeight,
    )
    inJustDecodeBounds = false
    openInputStream()?.use { BitmapFactory.decodeStream(it, null, this) }
}

internal fun calculateInSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    requestedWidth: Int,
    requestedHeight: Int,
): Int {
    if (sourceWidth <= 0 || sourceHeight <= 0 || requestedWidth <= 0 || requestedHeight <= 0) return 1

    val fitScale = minOf(
        requestedWidth.toDouble() / sourceWidth,
        requestedHeight.toDouble() / sourceHeight,
    )
    if (fitScale >= 1.0) return 1

    val targetWidth = (sourceWidth * fitScale).toInt().coerceAtLeast(1)
    val targetHeight = (sourceHeight * fitScale).toInt().coerceAtLeast(1)
    var sampleSize = 1
    val halfWidth = sourceWidth / 2
    val halfHeight = sourceHeight / 2
    while (
        halfWidth / sampleSize >= targetWidth &&
        halfHeight / sampleSize >= targetHeight
    ) {
        sampleSize *= 2
    }
    return sampleSize
}

private val messageImageCache = object : LruCache<String, ImageBitmap>(messageImageCacheSizeKb()) {
    override fun sizeOf(key: String, value: ImageBitmap): Int =
        ((value.width.toLong() * value.height * BYTES_PER_PIXEL) / 1024L)
            .coerceAtLeast(1L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
}

private fun messageImageCacheSizeKb(): Int =
    (Runtime.getRuntime().maxMemory() / 1024L / CACHE_MEMORY_DIVISOR)
        .coerceAtLeast(MIN_CACHE_SIZE_KB.toLong())
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()

private const val BYTES_PER_PIXEL = 4L
private const val CACHE_MEMORY_DIVISOR = 16L
private const val MIN_CACHE_SIZE_KB = 4 * 1024
