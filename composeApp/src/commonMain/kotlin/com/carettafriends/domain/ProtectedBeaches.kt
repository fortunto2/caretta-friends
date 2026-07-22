package com.carettafriends.domain

import kotlinx.serialization.Serializable

/**
 * An official protected sea-turtle nesting area. The catalogue lives in a baked data file
 * (`files/data/protected-beaches.json`), organised by country, so new countries (Greece, Cyprus…)
 * are added by editing data — not code. Loaded at startup into AppState.protectedAreas; the list
 * below is the offline fallback used before the file loads.
 */
@Serializable
data class ProtectedArea(
    val name: String,
    val lat: Double,
    val lng: Double,
    val country: String = "TR",
)

/** Fallback = Türkiye's official protected nesting beaches (national Circular 2009/10). Coordinates
 *  snapped onto the real OSM beach centroids (west→east) so the overview dot sits on the sand. */
val DEFAULT_PROTECTED_AREAS: List<ProtectedArea> = listOf(
    ProtectedArea("Ekincik", 36.822, 28.556),
    ProtectedArea("Dalyan / İztuzu", 36.7544, 28.6333),
    ProtectedArea("Dalaman", 36.6862, 28.7871),
    ProtectedArea("Fethiye / Çalış", 36.6519, 29.1028),
    ProtectedArea("Patara", 36.2713, 29.2897),
    ProtectedArea("Kale (Demre)", 36.23, 29.96),
    ProtectedArea("Kumluca", 36.3046, 30.3111),
    ProtectedArea("Çıralı", 36.4086, 30.4815),
    ProtectedArea("Tekirova", 36.51, 30.51),
    ProtectedArea("Belek", 36.851, 31.055),
    ProtectedArea("Kızılot", 36.6352, 31.7485),
    ProtectedArea("Demirtaş", 36.432, 32.1476),
    ProtectedArea("Gazipaşa", 36.256, 32.29),
    ProtectedArea("Anamur", 36.018, 32.803),
    ProtectedArea("Göksu Delta", 36.318, 33.982),
    ProtectedArea("Alata (Erdemli)", 36.601, 34.303),
    ProtectedArea("Kazanlı", 36.81, 34.77),
    ProtectedArea("Akyatan", 36.620, 35.280),
    ProtectedArea("Yumurtalık", 36.770, 35.790),
    ProtectedArea("Samandağ", 36.082, 35.950),
)

private const val PROTECTED_RADIUS_M = 15_000.0

/** The official protected area a point falls within (~15 km), or null. */
fun protectedAreaFor(point: GeoPoint, areas: List<ProtectedArea>): ProtectedArea? =
    areas.firstOrNull { distanceMeters(point, GeoPoint(it.lat, it.lng)) <= PROTECTED_RADIUS_M }
