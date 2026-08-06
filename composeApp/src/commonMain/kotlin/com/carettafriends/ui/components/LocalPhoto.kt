package com.carettafriends.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.carettafriends.data.LocalStore
import com.carettafriends.data.PhotoFiles
import com.carettafriends.data.decodeImageBytes
import com.carettafriends.domain.PhotoRef
import com.carettafriends.ui.theme.caretta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a photo stored on disk at an absolute [path] (e.g. from the native camera).
 * Cross-platform, dependency-free: reads bytes via okio, decodes via the platform decoder.
 * Falls back to an egg placeholder when the file is missing or can't be decoded.
 *
 * [hideWhenMissing] drops the placeholder entirely — for the moment before a shared photo has been
 * fetched, where a large empty egg card would read as broken rather than as "loading".
 */
/**
 * A nest photo, wherever it currently is.
 *
 * Shows the local file when this device took the picture, and otherwise fetches the shared copy
 * from storage once (see [PhotoFiles]) — that's what makes another volunteer's nest arrive with its
 * photo instead of an empty frame.
 */
@Composable
fun NestPhoto(
    photo: PhotoRef?,
    modifier: Modifier = Modifier,
    placeholder: String = "🥚",
    hideWhenMissing: Boolean = false,
    /** Longest side actually drawn, in pixels — a list thumbnail needs nothing like a full photo. */
    maxPx: Int = 1024,
) {
    var path by remember(photo?.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(photo?.id) {
        // Checking the file and, when it's someone else's photo, fetching it are both disk/network
        // work — never on the frame thread, this runs for every row of a scrolling feed.
        if (photo != null) path = withContext(Dispatchers.Default) { PhotoFiles.localPath(photo) }
    }
    LocalPhoto(path, modifier, placeholder, hideWhenMissing = hideWhenMissing && path == null, maxPx = maxPx)
}

@Composable
fun LocalPhoto(
    path: String?,
    modifier: Modifier = Modifier,
    placeholder: String = "🥚",
    hideWhenMissing: Boolean = false,
    maxPx: Int = 1024,
) {
    // Read + decode off the frame thread: `remember { decode(...) }` did both DURING composition,
    // so a beach feed decoded a dozen multi-megapixel JPEGs on the UI thread while scrolling.
    var bitmap by remember(path, maxPx) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path, maxPx) {
        bitmap = path?.let {
            withContext(Dispatchers.Default) {
                LocalStore.readBytesAbs(it)?.let { bytes -> decodeImageBytes(bytes, maxPx) }
            }
        }
    }
    when {
        bitmap != null -> Image(
            bitmap = bitmap!!,
            contentDescription = "Photo",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
        hideWhenMissing -> Unit
        else -> Box(modifier.background(caretta.sand), contentAlignment = Alignment.Center) {
            Text(placeholder, fontSize = 40.sp)
        }
    }
}
