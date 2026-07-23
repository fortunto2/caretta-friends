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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.carettafriends.content.AppStrings
import com.carettafriends.content.appStrings
import com.carettafriends.data.CarettaRepository
import com.carettafriends.data.platformShareImage
import com.carettafriends.data.MAX_BACKDATE_DAYS
import com.carettafriends.data.nestDay
import com.carettafriends.data.today
import com.carettafriends.domain.Nest
import com.carettafriends.domain.NestConfidence
import com.carettafriends.domain.NestStatus
import com.carettafriends.domain.NestUpdate
import com.carettafriends.domain.ObsCondition
import com.carettafriends.domain.SunExposure
import com.carettafriends.domain.UpdateKind
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.CountdownRing
import com.carettafriends.ui.components.GhostButton
import com.carettafriends.ui.components.LocalPhoto
import com.carettafriends.ui.components.Pill
import com.carettafriends.ui.components.PrimaryButton
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.SexRangeBar
import com.carettafriends.ui.components.StatusPill
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.rememberGalleryPicker
import com.carettafriends.ui.theme.caretta
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun NestDetailScreen(
    nest: Nest,
    repo: CarettaRepository,
    onBack: () -> Unit,
    onExcavate: () -> Unit,
    onOpenMember: (String) -> Unit = {},
) {
    val c = caretta
    // React to repo updates (Add update / Comment) so the timeline stays live.
    val state by repo.state.collectAsState()
    val n = state.nest(nest.id) ?: nest
    val beach = state.beach(n.beachId)
    val s = appStrings(state.profile.language)
    var watching by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(DetailSheet.NONE) }
    // Gallery photo → a timeline update; an old photo's EXIF date back-dates it (capped a month).
    val pickPhoto = rememberGalleryPicker { picked ->
        if (picked != null) {
            val obsDate = picked.exifEpochMillis?.let { millis ->
                Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    .coerceIn(today().minus(DatePeriod(days = MAX_BACKDATE_DAYS)), today())
            }
            repo.addUpdate(n.id, UpdateKind.OBSERVATION, "", obsDate = obsDate, photoPath = picked.path)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(n.code, onBack = onBack)

        // Hero header — sea→deep gradient with a big egg + Watch toggle.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(c.sea, c.deep))),
        ) {
            val photoPath = n.photos.firstOrNull()?.localUri
            if (photoPath != null) {
                LocalPhoto(photoPath, Modifier.fillMaxSize())
            } else {
                Text(
                    n.photos.firstOrNull()?.placeholder ?: "🥚",
                    fontSize = 56.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Pill(
                if (watching) s.watching else s.watch,
                fg = if (watching) c.deep else Color.White,
                bg = if (watching) c.sunlit else Color.White.copy(alpha = 0.22f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .clickable { watching = !watching },
            )
            // Share the photo (watermarked with the nest code · beach · date).
            n.photos.firstOrNull()?.localUri?.let { path ->
                Pill(
                    "↗",
                    fg = Color.White,
                    bg = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .clickable {
                            platformShareImage(path, "${n.code} · ${beach?.name ?: ""} · ${fmtDate(n.foundDate, s)}")
                        },
                )
            }
        }

        Column(
            Modifier.padding(horizontal = 15.dp).padding(top = 12.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title row: code + beach + found + status.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(n.code, color = c.deep, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${beach?.name ?: s.beachWord} · ${s.foundWord} ${fmtDate(n.foundDate, s)}  ✎",
                        color = c.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { sheet = DetailSheet.DATE }
                            .padding(vertical = 2.dp),
                    )
                }
                StatusPill(n.status, s)
            }

            // Unconfirmed banner (informational — a nest is confirmed by adding a photo/observation).
            if (n.confidence == NestConfidence.UNCONFIRMED) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.warn.copy(alpha = 0.16f))
                        .border(1.dp, c.warn.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        s.needsConfirm,
                        color = c.warn,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Who found this nest — tap through to their profile.
            Text(
                "🔎 ${s.foundByWord} ${n.foundBy} ›",
                color = c.sea,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onOpenMember(n.foundBy) },
            )

            // Countdown + hatch window + predicted sex range + conditions.
            CarettaCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        CountdownRing(day = nestDay(n), total = n.incubationDaysEst, dayLabel = s.dayLabel)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionLabel(s.hatchWindow)
                            Text(
                                hatchWindowLabel(n, s),
                                color = c.deep,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            if (n.predictedFemaleLow != null && n.predictedFemaleHigh != null) {
                                SexRangeBar(n.predictedFemaleLow!!, n.predictedFemaleHigh!!)
                                Text(
                                    s.predictedModel,
                                    color = c.muted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    // Conditions strip.
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ConditionChip("${exposureEmoji(n.exposure)} ${exposureLabel(n.exposure, s)}")
                        n.airTempC?.let { ConditionChip("🌡️ ${it.toInt()}° ${s.airWord}") }
                        n.rainMm7d?.let { ConditionChip("🌧️ ${s.rain7d} · ${it.toInt()}mm") }
                    }
                }
            }

            // Timeline.
            SectionLabel(s.timeline)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                n.updates.asReversed().forEach { u -> TimelineRow(u, timelineBody(u, n, s), s, onOpenMember) }
            }

            // Add update / Comment / Photo.
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                GhostButton(s.addUpdate, modifier = Modifier.weight(1f)) { sheet = DetailSheet.UPDATE }
                GhostButton(s.comment, modifier = Modifier.weight(1f)) { sheet = DetailSheet.COMMENT }
            }
            GhostButton(s.addPhoto, modifier = Modifier.fillMaxWidth()) { pickPhoto() }

            when (sheet) {
                DetailSheet.UPDATE -> AddUpdateDialog(comment = false, s = s, onDismiss = { sheet = DetailSheet.NONE }) { body, cond, date ->
                    repo.addUpdate(n.id, UpdateKind.OBSERVATION, body, cond, obsDate = date)
                    sheet = DetailSheet.NONE
                }
                DetailSheet.COMMENT -> AddUpdateDialog(comment = true, s = s, onDismiss = { sheet = DetailSheet.NONE }) { body, _, date ->
                    repo.addUpdate(n.id, UpdateKind.COMMENT, body, obsDate = date)
                    sheet = DetailSheet.NONE
                }
                DetailSheet.DATE -> EditFoundDateDialog(n.foundDate, s, onDismiss = { sheet = DetailSheet.NONE }) { d ->
                    repo.setFoundDate(n.id, d)
                    sheet = DetailSheet.NONE
                }
                DetailSheet.NONE -> {}
            }

            // Excavation payoff — delicate work, gated to experienced volunteers & beach leaders.
            if (state.profile.canExcavate) {
                PrimaryButton(s.excavation, onClick = onExcavate)
            } else {
                CarettaCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🔒", fontSize = 20.sp)
                        Column(Modifier.weight(1f)) {
                            Text(s.excavation, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                s.excavationLockedSub,
                                color = c.muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionChip(text: String) {
    val c = caretta
    Box(
        Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(c.sand)
            .border(1.dp, c.line, RoundedCornerShape(11.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text, color = c.ink, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}

/** Timeline meta line — the author name is tappable (→ their profile), followed by date/condition. */
@Composable
private fun AuthorMeta(author: String, rest: String, onOpenMember: (String) -> Unit) {
    val c = caretta
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            author,
            color = c.sea,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.clickable { onOpenMember(author) },
        )
        Text(rest, color = c.muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    }
}

/** Localized body for an auto-generated timeline entry — derived from the nest so it stays in the
 *  current language even for data stored (in English) before this screen existed. User-typed
 *  observations/comments keep their own text. */
internal fun timelineBody(u: NestUpdate, n: Nest, s: AppStrings): String = when (u.kind) {
    UpdateKind.FOUND -> if (n.isNest) s.tlNestFound else s.tlFalseCrawl
    UpdateKind.EXCAVATED ->
        "${s.excavatedWord.replaceFirstChar { it.uppercase() }} · ${n.excavation?.hatchSuccessPct ?: 0}% ${s.tlHatchSuccess}"
    else -> u.body
}

@Composable
private fun TimelineRow(u: NestUpdate, body: String, s: AppStrings, onOpenMember: (String) -> Unit) {
    val c = caretta
    val dot = dotColor(u)
    val isComment = u.kind == UpdateKind.COMMENT
    val dateStr = u.obsDate?.let { fmtDate(it, s) }
        ?: if (u.dateLabel.isBlank() || u.dateLabel == "Today") s.today else u.dateLabel
    val metaRest = " · $dateStr${conditionSuffix(u, s)}"
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // rail dot
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(4.dp))
            Box(Modifier.size(11.dp).clip(CircleShape).background(dot).border(2.dp, c.surface, CircleShape))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Attached photo (builds the nest's photo history over time).
            u.photo?.localUri?.let { uri ->
                LocalPhoto(uri, Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(13.dp)))
            }
            if (isComment) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(c.sand)
                        .border(1.dp, c.line, RoundedCornerShape(13.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("“$body”", color = c.ink, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                        AuthorMeta(u.author, metaRest, onOpenMember)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (body.isNotBlank()) {
                        Text(body, color = c.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                    AuthorMeta(u.author, metaRest, onOpenMember)
                }
            }
        }
    }
}

/** " · ⚠ predated" style suffix for notable observation conditions (OK stays clean). */
private fun conditionSuffix(u: NestUpdate, s: AppStrings): String = when (u.condition) {
    null, ObsCondition.OK, ObsCondition.OTHER -> ""
    ObsCondition.HATCHING -> " · 🐣 ${s.condHatching.lowercase()}"
    ObsCondition.HATCHED -> " · 🐢 ${s.condHatched.lowercase()}"
    ObsCondition.DISTURBED -> " · ⚠ ${s.condDisturbed.lowercase()}"
    ObsCondition.PREDATED -> " · ⚠ ${s.condPredated.lowercase()}"
    ObsCondition.WASHED_OVER -> " · 🌊 ${s.condWashed.lowercase()}"
    ObsCondition.POACHED -> " · ⚠ ${s.condPredated.lowercase()}"
    ObsCondition.RELOCATED -> " · ➡"
}

// --- helpers -------------------------------------------------------------

private fun fmtDate(d: LocalDate, s: AppStrings): String = "${s.months[d.month.ordinal]} ${d.dayOfMonth}"

private fun hatchWindowLabel(n: Nest, s: AppStrings): String {
    if (n.incubationDaysEst <= 0) return "—"
    val start = n.foundDate.plus(DatePeriod(days = (n.incubationDaysEst - 3).coerceAtLeast(0)))
    val end = n.foundDate.plus(DatePeriod(days = n.incubationDaysEst + 4))
    return "~${n.incubationDaysEst}d · ${fmtDate(start, s)}–${fmtDate(end, s)}"
}

private fun exposureEmoji(e: SunExposure?): String = when (e) {
    SunExposure.FULL_SUN -> "☀️"
    SunExposure.PARTIAL -> "⛅"
    SunExposure.SHADE -> "🌴"
    null -> "🌡️"
}

private fun exposureLabel(e: SunExposure?, s: AppStrings): String = when (e) {
    SunExposure.FULL_SUN -> s.exFullSun
    SunExposure.PARTIAL -> s.exPartial
    SunExposure.SHADE -> s.exShade
    null -> s.exUnknown
}

@Composable
private fun dotColor(u: NestUpdate): Color {
    val c = caretta
    return when (u.kind) {
        UpdateKind.HATCHED -> c.good
        UpdateKind.COMMENT -> c.muted
        UpdateKind.FOUND -> c.sea
        UpdateKind.RELOCATED -> c.warn
        UpdateKind.EXCAVATED -> c.good
        UpdateKind.STATUS_CHANGE -> statusColor(u.newStatus, c.sea)
        UpdateKind.OBSERVATION -> when (u.condition) {
            ObsCondition.OK, ObsCondition.HATCHED, ObsCondition.HATCHING -> c.good
            ObsCondition.PREDATED, ObsCondition.WASHED_OVER, ObsCondition.POACHED -> c.risk
            ObsCondition.DISTURBED -> c.warn
            else -> c.sea
        }
    }
}

@Composable
private fun statusColor(status: NestStatus?, fallback: Color): Color {
    val c = caretta
    return when (status) {
        NestStatus.HATCHED, NestStatus.EXCAVATED -> c.good
        NestStatus.HATCHING -> c.warn
        NestStatus.PREDATED, NestStatus.WASHED_OVER, NestStatus.POACHED -> c.risk
        NestStatus.LOST, NestStatus.FALSE_CRAWL -> c.muted
        else -> fallback
    }
}

// --- add-update / back-date dialogs --------------------------------------

/** Which bottom dialog is open on the nest detail (none / add update / comment / edit found-date). */
private enum class DetailSheet { NONE, UPDATE, COMMENT, DATE }

@Composable
private fun AddUpdateDialog(
    comment: Boolean,
    s: AppStrings,
    onDismiss: () -> Unit,
    onSave: (body: String, condition: ObsCondition?, obsDate: LocalDate) -> Unit,
) {
    val c = caretta
    var body by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf(ObsCondition.OK) }
    // Updates & comments are always "now" — back-dating belongs to the photo step (old gallery photo),
    // not to a live note. The nest's found date is editable separately (tap "found ✎").
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = c.surface) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    if (comment) s.comment else s.addUpdate,
                    color = c.deep, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (comment) s.writeComment else s.whatObserved) },
                    minLines = 2,
                )
                if (!comment) {
                    SectionLabel(s.condition)
                    ConditionPicker(condition, s) { condition = it }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(s.cancel, color = c.muted) }
                    PrimaryButton(s.save, modifier = Modifier.weight(1f), enabled = body.isNotBlank()) {
                        onSave(body.trim(), if (comment) null else condition, today())
                    }
                }
            }
        }
    }
}

