package com.carettafriends.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// iOS renders the map natively (SwiftUI MapLibreView in the hybrid shell) — MapScreen/OsmMap is
// Android-only in practice. This stub exists so commonMain `expect fun OsmMap` compiles for Apple.
@Composable
actual fun OsmMap(
    modifier: Modifier,
    points: List<MapMarker>,
    onClick: (String) -> Unit,
    onBeachTap: (String) -> Unit,
    onCommunityTap: (String) -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Map — ${points.size} markers")
    }
}
