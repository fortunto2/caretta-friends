package com.carettafriends.data

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

/** iOS reports the user's ordered language preferences; the first one we translate wins. */
actual fun systemLanguage(): String {
    val preferred = NSLocale.preferredLanguages
    for (tag in preferred) {
        val code = normalizeLanguage(tag as? String)
        if ((tag as? String)?.take(2)?.lowercase() == code) return code
    }
    return "en"
}
