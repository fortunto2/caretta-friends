package com.carettafriends.data

import com.carettafriends.domain.DEFAULT_PROTECTED_AREAS
import com.carettafriends.domain.ProtectedArea
import com.carettafriends.resources.Res
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ProtectedFile(val countries: List<ProtectedCountry> = emptyList())

@Serializable
private data class ProtectedCountry(val code: String = "", val name: String = "", val beaches: List<AreaEntry> = emptyList())

@Serializable
private data class AreaEntry(val name: String, val lat: Double, val lng: Double)

private val pbJson = Json { ignoreUnknownKeys = true }

/**
 * Load the baked protected-beaches catalogue (`files/data/protected-beaches.json`), organised by
 * country so Greece / Cyprus etc. are added by editing data. Falls back to the built-in list.
 */
suspend fun loadProtectedAreas(): List<ProtectedArea> = runCatching {
    val text = Res.readBytes("files/data/protected-beaches.json").decodeToString()
    val file = pbJson.decodeFromString<ProtectedFile>(text)
    file.countries.flatMap { c -> c.beaches.map { ProtectedArea(it.name, it.lat, it.lng, c.code) } }
        .ifEmpty { DEFAULT_PROTECTED_AREAS }
}.getOrDefault(DEFAULT_PROTECTED_AREAS)
