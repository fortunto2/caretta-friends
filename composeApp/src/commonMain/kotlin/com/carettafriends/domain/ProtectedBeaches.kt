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

/** Fallback = Türkiye's 21 official protected nesting beaches (national Circular 2009/10). */
val DEFAULT_PROTECTED_AREAS: List<ProtectedArea> = listOf(
    ProtectedArea("Ekincik", 36.822, 28.556),
    ProtectedArea("Dalyan / İztuzu", 36.761, 28.613),
    ProtectedArea("Dalaman", 36.693, 28.804),
    ProtectedArea("Fethiye / Çalış", 36.645, 29.108),
    ProtectedArea("Patara", 36.272, 29.312),
    ProtectedArea("Kale (Demre)", 36.200, 29.850),
    ProtectedArea("Kumluca", 36.300, 30.300),
    ProtectedArea("Çıralı", 36.418, 30.474),
    ProtectedArea("Tekirova", 36.510, 30.510),
    ProtectedArea("Belek", 36.851, 31.055),
    ProtectedArea("Kızılot", 36.657, 31.752),
    ProtectedArea("Demirtaş", 36.443, 32.160),
    ProtectedArea("Gazipaşa", 36.267, 32.300),
    ProtectedArea("Anamur", 36.018, 32.803),
    ProtectedArea("Göksu Delta", 36.318, 33.982),
    ProtectedArea("Alata (Erdemli)", 36.601, 34.303),
    ProtectedArea("Kazanlı", 36.833, 34.748),
    ProtectedArea("Akyatan", 36.620, 35.280),
    ProtectedArea("Yumurtalık", 36.770, 35.790),
    ProtectedArea("Samandağ", 36.082, 35.950),
)

private const val PROTECTED_RADIUS_M = 15_000.0

/** The official protected area a point falls within (~15 km), or null. */
fun protectedAreaFor(point: GeoPoint, areas: List<ProtectedArea>): ProtectedArea? =
    areas.firstOrNull { distanceMeters(point, GeoPoint(it.lat, it.lng)) <= PROTECTED_RADIUS_M }
