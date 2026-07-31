package com.carettafriends.data

/** The languages the app ships a full interface for (see content/AppStrings.kt). */
val SUPPORTED_LANGUAGES = setOf("en", "ru", "tr")

/**
 * The device's language on first run, so a volunteer in Gazipaşa opens the app in Turkish
 * instead of having to find the language picker. Falls back to English for everything else.
 * Once set, the volunteer's own choice in Profile → Language wins (it is persisted in state).
 */
expect fun systemLanguage(): String

/** Map a BCP-47 tag ("tr-TR", "ru_RU", "en") onto a language we actually translate. */
fun normalizeLanguage(tag: String?): String {
    val code = tag?.takeIf { it.isNotBlank() }?.take(2)?.lowercase()
    return if (code != null && code in SUPPORTED_LANGUAGES) code else "en"
}
