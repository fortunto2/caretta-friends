package com.carettafriends.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode

actual fun decodeImageBytes(bytes: ByteArray, maxPx: Int): ImageBitmap? = runCatching {
    val image = Image.makeFromEncoded(bytes)
    val longest = maxOf(image.width, image.height)
    if (longest <= maxPx) return@runCatching image.toComposeImageBitmap()
    // Scale down before handing it to Compose: a 12 MP photo behind a 52dp thumbnail is 48 MB of
    // bitmap either way, and a feed holds a dozen of them.
    val scale = maxPx.toFloat() / longest
    val w = (image.width * scale).toInt().coerceAtLeast(1)
    val h = (image.height * scale).toInt().coerceAtLeast(1)
    val surface = Surface.makeRasterN32Premul(w, h)
    surface.canvas.drawImageRect(
        image,
        Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
        Rect.makeWH(w.toFloat(), h.toFloat()),
        SamplingMode.LINEAR,
        null,
        true,
    )
    surface.makeImageSnapshot().toComposeImageBitmap()
}.getOrNull()
