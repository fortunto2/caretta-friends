package com.carettafriends

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.carettafriends.content.AppStrings
import com.carettafriends.content.appStrings
import com.carettafriends.data.CarettaRepository
import com.carettafriends.data.PendingPhoto
import com.carettafriends.domain.GeoPoint
import com.carettafriends.domain.nearestBeach
import com.carettafriends.ui.components.EmptyHint
import com.carettafriends.ui.screens.AddNestScreen
import com.carettafriends.ui.screens.BeachDetailScreen
import com.carettafriends.ui.screens.BeachesScreen
import com.carettafriends.ui.screens.CommunityScreen
import com.carettafriends.ui.screens.ExcavationScreen
import com.carettafriends.ui.screens.LearnScreen
import com.carettafriends.ui.screens.MapScreen
import com.carettafriends.ui.screens.MemberProfileScreen
import com.carettafriends.ui.screens.NestDetailScreen
import com.carettafriends.ui.screens.OnboardingScreen
import com.carettafriends.ui.screens.ProfileScreen
import com.carettafriends.ui.screens.StatsScreen
import com.carettafriends.ui.theme.CarettaTheme
import platform.UIKit.UIViewController

/**
 * iOS hybrid entry: SwiftUI owns the chrome (native iOS 26 Liquid Glass tab bar + navigation),
 * each content screen is the SHARED Compose UI, hosted per-screen and reading one shared repo.
 */
object SharedRepo {
    val repo = CarettaRepository()
}

/** A nest point for the native iOS MapLibre map. [dateShort] = found date "d.MM" (for the AR badge). */
data class IosMapPoint(
    val id: String, val lat: Double, val lng: Double, val title: String, val status: String,
    val dateShort: String = "",
)

private fun shortDate(d: kotlinx.datetime.LocalDate): String =
    "${d.dayOfMonth}.${d.monthNumber.toString().padStart(2, '0')}"

/** Nest points for the native SwiftUI MapLibre map (read from the shared repo). */
fun mapPoints(): List<IosMapPoint> {
    val s = SharedRepo.repo.state.value
    return s.nests.map {
        IosMapPoint(it.id, it.point.lat, it.point.lng, it.code, com.carettafriends.data.nestMapPhase(it), shortDate(it.foundDate))
    }
}

/** Timelapse bounds: the earliest nest found-day and today (epoch days). */
fun firstNestDay(): Int {
    val s = SharedRepo.repo.state.value
    val today = com.carettafriends.data.today().toEpochDays()
    return (s.nests.minOfOrNull { it.foundDate.toEpochDays() } ?: (today - 14)).coerceAtMost(today)
}

fun todayEpochDay(): Int = com.carettafriends.data.today().toEpochDays()

/** Nest points found on or before [day] (epoch days) — drives the map timelapse. */
fun mapPointsUpTo(day: Int): List<IosMapPoint> {
    val s = SharedRepo.repo.state.value
    return s.nests.filter { it.foundDate.toEpochDays() <= day }
        .map { IosMapPoint(it.id, it.point.lat, it.point.lng, it.code, com.carettafriends.data.nestMapPhase(it)) }
}

/** Beach points (name-labelled) for the native map. [status] encodes the dot colour:
 *  "beach-green" = protected, "beach-amber" = unprotected. Tappable → opens the beach card. */
fun beachPoints(): List<IosMapPoint> {
    val s = SharedRepo.repo.state.value
    return s.beaches.map {
        IosMapPoint(it.id, it.center.lat, it.center.lng, it.name, if (it.protected) "beach-green" else "beach-amber")
    }
}

/** The baked official protected nesting areas (green overview dots). Status "area" = not openable
 *  (they're country-overview markers, not community beaches with a detail page). */
fun protectedAreaPoints(): List<IosMapPoint> {
    val s = SharedRepo.repo.state.value
    return s.protectedAreas.map { IosMapPoint("pa:${it.name}", it.lat, it.lng, it.name, "area") }
}

/** Community hubs — our own + collected local groups (orange squares, tap → community screen). */
fun communityPoints(): List<IosMapPoint> {
    val s = SharedRepo.repo.state.value
    return s.allCommunities.map { IosMapPoint("cm:${it.id}", it.center.lat, it.center.lng, it.name, "community") }
}

