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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.carettafriends.data.CarettaRepository
import com.carettafriends.domain.AppState
import com.carettafriends.domain.LocationSource
import com.carettafriends.domain.MarkerType
import com.carettafriends.domain.SunExposure
import com.carettafriends.domain.Visibility
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.GhostButton
import com.carettafriends.ui.components.Pill
import com.carettafriends.ui.components.PrimaryButton
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

@Composable
fun AddNestScreen(repo: CarettaRepository, state: AppState, onDone: () -> Unit, onCamera: () -> Unit) {
    val c = caretta

    var markerType by remember { mutableStateOf(MarkerType.NEST) }
    var isNest by remember { mutableStateOf(true) }
    var hasPhoto by remember { mutableStateOf(false) }
    var exposure by remember { mutableStateOf(SunExposure.PARTIAL) }
    var cage by remember { mutableStateOf(false) }
    var visibility by remember { mutableStateOf(Visibility.PUBLIC) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("New marker", onBack = onDone)
        Column(
            Modifier.padding(horizontal = 15.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Type selector ──────────────────────────────────────────
            SectionLabel("What did you find?")
            Segmented(
                listOf(
                    SegOption("🥚 Nest", markerType == MarkerType.NEST) { markerType = MarkerType.NEST },
                    SegOption("🚩 Landmark", markerType == MarkerType.LANDMARK) { markerType = MarkerType.LANDMARK },
                    SegOption("🧺 Trash", markerType == MarkerType.TRASH) { markerType = MarkerType.TRASH },
                ),
            )

            // ── Nest vs false crawl (only for nests) ───────────────────
            if (markerType == MarkerType.NEST) {
                SectionLabel("Nest or false crawl?")
                Segmented(
                    listOf(
                        SegOption("🥚 Nest", isNest) { isNest = true },
                        SegOption("🌀 False crawl", !isNest) { isNest = false },
                    ),
                )
            }

            // ── Photo ──────────────────────────────────────────────────
            SectionLabel("Photo")
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                GhostButton("📷 Camera", Modifier.weight(1f)) { onCamera() }
                GhostButton("🖼️ Gallery", Modifier.weight(1f)) { hasPhoto = true }
            }
            if (hasPhoto) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("✅", fontSize = 13.sp)
                    Text(
                        "Photo added — reading location from EXIF",
                        color = c.good,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // ── Location ───────────────────────────────────────────────
            SectionLabel("Location")
            CarettaCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("📍", fontSize = 22.sp)
                    Column(Modifier.weight(1f)) {
                        Text("36.2694, 32.3108", color = c.deep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (hasPhoto) "From photo · EXIF" else "GPS ±5 m",
                            color = c.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (hasPhoto) Pill("Confirmed ✓", c.good) else Pill("Unconfirmed", c.muted)
                }
            }

            // ── Nest-only fields ───────────────────────────────────────
            if (markerType == MarkerType.NEST) {
                SectionLabel("Sun exposure")
                Segmented(
                    listOf(
                        SegOption("☀️ Sun", exposure == SunExposure.FULL_SUN) { exposure = SunExposure.FULL_SUN },
                        SegOption("⛅ Partial", exposure == SunExposure.PARTIAL) { exposure = SunExposure.PARTIAL },
                        SegOption("🌴 Shade", exposure == SunExposure.SHADE) { exposure = SunExposure.SHADE },
                    ),
                )
                Text(
                    "🌤️ 31°C · no rain — auto",
                    color = c.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )

                SectionLabel("Protection")
                ToggleRow("🛡️", "Cage installed", cage) { cage = !cage }
            }

            // ── Visibility ─────────────────────────────────────────────
            SectionLabel("Visibility")
            Segmented(
                listOf(
                    SegOption("🌍 Public", visibility == Visibility.PUBLIC) { visibility = Visibility.PUBLIC },
                    SegOption("🔒 Private", visibility == Visibility.PRIVATE) { visibility = Visibility.PRIVATE },
                ),
            )

            // ── Save ───────────────────────────────────────────────────
            Box(Modifier.size(4.dp))
            PrimaryButton(if (isNest) "Save nest 🐢" else "Save false crawl 🌀") {
                val beach = state.beaches.first()
                repo.addNest(
                    point = beach.center,
                    beachId = beach.id,
                    isNest = isNest,
                    exposure = exposure,
                    cageInstalled = cage,
                    clutchSizeEst = null,
                    hasPhoto = hasPhoto,
                    locationSource = if (hasPhoto) LocationSource.PHOTO_EXIF else LocationSource.DEVICE_GPS,
                    visibility = visibility,
                )
                onDone()
            }
        }
    }
}

/** One choice inside a [Segmented] control. */
private data class SegOption(val label: String, val selected: Boolean, val onClick: () -> Unit)

/** Rounded, sand-tray segmented control (glove-friendly, high contrast). */
@Composable
private fun Segmented(options: List<SegOption>) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(c.sand)
            .border(1.dp, c.line, RoundedCornerShape(15.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { o ->
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .then(if (o.selected) Modifier.background(c.surface) else Modifier)
                    .then(if (o.selected) Modifier.border(1.5.dp, c.sea, RoundedCornerShape(11.dp)) else Modifier)
                    .clickable { o.onClick() }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    o.label,
                    color = if (o.selected) c.sea else c.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

/** Simple labelled on/off toggle row (custom pill switch, no material Switch). */
@Composable
private fun ToggleRow(emoji: String, label: String, checked: Boolean, onToggle: () -> Unit) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .clickable { onToggle() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(label, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(width = 48.dp, height = 28.dp)
                .clip(CircleShape)
                .background(if (checked) c.good else c.line),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.padding(3.dp).size(22.dp).clip(CircleShape).background(Color.White))
        }
    }
}
