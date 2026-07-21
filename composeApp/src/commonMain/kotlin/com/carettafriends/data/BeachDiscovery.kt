package com.carettafriends.data

import com.carettafriends.domain.Beach
import com.carettafriends.domain.GeoPoint
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

    /** Named beaches within [radiusM] of (lat,lng), as [Beach] rows tied to [communityId]. */
    suspend fun nearby(lat: Double, lng: Double, radiusM: Int, communityId: String): List<Beach> {
        // nwr = node+way+relation (some beaches, e.g. Koru, are relations). POST the raw Overpass QL
        // as a text body; read the response as text and parse manually (no ContentNegotiation, which
        // would otherwise JSON-encode the query string on the way out).
        val query = "[out:json][timeout:20];nwr[\"natural\"=\"beach\"](around:$radiusM,$lat,$lng);out center tags;"
        val body = http.post("https://overpass-api.de/api/interpreter") {
            header("User-Agent", "CarettaFriends/1.0 (sea-turtle nest monitoring)")
            contentType(ContentType.Text.Plain)
            setBody(query)
        }.bodyAsText()
        val resp = json.decodeFromString<OverpassResp>(body)
        return resp.elements.mapNotNull { el ->
            val la = el.lat ?: el.center?.lat ?: return@mapNotNull null
            val lo = el.lon ?: el.center?.lon ?: return@mapNotNull null
            // Named beaches only — unnamed OSM polygons are noise for a volunteer picker.
            val name = el.tags["name"] ?: el.tags["name:tr"] ?: return@mapNotNull null
            Beach(
                id = "osm-${el.type}-${el.id}",   // stable across refetches → idempotent upsert
                communityId = communityId,
                name = name,
                city = el.tags["addr:city"] ?: "",
                center = GeoPoint(la, lo),
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
    val center: OverpassCenter? = null,
    val tags: Map<String, String> = emptyMap(),
)

@Serializable
private data class OverpassCenter(val lat: Double, val lon: Double)
