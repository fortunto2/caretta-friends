package com.carettafriends.data

import com.carettafriends.ui.topmostViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSString
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIRectFill
import platform.UIKit.drawInRect
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
actual fun platformShareImage(srcPath: String, caption: String) {
    val root = topmostViewController() ?: return
    val base = UIImage(contentsOfFile = srcPath)
    val shareItem: Any = if (base != null) watermark(base, caption) else caption
    val vc = UIActivityViewController(activityItems = listOf(shareItem, caption), applicationActivities = null)
    vc.popoverPresentationController?.sourceView = root.view
    root.presentViewController(vc, animated = true, completion = null)
}

/** Burn a caption bar (caption · 🐢 Caretta Friends) onto the bottom of [img]. Falls back to [img]. */
@OptIn(ExperimentalForeignApi::class)
private fun watermark(img: UIImage, caption: String): UIImage {
    val (w, h) = img.size.useContents { width to height }
    UIGraphicsBeginImageContextWithOptions(img.size, false, img.scale)
    img.drawInRect(CGRectMake(0.0, 0.0, w, h))
    val barH = maxOf(h * 0.14, 84.0)
    UIColor.blackColor.colorWithAlphaComponent(0.55).setFill()
    UIRectFill(CGRectMake(0.0, h - barH, w, barH))
    val titleAttrs = mapOf<Any?, Any?>(
        NSForegroundColorAttributeName to UIColor.whiteColor,
        NSFontAttributeName to UIFont.boldSystemFontOfSize(barH * 0.30),
    )
    val brandAttrs = mapOf<Any?, Any?>(
        NSForegroundColorAttributeName to UIColor.whiteColor.colorWithAlphaComponent(0.85),
        NSFontAttributeName to UIFont.systemFontOfSize(barH * 0.22),
    )
    (caption as NSString).drawInRect(CGRectMake(barH * 0.25, h - barH * 0.78, w - barH * 0.5, barH * 0.42), titleAttrs)
    ("🐢 Caretta Friends" as NSString).drawInRect(CGRectMake(barH * 0.25, h - barH * 0.38, w - barH * 0.5, barH * 0.32), brandAttrs)
    val out = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return out ?: img
}

actual fun platformShare(text: String) {
    val vc = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    val root = topmostViewController() ?: return
    vc.popoverPresentationController?.sourceView = root.view
    root.presentViewController(vc, animated = true, completion = null)
}
