package com.carettafriends.domain

/**
 * Türkiye's 21 official sea-turtle nesting beaches, protected under national legislation
 * (Circular 2009/10; Special Environmental Protection Areas of the Ministry of Environment).
 * Night access is banned on these beaches in the nesting season. A discovered beach is flagged
 * "protected" when it lies within ~[PROTECTED_RADIUS_M] of one of these official areas — so the
 * flag is set AUTOMATICALLY for any coastal city, not hand-entered per beach.
 *
 * West → east. Coordinates are approximate area centres (good enough for proximity matching).
 * Source: Turkey Circular 2009/10 (21 protected turtle nesting beaches), goturkiye.com, MEDASSET.
 */
data class ProtectedArea(val name: String, val lat: Double, val lng: Double)

val TURKEY_PROTECTED_BEACHES: List<ProtectedArea> = listOf(
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
fun protectedAreaFor(point: GeoPoint): ProtectedArea? =
    TURKEY_PROTECTED_BEACHES.firstOrNull { distanceMeters(point, GeoPoint(it.lat, it.lng)) <= PROTECTED_RADIUS_M }
