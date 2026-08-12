package com.carettafriends.domain

import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Enums mirror data-model.md (export-compatible with FWC / seaturtle.org). */
enum class MarkerType { NEST, LANDMARK, TRASH, PREDATOR_SIGN, CRAWL, OBSTACLE, VIOLATION, OTHER }

/** A rule violation category (banned on protected nesting beaches) — kept for CİMER complaints. */
enum class ViolationKind { TENT, VEHICLE, LIGHT, NOISE, DOG, LITTER, OTHER }
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

/** What kind of group a community is — shown as a badge to tell volunteer-run groups from
 *  university centers, NGOs and official bodies. */
enum class CommunityKind { COMMUNITY, NGO, UNIVERSITY, OFFICIAL }

/** How a nest is physically protected. Reed-fence OR cage is a CHOICE — both count as "protected".
 *  NONE = just found · MARKED = stakes + reed/cane fence + warning tape + sign · CAGED = metal cage. */
enum class ProtectionLevel { NONE, MARKED, CAGED }

@Serializable
data class GeoPoint(val lat: Double, val lng: Double)

/** A photo captured by the native camera, waiting to be attached — path, its GPS fix if the phone
 *  had one, and the identity of the source image (for duplicate detection). */
data class PendingPhotoRef(val path: String, val lat: Double?, val lng: Double?, val hash: String?)

/** The stand-in names every install starts with. They identify nobody, so they must never be used
 *  to decide who a nest belongs to (that bug put strangers' nests in everyone's profile). */
private val PLACEHOLDER_NAMES = setOf("you", "volunteer", "anonymous", "гость", "gönüllü")

fun isPlaceholderName(name: String): Boolean = name.trim().lowercase() in PLACEHOLDER_NAMES

@Serializable
data class Community(
    val id: String,
    val name: String,
    val tagline: String,
    /** A short human summary: who they are, what they do, how long, general contribution. Shown on
     *  the community card. Keep it general (metrics change). */
    val description: String = "",
    val websiteUrl: String,
    val whatsappUrl: String,
    val instagramUrl: String,
    /** Public admin contact (phone / handle) for other communities to reach this one. Empty = hidden.
     *  Groundwork for connecting new communities & inter-community coordination later. */
    val adminContact: String = "",
    /** The city/town this community is registered in — shown as an orange hub square on the map.
     *  Default = Gazipaşa (our root community). */
    val center: GeoPoint = GeoPoint(36.268, 32.319),
    /** Public phone / email for local groups people can reach directly. */
    val phone: String = "",
    val email: String = "",
    /** false = a STUB: a real local conservation group we collected but that has no admin on the app
     *  yet (people nearby can contact them; their admins can claim it later). true = run on the app. */
    val claimed: Boolean = true,
    /** The protected nesting area this group works around (links a stub to its beach). */
    val nearArea: String = "",
    /** Group type (volunteer community / NGO / university / official) — shown as a badge. */
    val kind: CommunityKind = CommunityKind.COMMUNITY,
    /** Who they're affiliated with (parent university, ministry, network) — "" if independent. */
    val affiliation: String = "",
    /** The official conservation/permitting authority this community coordinates with for regulated
     *  activity (nest excavation, organized beach events). Country-specific — Türkiye = DKMP, others set
     *  their own. Empty = fall back to a generic "local conservation authority" phrase. */
    val authorityName: String = "",
    val authorityUrl: String = "",
    /** Localized tagline / description (RU + TR). Empty → falls back to the English [tagline]/[description]. */
    val taglineRu: String = "",
    val taglineTr: String = "",
    val descriptionRu: String = "",
    val descriptionTr: String = "",
) {
    /** Tagline in [lang] ("ru"/"tr"), falling back to English. */
    fun taglineFor(lang: String): String = when (lang.lowercase()) {
        "ru" -> taglineRu.ifBlank { tagline }
        "tr" -> taglineTr.ifBlank { tagline }
        else -> tagline
    }

    /** Description in [lang] ("ru"/"tr"), falling back to English. */
    fun descriptionFor(lang: String): String = when (lang.lowercase()) {
        "ru" -> descriptionRu.ifBlank { description }
        "tr" -> descriptionTr.ifBlank { description }
        else -> description
    }
}

@Serializable
data class Beach(
    val id: String,
    val communityId: String,
    val name: String,
    val city: String,
    val center: GeoPoint,
    val leaderName: String? = null,
    val leaderAvatar: String = "🐢",
    /** OSM sand outline (the yellow beach polygon) — highlighted on the map instead of a point pin. */
    val polygon: List<GeoPoint> = emptyList(),
    /** True if this beach is one of Türkiye's official protected turtle nesting beaches (auto/overridable). */
    val protected: Boolean = false,
)

