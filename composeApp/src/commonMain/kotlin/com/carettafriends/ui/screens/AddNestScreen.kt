package com.carettafriends.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.carettafriends.content.appStrings
import com.carettafriends.data.CarettaRepository
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Beach
import com.carettafriends.domain.GeoPoint
import com.carettafriends.domain.LocationSource
import com.carettafriends.domain.MarkerType
import com.carettafriends.domain.ProtectionLevel
import com.carettafriends.domain.SunExposure
import com.carettafriends.domain.ViolationKind
import com.carettafriends.domain.Visibility
import com.carettafriends.domain.distanceLabel
import com.carettafriends.domain.distanceMeters
import com.carettafriends.domain.nearestBeach
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.GhostButton
import com.carettafriends.ui.components.LocalPhoto
import com.carettafriends.ui.components.Pill
import com.carettafriends.ui.components.PrimaryButton
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

@Composable
fun AddNestScreen(
    repo: CarettaRepository,
    state: AppState,
    onDone: () -> Unit,
    onCamera: () -> Unit,
    onNestSaved: (String) -> Unit = { onDone() },
) {
    val c = caretta
    val s = appStrings(state.profile.language)

    // Photo captured by the native camera (iOS) lands here, prefilling the form.
    val pending = remember { repo.takePendingPhoto() }
    // Nest location comes ONLY from the photo's GPS (EXIF) — nests are created by photographing
    // them on-site, never dropped by hand (avoids spam / bogus pins).
    val fixPoint = remember(pending) {
        pending?.let { if (it.lat != null && it.lng != null) GeoPoint(it.lat, it.lng) else null }
    }
    var markerType by remember { mutableStateOf(MarkerType.NEST) }
    var isNest by remember { mutableStateOf(true) }
    var hasPhoto by remember { mutableStateOf(pending != null) }
    var exposure by remember { mutableStateOf(SunExposure.PARTIAL) }
    var beachManual by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var protection by remember { mutableStateOf(ProtectionLevel.NONE) }
    var visibility by remember { mutableStateOf(Visibility.PUBLIC) }
    var violationKind by remember { mutableStateOf(ViolationKind.TENT) }
    var anonymous by remember { mutableStateOf(true) }
    val isViolation = markerType == MarkerType.VIOLATION
    // Turtles nest on beaches → bind every nest to a beach (→ community). Default = nearest to the
    // photo location; the volunteer can override. Falls back to the first beach when there's no fix.
    var selectedBeach by remember {
        mutableStateOf(
            fixPoint?.let { nearestBeach(it, state.beaches)?.first }
                ?: state.beaches.firstOrNull { it.id == state.profile.homeBeachId }
                ?: state.beaches.first(),
        )
    }
    val nearestDist = fixPoint?.let { distanceMeters(it, selectedBeach.center) }

    // Violations default to private (hidden from guests) — protects volunteers from retaliation.
    LaunchedEffect(markerType) { if (markerType == MarkerType.VIOLATION) visibility = Visibility.PRIVATE }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(s.newMarker, onBack = onDone)
        Column(
            Modifier.padding(horizontal = 15.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Type selector ──────────────────────────────────────────
            SectionLabel(s.whatFound)
            Segmented(
                listOf(
                    SegOption(s.segNest, markerType == MarkerType.NEST) { markerType = MarkerType.NEST },
                    SegOption(s.segTrash, markerType == MarkerType.TRASH) { markerType = MarkerType.TRASH },
                    SegOption(s.segViolation, markerType == MarkerType.VIOLATION) { markerType = MarkerType.VIOLATION },
                ),
            )

            // ── Nest vs false crawl (only for nests) ───────────────────
            if (markerType == MarkerType.NEST) {
                SectionLabel(s.nestOrFalse)
                Segmented(
                    listOf(
                        SegOption(s.segNest, isNest) { isNest = true },
                        SegOption(s.segFalseCrawl, !isNest) { isNest = false },
                    ),
                )
            }

            // ── Violation category + anonymity (private by default) ────
            if (isViolation) {
                SectionLabel(s.whatViolation)
                val vopts = listOf(
                    ViolationKind.TENT to s.vkTent, ViolationKind.VEHICLE to s.vkVehicle,
                    ViolationKind.LIGHT to s.vkLight, ViolationKind.NOISE to s.vkNoise,
                    ViolationKind.DOG to s.vkDog, ViolationKind.LITTER to s.vkLitter,
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    vopts.forEach { (k, label) -> ChoiceChip(label, violationKind == k) { violationKind = k } }
                }
                Text(s.violationPrivateHint, color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                ToggleRow("🕶️", s.anonymousWord, anonymous) { anonymous = !anonymous }
            }

            // ── Photo ──────────────────────────────────────────────────
            SectionLabel(s.photo)
            if (pending?.path != null) {
                // Show the just-captured photo right here in the form.
                LocalPhoto(
                    pending.path,
                    Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                GhostButton(if (pending?.path != null) s.retake else s.cameraBtn, Modifier.weight(1f)) { onCamera() }
                GhostButton(s.gallery, Modifier.weight(1f)) { hasPhoto = true }
            }
            if (hasPhoto && pending?.path == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("✅", fontSize = 13.sp)
                    Text(s.photoAdded, color = c.good, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Location ───────────────────────────────────────────────
            val shownPoint = fixPoint ?: selectedBeach.center
            SectionLabel(s.location)
            CarettaCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("📍", fontSize = 22.sp)
                    Column(Modifier.weight(1f)) {
                        Text("${coord(shownPoint.lat)}, ${coord(shownPoint.lng)}", color = c.deep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (fixPoint != null) s.fromExif else s.noGpsInPhoto,
                            color = c.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (fixPoint != null) Pill(s.confirmed, c.good) else Pill(s.unconfirmed, c.muted)
                }
            }

            // ── Beach — auto-detected from the location; manual pick only if unrecognized ──
            SectionLabel(s.beachWord)
            val autoDetected = fixPoint != null && nearestDist != null && nearestDist <= 1500
            if (autoDetected && !beachManual) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface)
                        .border(1.dp, c.line, RoundedCornerShape(14.dp)).padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📍", fontSize = 18.sp)
                        Column(Modifier.weight(1f)) {
                            Text(selectedBeach.name, color = c.deep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Text("${s.detectedFrom} · ${nearestDist!!.toInt()} m", color = c.muted, fontSize = 11.sp)
                        }
                        Text(s.changePlain, color = c.sea, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { beachManual = true }.padding(6.dp))
                    }
                }
            } else {
                if (fixPoint == null) {
                    Text(s.noLocationYet, color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else if (nearestDist != null && nearestDist > 1500) {
                    Text(s.notRecognizedBeach, color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                BeachPicker(state.beaches, selectedBeach, fixPoint) { selectedBeach = it; beachManual = true }
            }

            // ── Nest-only fields ───────────────────────────────────────
            if (markerType == MarkerType.NEST) {
                SectionLabel(s.protection)
                Segmented(
                    listOf(
                        SegOption(s.protNone, protection == ProtectionLevel.NONE) { protection = ProtectionLevel.NONE },
                        SegOption(s.protReed, protection == ProtectionLevel.MARKED) { protection = ProtectionLevel.MARKED },
                        SegOption(s.protCage, protection == ProtectionLevel.CAGED) { protection = ProtectionLevel.CAGED },
                    ),
                )
                Text(
                    s.protectionHelp,
                    color = c.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                // Sun exposure is optional (feeds the sex prediction; defaults to Partial) — tucked away.
                Text(
                    (if (showDetails) "▾ " else "▸ ") + s.optionalSun,
                    color = c.sea, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showDetails = !showDetails }.padding(vertical = 4.dp),
                )
                if (showDetails) {
                    Segmented(
                        listOf(
                            SegOption(s.segSun, exposure == SunExposure.FULL_SUN) { exposure = SunExposure.FULL_SUN },
                            SegOption(s.segPartial, exposure == SunExposure.PARTIAL) { exposure = SunExposure.PARTIAL },
                            SegOption(s.segShade, exposure == SunExposure.SHADE) { exposure = SunExposure.SHADE },
                        ),
                    )
                }
            }

            // ── Visibility ─────────────────────────────────────────────
            SectionLabel(s.visibility)
            Segmented(
                listOf(
                    SegOption(s.visPublic, visibility == Visibility.PUBLIC) { visibility = Visibility.PUBLIC },
                    SegOption(s.visPrivate, visibility == Visibility.PRIVATE) { visibility = Visibility.PRIVATE },
                ),
            )

            // ── Save ───────────────────────────────────────────────────
            Box(Modifier.size(4.dp))
            val saveLabel = when {
                isViolation -> s.saveViolation
                isNest -> s.saveNest
                else -> s.saveFalseCrawl
            }
            PrimaryButton(saveLabel) {
                if (isViolation) {
                    repo.addViolation(
                        point = shownPoint,
                        kind = violationKind,
                        note = "",
                        visibility = visibility,
                        anonymous = anonymous,
                        beachId = selectedBeach.id,
                    )
                    onDone()
                } else if (markerType == MarkerType.TRASH) {
                    repo.addSimpleMarker(MarkerType.TRASH, shownPoint, "")
                    onDone()
                } else {
                    // Open the just-created nest so you can act on it right away (add updates, excavate).
                    val newId = repo.addNest(
                        point = shownPoint,
                        beachId = selectedBeach.id,
                        isNest = isNest,
                        exposure = exposure,
                        protection = protection,
                        clutchSizeEst = null,
                        hasPhoto = hasPhoto,
                        photoPath = pending?.path,
                        locationSource = if (fixPoint != null) LocationSource.PHOTO_EXIF else LocationSource.NONE,
                        visibility = visibility,
                    )
                    onNestSaved(newId)
                }
            }
        }
    }
}

/** Coordinate trimmed to ~4 decimals (≈11 m) for display. */
private fun coord(v: Double): String = ((v * 10000).toLong() / 10000.0).toString()

/** Selectable list of the community's beaches; shows distance from the photo location if known. */
@Composable
private fun BeachPicker(beaches: List<Beach>, selected: Beach, from: GeoPoint?, onSelect: (Beach) -> Unit) {
    val c = caretta
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        beaches.forEach { b ->
            val isSel = b.id == selected.id
            val dist = from?.let { distanceMeters(it, b.center) }
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSel) c.surface else c.sand)
                    .border(if (isSel) 1.5.dp else 1.dp, if (isSel) c.sea else c.line, RoundedCornerShape(14.dp))
                    .clickable { onSelect(b) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(b.leaderAvatar, fontSize = 20.sp)
                Column(Modifier.weight(1f)) {
                    Text(b.name, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        b.city + (b.leaderName?.let { " · $it" } ?: ""),
                        color = c.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (dist != null) {
                    Text(distanceLabel(dist), color = if (isSel) c.sea else c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (isSel) Text("✓", color = c.sea, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
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

/** Small selectable chip (violation category). */
@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = caretta
    Box(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.sea else c.sand)
            .border(1.dp, if (selected) c.sea else c.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Text(label, color = if (selected) Color.White else c.deep, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
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
