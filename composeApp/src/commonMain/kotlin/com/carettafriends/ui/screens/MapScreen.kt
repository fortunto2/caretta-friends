package com.carettafriends.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.data.nestDay
import com.carettafriends.domain.AppState
import com.carettafriends.domain.GeoPoint
import com.carettafriends.domain.MarkerType
import com.carettafriends.domain.Nest
import com.carettafriends.domain.NestConfidence
import com.carettafriends.domain.NestStatus
import com.carettafriends.domain.SimpleMarker
import com.carettafriends.ui.theme.caretta

/**
 * Map (home) — the hero screen (design-spec 4.2).
 * MapLibre is wired in later by the integrator; for now we render a stylized coastal map:
 * a sea→sand gradient with nest/marker pins positioned by normalizing their lat/lng.
 */
@Composable
fun MapScreen(state: AppState, onAddMarker: () -> Unit, onOpenNest: (String) -> Unit) {
    val c = caretta
    var filter by remember { mutableStateOf("all") }

    // Bounds computed over ALL points so pins keep stable positions when filtering.
    val allPoints = state.nests.map { it.point } + state.markers.map { it.point }
    val n = allPoints.size
    val lats = allPoints.map { it.lat }
    val lngs = allPoints.map { it.lng }
    val minLat = lats.minOrNull() ?: 0.0
    val maxLat = lats.maxOrNull() ?: 0.0
    val minLng = lngs.minOrNull() ?: 0.0
    val maxLng = lngs.maxOrNull() ?: 0.0
    val latRange = maxLat - minLat
    val lngRange = maxLng - minLng

    val shore = lerp(c.sea, c.sand, 0.5f)
    val seaToBeach = Brush.verticalGradient(
        0.0f to c.sea,
        0.50f to c.sea,
        0.70f to shore,
        0.84f to c.sand,
        1.0f to c.sand,
    )

    BoxWithConstraints(Modifier.fillMaxSize().background(seaToBeach)) {
        val pinSize = 34.dp
        val padH = 26.dp
        val padTop = 110.dp
        val padBottom = 96.dp
        val availW = (maxWidth - padH - padH - pinSize).coerceAtLeast(0.dp)
        val availH = (maxHeight - padTop - padBottom).coerceAtLeast(0.dp)

        // Normalized placement: north (higher lat) → top, east (higher lng) → right.
        fun pos(p: GeoPoint, idx: Int): Pair<Dp, Dp> {
            val fx = when {
                lngRange > 1e-9 -> ((p.lng - minLng) / lngRange).toFloat()
                n > 1 -> idx.toFloat() / (n - 1)
                else -> 0.5f
            }.coerceIn(0f, 1f)
            val fy = when {
                latRange > 1e-9 -> ((maxLat - p.lat) / latRange).toFloat()
                n > 1 -> idx.toFloat() / (n - 1)
                else -> 0.5f
            }.coerceIn(0f, 1f)
            return (padH + availW * fx) to (padTop + availH * fy)
        }

        // --- Simple markers (drawn under nests) ---
        state.markers.forEachIndexed { i, m ->
            if (showMarker(filter, m)) {
                val (x, y) = pos(m.point, state.nests.size + i)
                Box(Modifier.offset(x, y)) { MarkerPin(glyphFor(m.type)) }
            }
        }

        // --- Nest pins ---
        state.nests.forEachIndexed { i, nest ->
            if (showNest(filter, nest)) {
                val (x, y) = pos(nest.point, i)
                Box(Modifier.offset(x, y)) {
                    NestPin(
                        nest = nest,
                        day = nestDay(nest),
                        onClick = { onOpenNest(nest.id) },
                    )
                }
            }
        }

        // --- Top overlays: filter chips + coverage chip ---
        Column(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(top = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
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

        // --- Start patrol pill (visual) ---
        Box(
            Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 24.dp)
                .clip(CircleShape).background(c.good)
                .padding(horizontal = 14.dp, vertical = 9.dp),
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

private fun glyphFor(type: MarkerType): String = when (type) {
    MarkerType.LANDMARK -> "🚩"
    MarkerType.TRASH -> "🧺"
    else -> "📍"
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
private fun NestPin(nest: Nest, day: Int, onClick: () -> Unit) {
    val c = caretta
    val unconfirmed = nest.confidence == NestConfidence.UNCONFIRMED
    val ring = when (nest.status) {
        NestStatus.INCUBATING -> c.sea
        NestStatus.HATCHING -> c.warn
        NestStatus.HATCHED, NestStatus.EXCAVATED -> c.good
        NestStatus.PREDATED, NestStatus.WASHED_OVER, NestStatus.POACHED -> c.risk
        NestStatus.LOST, NestStatus.FALSE_CRAWL -> c.muted
    }
    // Teardrop: rounded with one sharp bottom-end corner, pointing down at the location.
    val shape = RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 17.dp, bottomEnd = 3.dp)
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(34.dp)
                .alpha(if (unconfirmed) 0.55f else 1f)
                .clip(shape)
                .background(c.surface)
                .border(2.5.dp, ring, shape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) { Text("🥚", fontSize = 15.sp) }
        // Tiny "day" badge for incubating/hatching nests.
        if (nest.status == NestStatus.INCUBATING || nest.status == NestStatus.HATCHING) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp)
                    .clip(CircleShape).background(ring)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text("$day", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun MarkerPin(glyph: String) {
    val c = caretta
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(c.surface)
            .border(1.5.dp, c.line, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(glyph, fontSize = 13.sp) }
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
        Text(
            label,
            color = if (on) Color.White else c.ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun CoveragePill(state: AppState) {
    val c = caretta
    val patrol = state.patrols.firstOrNull()
    val label = if (patrol != null) {
        val km = (patrol.distanceMeters / 100).toInt() / 10.0
        "✓ Patrolled ${patrol.startedLabel} · ${patrol.by} · $km km"
    } else {
        "✓ No patrol yet today"
    }
    Box(
        Modifier.padding(start = 14.dp)
            .clip(CircleShape).background(c.good.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}
