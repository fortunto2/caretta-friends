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
fun MapScreen(state: AppState, onAddMarker: () -> Unit, onOpenNest: (String) -> Unit) {
    val c = caretta
    var filter by remember { mutableStateOf("all") }

    val markers = buildList {
        state.nests.filter { showNest(filter, it) }
            .forEach { add(MapMarker(it.id, it.point.lat, it.point.lng, isBeach = false)) }
        state.markers.filter { showMarker(filter, it) }
            .forEach { add(MapMarker(it.id, it.point.lat, it.point.lng, isBeach = false)) }
        // Beaches are always shown (reference) — highlighted by their OSM sand polygon.
        state.beaches.forEach { add(MapMarker(it.id, it.center.lat, it.center.lng, isBeach = true, label = it.name, polygon = it.polygon)) }
    }

    Box(Modifier.fillMaxSize()) {
        OsmMap(Modifier.fillMaxSize(), markers) { id -> onOpenNest(id) }

        // --- Top overlays: filter chips + coverage chip ---
        Column(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(top = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip("all", "All", filter) { filter = it }
                FilterChip("nests", "🥚 Nests", filter) { filter = it }
                FilterChip("hatching", "● Hatching soon", filter) { filter = it }
                FilterChip("trash", "🧺 Trash", filter) { filter = it }
            }
            Spacer(Modifier.height(10.dp))
            CoveragePill(state)
        }

        // --- Start patrol pill (visual on Android; the GPS recorder is on iOS for now) ---
        Box(
            Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 24.dp)
                .clip(CircleShape).background(c.good).padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text("● Start patrol", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }

        // --- Coral FAB ---
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 22.dp)
                .size(56.dp).clip(RoundedCornerShape(18.dp)).background(c.coral)
                .clickable { onAddMarker() },
            contentAlignment = Alignment.Center,
        ) {
            Text("＋", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun showNest(filter: String, nest: Nest): Boolean = when (filter) {
    "all", "nests" -> true
    "hatching" -> nest.status == NestStatus.HATCHING
    else -> false
}

private fun showMarker(filter: String, marker: SimpleMarker): Boolean = when (filter) {
    "all" -> true
    "trash" -> marker.type == MarkerType.TRASH
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
        "No patrol yet today"
    }
    Box(
        Modifier.padding(start = 14.dp)
            .clip(CircleShape).background(c.good.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}
