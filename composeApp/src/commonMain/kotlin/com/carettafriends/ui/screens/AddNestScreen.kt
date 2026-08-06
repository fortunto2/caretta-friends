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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.carettafriends.content.appStrings
import com.carettafriends.data.CarettaRepository
import com.carettafriends.data.DuplicateHit
import com.carettafriends.data.DuplicateReason
import com.carettafriends.data.MAX_BACKDATE_DAYS
import com.carettafriends.data.localDateOf
import com.carettafriends.data.today
import com.carettafriends.domain.UpdateKind
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
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
import com.carettafriends.ui.DialogAction
import com.carettafriends.ui.DialogStyle
import com.carettafriends.ui.PickedPhoto
import com.carettafriends.ui.PlatformChoiceDialog
import com.carettafriends.ui.choiceAction
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.rememberCameraCapture
import com.carettafriends.ui.rememberGalleryPicker
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
    var markerType by remember { mutableStateOf(MarkerType.NEST) }
    var isNest by remember { mutableStateOf(true) }
    // Gallery pick attaches a REAL photo file — WITH its EXIF location. Photos taken on a phone carry
    // a GPS fix, and dropping it (as this screen used to) silently pinned imported nests to the beach
    // centre, which is how several nests ended up stacked on one coordinate.
    var galleryPhoto by remember { mutableStateOf<PickedPhoto?>(null) }
    val galleryPick = rememberGalleryPicker { picked -> if (picked != null) galleryPhoto = picked }
    // Android captures through the system camera right here; iOS returns null and routes to its own
    // native camera screen via [onCamera].
    val cameraCapture = rememberCameraCapture { shot -> if (shot != null) galleryPhoto = shot }
    // ONE attached photo — the latest choice wins. Path, identity and coordinates all come from the
    // same picture: reading them from different sources let a camera shot be saved at an old gallery
    // photo's coordinates, and marked "confirmed".
    val attached = galleryPhoto ?: pending?.let { PickedPhoto(it.path, null, it.lat, it.lng, it.hash) }
    val photoPath = attached?.path
    val hasPhoto = photoPath != null
    val photoHash = attached?.hash
    // Nest location comes ONLY from the photo's GPS (EXIF) — nests are created by photographing
    // them on-site, never dropped by hand (avoids spam / bogus pins).
    val fixPoint = remember(attached) {
        val lat = attached?.lat
        val lng = attached?.lng
        if (lat != null && lng != null) GeoPoint(lat, lng) else null
    }
    var exposure by remember { mutableStateOf(SunExposure.PARTIAL) }
    var beachManual by remember { mutableStateOf(false) }
    var pickingBeach by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var protection by remember { mutableStateOf(ProtectionLevel.NONE) }
    var visibility by remember { mutableStateOf(Visibility.PUBLIC) }
    var violationKind by remember { mutableStateOf(ViolationKind.TENT) }
    var anonymous by remember { mutableStateOf(true) }
    var showPhotoMenu by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var foundDate by remember { mutableStateOf(com.carettafriends.data.today()) }
    // A nest this one looks like a repeat of — set on save, resolved by the volunteer.
    var duplicate by remember { mutableStateOf<DuplicateHit?>(null) }
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
    // The point the nest/marker will be saved at (photo EXIF fix, else the chosen beach centre).
    // Hoisted to function scope so the pinned Save footer can read it too.
    val shownPoint = fixPoint ?: selectedBeach.center

    // Violations default to private (hidden from guests) — protects volunteers from retaliation.
    LaunchedEffect(markerType) { if (markerType == MarkerType.VIOLATION) visibility = Visibility.PRIVATE }

    // A photo picked AFTER the screen opened brings its own location and capture date — re-home the
    // nest on the beach it was actually taken on, and back-date it to the day it was shot.
    LaunchedEffect(fixPoint) {
        if (fixPoint != null && !beachManual) {
            nearestBeach(fixPoint, state.beaches)?.first?.let { selectedBeach = it }
        }
    }
    LaunchedEffect(galleryPhoto) {
        val shot = galleryPhoto?.exifEpochMillis?.let { localDateOf(it) } ?: return@LaunchedEffect
        val oldest = today().minus(DatePeriod(days = MAX_BACKDATE_DAYS))
        if (shot in oldest..today()) foundDate = shot
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(s.newMarker, onBack = onDone)
        // Scrollable form; the Save button is pinned in a footer below so it's always reachable.
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 15.dp).padding(bottom = 12.dp),
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

            // ── Photo ── one action: tap the photo to view full-screen, "⋮" to change (camera/gallery).
            SectionLabel(s.photo)
            if (photoPath != null) {
                Box(Modifier.fillMaxWidth()) {
                    LocalPhoto(
                        photoPath,
                        Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp))
                            .clickable { fullscreen = true },
                    )
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp)
                            .clip(CircleShape).background(Color.Black.copy(alpha = 0.4f))
                            .clickable { showPhotoMenu = true },
                        contentAlignment = Alignment.Center,
                    ) { Text("⋮", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
                }
            } else {
                GhostButton(s.addPhotoBtn, Modifier.fillMaxWidth()) { showPhotoMenu = true }
            }

            // ── Location ───────────────────────────────────────────────
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
            // Without a fix the pin lands on the beach's centre point, not on the nest. Say so —
            // silently doing it stacked several nests on one coordinate.
            if (fixPoint == null && markerType == MarkerType.NEST) {
                Text(s.noGpsWarn, color = c.coral, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }

            // ── Beach — ONE auto-picked beach with "change" beside it. A full inline list of every
            //    beach pushed the rest of the form off-screen and made a smart default look like a
            //    decision the volunteer had to make; the list now lives behind "change".
            SectionLabel(s.beachWord)
            val autoDetected = fixPoint != null && nearestDist != null && nearestDist <= 1500
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface)
                    .border(1.dp, c.line, RoundedCornerShape(14.dp)).padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📍", fontSize = 18.sp)
                    Column(Modifier.weight(1f)) {
                        Text(selectedBeach.name, color = c.deep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            when {
                                beachManual -> selectedBeach.city
                                autoDetected -> "${s.detectedFrom} · ${nearestDist!!.toInt()} m"
                                fixPoint == null -> s.noLocationYet
                                else -> s.notRecognizedBeach
                            },
                            color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 2,
                        )
                    }
                    Text(
                        s.changePlain, color = c.sea, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { pickingBeach = true }.padding(6.dp),
                    )
                }
            }
            if (pickingBeach) {
                // Nearest first — the right answer is almost always at the top of the list.
                val ordered = fixPoint
                    ?.let { p -> state.beaches.sortedBy { distanceMeters(p, it.center) } }
                    ?: state.beaches
                PlatformChoiceDialog(
                    title = s.beachWord,
                    actions = ordered.map { b ->
                        val away = fixPoint?.let { " · ${distanceLabel(distanceMeters(it, b.center))}" }.orEmpty()
                        choiceAction(b.name + away, b.id == selectedBeach.id) {
                            selectedBeach = b
                            beachManual = true
                        }
                    } + DialogAction(s.cancel, DialogStyle.CANCEL),
                    onDismiss = { pickingBeach = false },
                )
            }

            // ── Details (optional) ─────────────────────────────────────
            // Smart defaults keep the common case one-tap ("photograph & save"): a nest starts NOT
            // protected (you just found it) and PUBLIC. Protection / sun / visibility hide behind this.
            Text(
                (if (showDetails) "▾ " else "▸ ") + s.moreDetails,
                color = c.sea, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showDetails = !showDetails }.padding(vertical = 6.dp),
            )
            if (showDetails) {
                if (markerType == MarkerType.NEST) {
                    SectionLabel(s.protection)
                    Segmented(
                        listOf(
                            SegOption(s.protNone, protection == ProtectionLevel.NONE) { protection = ProtectionLevel.NONE },
                            SegOption(s.protReed, protection == ProtectionLevel.MARKED) { protection = ProtectionLevel.MARKED },
                            SegOption(s.protCage, protection == ProtectionLevel.CAGED) { protection = ProtectionLevel.CAGED },
                        ),
                    )
                    Text(s.protectionHelp, color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    SectionLabel(s.optionalSun)
                    Segmented(
                        listOf(
                            SegOption(s.segSun, exposure == SunExposure.FULL_SUN) { exposure = SunExposure.FULL_SUN },
                            SegOption(s.segPartial, exposure == SunExposure.PARTIAL) { exposure = SunExposure.PARTIAL },
                            SegOption(s.segShade, exposure == SunExposure.SHADE) { exposure = SunExposure.SHADE },
                        ),
                    )
                }
                // Violations manage their audience via the anonymous toggle (forced private), so the
                // public/private switch only appears here for nests & landmarks.
                if (!isViolation) {
                    SectionLabel(s.visibility)
                    Segmented(
                        listOf(
                            SegOption(s.visPublic, visibility == Visibility.PUBLIC) { visibility = Visibility.PUBLIC },
                            SegOption(s.visPrivate, visibility == Visibility.PRIVATE) { visibility = Visibility.PRIVATE },
                        ),
                    )
                }

                // Note → becomes the first comment on the nest (optional).
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it.take(200) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = { Text(s.noteHint) },
                )
                // Found date — back-date a nest found earlier but photographed for the first time now.
                if (markerType == MarkerType.NEST) {
                    SectionLabel(s.nestFoundDate)
                    val minDate = today().minus(DatePeriod(days = MAX_BACKDATE_DAYS))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StepBtn("−") { if (foundDate > minDate) foundDate = foundDate.minus(DatePeriod(days = 1)) }
                        Text(
                            if (foundDate == today()) s.today else "${foundDate.dayOfMonth} ${s.months[foundDate.month.ordinal]}",
                            color = c.deep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f),
                        )
                        StepBtn("+") { if (foundDate < today()) foundDate = foundDate.plus(DatePeriod(days = 1)) }
                    }
                }
            }

        }

        // ── Save — pinned footer (always visible without scrolling) ──────
        val saveLabel = when {
            isViolation -> s.saveViolation
            isNest -> s.saveNest
            else -> s.saveFalseCrawl
        }
        // Open the just-created nest so you can act on it right away (add updates, excavate).
        val saveNest = {
            onNestSaved(
                repo.addNest(
                    point = shownPoint,
                    beachId = selectedBeach.id,
                    isNest = isNest,
                    exposure = exposure,
                    protection = protection,
                    clutchSizeEst = null,
                    hasPhoto = hasPhoto,
                    photoPath = photoPath,
                    locationSource = if (fixPoint != null) LocationSource.PHOTO_EXIF else LocationSource.NONE,
                    visibility = visibility,
                    foundDate = foundDate,
                    note = noteText,
                    photoHash = photoHash,
                ),
            )
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 15.dp).padding(top = 8.dp, bottom = 14.dp)) {
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
                    // The same photo sent twice, or a nest already marked on this patch of sand →
                    // ask instead of quietly minting a second record for one nest.
                    val hit = repo.findDuplicate(shownPoint, photoHash, hasFix = fixPoint != null)
                    if (hit == null) saveNest() else duplicate = hit
                }
            }
        }

        // Duplicate found — offer to fold this photo into the existing nest (the usual right answer)
        // before allowing a second nest to be created.
        duplicate?.let { hit ->
            val samePhoto = hit.reason == DuplicateReason.SAME_PHOTO
            PlatformChoiceDialog(
                title = if (samePhoto) s.dupPhotoTitle else s.dupSpotTitle,
                message = (if (samePhoto) s.dupPhotoBody else s.dupSpotBody) +
                    "\n\n${hit.nest.code} · ${distanceLabel(hit.distanceM)}",
                actions = listOf(
                    DialogAction(s.dupAddTo.replace("%s", hit.nest.code), DialogStyle.PRIMARY) {
                        if (photoPath != null || noteText.isNotBlank()) {
                            repo.addUpdate(
                                nestId = hit.nest.id,
                                kind = UpdateKind.OBSERVATION,
                                body = noteText.trim(),
                                obsDate = foundDate,
                                photoPath = photoPath,
                                photoHash = photoHash,
                            )
                        }
                        onNestSaved(hit.nest.id)
                    },
                    DialogAction(s.dupOpen.replace("%s", hit.nest.code)) { onNestSaved(hit.nest.id) },
                    DialogAction(s.dupSaveAnyway) { saveNest() },
                    DialogAction(s.cancel, DialogStyle.CANCEL),
                ),
                onDismiss = { duplicate = null },
            )
        }

        // Photo source chooser — the OS's own sheet: this is a two-way choice, exactly what the
        // platform dialog is for, and on iPhone a hand-drawn one reads as someone else's app.
        if (showPhotoMenu) {
            PlatformChoiceDialog(
                title = s.photo,
                actions = listOf(
                    DialogAction(s.cameraBtn, DialogStyle.PRIMARY) { cameraCapture?.invoke() ?: onCamera() },
                    DialogAction(s.gallery) { galleryPick() },
                    DialogAction(s.cancel, DialogStyle.CANCEL),
                ),
                onDismiss = { showPhotoMenu = false },
            )
        }

        // Full-screen photo viewer — tap anywhere to close.
        if (fullscreen && photoPath != null) {
            Dialog(
                onDismissRequest = { fullscreen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black).clickable { fullscreen = false },
                    contentAlignment = Alignment.Center,
                ) { LocalPhoto(photoPath, Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
private fun StepBtn(label: String, onClick: () -> Unit) {
    val c = caretta
    Box(
        Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(13.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = c.deep, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
}

/** Coordinate trimmed to ~4 decimals (≈11 m) for display. */
private fun coord(v: Double): String = ((v * 10000).toLong() / 10000.0).toString()

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
