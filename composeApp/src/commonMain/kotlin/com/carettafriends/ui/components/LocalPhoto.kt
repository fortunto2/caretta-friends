package com.carettafriends.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.carettafriends.data.LocalStore
import com.carettafriends.data.decodeImageBytes
import com.carettafriends.ui.theme.caretta

/**
 * Renders a photo stored on disk at an absolute [path] (e.g. from the native camera).
 * Cross-platform, dependency-free: reads bytes via okio, decodes via the platform decoder.
 * Falls back to an egg placeholder when the file is missing or can't be decoded.
 */
@Composable
fun LocalPhoto(path: String?, modifier: Modifier = Modifier, placeholder: String = "🥚") {
    val bitmap = remember(path) { path?.let { LocalStore.readBytesAbs(it) }?.let { decodeImageBytes(it) } }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Photo",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier.background(caretta.sand), contentAlignment = Alignment.Center) {
            Text(placeholder, fontSize = 40.sp)
        }
    }
}
