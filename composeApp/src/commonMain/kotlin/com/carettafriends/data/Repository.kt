package com.carettafriends.data

import com.carettafriends.domain.AppState
import com.carettafriends.domain.Badge
import com.carettafriends.domain.Beach
import com.carettafriends.domain.Community
import com.carettafriends.domain.CommunityKind
import com.carettafriends.domain.Excavation
import com.carettafriends.domain.Fact
import com.carettafriends.domain.GeoPoint
import com.carettafriends.content.randomGuardianName
import com.carettafriends.domain.classifyAir
import com.carettafriends.domain.isPlaceholderName
import com.carettafriends.domain.distanceMeters
import com.carettafriends.domain.GuideArticle
import com.carettafriends.domain.LocationSource
import com.carettafriends.domain.MarkerType
import com.carettafriends.domain.Member
import com.carettafriends.domain.MemberRole
import com.carettafriends.domain.Nest
import com.carettafriends.domain.NestConfidence
import com.carettafriends.domain.NestStatus
import com.carettafriends.domain.NestUpdate
import com.carettafriends.domain.ObsCondition
import com.carettafriends.domain.Patrol
import com.carettafriends.domain.PhotoRef
import com.carettafriends.domain.PhotoSource
import com.carettafriends.domain.Profile
import com.carettafriends.domain.ProtectionLevel
import com.carettafriends.domain.SimpleMarker
import com.carettafriends.domain.SunExposure
import com.carettafriends.domain.UpdateKind
import com.carettafriends.domain.Visibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

/** A photo captured/picked by the native camera, waiting to be attached to a new nest.
 *  [hash] identifies the ORIGINAL image (see [com.carettafriends.domain.PhotoRef.hash]) so the same
 *  photo imported twice is recognized instead of becoming a second nest. */
data class PendingPhoto(val path: String, val lat: Double?, val lng: Double?, val hash: String? = null)

/**
 * How close two nests must be before we ask whether it's the same one.
 *
 * Caretta females do nest within a few metres of each other on a good stretch of sand, so this has
 * to stay tight — 25 m would have flagged genuinely separate nests as duplicates. Kept just under
 * a phone's GPS scatter: the exact-photo check is what catches a re-upload, and this only catches
 * re-photographing the same nest on the spot. Either way it's a QUESTION with the distance shown,
 * never an automatic merge.
 */
const val DUPLICATE_RADIUS_M = 8.0

/** Why a new record looks like one that already exists. */
enum class DuplicateReason { SAME_PHOTO, SAME_SPOT }

/** An existing nest a to-be-saved one collides with (same photo, or same patch of sand). */
data class DuplicateHit(val nest: Nest, val reason: DuplicateReason, val distanceM: Double)

/** TSD prediction (qualitative V1 model, regional caveat — see design-spec / research). */
fun predictTsd(exposure: SunExposure?): Triple<Int, Int, Int> = when (exposure) {
    SunExposure.FULL_SUN -> Triple(80, 95, 50)
    SunExposure.PARTIAL -> Triple(65, 85, 55)
    SunExposure.SHADE -> Triple(45, 70, 60)
    null -> Triple(60, 85, 55)
}

/** How much warmer the nest sand runs than shaded air, by sun exposure (°C). */
private fun sandOffsetC(exposure: SunExposure?): Double = when (exposure) {
    SunExposure.FULL_SUN -> 3.5
    SunExposure.PARTIAL -> 2.0
    SunExposure.SHADE -> 0.5
    null -> 2.0
}

/**
 * Temperature-driven TSD (thermosensitive-period model). Loggerhead sex flips steeply around a pivot
 * of ~29.2°C (transitional range ~28–30.5°C). We approximate mid-incubation sand temperature as the
 * mean recorded air temp + a sun-exposure offset, then map it through a logistic curve to a female %.
 * Returns a female-share range (± daily variation), or null if there's no temperature series yet.
 * NOTE: uses the temps recorded so far (7-day window at creation) — a full per-day series over the
 * incubation is the next step; this already beats the pure-exposure guess when local temps exist.
 */
