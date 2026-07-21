package com.carettafriends.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// AI-TODO: wire a real map on iOS via MapLibre-iOS (MLNMapView) through the native
// SwiftUI hybrid shell (IosEntry.kt / ComposeUIViewController factories). This stub only
// exists so the commonMain `expect fun OsmMap` compiles for all Apple targets.
@Composable
actual fun OsmMap(
    modifier: Modifier,
    points: List<MapMarker>,
    onClick: (String) -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Map (iOS placeholder) — ${'$'}{points.size} markers")
    }
}