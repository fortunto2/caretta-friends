package com.carettafriends.data

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decode encoded image bytes (JPEG/PNG from the native camera) into a Compose bitmap, no larger
 * than [maxPx] on its longest side.
 *
 * A phone photo is ~12 MP; decoded full-size it costs ~48 MB of ARGB — a beach feed of fifteen
 * 52dp thumbnails would decode fifteen of those and run a mid-range phone out of memory. The
 * caller says how big it will actually draw the picture.
 */
expect fun decodeImageBytes(bytes: ByteArray, maxPx: Int = 2048): ImageBitmap?