fun predictFemaleRange(dailyAirC: List<Double>, exposure: SunExposure?): Pair<Int, Int>? {
    if (dailyAirC.isEmpty()) return null
    val sand = dailyAirC.average() + sandOffsetC(exposure)
    val female = (100.0 / (1.0 + kotlin.math.exp(-1.4 * (sand - 29.2)))).toInt().coerceIn(0, 100)
    val spread = 12
    return (female - spread).coerceAtLeast(0) to (female + spread).coerceAtMost(100)
}

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/** Epoch millis → the local calendar date it falls on (EXIF capture times, stored timestamps). */
fun localDateOf(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date

/** How far back a nest/update date may be set (word-of-mouth back-dating; guards against bogus far-past dates). */
const val MAX_BACKDATE_DAYS = 31

/** Days elapsed since the nest was found (incubation day). */
fun nestDay(nest: Nest, today: LocalDate = today()): Int =
    nest.foundDate.daysUntil(today).coerceAtLeast(0)

/**
 * Lifecycle phase that drives the map dot colour, so volunteers read a beach at a glance:
 *  - `incubating` — a normal nest, still deep in incubation
 *  - `soon`       — hatch window is near (last ~5 days) — watch it
 *  - `emerging`   — hatchlings have started coming out (status HATCHING) — excavate in 2-3 days to
 *                   free any stragglers; the distinct colour flags "act on this one"
 *  - `excavated`  — dug & counted (or hatched), still on the beach
 *  - `removed`    — excavated >2 days ago (cleared from the location) OR a lost/predated nest → grey
 */
fun nestMapPhase(nest: Nest, today: LocalDate = today()): String = when (nest.status) {
    NestStatus.HATCHING -> "emerging"
    NestStatus.EXCAVATED -> {
        val exMillis = nest.updates.lastOrNull { it.kind == UpdateKind.EXCAVATED }?.createdEpochMillis ?: 0L
        val exDate = if (exMillis > 0) localDateOf(exMillis) else null
        if (exDate != null && exDate.daysUntil(today) > 2) "removed" else "excavated"
    }
    NestStatus.HATCHED -> "excavated"
    NestStatus.INCUBATING -> if (nestDay(nest, today) >= nest.incubationDaysEst - 5) "soon" else "incubating"
    NestStatus.LOST, NestStatus.PREDATED, NestStatus.WASHED_OVER, NestStatus.POACHED, NestStatus.FALSE_CRAWL -> "removed"
    else -> "incubating"
}

/**
 * The next nest code, continued from the HIGHEST number already in use.
 *
 * It used to be `"GZP-${nests.size + 1}"`, which counts the WHOLE local list — including nests that
 * synced in from other volunteers, and excluding ones not pulled yet. Production has GZP-3 and
 * GZP-16 twice because of it. Max + 1 (with a uniqueness guard) keeps codes stable and monotonic.
 */
internal fun nextNestCode(nests: List<Nest>, prefix: String = "GZP"): String {
    val used = nests.mapTo(mutableSetOf()) { it.code }
    val highest = nests.mapNotNull { it.code.substringAfterLast('-').toIntOrNull() }.maxOrNull() ?: 0
    var n = highest + 1
    while ("$prefix-$n" in used) n++
    return "$prefix-$n"
}

/**
 * In-memory, offline-first repository. Single source of truth via StateFlow.
 * V1 storage is in memory + seed; SQLDelight / Supabase sync slot in behind this API (V2).
 */
// Bumped to v2 to drop the old demo-seeded local state (pre-launch, no real data yet) → clean start.
private const val STATE_FILE = "caretta_state_v2.json"

/**
 * Give every timeline entry a real timestamp.
 *
 * Entries written before 2026-08 got `createdEpochMillis = 0`: the activity feed sorts on it and
 * filters time ranges by it, so a nest you had just logged sank below everything else and vanished
 * entirely under Today/Week/Month. Backfill from the date the entry actually refers to (its
 * back-date, else the nest's found date) at midday, spaced a second apart to keep their order.
 */
private fun repairTimestamps(nests: List<Nest>): List<Nest> = nests.map { n ->
    if (n.updates.none { it.createdEpochMillis <= 0L }) return@map n
    n.copy(
        updates = n.updates.mapIndexed { i, u ->
            if (u.createdEpochMillis > 0L) u
            else u.copy(
                createdEpochMillis = (u.obsDate ?: n.foundDate)
                    .atStartOfDayIn(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds() + 12 * 3600 * 1000L + i * 1000L,
            )
        },
    )
}

private fun loadOrSeed(json: Json): AppState {
    val loaded = LocalStore.readText(STATE_FILE)?.let { runCatching { json.decodeFromString<AppState>(it) }.getOrNull() }
    // Community is static reference data → always refresh from seed so field additions (description,
    // email, phone, kind…) reach existing installs without wiping the user's local nests/patrols.
    val state = (loaded ?: seedState()).copy(community = seedCommunity())
        .let { it.copy(nests = repairTimestamps(it.nests)) }
        // Installs made before guardian names still carry the shared placeholder — give them one
        // now so their next find is signed by someone rather than by "Volunteer".
        .let {
            if (it.profile.nameSet || !isPlaceholderName(it.profile.displayName)) {
                it
            } else {
                it.copy(profile = it.profile.copy(displayName = randomGuardianName(it.profile.language)))
            }
        }

    // Follow the phone's language until the volunteer picks one themselves, so switching the
    // language in iOS Settings → Caretta Friends actually changes the app on the next launch.
    if (state.profile.languageExplicit) return state
    return state.copy(profile = state.profile.copy(language = systemLanguage()))
}

private fun persist(json: Json, s: AppState) {
    runCatching { LocalStore.writeText(STATE_FILE, json.encodeToString(s)) }
}

class CarettaRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val auth: AuthBackend = SupabaseAuth()
    private val cloud: CloudBackend = SupabaseCloud(auth)
    private val beachDiscovery = BeachDiscovery()
    private val weather = WeatherService()
    private val airQuality = AirQualityService()
    private val _state = MutableStateFlow(loadOrSeed(json))
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        // Offline-first: persist every change locally so data survives restarts and works offline.
        scope.launch { _state.drop(1).collect { persist(json, it) } }
        // Load the official protected-beach catalogue (by-country data file) → state.
        scope.launch { _state.value = _state.value.copy(protectedAreas = loadProtectedAreas()) }
        // Load baked STUB communities (local groups near other beaches, no admin yet) → state.
        scope.launch { _state.value = _state.value.copy(communities = loadStubCommunities()) }
        // Pull current air quality for the beach area (Sensor.Community) — supplementary dust layer.
        scope.launch { refreshAir() }
        // Best-effort cloud sync (no-op when offline).
        scope.launch { syncOnStart() }
    }

    /** Once-a-day per-incubating-nest temperature accrual → the thermosensitive-period TSD mean grows
     *  over the whole incubation, not just the 7-day window at creation. One fetch per beach (nests on a
     *  beach share the temperature); skips nests already updated today (lastTempDay). Best-effort/offline-safe. */
    private suspend fun refreshNestTemps() {
        val today = today().toEpochDays()
        val due = _state.value.nests.filter { it.status == NestStatus.INCUBATING && it.lastTempDay < today }
        due.groupBy { it.beachId }.forEach { (_, nests) ->
            val n0 = nests.first()
            val t = weather.fetch(n0.point.lat, n0.point.lng)?.airTempC ?: return@forEach
            nests.forEach { nest ->
                update(nest.id) {
                    val temps = it.temps + com.carettafriends.domain.TemperatureReading("day $today", "air_daily", t)
                    val range = predictFemaleRange(temps.map { r -> r.valueC }, it.exposure)
                    it.copy(
                        temps = temps, lastTempDay = today, airTempC = t,
                        predictedFemaleLow = range?.first ?: it.predictedFemaleLow,
                        predictedFemaleHigh = range?.second ?: it.predictedFemaleHigh,
                    )
                }
            }
        }
    }

    private suspend fun syncOnStart() {
        // Anonymous sign-in on first run → stable owner_id + RLS-scoped, attributable writes.
        auth.ensureSession()
        _state.value = _state.value.copy(accountEmail = auth.currentEmail())
        rememberMe()
        val cid = _state.value.community.id
        val owner = auth.currentUserId()
        // Purge any stale/ghost beaches left in the local cache before we push them back to the cloud.
        _state.value = _state.value.copy(beaches = normalizeBeaches(_state.value.beaches, cid))
        // Auto-discover nearby beaches from OpenStreetMap so they appear automatically for any coastal
        // city — no hand-entered lists. CACHED: only refetch if we have none yet or the cache is stale
        // (>30 days), so normal starts are instant + offline (Overpass is slow and has no SLA).
        // Refetch if we have no OSM beaches yet OR they lack polygons (older point-only cache).
        val haveOsm = _state.value.beaches.any { it.id.startsWith("osm-") && it.polygon.isNotEmpty() }
        val stale = nowMillis() - _state.value.beachesSyncedAt > 30L * 24 * 3600 * 1000
        if (!haveOsm || stale) {
            val center = _state.value.beaches.firstOrNull()?.center ?: GeoPoint(36.27, 32.30)
            val discovered = runCatching { beachDiscovery.nearby(center.lat, center.lng, 15_000, cid, _state.value.protectedAreas) }.getOrDefault(emptyList())
            if (discovered.isNotEmpty()) {
                _state.value = _state.value.copy(
                    beaches = normalizeBeaches(mergeById(_state.value.beaches, discovered) { it.id }, cid),
                    beachesSyncedAt = nowMillis(),
                )
            }
        }
        // Push local changes first (nothing made offline is lost), then pull & merge the truth.
        val local = _state.value
        // Parents FIRST so child FKs are always satisfiable server-side (FK-safe offline sync).
        runCatching { cloud.pushCommunity(local.community) }
        local.beaches.forEach { runCatching { cloud.pushBeach(it) } }
        // Then the aggregate roots — only the ones that are OURS. The local cache also holds every
        // nest synced down from other volunteers; re-pushing those stamped our uid on them (RLS
        // rejects it, so it was wasted traffic on every launch rather than a data loss).
        local.nests
            .filter { it.foundByUserId == null || it.foundByUserId in local.myUserIds }
            .forEach { runCatching { cloud.pushNest(it, cid, owner) } }
        local.markers.forEach { runCatching { cloud.pushMarker(it, owner) } }
        // Only PUBLISHED patrols sync — unpublished walks stay on-device (no live-location sharing).
        local.patrols.filter { it.published }.forEach { runCatching { cloud.pushPatrol(it, owner) } }
        // Pull merged truth. New beaches propagate; nests merge by LWW (scalars) + timeline union.
        runCatching {
            val remoteBeaches = cloud.pullBeaches()
            val remoteNests = cloud.pullNests()
            val remoteMarkers = cloud.pullMarkers()
            val s = _state.value
            _state.value = s.copy(
                // Normalize again: the cloud may still hold ghosts we can't delete via RLS, so keep them
                // off the map here (remote-wins merge would otherwise reintroduce them).
                beaches = normalizeBeaches(mergeById(s.beaches, remoteBeaches) { it.id }, cid),
                nests = repairTimestamps(mergeNests(s.nests, remoteNests)),
                markers = mergeById(s.markers, remoteMarkers) { it.id },
            )
        }
        // Once-a-day TSP temperature accrual for incubating nests (best-effort, after the merge).
        runCatching { refreshNestTemps() }
    }

    /** Union by id; remote wins on conflict — safe for append-only entities (beaches, markers). */
    private fun <T> mergeById(local: List<T>, remote: List<T>, id: (T) -> String): List<T> =
        (local.associateBy(id) + remote.associateBy(id)).values.toList()

    /** Remove stale/ghost beaches and reconcile seed beaches to their canonical coordinates.
     *  Keep a beach only if it's a current seed beach OR an OSM beach WITH a polygon outline. This
     *  drops the leftovers the old append-only merge kept forever: earlier point-only seed dups
     *  (old inland "bidibidi", "selinus") and centroid-less OSM relation duplicates (e.g. a "Koru
     *  Plaj" relation shadowing the real "Koru Plajı" way). Runs on start and after every pull so
     *  neither the local cache nor the cloud can reintroduce a ghost. */
    private fun normalizeBeaches(beaches: List<Beach>, communityId: String): List<Beach> {
        val seeds = seedBeaches(communityId).associateBy { it.id }
        return beaches
            .filter { it.id in seeds.keys || (it.id.startsWith("osm-") && it.polygon.isNotEmpty()) }
            .map { seeds[it.id] ?: it }          // reconcile seed beaches to canonical coord/protected
            .distinctBy { it.id }
    }

    /** Nest merge: scalars last-write-wins by updatedAtMillis, timeline UNIONed by update id.
     *  This is the fix for the old "remote clobbers local" bug — two volunteers adding observations
     *  offline to the same nest no longer lose each other's entries. */
    private fun mergeNests(local: List<Nest>, remote: List<Nest>): List<Nest> {
        val byId = local.associateBy { it.id }.toMutableMap()
        for (r in remote) byId[r.id] = byId[r.id]?.let { mergeNest(it, r) } ?: r
        return byId.values.toList()
    }

    private fun mergeNest(local: Nest, remote: Nest): Nest {
        val updates = (local.updates + remote.updates).distinctBy { it.id }.sortedBy { it.createdEpochMillis }
        val base = if (remote.updatedAtMillis >= local.updatedAtMillis) remote else local
        return base.copy(updates = updates)
    }

    /** Record the current auth uid as mine. Kept as a SET: an anonymous volunteer who later saves
     *  their account under an email (or signs into another one) must keep the nests already logged
     *  under the previous uid. This is what "my nests" is filtered by. */
    private fun rememberMe() {
        val uid = auth.currentUserId() ?: return
        val p = _state.value.profile
        if (p.userId == uid && uid in p.knownUserIds) return
        _state.value = _state.value.copy(
            profile = p.copy(userId = uid, knownUserIds = p.knownUserIds + uid),
        )
    }

    /** The uid every record this device writes is attributed to. */
    private fun myUserId(): String? = auth.currentUserId() ?: _state.value.profile.userId

    private fun syncNest(nest: Nest) {
        val cid = _state.value.community.id
        scope.launch { auth.ensureSession(); runCatching { cloud.pushNest(nest, cid, auth.currentUserId()) } }
    }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    /**
     * Build a timeline entry. Every entry needs an author, an owner and a creation time — those were
     * hand-copied at each construction site, and the one place that forgot the timestamp is why a
     * freshly logged nest fell out of the activity feed. One factory, nothing to forget.
     */
    private fun newUpdate(
        kind: UpdateKind,
        body: String = "",
        condition: ObsCondition? = null,
        newStatus: NestStatus? = null,
        obsDate: LocalDate? = null,
        photo: PhotoRef? = null,
        at: Long = nowMillis(),
    ): NestUpdate = NestUpdate(
        id = nextId("u"),
        kind = kind,
        condition = condition,
        newStatus = newStatus,
        body = body,
        author = _state.value.profile.displayName,
        authorUserId = myUserId(),
        createdEpochMillis = at,
        dateLabel = "Today",
        obsDate = obsDate,
        photo = photo,
    )

    // Client-generated UUIDs: stable offline PKs, collision-free upserts, FK-safe sync.
    // (Replaces the old in-memory counter that reset to 1000 every launch → PK collisions → data loss.)
    private fun nextId(prefix: String) = newUuid()

    /** Set by the native camera (iOS); consumed by the add-nest form to prefill photo + location. */
    var pendingPhoto: PendingPhoto? = null
    fun takePendingPhoto(): PendingPhoto? = pendingPhoto.also { pendingPhoto = null }

    /** Request the map centre on [p] (e.g. from a nest's geo card). Reactive on Android (StateFlow),
     *  read as a snapshot on iOS. Transient in [AppState] — never persisted. */
    fun focusMap(p: GeoPoint) { _state.value = _state.value.copy(mapFocus = p) }

    /** Read-and-clear the pending map focus (mirrors [takePendingPhoto]). Returns null if none pending. */
    fun takeMapFocus(): GeoPoint? =
        _state.value.mapFocus.also { if (it != null) _state.value = _state.value.copy(mapFocus = null) }

    /** Ask the on-screen nest detail to open its "add update" dialog (context-aware bottom "+", B3). */
    fun requestAddUpdate(nestId: String) { _state.value = _state.value.copy(addUpdateFor = nestId) }
    /** Clear the pending add-update request once the dialog has been opened. */
    fun clearAddUpdate() { if (_state.value.addUpdateFor != null) _state.value = _state.value.copy(addUpdateFor = null) }

    /** Set the volunteer's display name (offline, no login needed — used as the nest/patrol author). */
    fun setDisplayName(name: String) {
        val clean = name.trim().take(40)
        if (clean.isNotBlank()) {
            _state.value = _state.value.copy(
                profile = _state.value.profile.copy(displayName = clean, nameSet = true),
            )
        }
    }

    fun setLanguage(lang: String) {
        // An explicit pick sticks — from here on the phone's language is ignored.
        _state.value = _state.value.copy(
            profile = _state.value.profile.copy(language = lang, languageExplicit = true)
        )
    }

    /** Toggle watching a nest (persists → survives restarts). Returns the new watching state. */
    fun toggleWatch(nestId: String): Boolean {
        val cur = _state.value.profile.watchedNestIds
        val next = if (nestId in cur) cur - nestId else cur + nestId
        _state.value = _state.value.copy(profile = _state.value.profile.copy(watchedNestIds = next))
        return nestId in next
    }

    /** Mark the first-run onboarding as seen (persists → shown only once). */
    fun setOnboarded() {
        _state.value = _state.value.copy(profile = _state.value.profile.copy(onboarded = true))
    }

    /** Save the anonymous volunteer's account under an email (same owner_id → their nests & impact are
     *  kept). [onResult] gets null on success, or a human-readable error. */
    fun linkEmail(email: String, password: String, onResult: (String?) -> Unit) {
        scope.launch {
            val r = auth.linkEmail(email.trim(), password)
            if (r.ok) {
                _state.value = _state.value.copy(accountEmail = email.trim())
                rememberMe()   // same uid — but make sure it's on record as mine
            }
            onResult(if (r.ok) null else (r.error ?: "Couldn't save your account"))
        }
    }

    /** Sign in an existing email account (returning volunteer). Pulls their cloud data afterwards. */
    fun signInEmail(email: String, password: String, onResult: (String?) -> Unit) {
        scope.launch {
            val r = auth.signInEmail(email.trim(), password)
            if (r.ok) {
                _state.value = _state.value.copy(accountEmail = email.trim())
                rememberMe()                    // their account uid joins the ones that are mine
                runCatching { syncOnStart() }   // re-pull their data under the new uid
            }
            onResult(if (r.ok) null else (r.error ?: "Invalid email or password"))
        }
    }

    /**
     * Permanently delete the volunteer's account, on the server and on this device.
     *
     * Required by App Store guideline 5.1.1(v) for any app offering account creation. The nests,
     * markers and patrols they recorded are kept on the shared map but detached from them — they
     * are the community's conservation record, and the project rule is that field observations are
     * never destroyed (see supabase/migrations/0006_account_deletion.sql).
     *
     * Afterwards the app is back to a first-install state; the next sync signs in a brand-new
     * anonymous user.
     */
    fun deleteAccount(onResult: (String?) -> Unit) {
        scope.launch {
            val r = auth.deleteAccount()
            if (!r.ok) {
                onResult(r.error ?: "Couldn't delete your account")
                return@launch
            }
            LocalStore.delete(STATE_FILE)
            _state.value = seedState().copy(community = seedCommunity())
            onResult(null)
        }
    }

    /** Set (or clear with null) the volunteer's optional home beach. */
    fun setHomeBeach(beachId: String?) {
        _state.value = _state.value.copy(profile = _state.value.profile.copy(homeBeachId = beachId))
    }

    /** Last known device location, for "beaches near me" distances (set by the native map/GPS). */
    fun setDeviceLocation(lat: Double, lng: Double) {
        _state.value = _state.value.copy(deviceLocation = GeoPoint(lat, lng))
        scope.launch { refreshAir() }   // refresh air for where the volunteer actually is
    }

    /** Pull current air quality for the volunteer's location or the community beach. Primary =
     *  the user's Air Signal comfort API (merged PM2.5 + 0–100 index); Sensor.Community adds PM10
     *  (for dust) and is the fallback when the Air Signal server is unreachable. */
    suspend fun refreshAir() {
        val s = _state.value
        val p = s.deviceLocation ?: s.community.center
        val comfort = airQuality.comfort(p.lat, p.lng)
        val sample = airQuality.near(p.lat, p.lng)
        val pm25 = comfort?.pm25 ?: sample?.pm25
        _state.value = if (pm25 == null) {
            _state.value.copy(air = null)
        } else {
            _state.value.copy(
                air = classifyAir(
                    pm25, sample?.pm10 ?: pm25, sample?.sensors ?: 0, nowMillis(),
                    comfort?.comfort, comfort?.signals ?: emptyList(),
                ),
            )
        }
    }

    fun addNest(
        point: GeoPoint,
        beachId: String,
        isNest: Boolean,
        exposure: SunExposure?,
        protection: ProtectionLevel,
        clutchSizeEst: Int?,
        hasPhoto: Boolean,
        locationSource: LocationSource,
        visibility: Visibility,
        photoPath: String? = null,
        /** Back-date for a nest found earlier but photographed for the first time now (null = today). */
        foundDate: LocalDate? = null,
        /** Optional note/description the volunteer adds at first sighting → first comment on the nest. */
        note: String = "",
        /** Identity of the source image, for duplicate detection (see [PendingPhoto.hash]). */
        photoHash: String? = null,
    ): String {
        val s = _state.value
        val found = (foundDate ?: today()).coerceIn(today().minus(DatePeriod(days = MAX_BACKDATE_DAYS)), today())
        val (fLow, fHigh, inc) = predictTsd(exposure)
        val confirmed = hasPhoto && (locationSource == LocationSource.PHOTO_EXIF || locationSource == LocationSource.DEVICE_GPS)
        val id = nextId("nest")
        val code = nextNestCode(s.nests)
        val now = nowMillis()
        val owner = myUserId()
        val photos = if (hasPhoto) {
            listOf(PhotoRef(nextId("ph"), PhotoSource.CAMERA, localUri = photoPath, hash = photoHash))
        } else {
            emptyList()
        }
        val nest = Nest(
            id = id,
            code = code,
            beachId = beachId,
            point = point,
            isNest = isNest,
            confidence = if (confirmed) NestConfidence.CONFIRMED else NestConfidence.UNCONFIRMED,
            visibility = visibility,
            foundDate = found,
            clutchSizeEst = clutchSizeEst,
            protection = protection,
            exposure = exposure,
            locationSource = locationSource,
            incubationDaysEst = if (isNest) inc else 0,
            status = if (isNest) NestStatus.INCUBATING else NestStatus.FALSE_CRAWL,
            predictedFemaleLow = if (isNest) fLow else null,
            predictedFemaleHigh = if (isNest) fHigh else null,
            // No hardcoded weather — air temp / rain stay null until a real weather fetch (Open-Meteo)
            // is wired; the nest card hides those chips when unset (no fake "31° / 0mm").
            airTempC = null,
            rainMm7d = null,
            photos = photos,
            updates = buildList {
                val backDate = found.takeIf { it != today() }
                // The "found" entry MUST carry a timestamp — the activity feed sorts and time-filters
                // on it, and a zero here is why a just-logged nest never showed up in the profile.
                add(
                    newUpdate(
                        UpdateKind.FOUND,
                        body = if (isNest) "Nest found" else "False crawl logged",
                        obsDate = backDate,
                        at = now,
                    ),
                )
                if (note.isNotBlank()) {
                    add(newUpdate(UpdateKind.COMMENT, body = note.trim(), at = now + 1))
                }
            },
            temps = emptyList(),
            foundBy = s.profile.displayName,
            foundByUserId = owner,
            updatedAtMillis = now,
        )
        _state.value = s.copy(nests = s.nests + nest)
        syncNest(nest)
        // Best-effort: pull REAL weather (Open-Meteo, free) for this point and cache it on the nest.
        // Offline-safe — the nest already exists; this just enriches it when online.
        if (isNest) {
            scope.launch {
                weather.fetch(point.lat, point.lng)?.let { w ->
                    // Refine the sex prediction from the real local temperatures (thermosensitive model).
                    val range = predictFemaleRange(w.daily.map { it.valueC }, exposure)
                    update(id) {
                        it.copy(
                            airTempC = w.airTempC, rainMm7d = w.rainMm7d, temps = w.daily,
                            predictedFemaleLow = range?.first ?: it.predictedFemaleLow,
                            predictedFemaleHigh = range?.second ?: it.predictedFemaleHigh,
                        )
                    }
                }
            }
        }
        return id
    }

    /**
     * Does this photo/point already belong to a nest we know about?
     *
     * Volunteers re-send the same photo (a second attempt, a photo forwarded from a colleague) and
     * photograph the same nest twice on one walk. Before this check every attempt minted a fresh
     * nest — production has three nests at the same EXIF coordinate from one afternoon.
     *
     * Two signals, strongest first:
     *  - SAME_PHOTO — literally the same source image (see [PhotoRef.hash]); certain duplicate.
     *  - SAME_SPOT  — a nest within [DUPLICATE_RADIUS_M]; almost certainly the same nest, but the
     *                 volunteer decides (two real nests CAN be metres apart on a busy beach).
     * Records pinned to a beach centre for lack of GPS are excluded from the distance test — they
     * all share one coordinate, so they'd all look like duplicates of each other.
     */
    fun findDuplicate(point: GeoPoint, photoHash: String?, hasFix: Boolean): DuplicateHit? {
        val nests = _state.value.nests
        if (photoHash != null) {
            nests.firstOrNull { n -> n.photos.any { it.hash == photoHash } }?.let {
                return DuplicateHit(it, DuplicateReason.SAME_PHOTO, distanceMeters(point, it.point))
            }
        }
        if (!hasFix) return null
        return nests
            .filter { it.locationSource != LocationSource.NONE }
            .map { it to distanceMeters(point, it.point) }
            .filter { (_, d) -> d <= DUPLICATE_RADIUS_M }
            .minByOrNull { (_, d) -> d }
            ?.let { (n, d) -> DuplicateHit(n, DuplicateReason.SAME_SPOT, d) }
    }

    fun addSimpleMarker(type: MarkerType, point: GeoPoint, note: String) {
        val s = _state.value
        // Stamped like every other record: an unstamped marker sorts to the beginning of time.
        val marker = SimpleMarker(
            id = nextId("m"), type = type, point = point, note = note,
            createdBy = s.profile.displayName, createdEpochMillis = nowMillis(),
        )
        _state.value = s.copy(markers = s.markers + marker)
        scope.launch { auth.ensureSession(); runCatching { cloud.pushMarker(marker, auth.currentUserId()) } }
    }

    /** Report a rule violation (tents/cars/…). PRIVATE = hidden from guests + the reporter may stay
     *  [anonymous] (protects volunteers from retaliation over fines). Kept in the DB for CİMER
     *  complaints; only recent ones surface on the map (via the Violations filter). */
    fun addViolation(
        point: GeoPoint,
        kind: com.carettafriends.domain.ViolationKind,
        note: String,
        visibility: Visibility,
        anonymous: Boolean,
        beachId: String? = null,
    ) {
        val s = _state.value
        val marker = SimpleMarker(
            id = nextId("m"),
            type = MarkerType.VIOLATION,
            point = point,
            note = note,
            beachId = beachId,
            createdBy = if (anonymous) "anonymous" else s.profile.displayName,
            violationKind = kind,
            visibility = visibility,
            anonymous = anonymous,
            createdEpochMillis = nowMillis(),
        )
        _state.value = s.copy(markers = s.markers + marker)
        scope.launch { auth.ensureSession(); runCatching { cloud.pushMarker(marker, auth.currentUserId()) } }
    }

    /** Append a timeline entry. [obsDate] back-dates it (word-of-mouth / gallery-EXIF photo from the past);
     *  when it equals today it stays a live "Today" entry. [photoPath] attaches a photo, which also joins the
     *  nest's photo history. */
    fun addUpdate(
        nestId: String,
        kind: UpdateKind,
        body: String,
        condition: ObsCondition? = null,
        obsDate: LocalDate? = null,
        photoPath: String? = null,
        photoHash: String? = null,
    ) {
        val backDate = obsDate?.takeIf { it != today() }
        val photo = photoPath?.let { PhotoRef(nextId("ph"), PhotoSource.GALLERY, localUri = it, hash = photoHash) }
        update(nestId) { n ->
            // Observing "hatching / hatched" advances the nest's lifecycle — otherwise a nest sits on
            // INCUBATING forever and the countdown / "hatching" filter / hatch payoff never resolve.
            // Only advance FORWARD — never downgrade a more-advanced/terminal status (e.g. a late
            // "hatching" note must not knock an EXCAVATED/HATCHED nest back).
            val advanced = when (condition) {
                ObsCondition.HATCHING -> if (n.status == NestStatus.INCUBATING) NestStatus.HATCHING else n.status
                ObsCondition.HATCHED ->
                    if (n.status == NestStatus.INCUBATING || n.status == NestStatus.HATCHING) NestStatus.HATCHED else n.status
                else -> n.status
            }
            n.copy(
                status = advanced,
                updates = n.updates + newUpdate(kind, body, condition, obsDate = backDate, photo = photo),
                photos = if (photo != null) n.photos + photo else n.photos,
            )
        }
    }

    /** Back-date a nest's found date — admin knows from word-of-mouth it was found earlier ("3 days ago")
     *  but the photo only arrived now. Clamped to [today-[MAX_BACKDATE_DAYS], today] to block bogus far-past dates. */
    fun setFoundDate(nestId: String, date: LocalDate) {
        val clamped = date.coerceIn(today().minus(DatePeriod(days = MAX_BACKDATE_DAYS)), today())
        update(nestId) { it.copy(foundDate = clamped) }
    }

    fun setStatus(nestId: String, status: NestStatus, comment: String = "") {
        update(nestId) { n ->
            n.copy(
                status = status,
                updates = n.updates + newUpdate(UpdateKind.STATUS_CHANGE, comment, newStatus = status),
            )
        }
    }

    fun setExcavation(nestId: String, exc: Excavation) {
        val before = _state.value.nest(nestId)
        update(nestId) { n ->
            n.copy(
                status = NestStatus.EXCAVATED,
                excavation = exc,
                updates = n.updates + newUpdate(
                    UpdateKind.EXCAVATED,
                    "Excavated · ${exc.hatchSuccessPct ?: 0}% hatch success",
                ),
            )
        }
        // credit hatchlings to the volunteer's impact
        val added = exc.hatchlingsToSea - (before?.excavation?.hatchlingsToSea ?: 0)
        if (added != 0) {
            val s = _state.value
            _state.value = s.copy(profile = s.profile.copy(hatchlingsReached = (s.profile.hatchlingsReached + added).coerceAtLeast(0)))
        }
    }

    /** Save a recorded patrol (GPS track). Stays ON-DEVICE ONLY until [publishPatrol] — no live sharing. */
    fun addPatrol(beachId: String, meters: Int, track: List<GeoPoint>, durationSec: Int): String {
        val s = _state.value
        val id = nextId("p")
        val p = Patrol(id, beachId, meters, "Today", s.profile.displayName, track = track, durationSec = durationSec)
        _state.value = s.copy(
            patrols = s.patrols + p,
            profile = s.profile.copy(
                patrols = s.profile.patrols + 1,
                kmWalked = s.profile.kmWalked + meters / 1000.0,
            ),
        )
        return id
    }

    /** Publish a recorded patrol → only now may it sync to the cloud (safety: routes aren't shared live). */
    fun publishPatrol(id: String) {
        val s = _state.value
        val p = s.patrols.firstOrNull { it.id == id }?.copy(published = true) ?: return
        _state.value = s.copy(patrols = s.patrols.map { if (it.id == id) p else it })
        val owner = auth.currentUserId()
        scope.launch { auth.ensureSession(); runCatching { cloud.pushPatrol(p, owner) } }
    }

    /** Most-recent patrol (for the map status pill), or null if none recorded yet. */
    fun lastPatrol(): Patrol? = _state.value.patrols.lastOrNull()

    private fun update(nestId: String, transform: (Nest) -> Nest) {
        val s = _state.value
        // Stamp the client change-clock on every mutation so LWW merge keeps the newest edit.
        _state.value = s.copy(nests = s.nests.map { if (it.id == nestId) transform(it).copy(updatedAtMillis = nowMillis()) else it })
        _state.value.nest(nestId)?.let { syncNest(it) }
    }
}