/** Recent rule-violation reports (red dots) — only the last 14 days surface on the map (they stay in
 *  the DB for CİMER complaints). Shown only under the map's Violations filter. */
fun violationPoints(): List<IosMapPoint> {
    val s = SharedRepo.repo.state.value
    val cutoff = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - 14L * 24 * 3600 * 1000
    return s.markers
        .filter { it.type == com.carettafriends.domain.MarkerType.VIOLATION && it.createdEpochMillis >= cutoff }
        .map { IosMapPoint("vi:${it.id}", it.point.lat, it.point.lng, it.violationKind?.name ?: "", "violation") }
}

/** A beach's OSM sand outline for the native map. [polygonCsv] = "lat,lng;lat,lng;…" (empty = no outline). */
data class IosBeachShape(val id: String, val name: String, val isProtected: Boolean, val polygonCsv: String)

fun beachShapes(): List<IosBeachShape> {
    val s = SharedRepo.repo.state.value
    return s.beaches.map { b ->
        IosBeachShape(b.id, b.name, b.protected, b.polygon.joinToString(";") { "${it.lat},${it.lng}" })
    }
}

/** The id of our own (primary) community — for opening it from the Profile screen. */
fun primaryCommunityId(): String = SharedRepo.repo.state.value.community.id

/** Localized bottom-nav labels for the native iOS tab bar (follows Profile → Language). */
data class IosNavLabels(val map: String, val beaches: String, val learn: String, val profile: String)

fun navLabels(): IosNavLabels {
    val s = appStrings(SharedRepo.repo.state.value.profile.language)
    return IosNavLabels(s.navMap, s.navBeaches, s.navLearn, s.navProfile)
}

/** The current localized string catalog for the native iOS chrome (map chips, beach tooltip, patrol
 *  dialog, camera). Swift reads fields directly (`currentStrings().filterAll`, `.protectedBeachLabel`…)
 *  so no per-string bridge struct has to grow. Follows Profile → Language, same as [navLabels]. */
fun currentStrings(): AppStrings = appStrings(SharedRepo.repo.state.value.profile.language)

/** Current air quality for the native map's pill. level = GOOD/MODERATE/UNHEALTHY/DUST. null = no sensor. */
data class IosAirSignal(val emoji: String, val label: String, val value: String)

data class IosAir(
    val level: String,
    val pm25: Int,
    val pm10: Int,
    val patrolAdvisable: Boolean,
    val advice: String,
    val comfort: Int, // -1 = none
    val signals: List<IosAirSignal>,
)

fun airStatus(): IosAir? {
    val a = SharedRepo.repo.state.value.air ?: return null
    return IosAir(
        a.level.name, a.pm25.toInt(), a.pm10.toInt(), a.patrolAdvisable, a.advice, a.comfort ?: -1,
        a.signals.map { IosAirSignal(it.emoji, it.label, it.value) },
    )
}

/** Report the device's current location (one-shot from the native map) for "beaches near me". */
fun setDeviceLocation(lat: Double, lng: Double) = SharedRepo.repo.setDeviceLocation(lat, lng)

/** A map-focus request coordinate for the native map (bridged from [CarettaRepository.takeMapFocus]). */
data class IosGeo(val lat: Double, val lng: Double)

/** Read-and-clear a pending "centre the map here" request (set by a nest's geo card). null = none. */
fun takeMapFocus(): IosGeo? = SharedRepo.repo.takeMapFocus()?.let { IosGeo(it.lat, it.lng) }

/** Context-aware "+" (B3): ask the on-screen nest detail to open its add-update dialog for [nestId]. */
fun requestAddUpdate(nestId: String) = SharedRepo.repo.requestAddUpdate(nestId)

/** Save a recorded patrol from the Swift GPS recorder. trackCsv = "lat,lng;lat,lng;…". Returns the id.
 *  On-device only until [publishPatrol] — the walk is never shared live. */
fun savePatrol(meters: Int, seconds: Int, trackCsv: String): String {
    val track = trackCsv.split(";").mapNotNull { seg ->
        val parts = seg.split(",")
        val lat = parts.getOrNull(0)?.toDoubleOrNull()
        val lng = parts.getOrNull(1)?.toDoubleOrNull()
        if (lat != null && lng != null) GeoPoint(lat, lng) else null
    }
    val beaches = SharedRepo.repo.state.value.beaches
    val beachId = track.firstOrNull()?.let { nearestBeach(it, beaches)?.first?.id }
        ?: beaches.firstOrNull()?.id ?: ""
    return SharedRepo.repo.addPatrol(beachId, meters, track, seconds)
}

