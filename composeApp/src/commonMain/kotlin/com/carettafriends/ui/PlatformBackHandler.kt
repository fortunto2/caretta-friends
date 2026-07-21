package com.carettafriends.ui

import androidx.compose.runtime.Composable

/** Platform system-back handling (Android predictive back; no-op elsewhere for now). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