/** Canonical seeded beaches (the offline FK fallback). Bıdı Bıdı at its real coastal cove; every
 *  other beach is auto-discovered from OSM. This is the source of truth used to reconcile stale
 *  cached copies (e.g. an earlier inland "bidibidi") back to the correct coordinate. */
internal fun seedBeaches(communityId: String): List<Beach> = listOf(
    Beach("bidibidi", communityId, "Bıdı Bıdı", "Gazipaşa", GeoPoint(36.2529, 32.2869), protected = true),
)

// Clean first-run state: real reference data (community + beaches + facts + guide) but NO demo
// activity — nests/markers/patrols start empty and are filled by real volunteers. Profile is a
// fresh organiser. (Beach coordinates are approximate placeholders — refine with on-site GPS.)
/** Our own (primary) community — static reference data, reconciled onto existing installs at load. */
internal fun seedCommunity(): Community = Community(
    id = "gazipasa-caretta",
    name = "Gazipaşa Caretta",
    tagline = "Protecting loggerheads & sand lilies",
    taglineRu = "Защищаем логгерхедов и песчаные лилии",
    taglineTr = "Caretta ve kum zambaklarını koruyoruz",
    description = "Local volunteers protecting loggerhead (Caretta caretta) nests along Gazipaşa's " +
        "beaches — finding and marking nests, watching the ~50-day incubation, guarding hatchling " +
        "emergences and recording excavation counts, plus beach clean-ups and awareness. " +
        "Community-run, working to Türkiye's national monitoring standards.",
    descriptionRu = "Местные волонтёры защищают гнёзда черепах-логгерхедов (Caretta caretta) на пляжах " +
        "Газипаши — находят и отмечают гнёзда, следят за ~50-дневной инкубацией, охраняют выход " +
        "черепашат и ведут учёт при вскрытии, а также убирают пляжи и просвещают людей. " +
        "Работаем силами сообщества по национальным стандартам мониторинга Турции.",
    descriptionTr = "Yerel gönüllüler Gazipaşa plajlarında caretta (Caretta caretta) yuvalarını koruyor — " +
        "yuvaları bulup işaretliyor, ~50 günlük kuluçkayı izliyor, yavru çıkışlarını koruyor ve kazı " +
        "sayımlarını kaydediyor; ayrıca plaj temizliği ve farkındalık çalışmaları yapıyor. " +
        "Topluluk yönetiminde, Türkiye'nin ulusal izleme standartlarına göre çalışıyor.",
    websiteUrl = "https://carettafriends.com",
    whatsappUrl = "https://wa.me/905013794326",
    instagramUrl = "https://www.instagram.com/gazipasa_caretta_ve_kumzambagi",
    center = GeoPoint(36.268, 32.319),   // registered in Gazipaşa town (orange hub square)
    phone = "+90 501 379 4326",
    email = "info@carettafriends.com",
    kind = CommunityKind.COMMUNITY,
    // Türkiye's permitting authority for nesting-beach work (excavation, organized events): the
    // Ministry's DKMP regional directorate for Antalya. Other countries/communities set their own.
    authorityName = "DKMP 6. Bölge Müdürlüğü (Antalya)",
    authorityUrl = "https://bolge6.tarimorman.gov.tr",
)