@Serializable
data class PhotoRef(
    val id: String,
    val source: PhotoSource,
    val localUri: String? = null,      // content:// or file path (Android)
    val placeholder: String = "🥚", // emoji stand-in when no image
    val exifLat: Double? = null,
    val exifLng: Double? = null,
    /** Where this photo lives in cloud storage ("<uid>/<nest>/<photo>.jpg"), once uploaded. The
     *  local path is meaningless on any other device — this is what makes a nest's photo visible to
     *  the rest of the community. Null = still only on the phone that took it. */
    val remotePath: String? = null,
    /** Opaque identity of the ORIGINAL image ("md5:…" on Android, "asset:…" on iOS), so re-picking
     *  the same photo is recognized as a duplicate instead of creating a second nest. The file on
     *  disk can't be hashed for this — iOS re-encodes every import (burned-in overlay) and Android
     *  copies to a fresh path, so both give a different file each time. */
    val hash: String? = null,
)

/** A simple non-nest marker (landmark / trash / violation / ...). */
@Serializable
data class SimpleMarker(
    val id: String,
    val type: MarkerType,
    val point: GeoPoint,
    val note: String = "",
    val beachId: String? = null,
    val createdBy: String = "you",
    /** For a VIOLATION: what's banned (tents/cars/…). Ignored for other types. */
    val violationKind: ViolationKind? = null,
    /** PRIVATE violations are hidden from guests — protects volunteers from retaliation over fines. */
    val visibility: Visibility = Visibility.PUBLIC,
    /** true → the reporter's name isn't shown (anonymous violation report). */
    val anonymous: Boolean = false,
    /** When it was reported — violations fade off the map after a short window but stay in the DB. */
    val createdEpochMillis: Long = 0L,
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
    /** Stable identity of the author (auth uid) — [author] is only a display name and every install
     *  starts with the same default, so names can't tell two volunteers apart. */
    val authorUserId: String? = null,
    val createdEpochMillis: Long = 0L,
    val dateLabel: String = "",
    /** The real date this update REFERS to (back-datable — "found 3 days ago, photo arrived today").
     *  null → a live update, shown as [dateLabel]. When set, the timeline shows this date. */
    val obsDate: LocalDate? = null,
    /** A photo attached to this timeline entry — builds the nest's photo history over time. */
    val photo: PhotoRef? = null,
)

/** FWC excavation counts → auto hatch / emergence success. */
@Serializable
data class Excavation(
    val shells: Int = 0,          // hatched (empty shells)
    val unhatched: Int = 0,       // whole eggs
    val pipped: Int = 0,          // pipped live + dead in egg
    val inNest: Int = 0,          // hatched but stuck in chamber
    val helpedOut: Int = 0,       // rescued alive
    /** Official record fields for the excavation report (tutanak): date and who did it. */
    val excavatedOn: LocalDate? = null,
    val team: String = "",
) {
    /** Clutch size, per Miller (1999): every egg the nest held, whatever became of it. */
    val eggsTotal: Int get() = shells + unhatched + pipped

    /**
     * Hatchlings that never left the chamber — and never more than the number of eggs that hatched.
     *
     * A count above [shells] is a miscount at the nest, not a discovery: a hatchling in the chamber
     * came out of one of those shells. Bounding it here keeps a slip of the finger from producing a
     * negative emergence rate in an official record.
     */
    private val stuck: Int get() = inNest.coerceIn(0, shells)

    /**
     * Of the stuck ones, those dug out alive — a SUBSET of [stuck], not a separate group.
     *
     * Counted as its own number it inflates the nest: a record saying 94 shells and 3 rescued used
     * to report 97 hatchlings reaching the sea, three of them from eggs that never existed. That
     * figure feeds the volunteer's profile, the beach total and the community ranking.
     */
    private val rescued: Int get() = helpedOut.coerceIn(0, stuck)

    /** Miller's hatching success: of every egg laid, the share that hatched. */
    val hatchSuccessPct: Int? get() = percentOf(shells, eggsTotal)

    /** Miller's emergence success: hatched AND out of the nest under its own power. */
    val emergenceSuccessPct: Int? get() = percentOf(shells - stuck, eggsTotal)

    /** Reached the sea: the ones that crawled out, plus the ones we carried out alive. */
    val hatchlingsToSea: Int get() = shells - stuck + rescued

    /** True when the counts contradict each other — the volunteer is shown which one to re-check. */
    val inconsistent: Boolean get() = inNest > shells || helpedOut > inNest
}

