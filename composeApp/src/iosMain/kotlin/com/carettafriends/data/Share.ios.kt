package com.carettafriends.data

import com.carettafriends.ui.topmostViewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIImage
import platform.UIKit.popoverPresentationController

/**
 * Share a nest photo + caption via the system share sheet. The photo is shared as-is — the in-app
 * camera already burns the date / GPS / author overlay onto it, so we deliberately do NOT re-draw a
 * watermark here (the Core Graphics path was crash-prone on device; a plain image+text share is safe).
 */
actual fun platformShareImage(srcPath: String, caption: String) {
    val root = topmostViewController() ?: return
    val image = UIImage(contentsOfFile = srcPath)
    val items: List<*> = if (image != null) listOf(image, caption) else listOf(caption)
    val vc = UIActivityViewController(activityItems = items, applicationActivities = null)
    // iPad presents this as a popover and needs an anchor; on iPhone it's an ignored no-op.
    vc.popoverPresentationController?.sourceView = root.view
    root.presentViewController(vc, animated = true, completion = null)
}

actual fun platformShare(text: String) {
    val root = topmostViewController() ?: return
    val vc = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    vc.popoverPresentationController?.sourceView = root.view
    root.presentViewController(vc, animated = true, completion = null)
}
