package com.carettafriends.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A map marker with a STABLE, app-level id (nest id / beach id). [isBeach] picks the icon
 * (teal dot for beaches, red pin for nests) so beaches read clearly, matching the iOS map.
 */
data class MapMarker(
    val id: String,
    val lat: Double,
    val lng: Double,
    val isBeach: Boolean = false,
    val label: String = "",
)

/**
 * Real OSM / MapLibre map, rendered natively per platform.
 *
 * @param points  markers to draw. Camera centers on their average; if empty -> Gazipaşa (~36.27, 32.31).
 * @param onClick invoked with the tapped marker's [MapMarker.id] (beaches ignored by the caller).
 *
 * Default values are declared ONLY here (expect); actuals must NOT repeat them.
 */
@Composable
expect fun OsmMap(
    modifier: Modifier = Modifier,
    points: List<MapMarker>,
    onClick: (markerId: String) -> Unit,
)