/**
 * A percentage for a record someone signs.
 *
 * Rounded, not truncated: a volunteer who counts 2 of 3 by hand gets 67%, and an app that answers
 * 66% is an app they stop trusting — and over a season truncation drags every reported success rate
 * downwards. But rounding never reaches a perfect score it hasn't earned: 199 of 200 is 99.5%, and
 * printing "100%" would claim an egg hatched that didn't. Same at the bottom — 1 of 500 is not 0%.
 */
private fun percentOf(part: Int, whole: Int): Int? {
    if (whole <= 0) return null
    val rounded = (100.0 * part / whole).roundToInt().coerceIn(0, 100)
    return when {
        rounded == 100 && part < whole -> 99
        rounded == 0 && part > 0 -> 1
        else -> rounded
    }
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
    val protection: ProtectionLevel = ProtectionLevel.NONE,
    val exposure: SunExposure? = null,
    val locationSource: LocationSource = LocationSource.DEVICE_GPS,
    val incubationDaysEst: Int = 55,
    val status: NestStatus = NestStatus.INCUBATING,
    val predictedFemaleLow: Int? = null,    // TSD range
    val predictedFemaleHigh: Int? = null,
    val airTempC: Double? = null,
    val rainMm7d: Double? = null,
    /** Epoch-day of the last DAILY temperature appended to [temps] — drives the once-a-day TSP accrual. */
    val lastTempDay: Int = 0,
    val photos: List<PhotoRef> = emptyList(),
    val updates: List<NestUpdate> = emptyList(),
    val excavation: Excavation? = null,
    val temps: List<TemperatureReading> = emptyList(),
    val foundBy: String = "you",
    /** Who found it, as a STABLE id (Supabase auth uid = the row's owner_id). [foundBy] is a display
     *  name and defaults to the same "Volunteer" on every install, so it can't identify anyone —
     *  this is what "my nests" is filtered by. Null on rows recorded before this field existed. */
    val foundByUserId: String? = null,
    /** Client-side change clock for last-write-wins merge (scalars LWW, timeline union). Server has its own updated_at. */
    val updatedAtMillis: Long = 0L,
)

@Serializable
data class Fact(val id: String, val emoji: String, val title: String)

@Serializable
data class GuideArticle(val id: String, val emoji: String, val title: String, val sourceUrl: String)

@Serializable
data class Badge(val code: String, val emoji: String, val name: String, val earned: Boolean)

@Serializable
data class Patrol(
    val id: String,
    val beachId: String,
    val distanceMeters: Int,
    val startedLabel: String,
    val by: String,
    /** Recorded GPS breadcrumb track (Strava-style). Empty until we record real walks. */
    val track: List<GeoPoint> = emptyList(),
    val durationSec: Int = 0,
    /** Local until the volunteer chooses to publish. NEVER live-shared (safety: no live position). */
    val published: Boolean = false,
)

/** A community member (volunteer / beach leader / admin) shown on the Community screen. */
@Serializable
data class Member(
    val id: String,
    val name: String,
    val role: MemberRole,
    val avatar: String = "🐢",
    val homeBeach: String? = null,
    val note: String = "",
    /** One public link a member can share — Instagram, blog or website (tappable). Groundwork for
     *  richer contacts / inter-community messaging later; empty = nothing shown. */
    val link: String = "",
    /** Auth uid, when known — lets their nests be found by owner instead of by display name. */
    val userId: String? = null,
)

