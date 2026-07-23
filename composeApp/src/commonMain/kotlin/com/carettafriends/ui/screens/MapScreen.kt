package com.carettafriends.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.domain.AppState
import com.carettafriends.domain.MarkerType
import com.carettafriends.domain.Nest
import com.carettafriends.domain.NestStatus
import com.carettafriends.domain.SimpleMarker
import com.carettafriends.ui.map.MapMarker
import com.carettafriends.ui.map.OsmMap
import com.carettafriends.ui.theme.caretta

/**
 * Map (home) — a real OSM / MapLibre map ([OsmMap]) with SwiftUI-style Compose overlays.
 * Nests are coral dots, beaches are teal labelled dots; tapping a nest opens its detail.
 */
@Composable
fun MapScreen(
    state: AppState,
    onAddMarker: () -> Unit,
    onOpenNest: (String) -> Unit,
    onOpenBeach: (String) -> Unit = {},
    onOpenCommunity: (String) -> Unit = {},
) {
    val c = caretta
    val s = com.carettafriends.content.appStrings(state.profile.language)
    var filter by remember { mutableStateOf("all") }
    var tappedBeach by remember { mutableStateOf<String?>(null) }
    var airExpanded by remember { mutableStateOf(false) }

    val markers = buildList {
        state.nests.filter { showNest(filter, it) }
            .forEach { add(MapMarker(it.id, it.point.lat, it.point.lng, isBeach = false)) }
        // Violations fade off the map after 14 days (they stay in the DB for complaints).
        val recentCutoff = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - 14L * 24 * 3600 * 1000
        state.markers
            .filter { showMarker(filter, it) }
            .filter { it.type != MarkerType.VIOLATION || it.createdEpochMillis >= recentCutoff }
            .forEach { add(MapMarker(it.id, it.point.lat, it.point.lng, isBeach = false, isViolation = it.type == MarkerType.VIOLATION)) }
        // Baked-in: the official protected nesting beaches (loaded from the by-country data file) —
        // green dots, always shown (zoom out for the whole-country overview).
        state.protectedAreas.forEach {
            add(MapMarker("pa:${it.name}", it.lat, it.lng, isBeach = true, label = it.name, protected = true))
        }
        // Auto-discovered community beaches (cached on demand): sand polygon + dot, green if protected.
        state.beaches.forEach {
            add(MapMarker(it.id, it.center.lat, it.center.lng, isBeach = true, label = it.name, polygon = it.polygon, protected = it.protected))
        }
        // Community hubs — our own + collected local groups (orange squares, tap → community).
        state.allCommunities.forEach {
            add(MapMarker("cm:${it.id}", it.center.lat, it.center.lng, label = it.name, isCommunity = true))
        }
    }

    Box(Modifier.fillMaxSize()) {
        OsmMap(
            Modifier.fillMaxSize(),
            markers,
            onClick = { id -> if (state.nest(id) != null) onOpenNest(id) },
            onBeachTap = { id -> tappedBeach = id },
            onCommunityTap = { id -> onOpenCommunity(id.removePrefix("cm:")) },
        )

        // --- Top overlays: filter chips + coverage chip ---
        Column(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(top = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip("all", s.filterAll, filter) { filter = it }
                FilterChip("nests", s.filterNests, filter) { filter = it }
                FilterChip("hatching", s.filterHatching, filter) { filter = it }
                FilterChip("trash", s.filterTrash, filter) { filter = it }
                FilterChip("violations", s.filterViolations, filter) { filter = it }
            }
            Spacer(Modifier.height(10.dp))
            CoveragePill(state)
            state.air?.let { air ->
                Spacer(Modifier.height(8.dp))
                AirPill(air, s) { airExpanded = !airExpanded }
                if (airExpanded) {
                    Spacer(Modifier.height(6.dp))
                    AirDetail(air)
                }
            }
        }

        // --- Start patrol pill (visual on Android; the GPS recorder is on iOS for now).
        //     Turns to a dust warning when the air layer says patrolling isn't advisable. ---
        val patrolBlocked = state.air?.patrolAdvisable == false
        Box(
            Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 24.dp)
                .clip(CircleShape).background(if (patrolBlocked) c.coral else c.good)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(
                if (patrolBlocked) s.dustNotAdvised else s.startPatrol,
                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
            )
        }

        // (Add-nest "+" moved to the centre of the bottom bar — no map FAB.)

        // --- Beach tooltip (shown when a beach is tapped) ---
        tappedBeach?.let { id ->
            val beach = state.beaches.firstOrNull { it.id == id }
            val protectedFlag = beach?.protected ?: id.startsWith("pa:")
            BeachTooltip(
                name = beach?.name ?: id.removePrefix("pa:"),
                protectedBeach = protectedFlag,
                official = id.startsWith("pa:"),
                canOpen = beach != null,
                onOpen = { beach?.let { onOpenBeach(it.id) }; tappedBeach = null },
                onDismiss = { tappedBeach = null },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp, start = 14.dp, end = 14.dp),
            )
        }
    }
}

