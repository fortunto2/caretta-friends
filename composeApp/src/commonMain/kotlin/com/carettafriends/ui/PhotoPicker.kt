package com.carettafriends.ui

import androidx.compose.runtime.Composable

/**
 * A photo picked from the gallery: a local file path plus whatever its EXIF tells us —
 * when it was taken and, crucially, WHERE. The location used to be dropped, so importing a photo
 * of a nest pinned it to the beach centre instead of the sand it was taken on.
 * [hash] is the file's content identity, used to recognize the same photo imported twice.
 */
data class PickedPhoto(
    val path: String,
    val exifEpochMillis: Long?,
    val lat: Double? = null,
    val lng: Double? = null,
    val hash: String? = null,
)

/**
 * Remember a gallery-picker launcher. Call the returned lambda to open the OS gallery; [onPicked]
 * fires with the copied local path + EXIF date, or null if cancelled/failed. EXIF lets an old photo
 * back-date the nest/update.
 */
@Composable
expect fun rememberGalleryPicker(onPicked: (PickedPhoto?) -> Unit): () -> Unit

/**
 * Remember a "take a photo now" launcher, or null when the platform handles capture outside Compose.
 *
 * iOS returns null — it presents its own native camera (overlay burn-in, EXIF/GPS writing) from the
 * SwiftUI shell. Android returns a launcher for the system camera, which is what finally makes a
 * capture on Android produce a real photo with real coordinates instead of a mock viewfinder.
 */
@Composable
expect fun rememberCameraCapture(onCaptured: (PickedPhoto?) -> Unit): (() -> Unit)?
