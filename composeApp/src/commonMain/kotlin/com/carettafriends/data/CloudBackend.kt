package com.carettafriends.data

import com.carettafriends.domain.Beach
import com.carettafriends.domain.Community
import com.carettafriends.domain.Nest
import com.carettafriends.domain.Patrol
import com.carettafriends.domain.SimpleMarker
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Cloud sync backend behind an interface so the DB/storage layer can be swapped later
 * (Supabase now → Cloudflare later) without touching the app. Auth stays on Supabase.
 *
 * Relational (hybrid) model: community → beach are relational parents; nest/marker/patrol are
 * aggregate roots with FOREIGN KEYS to their parents. The nest timeline (updates/excavation/temps)
 * stays in the JSONB payload (one aggregate = one document). To keep FK sync safe, parents are
 * pushed BEFORE children (see [CarettaRepository.syncOnStart]) and every write is an idempotent upsert.
 */
interface CloudBackend {
    suspend fun pushCommunity(community: Community)
    suspend fun pushBeach(beach: Beach)
    suspend fun pushNest(nest: Nest, communityId: String, ownerId: String?)
    suspend fun pushMarker(marker: SimpleMarker, ownerId: String?)
    suspend fun pushPatrol(patrol: Patrol, ownerId: String?)
    suspend fun pullBeaches(): List<Beach>
    suspend fun pullNests(): List<Nest>
    suspend fun pullMarkers(): List<SimpleMarker>
}

private val cloudJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
private data class CommunityRow(
    val id: String,
    val slug: String,
    val name: String,
    val tagline: String,
    @SerialName("website_url") val websiteUrl: String,
    @SerialName("whatsapp_url") val whatsappUrl: String,
    @SerialName("instagram_url") val instagramUrl: String,
    val payload: JsonElement,
)

@Serializable
private data class BeachRow(
    val id: String,
    @SerialName("community_id") val communityId: String,
    val name: String,
    val city: String,
    val lat: Double,
    val lng: Double,
    @SerialName("leader_name") val leaderName: String? = null,
    @SerialName("leader_avatar") val leaderAvatar: String = "🐢",
    val payload: JsonElement,
)

@Serializable
private data class NestRow(
    val id: String,
    val code: String,
    @SerialName("beach_id") val beachId: String,
    @SerialName("community_id") val communityId: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
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
    @SerialName("beach_id") val beachId: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    val lat: Double,
    val lng: Double,
    val note: String,
    val payload: JsonElement,
)

@Serializable
private data class PatrolRow(
    val id: String,
    @SerialName("beach_id") val beachId: String,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("distance_m") val distanceM: Int,
    @SerialName("by_name") val byName: String,
    val payload: JsonElement,
)

/**
 * PostgREST-over-ktor implementation of [CloudBackend]. No supabase-kt: raw REST keeps the client
 * backend-agnostic (swap [base] + headers for Cloudflare later) and avoids the supabase-kt/Kotlin
 * ABI coupling that blocks the iOS Native target. Auth (V2) stays Supabase, layered separately.
 */
class SupabaseCloud(private val auth: AuthBackend) : CloudBackend {
    private val base = SupabaseConfig.URL.trimEnd('/') + "/rest/v1"
    private val http = HttpClient {
        install(ContentNegotiation) { json(cloudJson) }
        install(DefaultRequest) { header("apikey", SupabaseConfig.ANON_KEY) }
    }

    /** Authenticated as the volunteer (JWT) when signed in, else anon key (RLS decides what that can do). */
    private suspend fun bearer(): String = "Bearer ${auth.accessToken() ?: SupabaseConfig.ANON_KEY}"

    /** PostgREST upsert: POST a row array; merge-duplicates resolves conflicts on the primary key. */
    private suspend inline fun <reified T> upsert(table: String, row: T) {
        http.post("$base/$table") {
            header("Authorization", bearer())
            header("Prefer", "resolution=merge-duplicates,return=minimal")
            contentType(ContentType.Application.Json)
            setBody(listOf(row))
        }
    }

    /** PostgREST select of live rows only (soft-deleted tombstones filtered server-side). */
    private suspend inline fun <reified T> selectLive(table: String): List<T> =
        http.get("$base/$table?select=*&deleted_at=is.null") { header("Authorization", bearer()) }.body()

    override suspend fun pushCommunity(community: Community) {
        upsert(
            "community",
            CommunityRow(
                id = community.id,
                slug = community.id,
                name = community.name,
                tagline = community.tagline,
                websiteUrl = community.websiteUrl,
                whatsappUrl = community.whatsappUrl,
                instagramUrl = community.instagramUrl,
                payload = cloudJson.encodeToJsonElement(Community.serializer(), community),
            ),
        )
    }

    override suspend fun pushBeach(beach: Beach) {
        upsert(
            "beach",
            BeachRow(
                id = beach.id,
                communityId = beach.communityId,
                name = beach.name,
                city = beach.city,
                lat = beach.center.lat,
                lng = beach.center.lng,
                leaderName = beach.leaderName,
                leaderAvatar = beach.leaderAvatar,
                payload = cloudJson.encodeToJsonElement(Beach.serializer(), beach),
            ),
        )
    }

    override suspend fun pushNest(nest: Nest, communityId: String, ownerId: String?) {
        upsert(
            "nests",
            NestRow(
                id = nest.id,
                code = nest.code,
                beachId = nest.beachId,
                communityId = communityId,
                ownerId = ownerId,
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

    override suspend fun pushMarker(marker: SimpleMarker, ownerId: String?) {
        upsert(
            "markers",
            MarkerRow(
                id = marker.id,
                type = marker.type.name,
                beachId = marker.beachId,
                ownerId = ownerId,
                lat = marker.point.lat,
                lng = marker.point.lng,
                note = marker.note,
                payload = cloudJson.encodeToJsonElement(SimpleMarker.serializer(), marker),
            ),
        )
    }

    override suspend fun pushPatrol(patrol: Patrol, ownerId: String?) {
        upsert(
            "patrols",
            PatrolRow(
                id = patrol.id,
                beachId = patrol.beachId,
                ownerId = ownerId,
                distanceM = patrol.distanceMeters,
                byName = patrol.by,
                payload = cloudJson.encodeToJsonElement(Patrol.serializer(), patrol),
            ),
        )
    }

    override suspend fun pullBeaches(): List<Beach> =
        selectLive<BeachRow>("beach")
            .mapNotNull { runCatching { cloudJson.decodeFromJsonElement(Beach.serializer(), it.payload) }.getOrNull() }

    override suspend fun pullNests(): List<Nest> =
        selectLive<NestRow>("nests")
            .mapNotNull { runCatching { cloudJson.decodeFromJsonElement(Nest.serializer(), it.payload) }.getOrNull() }

    override suspend fun pullMarkers(): List<SimpleMarker> =
        selectLive<MarkerRow>("markers")
            .mapNotNull { runCatching { cloudJson.decodeFromJsonElement(SimpleMarker.serializer(), it.payload) }.getOrNull() }
}
