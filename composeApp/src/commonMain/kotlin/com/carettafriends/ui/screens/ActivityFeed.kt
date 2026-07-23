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
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Member
import com.carettafriends.domain.NestUpdate
import com.carettafriends.domain.UpdateKind
import com.carettafriends.ui.components.LocalPhoto
import com.carettafriends.ui.theme.caretta

/**
 * ONE activity-feed entry (a photo/observation tied to a nest). The feed is the app's home for
 * "what happened lately" — a volunteer's usual mode is "snap a photo & move on", so their trail
 * shows up here automatically, scoped by user / beach / community.
 */
data class FeedEntry(
    val key: String,
    val photoUri: String?,
    val emoji: String,
    val title: String,      // nest code
    val subtitle: String,   // what happened (localized)
    val date: String,
    val millis: Long,
    val nestId: String,
)

/**
 * Build the activity feed, scoped: pass [member] for one volunteer, [beachId] for one beach,
 * [communityId] for a whole community (→ its beaches → their nests), or nothing for everything.
 * (City scope can layer on later.) One builder, one [ActivityFeed] component — reused everywhere.
 */
fun activityFeed(
    state: AppState,
    s: AppStrings,
    member: Member? = null,
    beachId: String? = null,
    communityId: String? = null,
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
    return nests.flatMap { n ->
        n.updates.map { u ->
            FeedEntry(
                key = u.id,
                photoUri = u.photo?.localUri,
                emoji = feedEmoji(u),
                title = n.code,
                subtitle = timelineBody(u, n, s),
                date = u.obsDate?.let { fmtDate(it, s) }
                    ?: if (u.dateLabel.isBlank() || u.dateLabel == "Today") s.today else u.dateLabel,
                millis = u.createdEpochMillis,
                nestId = n.id,
            )
        }
    }.sortedByDescending { it.millis }.take(limit)
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
        if (e.photoUri != null) {
            LocalPhoto(e.photoUri, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
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