private fun seedState(): AppState {
    val community = seedCommunity()
    // Only Bıdı Bıdı is seeded (real OSM coordinate) — every other beach is auto-discovered from
    // OpenStreetMap at runtime (see BeachDiscovery), so no per-city hand-entered lists.
    return AppState(
        community = community,
        beaches = seedBeaches(community.id),
        nests = emptyList(),
        markers = emptyList(),
        patrols = emptyList(),
        facts = listOf(
            Fact("f1", "🌡️", "Warmer sand makes more females — above ~29°C a nest skews female."),
            Fact("f2", "🌙", "Hatchlings emerge mostly at night and find the sea by the bright horizon."),
            Fact("f3", "🔦", "Artificial light disorients hatchlings — keep beaches dark in nesting season."),
        ),
        guide = listOf(
            GuideArticle("g1", "🥚", "Found a nest? Do this", "https://carettafriends.com/en/what-you-can-do/found-nest"),
            GuideArticle("g2", "🔦", "No flash, no lights at night", "https://carettafriends.com/en/what-you-can-do/found-nest"),
            GuideArticle("g3", "🚸", "Found a turtle / disturbance?", "https://carettafriends.com/en/what-you-can-do/found-turtle"),
            GuideArticle("g4", "🐣", "Helping stuck hatchlings out", "https://carettafriends.com/en/what-you-can-do/found-nest"),
        ),
        badges = listOf(
            Badge("first_nest", "🥚", "First nest", false),
            Badge("first_dig", "⛏️", "First excavation", false),
            Badge("rescuer", "🐢", "Rescuer", false),
            Badge("season50", "👑", "Season 50", false),
        ),
        profile = Profile(
            // A guardian name instead of the shared literal "Volunteer": every install had the same
            // one, so nobody could tell whose find a nest was. Replaceable in the profile.
            displayName = randomGuardianName(systemLanguage()), avatar = "🐢", role = "Guardian",
            // Open in the phone's language — most volunteers here are Turkish-speaking and should
            // not have to hunt for the language picker. Their own pick is persisted and wins after.
            language = systemLanguage(),
            memberRole = MemberRole.BEACH_LEADER, // current user is the organiser → can excavate
        ),
        members = listOf(
            Member("saban", "Şaban", MemberRole.ADMIN, "🧔", note = "Gazipaşa lead"),
            Member("alina", "Alina", MemberRole.VOLUNTEER, "🌸", note = "Helps Şaban · often at Bıdı Bıdı"),
        ),
    )
}
