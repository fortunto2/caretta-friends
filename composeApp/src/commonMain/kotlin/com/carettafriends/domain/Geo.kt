package com.carettafriends.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle distance in metres between two points (haversine). */
fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val r = 6_371_000.0
    val dLat = (b.lat - a.lat) * PI / 180.0
    val dLng = (b.lng - a.lng) * PI / 180.0
    val la1 = a.lat * PI / 180.0
    val la2 = b.lat * PI / 180.0
    val h = sin(dLat / 2).pow(2) + cos(la1) * cos(la2) * sin(dLng / 2).pow(2)
    return 2 * r * asin(min(1.0, sqrt(h)))
}

/** Nearest beach to a point + its distance in metres, or null if there are no beaches. */
fun nearestBeach(point: GeoPoint, beaches: List<Beach>): Pair<Beach, Double>? =
    beaches.map { it to distanceMeters(point, it.center) }.minByOrNull { it.second }

/** Short human distance label ("140 m" / "1.2 km"). */
fun distanceLabel(meters: Double): String =
    if (meters < 950) "${meters.toInt()} m" else "${((meters / 100).toInt()) / 10.0} km"
