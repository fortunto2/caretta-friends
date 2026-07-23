package com.carettafriends.data

import android.content.Intent

actual fun platformShare(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    runCatching { AndroidApp.context.startActivity(chooser) }
}
