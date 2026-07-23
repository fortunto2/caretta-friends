package com.carettafriends.data

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.popoverPresentationController

actual fun platformShareImage(srcPath: String, caption: String) {
    // iOS burns no watermark yet (NSString CoreGraphics drawing is a cinterop follow-up) — the photo
    // is shared with the caption text so the receiver still sees code · beach · date.
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    val img = UIImage(contentsOfFile = srcPath)
    val items = if (img != null) listOf(img, caption) else listOf(caption)
    val vc = UIActivityViewController(activityItems = items, applicationActivities = null)
    vc.popoverPresentationController?.sourceView = root.view
    root.presentViewController(vc, animated = true, completion = null)
}

actual fun platformShare(text: String) {
    val vc = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    vc.popoverPresentationController?.sourceView = root.view
    root.presentViewController(vc, animated = true, completion = null)
}
