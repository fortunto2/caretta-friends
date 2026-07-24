package com.carettafriends.data

import com.carettafriends.ui.topmostViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile
import platform.UIKit.NSFontAttributeName
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererContext
import platform.UIKit.UIImage
import platform.UIKit.drawAtPoint
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

@OptIn(ExperimentalForeignApi::class)
actual fun platformSharePdf(fileName: String, title: String, lines: List<String>) {
    val root = topmostViewController() ?: return
    val bounds = CGRectMake(0.0, 0.0, 595.0, 842.0)   // A4 @ 72dpi
    val renderer = UIGraphicsPDFRenderer(bounds = bounds)
    val data = renderer.PDFDataWithActions { _: UIGraphicsPDFRendererContext? ->
        val margin = 48.0
        var y = 44.0
        val titleAttrs = mapOf<Any?, Any?>(NSFontAttributeName to UIFont.boldSystemFontOfSize(18.0))
        val bodyAttrs = mapOf<Any?, Any?>(NSFontAttributeName to UIFont.systemFontOfSize(12.0))
        (title as NSString).drawAtPoint(CGPointMake(margin, y), titleAttrs)
        y += 28.0
        for (line in lines) {
            (line as NSString).drawAtPoint(CGPointMake(margin, y), bodyAttrs)
            y += 20.0
        }
    }
    val path = (NSTemporaryDirectory() as NSString).stringByAppendingPathComponent("$fileName.pdf")
    data.writeToFile(path, atomically = true)
    val url = NSURL.fileURLWithPath(path)
    val vc = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
    vc.popoverPresentationController?.sourceView = root.view
    root.presentViewController(vc, animated = true, completion = null)
}
