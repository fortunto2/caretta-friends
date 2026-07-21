package com.carettafriends.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** Enums mirror data-model.md (export-compatible with FWC / seaturtle.org). */
enum class MarkerType { NEST, LANDMARK, TRASH, PREDATOR_SIGN, CRAWL, OBSTACLE, OTHER }
enum class NestStatus { INCUBATING, HATCHING, HATCHED, EXCAVATED, LOST, PREDATED, WASHED_OVER, POACHED, FALSE_CRAWL }
enum class ObsCondition { OK, PREDATED, WASHED_OVER, DISTURBED, POACHED, HATCHING, HATCHED, RELOCATED, OTHER }
enum class UpdateKind { FOUND, OBSERVATION, STATUS_CHANGE, COMMENT, HATCHED, RELOCATED, EXCAVATED }
enum class Visibility { PUBLIC, PRIVATE }
enum class Species { CARETTA_CARETTA, CHELONIA_MYDAS, OTHER, UNKNOWN }
enum class LocationSource { PHOTO_EXIF, DEVICE_GPS, MANUAL_MAP, NONE }
enum class NestConfidence { CONFIRMED, UNCONFIRMED }
enum class SunExposure { FULL_SUN, PARTIAL, SHADE }
enum class PhotoSource { CAMERA, GALLERY }
enum class MemberRole { ADMIN, BEACH_LEADER, VOLUNTEER }

@Serializable
data class GeoPoint(val lat: Double, val lng: Double)

@Serializable
data class Community(
    val id: String,
    val name: String,
    val tagline: String,
    val websiteUrl: String,
    val whatsappUrl: String,
    val instagramUrl: String,
)

@Serializable
data class Beach(
    val id: String,
    val communityId: String,
    val name: String,
    val city: String,
    val center: GeoPoint,
    val leaderName: String? = null,
    val leaderAvatar: String = "🐢",
)

@Serializable
data class PhotoRef(
    val id: String,
    val source: PhotoSource,
    val localUri: String? = null,      // content:// or file path (Android)
    val placeholder: String = "🥚", // emoji stand-in when no image
    val exifLat: Double? = null,
    val exifLng: Double? = null,
)

/** A simple non-nest marker (landmark / trash / ...). */
@Serializable
data class SimpleMarker(
    val id: String,
    val type: MarkerType,
    val point: GeoPoint,
    val note: String = "",
    val beachId: String? = null,
    val createdBy: String = "you",
)

/** One entry in a nest's unified timeline (observation | comment | status change | hatch). */
@Serializable
data class NestUpdate(
    val id: String,
    val kind: UpdateKind,
    val condition: ObsCondition? = null,
    val newStatus: NestStatus? = null,
    val body: String = "",
    val author: String = "you",
    val createdEpochMillis: Long = 0L,
    val dateLabel: String = "",
)

/** FWC excavation counts → auto hatch / emergence success. */
@Serializable
data class Excavation(
    val shells: Int = 0,          // hatched (empty shells)
    val unhatched: Int = 0,       // whole eggs
    val pipped: Int = 0,          // pipped live + dead in egg
    val inNest: Int = 0,          // hatched but stuck in chamber
    val helpedOut: Int = 0,       // rescued alive
) {
    val eggsTotal: Int get() = shells + unhatched + pipped
    val hatchSuccessPct: Int?
        get() = if (eggsTotal > 0) (100.0 * shells / eggsTotal).toInt() else null
    val emergenceSuccessPct: Int?
        get() = if (eggsTotal > 0) (100.0 * (shells - inNest) / eggsTotal).toInt().coerceAtLeast(0) else null
    val hatchlingsToSea: Int get() = (shells - inNest).coerceAtLeast(0) + helpedOut
}

@Serializable
data class TemperatureReading(
    val recordedLabel: String,
    val source: String,        // weather_api / water / air_shade / logger_*
    val valueC: Double,
)

/** Rich nest aggregate (projection of marker + nest + updates + excavation for V1 UI). */
@Serializable
data class Nest(
    val id: String,
    val code: String,                       // GZP-24
    val beachId: String,
    val point: GeoPoint,
    val species: Species = Species.CARETTA_CARETTA,
    val isNest: Boolean = true,             // false = false crawl
    val confidence: NestConfidence = NestConfidence.UNCONFIRMED,
    val visibility: Visibility = Visibility.PUBLIC,
    val foundDate: LocalDate,
    val clutchSizeEst: Int? = null,
    val cageInstalled: Boolean = false,
    val exposure: SunExposure? = null,
    val locationSource: LocationSource = LocationSource.DEVICE_GPS,
    val incubationDaysEst: Int = 55,
    val status: NestStatus = NestStatus.INCUBATING,
    val predictedFemaleLow: Int? = null,    // TSD range
    val predictedFemaleHigh: Int? = null,
    val airTempC: Double? = null,
    val rainMm7d: Double? = null,
    val photos: List<PhotoRef> = emptyList(),
    val updates: List<NestUpdate> = emptyList(),
    val excavation: Excavation? = null,
    val temps: List<TemperatureReading> = emptyList(),
    val foundBy: String = "you",
)

@Serializable
data class Fact(val id: String, val emoji: String, val title: String)

@Serializable
data class GuideArticle(val id: String, val emoji: String, val title: String, val sourceUrl: String)

@Serializable
data class Badge(val code: String, val emoji: String, val name: String, val earned: Boolean)

@Serializable
data class Patrol(val id: String, val beachId: String, val distanceMeters: Int, val startedLabel: String, val by: String)

@Serializable
data class Profile(
    val displayName: String = "You",
    val avatar: String = "🐢",
    val role: String = "Volunteer",
    val language: String = "en",
    val hatchlingsReached: Int = 0,
    val streakDays: Int = 0,
    val kmWalked: Double = 0.0,
    val patrols: Int = 0,
)

/** Whole app state (single source of truth for the in-memory repository). */
@Serializable
data class AppState(
    val community: Community,
    val beaches: List<Beach>,
    val nests: List<Nest>,
    val markers: List<SimpleMarker>,
    val patrols: List<Patrol>,
    val facts: List<Fact>,
    val guide: List<GuideArticle>,
    val badges: List<Badge>,
    val profile: Profile,
) {
    fun beach(id: String): Beach? = beaches.firstOrNull { it.id == id }
    fun nest(id: String): Nest? = nests.firstOrNull { it.id == id }
}
