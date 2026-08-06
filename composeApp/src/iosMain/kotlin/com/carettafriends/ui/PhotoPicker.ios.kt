package com.carettafriends.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.carettafriends.data.photoFingerprint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import platform.CoreImage.CIImage
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.ImageIO.kCGImagePropertyExifDateTimeOriginal
import platform.ImageIO.kCGImagePropertyExifDictionary
import platform.ImageIO.kCGImagePropertyGPSDictionary
import platform.ImageIO.kCGImagePropertyGPSLatitude
import platform.ImageIO.kCGImagePropertyGPSLatitudeRef
import platform.ImageIO.kCGImagePropertyGPSLongitude
import platform.ImageIO.kCGImagePropertyGPSLongitudeRef
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberGalleryPicker(onPicked: (PickedPhoto?) -> Unit): () -> Unit {
    val delegate = remember { PhotoPickerDelegate() }
    delegate.onPicked = onPicked
    return {
        val config = PHPickerConfiguration()
        config.selectionLimit = 1
        config.filter = PHPickerFilter.imagesFilter()
        val picker = PHPickerViewController(configuration = config)
        picker.delegate = delegate
        topmostViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

/** iOS captures through its own native camera screen (SwiftUI shell), not through Compose. */
@Composable
actual fun rememberCameraCapture(onCaptured: (PickedPhoto?) -> Unit): (() -> Unit)? = null

@OptIn(ExperimentalForeignApi::class)
private class PhotoPickerDelegate : NSObject(), PHPickerViewControllerDelegateProtocol {
    var onPicked: ((PickedPhoto?) -> Unit)? = null

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val cb = onPicked
        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            finish(cb, null)
            return
        }
        result.itemProvider.loadFileRepresentationForTypeIdentifier("public.image") { url: NSURL?, _ ->
            val path = url?.path
            if (path == null) {
                finish(cb, null)
            } else {
                // The file rep is a temp URL that disappears after the block — copy it out.
                val dest = NSTemporaryDirectory() + "nest_" + NSUUID().UUIDString() + ".jpg"
                NSFileManager.defaultManager.copyItemAtPath(path, toPath = dest, error = null)
                val gps = exifLocation(dest)
                finish(
                    cb,
                    PickedPhoto(
                        path = dest,
                        exifEpochMillis = exifMillis(dest),
                        lat = gps?.first,
                        lng = gps?.second,
                        // A byte-for-byte copy of the original → a stable identity for duplicate checks.
                        hash = photoFingerprint(dest),
                    ),
                )
            }
        }
    }

    private fun finish(cb: ((PickedPhoto?) -> Unit)?, photo: PickedPhoto?) {
        dispatch_async(dispatch_get_main_queue()) { cb?.invoke(photo) }
    }
}

/**
 * Read the photo's GPS fix → (lat, lng), or null when it carries none.
 *
 * EXIF stores the magnitude plus a N/S · E/W reference, so the sign has to be reapplied — without
 * it a southern/western photo lands on the wrong side of the planet. This is what lets an imported
 * photo pin the nest where it was actually taken instead of on the beach's centre point.
 */
@OptIn(ExperimentalForeignApi::class)
private fun exifLocation(path: String): Pair<Double, Double>? {
    val ci = CIImage.imageWithContentsOfURL(NSURL.fileURLWithPath(path)) ?: return null
    val gps = ci.properties()[kCGImagePropertyGPSDictionary] as? Map<*, *> ?: return null
    // ImageIO hands these back as CFNumbers; take either bridging (NSNumber or a Kotlin Number).
    val lat = gps[kCGImagePropertyGPSLatitude].asDouble() ?: return null
    val lng = gps[kCGImagePropertyGPSLongitude].asDouble() ?: return null
    if (lat == 0.0 && lng == 0.0) return null
    val south = (gps[kCGImagePropertyGPSLatitudeRef] as? String)?.uppercase() == "S"
    val west = (gps[kCGImagePropertyGPSLongitudeRef] as? String)?.uppercase() == "W"
    return (if (south) -lat else lat) to (if (west) -lng else lng)
}

private fun Any?.asDouble(): Double? = (this as? NSNumber)?.doubleValue ?: (this as? Number)?.toDouble()

/** Read EXIF DateTimeOriginal ("yyyy:MM:dd HH:mm:ss") → epoch millis, or null. Graceful: any failure
 *  (no EXIF, key mismatch) just returns null → the update falls back to "today". */
@OptIn(ExperimentalForeignApi::class)
private fun exifMillis(path: String): Long? {
    val ci = CIImage.imageWithContentsOfURL(NSURL.fileURLWithPath(path)) ?: return null
    val exif = ci.properties()[kCGImagePropertyExifDictionary] as? Map<*, *> ?: return null
    val dt = exif[kCGImagePropertyExifDateTimeOriginal] as? String ?: return null
    // "yyyy:MM:dd HH:mm:ss" — parse with kotlinx.datetime (no NSDate cinterop).
    return runCatching {
        val parts = dt.split(" ")
        val d = parts[0].split(":").map { it.toInt() }
        val t = (parts.getOrElse(1) { "0:0:0" }).split(":").map { it.toIntOrNull() ?: 0 }
        LocalDateTime(d[0], d[1], d[2], t.getOrElse(0) { 0 }, t.getOrElse(1) { 0 }, t.getOrElse(2) { 0 })
            .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }.getOrNull()
}
