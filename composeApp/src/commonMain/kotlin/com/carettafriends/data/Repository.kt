package com.carettafriends.data

import com.carettafriends.domain.AppState
import com.carettafriends.domain.Badge
import com.carettafriends.domain.Beach
import com.carettafriends.domain.Community
import com.carettafriends.domain.Excavation
import com.carettafriends.domain.Fact
import com.carettafriends.domain.GeoPoint
import com.carettafriends.domain.GuideArticle
import com.carettafriends.domain.LocationSource
import com.carettafriends.domain.MarkerType
import com.carettafriends.domain.Nest
import com.carettafriends.domain.NestConfidence
import com.carettafriends.domain.NestStatus
import com.carettafriends.domain.NestUpdate
import com.carettafriends.domain.ObsCondition
import com.carettafriends.domain.Patrol
import com.carettafriends.domain.PhotoRef
import com.carettafriends.domain.PhotoSource
import com.carettafriends.domain.Profile
import com.carettafriends.domain.SimpleMarker
import com.carettafriends.domain.SunExposure
import com.carettafriends.domain.TemperatureReading
import com.carettafriends.domain.UpdateKind
import com.carettafriends.domain.Visibility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

/** TSD prediction (qualitative V1 model, regional caveat — see design-spec / research). */
fun predictTsd(exposure: SunExposure?): Triple<Int, Int, Int> = when (exposure) {
    SunExposure.FULL_SUN -> Triple(80, 95, 50)
    SunExposure.PARTIAL -> Triple(65, 85, 55)
    SunExposure.SHADE -> Triple(45, 70, 60)
    null -> Triple(60, 85, 55)
}

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/** Days elapsed since the nest was found (incubation day). */
fun nestDay(nest: Nest, today: LocalDate = today()): Int =
    nest.foundDate.daysUntil(today).coerceAtLeast(0)

/**
 * In-memory, offline-first repository. Single source of truth via StateFlow.
 * V1 storage is in memory + seed; SQLDelight / Supabase sync slot in behind this API (V2).
 */
