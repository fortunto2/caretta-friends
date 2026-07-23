package com.carettafriends.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import java.util.Calendar

@Composable
actual fun rememberGalleryPicker(onPicked: (PickedPhoto?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            onPicked(null)
        } else {
            runCatching {
                // Copy to app storage — a content:// grant is transient, a file path survives.
                val dest = File(context.filesDir, "nest_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                }
                val exif = context.contentResolver.openInputStream(uri)?.use { readExifMillis(it) }
                onPicked(PickedPhoto(dest.absolutePath, exif))
            }.getOrElse { onPicked(null) }
        }
    }
    return { launcher.launch("image/*") }
}

/** Parse EXIF DateTimeOriginal ("yyyy:MM:dd HH:mm:ss") → epoch millis, or null. */
private fun readExifMillis(input: InputStream): Long? = runCatching {
    val exif = ExifInterface(input)
    val dt = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        ?: return null
    val (dPart, tPart) = (dt.split(" ") + "00:00:00").let { it[0] to it[1] }
    val d = dPart.split(":").map { it.toInt() }
    val t = (tPart.split(":") + listOf("0", "0", "0")).map { it.toIntOrNull() ?: 0 }
    Calendar.getInstance().apply { set(d[0], d[1] - 1, d[2], t[0], t[1], t[2]) }.timeInMillis
}.getOrNull()