@Serializable
data class Profile(
    val displayName: String = "You",
    val avatar: String = "🐢",
    val role: String = "Volunteer",
    /** True once the volunteer typed their own name. Until then [displayName] is the shared default
     *  ("Volunteer"), which identifies nobody — the profile nudges them to set a real one. */
    val nameSet: Boolean = false,
    val language: String = "en",
    /** True once the volunteer picks a language themselves. Until then the app follows the phone's
     *  language on every launch, so changing it in iOS Settings (or Android system settings) works. */
    val languageExplicit: Boolean = false,
    /** Kept only so saved state from older builds still parses; nothing reads it. "Hatchlings
     *  reached the sea" is summed from the nests wherever it's shown, because a stored total goes
     *  stale the moment a nest is excavated on another phone or retired by a coordinator. */
    @Deprecated("Sum excavation.hatchlingsToSea over the volunteer's nests instead")
    val hatchlingsReached: Int = 0,
    val streakDays: Int = 0,
    val kmWalked: Double = 0.0,
    val patrols: Int = 0,
    /** Optional "home" beach. null = free volunteer (the majority) — patrol wherever's closest. */
    val homeBeachId: String? = null,
    val memberRole: MemberRole = MemberRole.VOLUNTEER,
    /** An experienced volunteer trusted to excavate (set by a beach leader / admin). */
    val experienced: Boolean = false,
    /** One public link the volunteer can share (Instagram / blog / website). Empty = none. */
    val link: String = "",
    /** Optional profile photo (local path) the volunteer may add later; null → emoji avatar. */
    val photoPath: String? = null,
    /** Nests this volunteer is watching (persisted). The hatch-watch retention hook — survives
     *  restarts; a local notification near the hatch window is a follow-up. */
    val watchedNestIds: Set<String> = emptySet(),
    /** First-run onboarding seen? false → show the 3-page intro once. */
    val onboarded: Boolean = false,
    /** Current auth uid (anonymous or email account). The identity everything is attributed to. */
    val userId: String? = null,
    /** EVERY uid this device has signed in as. An anonymous volunteer who later saves their account
     *  under an email — or signs into an existing one — must keep seeing the nests they already
     *  logged, so ownership is a set, not one id. */
    val knownUserIds: Set<String> = emptySet(),
) {
    /** Excavating a nest is delicate → only experienced volunteers, beach leaders and admins. */
    val canExcavate: Boolean
        get() = experienced || memberRole == MemberRole.BEACH_LEADER || memberRole == MemberRole.ADMIN

    /** Recording a patrol is coordination work, not something a passing visitor should be offered.
     *  Sits beside [canExcavate] so "trusted enough to…" is decided in one place, not per screen. */
    val hasRole: Boolean
        get() = memberRole != MemberRole.VOLUNTEER
}