@Composable
private fun EditFoundDateDialog(initial: LocalDate, s: AppStrings, onDismiss: () -> Unit, onSave: (LocalDate) -> Unit) {
    val c = caretta
    var date by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = c.surface) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(s.nestFoundDate, color = c.deep, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    s.backDateHelp,
                    color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                )
                DateStepper(date, s) { date = it }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(s.cancel, color = c.muted) }
                    PrimaryButton(s.save, modifier = Modifier.weight(1f)) { onSave(date) }
                }
            }
        }
    }
}

@Composable
private fun ConditionPicker(selected: ObsCondition, s: AppStrings, onSelect: (ObsCondition) -> Unit) {
    val opts = listOf(
        ObsCondition.OK to s.condOk,
        ObsCondition.HATCHING to s.condHatching,
        ObsCondition.HATCHED to s.condHatched,
        ObsCondition.DISTURBED to s.condDisturbed,
        ObsCondition.PREDATED to s.condPredated,
        ObsCondition.WASHED_OVER to s.condWashed,
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        opts.forEach { (cond, label) -> SelectableChip(label, selected == cond) { onSelect(cond) } }
    }
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = caretta
    Box(
        Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) c.sea else c.sand)
            .border(1.dp, if (selected) c.sea else c.line, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(label, color = if (selected) Color.White else c.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Date picker with no platform dialog: −/+ day steppers clamped to [today-[MAX_BACKDATE_DAYS], today].
 *  "3 days ago" is three taps on −. Identical on iOS & Android. */
@Composable
private fun DateStepper(date: LocalDate, s: AppStrings, onChange: (LocalDate) -> Unit) {
    val minDate = today().minus(DatePeriod(days = MAX_BACKDATE_DAYS))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StepButton("−", date > minDate) { onChange(date.minus(DatePeriod(days = 1))) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(fmtDate(date, s), color = caretta.deep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(dayAgoLabel(date, s), color = caretta.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        StepButton("+", date < today()) { onChange(date.plus(DatePeriod(days = 1))) }
    }
}

@Composable
private fun StepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val c = caretta
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (enabled) c.sand else c.sand.copy(alpha = 0.5f))
            .border(1.dp, c.line, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = if (enabled) c.deep else c.muted, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun dayAgoLabel(date: LocalDate, s: AppStrings): String = when (val d = date.daysUntil(today())) {
    0 -> s.today
    1 -> s.yesterday
    else -> s.daysAgo.replace("%d", d.toString())
}
