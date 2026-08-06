package com.carettafriends.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carettafriends.domain.GeoPoint

/**
 * A map marker with a STABLE, app-level id (nest id / beach id). Nests render as a dot; beaches
 * render as a highlighted [polygon] (their OSM sand outline) when available, else a dot.
 */
data class MapMarker(
    val id: String,
    val lat: Double,
    val lng: Double,
    val isBeach: Boolean = false,
    val label: String = "",
    val polygon: List<GeoPoint> = emptyList(),
    /** Protected nesting beach → green highlight; otherwise amber/yellow. */
    val protected: Boolean = false,
    /** A community hub (its registered city) → orange dot, tap opens the community. */
    val isCommunity: Boolean = false,
    /** A rule-violation report → red dot (shown only under the Violations filter). */
    val isViolation: Boolean = false,
    /** Nest lifecycle phase → dot colour (incubating/soon/emerging/excavated/removed). See nestMapPhase. */
    val phase: String = "",
)

/**
 * Real OSM / MapLibre map, rendered natively per platform.
 *
 * @param points  markers to draw. Camera centers on their average; if empty -> Gazipaşa (~36.27, 32.31).
 * @param onClick invoked with the tapped marker's [MapMarker.id] (beaches ignored by the caller).
 * @param focus   one-shot request to animate the camera onto this point (e.g. a nest's geo card).
 *                Null = no pending focus. After centring, the actual calls [onFocusConsumed].
 * @param onFocusConsumed invoked once the camera has been moved to [focus] (clears the pending focus).
 * @param recenterTick bump this to centre the camera on the volunteer's own position ("locate me").
 *
 * Default values are declared ONLY here (expect); actuals must NOT repeat them.
 */
@Composable
expect fun OsmMap(
    modifier: Modifier = Modifier,
    points: List<MapMarker>,
    onClick: (markerId: String) -> Unit,
    onBeachTap: (beachId: String) -> Unit = {},
    onCommunityTap: (communityId: String) -> Unit = {},
    focus: GeoPoint? = null,
    onFocusConsumed: () -> Unit = {},
    recenterTick: Int = 0,
)
