package com.carettafriends.ui

import androidx.compose.runtime.Composable

/** iOS records patrols in its SwiftUI map shell (`PatrolRecorder.swift`), not through Compose. */
@Composable
actual fun rememberPatrolRecorder(lang: String): PatrolRecorder? = null
