package com.carettafriends.ui

import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

/**
 * The top-most view controller to present modals (PHPicker / share sheet) from. Reliable across
 * iOS 13+ where `keyWindow` can be nil (multi-scene) — falls back to the scene's key/first window,
 * then walks the presented-VC chain so we present over whatever's already on screen.
 */
fun topmostViewController(): UIViewController? {
    val app = UIApplication.sharedApplication
    val windows = app.windows.mapNotNull { it as? UIWindow }
    var vc: UIViewController? = app.keyWindow?.rootViewController
        ?: windows.firstOrNull { it.isKeyWindow() }?.rootViewController
        ?: windows.firstOrNull()?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}
