package com.carettafriends.ui

import androidx.compose.runtime.Composable

/**
 * iOS gallery picking runs through the native SwiftUI layer (PHPicker), not Compose — wiring it via
 * IosEntry/ContentView is the follow-up. For now the launcher is a no-op so shared UI compiles.
 */
@Composable
actual fun rememberGalleryPicker(onPicked: (PickedPhoto?) -> Unit): () -> Unit = { onPicked(null) }
