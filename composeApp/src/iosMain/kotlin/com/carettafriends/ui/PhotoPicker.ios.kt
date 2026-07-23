package com.carettafriends.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
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
                finish(cb, PickedPhoto(dest, null)) // EXIF back-date on iOS is a follow-up (uses "today")
            }
        }
    }

    private fun finish(cb: ((PickedPhoto?) -> Unit)?, photo: PickedPhoto?) {
        dispatch_async(dispatch_get_main_queue()) { cb?.invoke(photo) }
    }
}