/** Whole app state (single source of truth for the in-memory repository). */
@Serializable
data class AppState(
    val community: Community,
    /** All communities shown on the map (our own + collected STUB groups near other beaches).
     *  Includes [community]. Orange squares; tap → community screen (contacts). */
    val communities: List<Community> = emptyList(),
    val beaches: List<Beach>,
    val nests: List<Nest>,
    val markers: List<SimpleMarker>,
    val patrols: List<Patrol>,
    val facts: List<Fact>,
    val guide: List<GuideArticle>,
    val badges: List<Badge>,
    val profile: Profile,
    val members: List<Member> = emptyList(),
    /** Official protected nesting areas (loaded from the baked country data file). */
    val protectedAreas: List<ProtectedArea> = DEFAULT_PROTECTED_AREAS,
    /** Epoch millis of the last successful OSM beach discovery (cache freshness). */
    val beachesSyncedAt: Long = 0,
    /** OSM shoreline polylines around the community — what decides whether a photo was taken
     *  somewhere a turtle could have nested. Cached with the beaches so the check works offline. */
    val shoreline: List<List<GeoPoint>> = emptyList(),
    /** Last known device location (for "beaches near me" distances). Local only. */
    val deviceLocation: GeoPoint? = null,
    /** Current air quality for the community's beach area (Sensor.Community). null = no nearby sensor. */
    val air: AirStatus? = null,
    /** Signed-in email, or null while still an anonymous volunteer (drives the profile sign-in CTA). */
    val accountEmail: String? = null,
    /** One-shot "centre the map here" request (e.g. from a nest's geo card). Transient — never
     *  serialized, so it can't survive a relaunch and re-focus the map. Set via [focusMap], consumed
     *  via [takeMapFocus]. */
    @Transient val mapFocus: GeoPoint? = null,
    /** One-shot "open add-update for this nest" request from the context-aware bottom "+" (B3). The
     *  nest detail on screen opens its AddUpdate dialog when this matches its id. Transient. */
    @Transient val addUpdateFor: String? = null,
    /** A photo the native camera just handed over, waiting for the add-nest form to pick it up.
     *  It lives in the state (not in a plain field) so a form ALREADY on screen sees it arrive —
     *  otherwise iOS had to rebuild the screen to read it, throwing away everything typed so far.
     *  Transient: a photo in flight must never survive a relaunch. */
    @Transient val pendingPhoto: PendingPhotoRef? = null,
) {
    fun beach(id: String): Beach? = beaches.firstOrNull { it.id == id }
    fun nest(id: String): Nest? = nests.firstOrNull { it.id == id }

    /** All communities to render (our own primary + any collected stubs), de-duplicated by id. */
    val allCommunities: List<Community>
        get() = (listOf(community) + communities).distinctBy { it.id }

    fun communityOrPrimary(id: String): Community =
        allCommunities.firstOrNull { it.id == id } ?: community

    /** The signed-in user projected as a [Member], so the whole people-graph (avatars, names,
     *  the leaderboard, timeline authors) links to one MemberProfile screen — including yourself. */
    val meAsMember: Member
        get() = Member(
            id = "you",
            name = profile.displayName,
            role = profile.memberRole,
            avatar = profile.avatar,
            homeBeach = profile.homeBeachId?.let { beach(it)?.name },
            link = profile.link,
            userId = profile.userId,
        )

    /** Every auth uid that is me (anonymous first run → linked email → a later sign-in). */
    val myUserIds: Set<String>
        get() = profile.knownUserIds + setOfNotNull(profile.userId)

    /** May this volunteer record a patrol? Signed in (their walk is attributable) and holding a
     *  role — see [Profile.hasRole]. Stated here so screens don't each invent their own rule. */
    val canRecordPatrol: Boolean
        get() = accountEmail != null && profile.hasRole

    /** Is this nest mine? Owner id when the record has one, display name only as a legacy fallback. */
    fun isMine(n: Nest): Boolean = isMine(n, myUserIds)

    private fun isMine(n: Nest, mine: Set<String>): Boolean =
        if (n.foundByUserId != null) n.foundByUserId in mine
        else !isPlaceholderName(n.foundBy) && n.foundBy.equals(profile.displayName, ignoreCase = true)

    /**
     * Resolve a person reference to a [Member]. The key is an auth uid wherever the record has one,
     * falling back to a [Member] id or a stored display name for rows written before uids existed.
     *
     * The uid must be tried FIRST. Guardian names are drawn from a small vocabulary, so two
     * volunteers on one beach can genuinely share "Dune Keeper" — matching on the name first would
     * open your own profile, with your nests, when you tapped theirs.
     */
    fun resolveMember(key: String): Member {
        if (key == "you" || key in myUserIds) return meAsMember
        members.firstOrNull { it.userId == key }?.let { return it }
        members.firstOrNull { it.id == key }?.let { return it }
        // A uid we've only ever seen on a nest: name them from the record they signed.
        nests.firstOrNull { it.foundByUserId == key }?.let {
            return Member(id = key, name = it.foundBy, role = MemberRole.VOLUNTEER, userId = key)
        }
        if (key.equals(profile.displayName, ignoreCase = true)) return meAsMember
        members.firstOrNull { it.name.equals(key, ignoreCase = true) }?.let { return it }
        return Member(id = key, name = key, role = MemberRole.VOLUNTEER)
    }

    /**
     * Nests this member found. Matched by OWNER ID, never by display name alone: every install
     * starts as "Volunteer" and every timeline entry as "you", so name matching handed one
     * volunteer everybody else's nests while their own (logged under a since-changed name) went
     * missing. Names still resolve legacy rows that carry no owner id — but only real ones.
     */
    fun nestsBy(m: Member): List<Nest> {
        val mine = myUserIds        // hoisted: the getter builds a Set on every read
        return nests.filter { n ->
            when {
                m.id == "you" -> isMine(n, mine)
                m.userId != null && n.foundByUserId != null -> n.foundByUserId == m.userId
                else -> !isPlaceholderName(n.foundBy) && n.foundBy.equals(m.name, ignoreCase = true)
            }
        }
    }

    /** Badges a member has earned, derived from their nest activity (V1 keeps no per-member badge store). */
    fun earnedBadgesFor(m: Member): List<Badge> = earnedBadgesFrom(nestsBy(m))

    /** Same, from an already-computed nest list — screens usually have one in hand. */
    fun earnedBadgesFrom(ns: List<Nest>): List<Badge> {
        val hatchlings = ns.sumOf { it.excavation?.hatchlingsToSea ?: 0 }
        return buildList {
            if (ns.isNotEmpty()) add(Badge("first_nest", "🥚", "First nest", true))
            if (ns.any { it.excavation != null }) add(Badge("first_dig", "⛏️", "First excavation", true))
            if (hatchlings > 0) add(Badge("rescuer", "🐢", "Rescuer", true))
            if (hatchlings >= 50) add(Badge("season50", "👑", "Season 50", true))
        }
    }
}
