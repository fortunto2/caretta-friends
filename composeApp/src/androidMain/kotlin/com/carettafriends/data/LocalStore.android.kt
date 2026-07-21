package com.carettafriends.data

import android.content.Context

/** Set once from the Android entry point (MainActivity) before the repository is created. */
object AndroidApp {
    lateinit var context: Context
}

actual fun appDirPath(): String = AndroidApp.context.filesDir.absolutePath
