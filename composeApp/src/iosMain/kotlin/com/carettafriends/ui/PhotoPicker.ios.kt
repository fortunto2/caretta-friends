package com.carettafriends.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import platform.CoreImage.CIImage
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.ImageIO.kCGImagePropertyExifDateTimeOriginal
import platform.ImageIO.kCGImagePropertyExifDictionary
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
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
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(picker, animated = true, completion = null)
    }
}

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
                finish(cb, PickedPhoto(dest, exifMillis(dest)))
            }
        }
    }

    private fun finish(cb: ((PickedPhoto?) -> Unit)?, photo: PickedPhoto?) {
        dispatch_async(dispatch_get_main_queue()) { cb?.invoke(photo) }
    }
}

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
