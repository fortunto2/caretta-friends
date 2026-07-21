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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import com.carettafriends.data.CarettaRepository
import com.carettafriends.data.nestDay
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
import com.carettafriends.ui.theme.caretta
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@Composable
fun NestDetailScreen(nest: Nest, repo: CarettaRepository, onBack: () -> Unit, onExcavate: () -> Unit) {
    val c = caretta
    // React to repo updates (Add update / Comment) so the timeline stays live.
    val state by repo.state.collectAsState()
    val n = state.nest(nest.id) ?: nest
    val beach = state.beach(n.beachId)
    var watching by remember { mutableStateOf(false) }

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
                if (watching) "🔔 Watching" else "🔔 Watch",
                fg = if (watching) c.deep else Color.White,
                bg = if (watching) c.sunlit else Color.White.copy(alpha = 0.22f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .clickable { watching = !watching },
            )
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
                        "${beach?.name ?: "Beach"} · found ${fmtDate(n.foundDate)}",
                        color = c.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                StatusPill(n.status)
            }

            // Unconfirmed banner.
            if (n.confidence == NestConfidence.UNCONFIRMED) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.warn.copy(alpha = 0.16f))
                        .border(1.dp, c.warn.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable { }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        "⚠️  Needs a photo / precise location — tap to confirm",
                        color = c.warn,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Countdown + hatch window + predicted sex range + conditions.
            CarettaCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        CountdownRing(day = nestDay(n), total = n.incubationDaysEst)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionLabel("Hatch window")
                            Text(
                                hatchWindowLabel(n),
                                color = c.deep,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            if (n.predictedFemaleLow != null && n.predictedFemaleHigh != null) {
                                SexRangeBar(n.predictedFemaleLow!!, n.predictedFemaleHigh!!)
                                Text(
                                    "predicted · regional model (Anamur 28.9°C)",
                                    color = c.muted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    // Conditions strip.
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ConditionChip("${exposureEmoji(n.exposure)} ${exposureLabel(n.exposure)}")
                        n.airTempC?.let { ConditionChip("🌡️ ${it.toInt()}° air") }
                        ConditionChip("🌧️ rain 7d · ${n.rainMm7d?.toInt() ?: 0}mm")
                    }
                }
            }

            // Timeline.
            SectionLabel("Timeline · updates & comments")
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                n.updates.asReversed().forEach { u -> TimelineRow(u) }
            }

            // Add update / Comment.
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                GhostButton(
                    "＋ Add update",
                    modifier = Modifier.weight(1f),
                ) { repo.addUpdate(n.id, UpdateKind.OBSERVATION, "Patrol — all OK", ObsCondition.OK) }
                GhostButton(
                    "💬 Comment",
                    modifier = Modifier.weight(1f),
                ) { repo.addUpdate(n.id, UpdateKind.COMMENT, "Looks good today") }
            }

            // Excavation payoff.
            PrimaryButton("⛏️ Excavation", onClick = onExcavate)
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

@Composable
private fun TimelineRow(u: NestUpdate) {
    val c = caretta
    val dot = dotColor(u)
    val isComment = u.kind == UpdateKind.COMMENT
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // rail dot
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(4.dp))
            Box(Modifier.size(11.dp).clip(CircleShape).background(dot).border(2.dp, c.surface, CircleShape))
        }
        if (isComment) {
            // Italic comment bubble.
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(c.sand)
                    .border(1.dp, c.line, RoundedCornerShape(13.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "“${u.body}”",
                        color = c.ink,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                    )
                    Text("${u.author} · ${u.dateLabel}", color = c.muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(u.body, color = c.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Text("${u.author} · ${u.dateLabel}", color = c.muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- helpers -------------------------------------------------------------

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun fmtDate(d: LocalDate): String = "${MONTHS[d.month.ordinal]} ${d.dayOfMonth}"

private fun hatchWindowLabel(n: Nest): String {
    if (n.incubationDaysEst <= 0) return "not incubating"
    val start = n.foundDate.plus(DatePeriod(days = (n.incubationDaysEst - 3).coerceAtLeast(0)))
    val end = n.foundDate.plus(DatePeriod(days = n.incubationDaysEst + 4))
    return "~${n.incubationDaysEst}d · ${fmtDate(start)}–${fmtDate(end)}"
}

private fun exposureEmoji(e: SunExposure?): String = when (e) {
    SunExposure.FULL_SUN -> "☀️"
    SunExposure.PARTIAL -> "⛅"
    SunExposure.SHADE -> "🌴"
    null -> "🌡️"
}

private fun exposureLabel(e: SunExposure?): String = when (e) {
    SunExposure.FULL_SUN -> "full sun"
    SunExposure.PARTIAL -> "partial"
    SunExposure.SHADE -> "shade"
    null -> "exposure ?"
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
