package com.carettafriends.data

import androidx.compose.ui.graphics.ImageBitmap

/** Decode encoded image bytes (JPEG/PNG from the native camera) into a Compose bitmap. */
expect fun decodeImageBytes(bytes: ByteArray): ImageBitmap?