class CarettaRepository {
    private val _state = MutableStateFlow(seedState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var counter = 1000
    private fun nextId(prefix: String) = "$prefix-${counter++}"

    fun addNest(
        point: GeoPoint,
        beachId: String,
        isNest: Boolean,
        exposure: SunExposure?,
        cageInstalled: Boolean,
        clutchSizeEst: Int?,
        hasPhoto: Boolean,
        locationSource: LocationSource,
        visibility: Visibility,
    ): String {
        val s = _state.value
        val (fLow, fHigh, inc) = predictTsd(exposure)
        val confirmed = hasPhoto && (locationSource == LocationSource.PHOTO_EXIF || locationSource == LocationSource.DEVICE_GPS)
        val id = nextId("nest")
        val code = "GZP-${s.nests.size + 25}"
        val photos = if (hasPhoto) listOf(PhotoRef(nextId("ph"), PhotoSource.CAMERA)) else emptyList()
        val nest = Nest(
            id = id,
            code = code,
            beachId = beachId,
            point = point,
            isNest = isNest,
            confidence = if (confirmed) NestConfidence.CONFIRMED else NestConfidence.UNCONFIRMED,
            visibility = visibility,
            foundDate = today(),
            clutchSizeEst = clutchSizeEst,
            cageInstalled = cageInstalled,
            exposure = exposure,
            locationSource = locationSource,
            incubationDaysEst = if (isNest) inc else 0,
            status = if (isNest) NestStatus.INCUBATING else NestStatus.FALSE_CRAWL,
            predictedFemaleLow = if (isNest) fLow else null,
            predictedFemaleHigh = if (isNest) fHigh else null,
            airTempC = 31.0,
            rainMm7d = 0.0,
            photos = photos,
            updates = listOf(
                NestUpdate(nextId("u"), UpdateKind.FOUND, body = if (isNest) "Nest found" else "False crawl logged", dateLabel = "Today"),
            ),
            temps = listOf(TemperatureReading("Today", "weather_api", 31.0)),
        )
        _state.value = s.copy(nests = s.nests + nest)
        return id
    }

    fun addSimpleMarker(type: MarkerType, point: GeoPoint, note: String) {
        val s = _state.value
        _state.value = s.copy(markers = s.markers + SimpleMarker(nextId("m"), type, point, note))
    }

    fun addUpdate(nestId: String, kind: UpdateKind, body: String, condition: ObsCondition? = null) {
        update(nestId) { n ->
            n.copy(updates = n.updates + NestUpdate(nextId("u"), kind, condition = condition, body = body, dateLabel = "Today"))
        }
    }

    fun setStatus(nestId: String, status: NestStatus, comment: String = "") {
        update(nestId) { n ->
            val upd = n.updates + NestUpdate(nextId("u"), UpdateKind.STATUS_CHANGE, newStatus = status, body = comment, dateLabel = "Today")
            n.copy(status = status, updates = upd)
        }
    }

    fun setExcavation(nestId: String, exc: Excavation) {
        val before = _state.value.nest(nestId)
        update(nestId) { n ->
            n.copy(
                status = NestStatus.EXCAVATED,
                excavation = exc,
                updates = n.updates + NestUpdate(nextId("u"), UpdateKind.EXCAVATED, body = "Excavated · ${exc.hatchSuccessPct ?: 0}% hatch success", dateLabel = "Today"),
            )
        }
        // credit hatchlings to the volunteer's impact
        val added = exc.hatchlingsToSea - (before?.excavation?.hatchlingsToSea ?: 0)
        if (added != 0) {
            val s = _state.value
            _state.value = s.copy(profile = s.profile.copy(hatchlingsReached = (s.profile.hatchlingsReached + added).coerceAtLeast(0)))
        }
    }

    fun addPatrol(beachId: String, meters: Int) {
        val s = _state.value
        val p = Patrol(nextId("p"), beachId, meters, "Today", s.profile.displayName)
        _state.value = s.copy(
            patrols = s.patrols + p,
            profile = s.profile.copy(
                patrols = s.profile.patrols + 1,
                kmWalked = s.profile.kmWalked + meters / 1000.0,
            ),
        )
    }

    private inline fun update(nestId: String, transform: (Nest) -> Nest) {
        val s = _state.value
        _state.value = s.copy(nests = s.nests.map { if (it.id == nestId) transform(it) else it })
    }
}

private fun seedState(): AppState {
    val t = today()
    fun ago(days: Int) = t.minus(DatePeriod(days = days))

    val community = Community(
        id = "gazipasa-caretta",
        name = "Gazipaşa Caretta",
        tagline = "Protecting loggerheads & sand lilies",
        websiteUrl = "https://carettafriends.com",
        whatsappUrl = "https://chat.whatsapp.com/gazipasa-caretta",
        instagramUrl = "https://instagram.com/gazipasa_caretta_ve_kumzambagi",
    )
    val bidibidi = Beach("bidibidi", community.id, "Bıdı Bıdı", "Gazipaşa", GeoPoint(36.2691, 32.3108), "Mert", "🦊")
    val selinus = Beach("selinus", community.id, "Selinus", "Gazipaşa", GeoPoint(36.2760, 32.2980), "Lena", "🐬")

    val nest24 = Nest(
        id = "nest-24", code = "GZP-24", beachId = bidibidi.id, point = GeoPoint(36.2694, 32.3111),
        confidence = NestConfidence.CONFIRMED, foundDate = ago(38), clutchSizeEst = 92, cageInstalled = true,
        exposure = SunExposure.PARTIAL, locationSource = LocationSource.DEVICE_GPS, incubationDaysEst = 55,
        status = NestStatus.INCUBATING, predictedFemaleLow = 70, predictedFemaleHigh = 85, airTempC = 31.0, rainMm7d = 0.0,
        photos = listOf(PhotoRef("ph1", PhotoSource.CAMERA)),
        updates = listOf(
            NestUpdate("u1", UpdateKind.FOUND, body = "Found & caged · clutch ~92", author = "Ayşe", dateLabel = "May 30"),
            NestUpdate("u2", UpdateKind.OBSERVATION, condition = ObsCondition.OK, body = "Patrol — all OK", dateLabel = "Jun 14"),
            NestUpdate("u3", UpdateKind.OBSERVATION, condition = ObsCondition.PREDATED, body = "Fox tracks nearby · cage reinforced", author = "Mert", dateLabel = "Jul 02"),
            NestUpdate("u4", UpdateKind.COMMENT, body = "Storm tonight — check the cage tomorrow AM", author = "Mert", dateLabel = "Jul 15"),
        ),
        temps = listOf(TemperatureReading("today", "weather_api", 31.0)),
    )
    val nest25 = Nest(
        id = "nest-25", code = "GZP-25", beachId = bidibidi.id, point = GeoPoint(36.2688, 32.3101),
        confidence = NestConfidence.CONFIRMED, foundDate = ago(51), clutchSizeEst = 78, cageInstalled = true,
        exposure = SunExposure.FULL_SUN, incubationDaysEst = 55, status = NestStatus.HATCHING,
        predictedFemaleLow = 80, predictedFemaleHigh = 95, airTempC = 32.0,
        photos = listOf(PhotoRef("ph2", PhotoSource.CAMERA)),
        updates = listOf(NestUpdate("u5", UpdateKind.FOUND, body = "Found & caged", author = "Lena", dateLabel = "May 17")),
    )
    val nest26 = Nest(
        id = "nest-26", code = "GZP-26", beachId = selinus.id, point = GeoPoint(36.2762, 32.2984),
        confidence = NestConfidence.UNCONFIRMED, foundDate = ago(3), locationSource = LocationSource.MANUAL_MAP,
        exposure = SunExposure.SHADE, incubationDaysEst = 60, status = NestStatus.INCUBATING,
        predictedFemaleLow = 45, predictedFemaleHigh = 70,
        updates = listOf(NestUpdate("u6", UpdateKind.FOUND, body = "Reported — needs photo/pin", dateLabel = "Today")),
    )
    val nest20 = Nest(
        id = "nest-20", code = "GZP-20", beachId = selinus.id, point = GeoPoint(36.2758, 32.2975),
        confidence = NestConfidence.CONFIRMED, foundDate = ago(64), incubationDaysEst = 55, status = NestStatus.EXCAVATED,
        predictedFemaleLow = 75, predictedFemaleHigh = 90,
        excavation = Excavation(shells = 71, unhatched = 6, pipped = 2, inNest = 1, helpedOut = 3),
        photos = listOf(PhotoRef("ph3", PhotoSource.CAMERA)),
        updates = listOf(
            NestUpdate("u7", UpdateKind.FOUND, body = "Found & caged", dateLabel = "May 4"),
            NestUpdate("u8", UpdateKind.HATCHED, newStatus = NestStatus.HATCHED, body = "Hatched! 🐢", dateLabel = "Jun 30"),
        ),
    )

    return AppState(
        community = community,
        beaches = listOf(bidibidi, selinus),
        nests = listOf(nest24, nest25, nest26, nest20),
        markers = listOf(
            SimpleMarker("m1", MarkerType.LANDMARK, GeoPoint(36.2680, 32.3120), "Big rock reference"),
            SimpleMarker("m2", MarkerType.TRASH, GeoPoint(36.2700, 32.3095), "Net washed ashore"),
        ),
        patrols = listOf(Patrol("p1", bidibidi.id, 2300, "6:10 today", "Mert")),
        facts = listOf(
            Fact("f1", "🌡️", "Warmer sand makes more females — above ~29°C a nest skews female."),
            Fact("f2", "🌙", "Hatchlings emerge mostly at night and find the sea by the bright horizon."),
            Fact("f3", "🔦", "Artificial light disorients hatchlings — keep beaches dark in nesting season."),
        ),
        guide = listOf(
            GuideArticle("g1", "🥚", "Found a nest? Do this", "https://carettafriends.com/found-a-nest"),
            GuideArticle("g2", "🔦", "No flash, no lights at night", "https://carettafriends.com/lights"),
            GuideArticle("g3", "🚸", "Someone disturbing a nest?", "https://carettafriends.com/disturbance"),
            GuideArticle("g4", "🐣", "Helping stuck hatchlings out", "https://carettafriends.com/hatchlings"),
        ),
        badges = listOf(
            Badge("first_nest", "🥚", "First nest", true),
            Badge("first_dig", "⛏️", "First excavation", true),
            Badge("rescuer", "🐢", "Rescuer", true),
            Badge("season50", "👑", "Season 50", false),
        ),
        profile = Profile(
            displayName = "Ayşe K.", avatar = "🐢", role = "Guardian · Bıdı Bıdı & Selinus",
            hatchlingsReached = 312, streakDays = 12, kmWalked = 48.0, patrols = 24,
        ),
    )
}