private val protectedGreen = Color(0xFF2E9E5B)
private val unprotectedAmber = Color(0xFFE0A82E)

@Composable
private fun BeachTooltip(
    name: String,
    protectedBeach: Boolean,
    official: Boolean,
    canOpen: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = caretta
    val accent = if (protectedBeach) protectedGreen else unprotectedAmber
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface)
            .border(1.5.dp, accent, RoundedCornerShape(16.dp))
            .then(if (canOpen) Modifier.clickable { onOpen() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(accent))
        Column(Modifier.weight(1f)) {
            Text(name, color = c.deep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                if (protectedBeach) "🛡️ Protected nesting beach" else "Beach",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (canOpen) Text("Open ›", color = c.sea, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Text("✕", color = c.muted, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clickable { onDismiss() })
    }
}

private fun showNest(filter: String, nest: Nest): Boolean = when (filter) {
    "all", "nests" -> true
    "hatching" -> nest.status == NestStatus.HATCHING
    else -> false
}

private fun showMarker(filter: String, marker: SimpleMarker): Boolean = when (filter) {
    "all" -> marker.type != MarkerType.VIOLATION // violations are filter-gated (hidden from casual view)
    "trash" -> marker.type == MarkerType.TRASH
    "violations" -> marker.type == MarkerType.VIOLATION
    else -> false
}

@Composable
private fun FilterChip(key: String, label: String, selected: String, onSelect: (String) -> Unit) {
    val c = caretta
    val on = key == selected
    Box(
        Modifier.clip(CircleShape)
            .background(if (on) c.sea else c.surface)
            .border(1.dp, if (on) c.sea else c.line, CircleShape)
            .clickable { onSelect(key) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (on) Color.White else c.ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun CoveragePill(state: AppState) {
    val c = caretta
    val patrol = state.patrols.lastOrNull()
    val label = if (patrol != null) {
        val km = (patrol.distanceMeters / 100).toInt() / 10.0
        "✓ Patrolled ${patrol.startedLabel} · ${patrol.by} · $km km"
    } else {
        com.carettafriends.content.appStrings(state.profile.language).noPatrolYet
    }
    Box(
        Modifier.padding(start = 14.dp)
            .clip(CircleShape).background(c.good.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Supplementary air-quality pill (Air Signal + Sensor.Community). Shown only where a sensor/model
 *  reading exists; turns red on dust / unhealthy so volunteers know when patrolling isn't advisable.
 *  Tap to expand the extra signals (waves / fire / UV / …). */
@Composable
private fun AirPill(air: com.carettafriends.domain.AirStatus, s: com.carettafriends.content.AppStrings, onClick: () -> Unit) {
    val amber = Color(0xFFE0A82E)
    val red = Color(0xFFE0533D)
    val green = Color(0xFF2E9E5B)
    val (bg, emoji, text) = when (air.level) {
        com.carettafriends.domain.AirLevel.DUST -> Triple(red, "🌫️", "${s.airDustLabel} · PM10 ${air.pm10.toInt()} — ${s.patrolNotAdvised}")
        com.carettafriends.domain.AirLevel.UNHEALTHY -> Triple(red, "😷", "${s.airUnhealthy} · PM2.5 ${air.pm25.toInt()}")
        com.carettafriends.domain.AirLevel.MODERATE -> Triple(amber, "🌤️", "${s.airModerate} · PM2.5 ${air.pm25.toInt()}")
        com.carettafriends.domain.AirLevel.GOOD -> Triple(green, "🍃", "${s.airCleanLabel} · PM2.5 ${air.pm25.toInt()}")
    }
    val comfortSuffix = air.comfort?.let { " · ☺ $it" } ?: ""
    val more = if (air.signals.isNotEmpty()) "  ›" else ""
    Box(
        Modifier.padding(start = 14.dp).clip(CircleShape).background(bg.copy(alpha = 0.94f))
            .clickable(enabled = air.signals.isNotEmpty()) { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text("$emoji $text$comfortSuffix$more", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Expanded air panel (tap the pill): the safety advice + the extra environmental signals. */
@Composable
private fun AirDetail(air: com.carettafriends.domain.AirStatus) {
    val c = caretta
    Box(
        Modifier.padding(start = 14.dp, end = 14.dp).clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.96f)).padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(air.advice, color = c.deep, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            air.signals.forEach { s ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.emoji, fontSize = 13.sp)
                    Text(s.label, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(88.dp))
                    Text(s.value, color = c.deep, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
