package com.carettafriends.data

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.popoverPresentationController

actual fun platformShareImage(srcPath: String, caption: String) {
    // iOS watermark compositing is a follow-up — share the photo + caption text for now.
    val img = UIImage(contentsOfFile = srcPath)
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    val items = if (img != null) listOf(img, caption) else listOf(caption)
    val vc = UIActivityViewController(activityItems = items, applicationActivities = null)
    vc.popoverPresentationController?.sourceView = root.view
    root.presentViewController(vc, animated = true, completion = null)
}

actual fun platformShare(text: String) {
    val vc = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    // iPad needs a source for the popover — anchor to the root view (harmless on iPhone).
    vc.popoverPresentationController?.sourceView = root.view
    root.presentViewController(vc, animated = true, completion = null)
}
