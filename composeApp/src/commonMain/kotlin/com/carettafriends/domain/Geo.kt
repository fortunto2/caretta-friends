package com.carettafriends.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
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

/**
 * GPS drift, plus the slack an OSM outline needs: it traces the wet sand, while turtles nest at the
 * back of the beach and volunteers stand a few metres off when they photograph.
 */
const val ON_BEACH_TOLERANCE_M = 150.0

/** A beach with no outline (hand-seeded, not from OSM) is treated as a disc around its centre. */
const val BEACH_DISC_RADIUS_M = 400.0

/**
 * Metres from a point to the edge of a beach outline — 0 when the point is on the sand.
 *
 * The ring is projected flat around the point first: an OSM beach is a few hundred metres across,
 * a scale at which the error of dropping the sphere is centimetres, and it turns both the
 * containment test and the edge distance into plain 2-D arithmetic.
 */
fun metersToRing(point: GeoPoint, ring: List<GeoPoint>): Double {
    if (ring.size < 3) return Double.MAX_VALUE
    val perLat = 111_320.0
    val perLng = 111_320.0 * cos(point.lat * PI / 180.0)
    var inside = false
    var nearest = Double.MAX_VALUE
    for (i in ring.indices) {
        val a = ring[i]
        val b = ring[(i + 1) % ring.size]
        val ax = (a.lng - point.lng) * perLng
        val ay = (a.lat - point.lat) * perLat
        val bx = (b.lng - point.lng) * perLng
        val by = (b.lat - point.lat) * perLat
        // Ray cast from the point along +x: an odd number of crossings means it's inside.
        if ((ay > 0.0) != (by > 0.0) && ax + (-ay / (by - ay)) * (bx - ax) > 0.0) inside = !inside
        // Distance to the segment a→b, clamped to its ends.
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else (((-ax) * dx + (-ay) * dy) / len2).coerceIn(0.0, 1.0)
        val cx = ax + t * dx
        val cy = ay + t * dy
        nearest = min(nearest, sqrt(cx * cx + cy * cy))
    }
    return if (inside) 0.0 else nearest
}

/**
 * How close to the sea a nest has to be.
 *
 * A loggerhead crawls up the beach and digs above the high-water line — tens of metres from the
 * water, not hundreds. The allowance covers wide beaches, the dune path a volunteer photographs
 * from, and phone GPS scatter, while still ruling out a photo taken in town.
 */
const val SHORE_TOLERANCE_M = 250.0

/** Metres from a point to the nearest shoreline segment, or null when we hold no coastline yet. */
fun metersToShore(point: GeoPoint, shoreline: List<List<GeoPoint>>): Double? {
    if (shoreline.isEmpty()) return null
    val perLat = 111_320.0
    val perLng = 111_320.0 * cos(point.lat * PI / 180.0)
    var nearest = Double.MAX_VALUE
    for (line in shoreline) {
        for (i in 0 until line.size - 1) {
            val a = line[i]
            val b = line[i + 1]
            val ax = (a.lng - point.lng) * perLng
            val ay = (a.lat - point.lat) * perLat
            val bx = (b.lng - point.lng) * perLng
            val by = (b.lat - point.lat) * perLat
            val dx = bx - ax
            val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else (((-ax) * dx + (-ay) * dy) / len2).coerceIn(0.0, 1.0)
            val cx = ax + t * dx
            val cy = ay + t * dy
            nearest = min(nearest, sqrt(cx * cx + cy * cy))
        }
    }
    return nearest.takeIf { it < Double.MAX_VALUE }
}

/**
 * Could a turtle have nested here? Metres to the nearest sand or sea, or null when we can't tell.
 *
 * Null is the answer whenever we lack the geometry to judge — no coastline cached, no beach
 * outlines — and it means "let it through". A volunteer standing on a beach with a fresh nest must
 * never be refused because our cache is empty; the check exists to catch photos taken in a living
 * room, and those only need catching when we actually know where the sea is.
 */
fun metersFromNestingGround(
    point: GeoPoint,
    beaches: List<Beach>,
    shoreline: List<List<GeoPoint>>,
): Double? {
    val toShore = metersToShore(point, shoreline)
    if (toShore != null && toShore <= SHORE_TOLERANCE_M) return 0.0
    if (beachAt(point, beaches) != null) return 0.0
    val toSand = beaches.filter { it.polygon.size >= 3 }.minOfOrNull { metersToRing(point, it.polygon) }
    // Whichever is nearer — the point is that far from anywhere a turtle could have dug.
    return listOfNotNull(toShore, toSand).minOrNull()
}

/** How far outside this beach the point falls; 0 means it is on it. */
fun metersOffBeach(point: GeoPoint, beach: Beach): Double =
    if (beach.polygon.size >= 3) {
        metersToRing(point, beach.polygon)
    } else {
        max(0.0, distanceMeters(point, beach.center) - BEACH_DISC_RADIUS_M)
    }

/**
 * The beach a point actually sits on, or null when it sits on none.
 *
 * Ranked by distance to the outline rather than to the centre — two beaches here share a shoreline,
 * and the centre of the longer one can be nearer than the beach the volunteer is standing on.
 */
fun beachAt(
    point: GeoPoint,
    beaches: List<Beach>,
    tolerance: Double = ON_BEACH_TOLERANCE_M,
): Pair<Beach, Double>? =
    beaches.map { it to metersOffBeach(point, it) }
        .minByOrNull { it.second }
        ?.takeIf { it.second <= tolerance }

/** Short human distance label ("140 m" / "1.2 km"). */
fun distanceLabel(meters: Double): String =
    if (meters < 950) "${meters.toInt()} m" else "${((meters / 100).toInt()) / 10.0} km"