/** Publish a recorded patrol (Swift). Only then does it sync to the cloud. */
fun publishPatrol(id: String) = SharedRepo.repo.publishPatrol(id)

/** Called from Swift after the native camera captures/picks a photo (with optional GPS).
 *  [sourceId] identifies the ORIGINAL image the volunteer chose (the Photos asset id) — the file
 *  itself can't be used: the camera screen re-encodes every import with a burned-in overlay, so the
 *  same photo would produce different bytes and slip past the duplicate check. Empty for a live
 *  capture, which is new by definition. */
fun setPendingPhoto(path: String, lat: Double, lng: Double, hasLocation: Boolean, sourceId: String) {
    SharedRepo.repo.pendingPhoto = PendingPhoto(
        path = path,
        lat = if (hasLocation) lat else null,
        lng = if (hasLocation) lng else null,
        hash = sourceId.takeIf { it.isNotBlank() }?.let { "asset:$it" },
    )
}

private fun host(content: @Composable () -> Unit): UIViewController =
    ComposeUIViewController { CarettaTheme { content() } }

fun MapVC(onOpenNest: (String) -> Unit, onAdd: () -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    MapScreen(state, onAdd, onOpenNest)
}

fun BeachesVC(onOpenNest: (String) -> Unit, onOpenBeach: (String) -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    BeachesScreen(state, onOpenNest, onOpenBeach)
}

fun BeachDetailVC(
    beachId: String,
    onBack: () -> Unit,
    onOpenNest: (String) -> Unit,
    onOpenMember: (String) -> Unit,
): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    val b = state.beach(beachId)
    if (b != null) BeachDetailScreen(b, state, onBack, onOpenNest, onOpenMember) else EmptyHint("🏖️", appStrings(state.profile.language).beachNotFound)
}

fun LearnVC(): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    LearnScreen(state, SharedRepo.repo)
}

fun ProfileVC(
    onOpenCommunity: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenNest: (String) -> Unit,
    onOpenBeach: (String) -> Unit,
    onAddNest: () -> Unit,
): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    ProfileScreen(SharedRepo.repo, state, onOpenCommunity, onOpenStats, onOpenNest, onOpenBeach, onAddNest)
}

fun StatsVC(onBack: () -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    StatsScreen(state, onBack)
}

/** True if the first-run intro was already dismissed. */
fun isOnboarded(): Boolean = SharedRepo.repo.state.value.profile.onboarded

fun OnboardingVC(onDone: () -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    OnboardingScreen(state) { SharedRepo.repo.setOnboarded(); onDone() }
}

fun AddNestVC(onDone: () -> Unit, onCamera: () -> Unit, onNestSaved: (String) -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    AddNestScreen(SharedRepo.repo, state, onDone, onCamera, onNestSaved)
}

fun NestDetailVC(
    nestId: String,
    onBack: () -> Unit,
    onExcavate: (String) -> Unit,
    onOpenMember: (String) -> Unit,
    onOpenMap: () -> Unit,
): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    val n = state.nest(nestId)
    if (n != null) {
        NestDetailScreen(n, SharedRepo.repo, onBack, { onExcavate(n.id) }, onOpenMember, onOpenMap)
    } else {
        EmptyHint("🐢", appStrings(state.profile.language).nestNotFound)
    }
}

fun ExcavationVC(nestId: String, onBack: () -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    val n = state.nest(nestId)
    if (n != null) {
        ExcavationScreen(n, SharedRepo.repo, state.profile.language, onBack)
    } else {
        EmptyHint("🐢", appStrings(state.profile.language).nestNotFound)
    }
}

fun CommunityVC(communityId: String, onBack: () -> Unit, onOpenMember: (String) -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    CommunityScreen(state.communityOrPrimary(communityId), state, onBack, onOpenMember)
}

fun MemberVC(
    memberKey: String,
    onBack: () -> Unit,
    onOpenNest: (String) -> Unit,
    onOpenBeach: (String) -> Unit,
): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    MemberProfileScreen(state.resolveMember(memberKey), state, onBack, onOpenNest, onOpenBeach)
}
