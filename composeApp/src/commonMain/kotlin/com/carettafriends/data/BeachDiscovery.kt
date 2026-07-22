package com.carettafriends.data

import com.carettafriends.domain.Beach
import com.carettafriends.domain.GeoPoint
import com.carettafriends.domain.ProtectedArea
import com.carettafriends.domain.protectedAreaFor
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Auto-discovers beaches near a point from OpenStreetMap (Overpass API `natural=beach`), so beaches
 * appear AUTOMATICALLY for any coastal city — no hand-entered per-city lists. Offline-first: called
 * best-effort when online; results are merged into (and cached by) the app state.
 */
class BeachDiscovery {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient {
        // Overpass computes server-side and can take 15-25s → generous socket read timeout.
        install(HttpTimeout) {
            requestTimeoutMillis = 40_000
            connectTimeoutMillis = 12_000
            socketTimeoutMillis = 35_000
        }
    }

    /** Named beaches within [radiusM] of (lat,lng), as [Beach] rows tied to [communityId].
     *  Each carries its OSM sand POLYGON (`out geom`) so the map highlights the real beach outline
     *  instead of a point pin (whose centroid can land inland). */
    suspend fun nearby(lat: Double, lng: Double, radiusM: Int, communityId: String, areas: List<ProtectedArea>): List<Beach> {
        // nwr = node+way+relation (some beaches, e.g. Koru, are relations). `out geom` returns the
        // full geometry (way node coords / relation members). POST raw QL + manual parse.
        val query = "[out:json][timeout:25];nwr[\"natural\"=\"beach\"](around:$radiusM,$lat,$lng);out geom tags;"
        val body = http.post("https://overpass-api.de/api/interpreter") {
            header("User-Agent", "CarettaFriends/1.0 (sea-turtle nest monitoring)")
            contentType(ContentType.Text.Plain)
            setBody(query)
        }.bodyAsText()
        val resp = json.decodeFromString<OverpassResp>(body)
        return resp.elements.mapNotNull { el ->
            // Named beaches only — unnamed OSM polygons are noise for a volunteer picker.
            val name = el.tags["name"] ?: el.tags["name:tr"] ?: return@mapNotNull null
            // Outline: way geometry, else concatenated relation-member geometry.
            val ring = (el.geometry.takeIf { it.isNotEmpty() } ?: el.members.flatMap { it.geometry })
                .map { GeoPoint(it.lat, it.lon) }
            val center = when {
                ring.isNotEmpty() -> GeoPoint(ring.map { it.lat }.average(), ring.map { it.lng }.average())
                el.lat != null && el.lon != null -> GeoPoint(el.lat, el.lon)
                else -> return@mapNotNull null
            }
            Beach(
                id = "osm-${el.type}-${el.id}",   // stable across refetches → idempotent upsert
                communityId = communityId,
                name = name,
                city = el.tags["addr:city"] ?: "",
                center = center,
                polygon = ring,
                protected = protectedAreaFor(center, areas) != null,
            )
        }
    }
}

@Serializable
private data class OverpassResp(val elements: List<OverpassEl> = emptyList())

@Serializable
private data class OverpassEl(
    val type: String = "",
    val id: Long = 0,
    val lat: Double? = null,
    val lon: Double? = null,
    val tags: Map<String, String> = emptyMap(),
    val geometry: List<LatLon> = emptyList(),
    val members: List<OverpassMember> = emptyList(),
)

@Serializable
private data class OverpassMember(val type: String = "", val geometry: List<LatLon> = emptyList())

@Serializable
private data class LatLon(val lat: Double, val lon: Double)
