package com.carettafriends.data

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeImageBytes(bytes: ByteArray, maxPx: Int): ImageBitmap? = runCatching {
    // Two passes: read the dimensions without allocating, then decode with the smallest
    // power-of-two subsample that still covers [maxPx].
    val probe = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, probe)
    val longest = maxOf(probe.outWidth, probe.outHeight)
    var sample = 1
    while (longest / sample > maxPx) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
}.getOrNull()
