package com.carettafriends.data

import java.util.Locale

actual fun systemLanguage(): String = normalizeLanguage(Locale.getDefault().language)
