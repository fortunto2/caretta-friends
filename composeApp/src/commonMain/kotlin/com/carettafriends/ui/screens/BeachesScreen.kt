package com.carettafriends.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Beach
import com.carettafriends.domain.NestStatus
import com.carettafriends.domain.distanceLabel
import com.carettafriends.domain.distanceMeters
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.EmptyHint
import com.carettafriends.ui.components.Pill
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

@Composable
fun BeachesScreen(state: AppState, onOpenNest: (String) -> Unit, onOpenBeach: (String) -> Unit = {}) {
    // Scope: null = "near me" (geo, auto) / all; a city string = pinned to that town.
    var cityScope by remember { mutableStateOf<String?>(null) }
    var showScopePicker by remember { mutableStateOf(false) }
    val me = state.deviceLocation
    // Untagged (OSM-discovered) beaches fall under the home town so the picker stays coherent now
    // and splits naturally once beaches carry real city tags in more towns.
    val homeCity = state.beaches.firstOrNull { it.city.isNotBlank() }?.city?.trim() ?: "Gazipaşa"
    val cityOf: (Beach) -> String = { it.city.trim().ifBlank { homeCity } }
    val cities = state.beaches.map(cityOf).distinct().sorted()
    val s = com.carettafriends.content.appStrings(state.profile.language)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(s.beaches)
        Column(
            Modifier.padding(horizontal = 15.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScopeHeader(
                label = cityScope ?: if (me != null) s.nearYou else s.allBeaches,
                change = s.change,
                canChange = cities.isNotEmpty(),
                onChange = { showScopePicker = true },
            )
            if (state.beaches.isEmpty()) {
                EmptyHint("🏖️", s.findingBeaches)
            } else {
                val scoped = if (cityScope != null) {
                    state.beaches.filter { cityOf(it).equals(cityScope, ignoreCase = true) }
                } else {
                    state.beaches
                }
                val ordered = if (me != null) scoped.sortedBy { distanceMeters(me, it.center) } else scoped
                if (ordered.isEmpty()) {
                    EmptyHint("🏖️", "No beaches in $cityScope yet.")
                } else {
                    ordered.forEach { beach ->
                        val nests = state.nests.filter { it.beachId == beach.id }
                        BeachListCard(
                            beach = beach,
                            total = nests.size,
                            active = nests.count { it.status == NestStatus.INCUBATING || it.status == NestStatus.HATCHING },
                            hatchingSoon = nests.count { it.status == NestStatus.HATCHING },
                            hatched = nests.count { it.status == NestStatus.HATCHED || it.status == NestStatus.EXCAVATED },
                            patrolled = state.patrols.any { it.beachId == beach.id },
                            distanceAway = me?.let { distanceLabel(distanceMeters(it, beach.center)) },
                            onOpenBeach = onOpenBeach,
                        )
                    }
                }
            }
        }
    }

    if (showScopePicker) {
        ScopePickerDialog(
            title = s.showBeaches,
            nearMe = s.nearMeAuto,
            allLabel = s.allBeaches,
            close = s.close,
            cities = cities,
            current = cityScope,
            hasGeo = me != null,
            onPick = { cityScope = it; showScopePicker = false },
            onDismiss = { showScopePicker = false },
        )
    }
}

/** Location scope header: "📍 Near you / [City]" + a Change chip that opens the city picker. */
@Composable
private fun ScopeHeader(label: String, change: String, canChange: Boolean, onChange: () -> Unit) {
    val c = caretta
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("📍 $label", color = c.deep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        if (canChange) {
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(c.sand)
                    .border(1.dp, c.line, RoundedCornerShape(10.dp))
                    .clickable(onClick = onChange).padding(horizontal = 11.dp, vertical = 6.dp),
            ) { Text(change, color = c.sea, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun ScopePickerDialog(
    title: String,
    nearMe: String,
    allLabel: String,
    close: String,
    cities: List<String>,
    current: String?,
    hasGeo: Boolean,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = caretta
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = c.surface) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = c.deep, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                ScopeRow(if (hasGeo) nearMe else "🏖️ $allLabel", current == null) { onPick(null) }
                cities.forEach { city -> ScopeRow("🏙️ $city", current.equals(city, ignoreCase = true)) { onPick(city) } }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(close, color = c.muted) }
            }
        }
    }
}

@Composable
private fun ScopeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.sea.copy(alpha = 0.12f) else c.sand)
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (selected) Text("✓", color = c.sea, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** One compact beach card in the list — metrics only; tap to open the beach (its nests live inside). */
@Composable
private fun BeachListCard(
    beach: Beach,
    total: Int,
    active: Int,
    hatchingSoon: Int,
    hatched: Int,
    patrolled: Boolean,
    distanceAway: String?,
    onOpenBeach: (String) -> Unit,
) {
    val c = caretta
    val amber = Color(0xFFE0A82E)
    val accent = if (beach.protected) c.good else amber
    CarettaCard(modifier = Modifier.clickable { onOpenBeach(beach.id) }) {
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // thumbnail — protection-tinted (beach photo goes here once uploaded).
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(listOf(accent, c.deep))),
                    contentAlignment = Alignment.Center,
                ) { Text("🏖️", fontSize = 24.sp) }
                Column(Modifier.weight(1f)) {
                    Text(beach.name, color = c.deep, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        buildString {
                            append(if (distanceAway != null) "$distanceAway away" else beach.city.ifBlank { "Beach" })
                            if (beach.protected) append(" · 🛡️ protected")
                        },
                        color = if (beach.protected) accent else c.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (patrolled) Pill("✓", c.good)
                Text("›", color = c.muted, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            // metrics only — the nests themselves are inside the beach.
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Pill("🥚 $total nests", c.deep)
                if (active > 0) Pill("● $active active", c.sea)
                if (hatchingSoon > 0) Pill("🐣 $hatchingSoon soon", c.warn)
                if (hatched > 0) Pill("🐢 $hatched hatched", c.good)
            }
        }
    }
}
