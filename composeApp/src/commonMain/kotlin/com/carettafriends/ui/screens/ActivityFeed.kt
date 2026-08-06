package com.carettafriends.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.content.AppStrings
import com.carettafriends.data.today
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Member
import com.carettafriends.domain.NestUpdate
import com.carettafriends.domain.PhotoRef
import com.carettafriends.domain.UpdateKind
import com.carettafriends.ui.components.NestPhoto
import com.carettafriends.ui.theme.caretta
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/**
 * ONE activity-feed entry (a photo/observation tied to a nest). The feed is the app's home for
 * "what happened lately" — a volunteer's usual mode is "snap a photo & move on", so their trail
 * shows up here automatically, scoped by user / beach / community.
 */
data class FeedEntry(
    val key: String,
    /** The photo itself, not a path: on another volunteer's entry the file is fetched from storage. */
    val photo: PhotoRef?,
    val emoji: String,
    val title: String,      // nest code
    val subtitle: String,   // what happened (localized)
    val date: String,
    val millis: Long,
    val nestId: String,
)

/** Time window for the activity feed (chips on the profile): today, last week, last month, everything. */
enum class FeedRange { TODAY, WEEK, MONTH, ALL }

/** Epoch-millis cutoff for a [FeedRange]: only entries at/after it are shown (ALL = no cutoff). */
private fun rangeCutoff(range: FeedRange): Long {
    val tz = TimeZone.currentSystemDefault()
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val dayMs = 24L * 3600 * 1000
    return when (range) {
        FeedRange.TODAY -> today().atStartOfDayIn(tz).toEpochMilliseconds()
        FeedRange.WEEK -> nowMs - 7 * dayMs
        FeedRange.MONTH -> nowMs - 30 * dayMs
        FeedRange.ALL -> 0L
    }
}

/**
 * Build the activity feed, scoped: pass [member] for one volunteer, [beachId] for one beach,
 * [communityId] for a whole community (→ its beaches → their nests), or nothing for everything.
 * [range] narrows to a time window (today/week/month/all). One builder, one [ActivityFeed] component.
 */
fun activityFeed(
    state: AppState,
    s: AppStrings,
    member: Member? = null,
    beachId: String? = null,
    communityId: String? = null,
    range: FeedRange = FeedRange.ALL,
    limit: Int = 30,
): List<FeedEntry> {
    val memberNests = member?.let { state.nestsBy(it).map { n -> n.id }.toSet() }
    val beachIds: Set<String>? = when {
        beachId != null -> setOf(beachId)
        communityId != null -> state.beaches.filter { it.communityId == communityId }.map { it.id }.toSet()
        else -> null
    }
    val nests = state.nests.filter { n ->
        (memberNests == null || n.id in memberNests) && (beachIds == null || n.beachId in beachIds)
    }
    val cutoff = rangeCutoff(range)
    return nests.flatMap { n ->
        n.updates.map { u ->
            FeedEntry(
                key = u.id,
                photo = u.photo,
                emoji = feedEmoji(u),
                title = n.code,
                subtitle = timelineBody(u, n, s),
                date = u.obsDate?.let { fmtDate(it, s) }
                    ?: if (u.dateLabel.isBlank() || u.dateLabel == "Today") s.today else u.dateLabel,
                millis = u.createdEpochMillis,
                nestId = n.id,
            )
        }
    }.filter { it.millis >= cutoff }.sortedByDescending { it.millis }.take(limit)
}

/** A compact, shareable "today" summary (emoji-labelled, i18n-light): nests found · hatchings · updates. */
fun todayReportText(state: AppState, s: AppStrings): String {
    val startToday = today().atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    val todays = state.nests.flatMap { it.updates }.filter { it.createdEpochMillis >= startToday }
    val found = todays.count { it.kind == UpdateKind.FOUND }
    val hatch = todays.count { it.kind == UpdateKind.HATCHED }
    return "🐢 Caretta Friends · ${s.today}\n🥚 $found · 🐣 $hatch · 📝 ${todays.size}"
}

private fun feedEmoji(u: NestUpdate): String = when (u.kind) {
    UpdateKind.FOUND -> "🥚"
    UpdateKind.HATCHED -> "🐣"
    UpdateKind.EXCAVATED -> "⛏️"
    UpdateKind.COMMENT -> "💬"
    UpdateKind.RELOCATED -> "📦"
    UpdateKind.STATUS_CHANGE -> "🔄"
    else -> "📝"
}

/** The one activity-feed component. Each row: photo (or emoji) + "nest · what happened" + date. */
@Composable
fun ActivityFeed(entries: List<FeedEntry>, onOpenNest: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { e -> FeedRow(e, onOpenNest) }
    }
}

@Composable
private fun FeedRow(e: FeedEntry, onOpenNest: (String) -> Unit) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(16.dp))
            .clickable { onOpenNest(e.nestId) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        if (e.photo != null) {
            NestPhoto(e.photo, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
        } else {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(c.sand),
                contentAlignment = Alignment.Center,
            ) { Text(e.emoji, fontSize = 24.sp) }
        }
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(e.title, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(e.subtitle, color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
            Text(e.date, color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text("›", color = c.muted, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}
