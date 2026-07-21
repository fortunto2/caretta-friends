package com.carettafriends.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A map marker with a STABLE, app-level id (e.g. nest id / marker id / beach id).
 * SymbolManager assigns its own Long id per symbol; we map that back to this id on click.
 */
data class MapMarker(
    val id: String,
    val lat: Double,
    val lng: Double,
)

/**
 * Real OSM / MapLibre map, rendered natively per platform.
 *
 * @param points  markers to draw. Camera centers on their average; if empty -> Gazipaşa (~36.27, 32.31).
 * @param onClick invoked with the tapped marker's [MapMarker.id].
 *
 * NOTE: default values are declared ONLY here (expect). Actuals must NOT repeat them.
 */
@Composable
expect fun OsmMap(
    modifier: Modifier = Modifier,
    points: List<MapMarker>,
    onClick: (markerId: String) -> Unit,
)