package com.carettafriends.data

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

actual fun platformShare(text: String) {
    val vc = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    // iPad needs a source for the popover — anchor to the root view (harmless on iPhone).
    vc.popoverPresentationController?.sourceView = root.view
    root.presentViewController(vc, animated = true, completion = null)
}
