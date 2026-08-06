package com.carettafriends.data

import platform.Foundation.NSBundle

actual fun platformName(): String = "ios"

actual fun appVersion(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
