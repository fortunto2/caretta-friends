package com.carettafriends.data

import com.carettafriends.BuildConfig

actual fun platformName(): String = "android"

actual fun appVersion(): String = BuildConfig.VERSION_NAME
