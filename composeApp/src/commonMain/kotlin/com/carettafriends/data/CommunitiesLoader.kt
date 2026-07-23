package com.carettafriends.data

import com.carettafriends.domain.Community
import com.carettafriends.domain.CommunityKind
import com.carettafriends.domain.GeoPoint
import com.carettafriends.resources.Res
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class StubFile(val communities: List<StubEntry> = emptyList())

@Serializable
private data class StubEntry(
    val id: String,
    val name: String,
    val tagline: String = "",
    val description: String = "",
    val kind: String = "COMMUNITY",
    val affiliation: String = "",
    val website: String = "",
    val instagram: String = "",
    val phone: String = "",
    val email: String = "",
    val lat: Double,
    val lng: Double,
    val nearArea: String = "",
    val taglineRu: String = "",
    val taglineTr: String = "",
    val descriptionRu: String = "",
    val descriptionTr: String = "",
)

private val cJson = Json { ignoreUnknownKeys = true }

/**
 * Load the baked STUB communities (`files/data/communities.json`) — real local conservation groups
 * near the protected beaches that aren't on the app yet (no admin). Shown as orange squares so people
 * nearby can reach them directly; their admins can claim the community later. Empty list on failure.
 */
suspend fun loadStubCommunities(): List<Community> = runCatching {
    val text = Res.readBytes("files/data/communities.json").decodeToString()
    cJson.decodeFromString<StubFile>(text).communities.map {
        Community(
            id = it.id,
            name = it.name,
            tagline = it.tagline,
            description = it.description,
            websiteUrl = it.website,
            whatsappUrl = "",
            instagramUrl = it.instagram,
            center = GeoPoint(it.lat, it.lng),
            phone = it.phone,
            email = it.email,
            claimed = false,
            nearArea = it.nearArea,
            kind = runCatching { CommunityKind.valueOf(it.kind.uppercase()) }.getOrDefault(CommunityKind.COMMUNITY),
            affiliation = it.affiliation,
            taglineRu = it.taglineRu,
            taglineTr = it.taglineTr,
            descriptionRu = it.descriptionRu,
            descriptionTr = it.descriptionTr,
        )
    }
}.getOrDefault(emptyList())
