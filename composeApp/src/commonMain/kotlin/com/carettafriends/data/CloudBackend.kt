package com.carettafriends.data

import com.carettafriends.domain.Nest
import com.carettafriends.domain.Patrol
import com.carettafriends.domain.SimpleMarker
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Cloud sync backend behind an interface so the DB/storage layer can be swapped later
 * (Supabase now → Cloudflare later) without touching the app. Auth stays on Supabase.
 */
interface CloudBackend {
    suspend fun pushNest(nest: Nest)
    suspend fun pushMarker(marker: SimpleMarker)
    suspend fun pushPatrol(patrol: Patrol)
    suspend fun pullNests(): List<Nest>
    suspend fun pullMarkers(): List<SimpleMarker>
}

private val cloudJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
private data class NestRow(
    val id: String,
    val code: String,
    @SerialName("beach_id") val beachId: String,
    val lat: Double,
    val lng: Double,
    val species: String,
    @SerialName("is_nest") val isNest: Boolean,
    val confidence: String,
    val status: String,
    @SerialName("found_date") val foundDate: String,
    @SerialName("created_by") val createdBy: String,
    val payload: JsonElement,
)

@Serializable
private data class MarkerRow(
    val id: String,
    val type: String,
    val lat: Double,
    val lng: Double,
    val note: String,
    val payload: JsonElement,
)

@Serializable
private data class PatrolRow(
    val id: String,
    @SerialName("beach_id") val beachId: String,
    @SerialName("distance_m") val distanceM: Int,
    @SerialName("by_name") val byName: String,
    val payload: JsonElement,
)

/** Supabase (Postgrest) implementation of [CloudBackend]. */
class SupabaseCloud : CloudBackend {
    private val client = createSupabaseClient(SupabaseConfig.URL, SupabaseConfig.ANON_KEY) {
        install(Postgrest)
    }

    override suspend fun pushNest(nest: Nest) {
        client.from("nests").upsert(
            NestRow(
                id = nest.id,
                code = nest.code,
                beachId = nest.beachId,
                lat = nest.point.lat,
                lng = nest.point.lng,
                species = nest.species.name,
                isNest = nest.isNest,
                confidence = nest.confidence.name,
                status = nest.status.name,
                foundDate = nest.foundDate.toString(),
                createdBy = nest.foundBy,
                payload = cloudJson.encodeToJsonElement(Nest.serializer(), nest),
            ),
        )
    }

    override suspend fun pushMarker(marker: SimpleMarker) {
        client.from("markers").upsert(
            MarkerRow(
                id = marker.id,
                type = marker.type.name,
                lat = marker.point.lat,
                lng = marker.point.lng,
                note = marker.note,
                payload = cloudJson.encodeToJsonElement(SimpleMarker.serializer(), marker),
            ),
        )
    }

    override suspend fun pushPatrol(patrol: Patrol) {
        client.from("patrols").upsert(
            PatrolRow(
                id = patrol.id,
                beachId = patrol.beachId,
                distanceM = patrol.distanceMeters,
                byName = patrol.by,
                payload = cloudJson.encodeToJsonElement(Patrol.serializer(), patrol),
            ),
        )
    }

    override suspend fun pullNests(): List<Nest> =
        client.from("nests").select().decodeList<NestRow>()
            .mapNotNull { runCatching { cloudJson.decodeFromJsonElement(Nest.serializer(), it.payload) }.getOrNull() }

    override suspend fun pullMarkers(): List<SimpleMarker> =
        client.from("markers").select().decodeList<MarkerRow>()
            .mapNotNull { runCatching { cloudJson.decodeFromJsonElement(SimpleMarker.serializer(), it.payload) }.getOrNull() }
}
