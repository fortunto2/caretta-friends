package com.carettafriends.ui

import androidx.compose.runtime.Composable

/** iOS has no hardware/system back button — in-app back + edge swipe handle it. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no-op on iOS
}
