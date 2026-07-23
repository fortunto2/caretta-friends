package com.carettafriends.ui

import androidx.compose.runtime.Composable

/** A photo picked from the gallery: a local file path + the EXIF capture time (epoch millis, if any). */
data class PickedPhoto(val path: String, val exifEpochMillis: Long?)

/**
 * Remember a gallery-picker launcher. Call the returned lambda to open the OS gallery; [onPicked]
 * fires with the copied local path + EXIF date, or null if cancelled/failed. EXIF lets an old photo
 * back-date the nest/update.
 */
@Composable
expect fun rememberGalleryPicker(onPicked: (PickedPhoto?) -> Unit): () -> Unit
