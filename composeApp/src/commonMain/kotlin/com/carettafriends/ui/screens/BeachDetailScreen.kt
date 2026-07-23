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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.content.AppStrings
import com.carettafriends.content.appStrings
import com.carettafriends.data.nestDay
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Beach
import com.carettafriends.domain.Nest
import com.carettafriends.domain.NestStatus
import com.carettafriends.ui.components.LocalPhoto
import com.carettafriends.ui.components.Pill
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.StatusPill
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

/** Beach card: stats + a feed of its nests (photos, status, latest update). */
@Composable
fun BeachDetailScreen(beach: Beach, state: AppState, onBack: () -> Unit, onOpenNest: (String) -> Unit) {
    val c = caretta
    val nests = state.nests.filter { it.beachId == beach.id }.sortedByDescending { it.updatedAtMillis }
    val active = nests.count { it.status == NestStatus.INCUBATING || it.status == NestStatus.HATCHING }
    val hatched = nests.count { it.status == NestStatus.HATCHED || it.status == NestStatus.EXCAVATED }
    val hatchlings = nests.sumOf { it.excavation?.hatchlingsToSea ?: 0 }
    // Card accent mirrors the map: green = official protected beach, amber = unprotected.
    val amber = Color(0xFFE0A82E)
    val accent = if (beach.protected) c.good else amber
    val s = appStrings(state.profile.language)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // cover — tinted by protection so the card reads at a glance like the map dot/polygon.
        Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.linearGradient(listOf(accent, c.deep)))) {
            Text("🏖️", fontSize = 60.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp))
            Box(
                Modifier.align(Alignment.TopStart).padding(12.dp).size(34.dp)
                    .clip(RoundedCornerShape(11.dp)).background(Color.White.copy(alpha = 0.22f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("‹", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 15.dp, bottom = 14.dp, end = 70.dp)) {
                Text(beach.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(beach.city.ifBlank { s.beachWord }, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
            }
        }

        Column(
            Modifier.padding(horizontal = 15.dp).padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (beach.protected) s.protectedBeachBanner else s.unprotectedBeachBanner,
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            if (beach.leaderName != null) {
                Text("👑 ${beach.leaderAvatar} ${beach.leaderName} · ${s.beachLeader}", color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // stats
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("${nests.size}", s.nestsWord, Modifier.weight(1f))
                StatCard("$active", s.activeWord, Modifier.weight(1f))
                StatCard("$hatched", s.hatchedWord, Modifier.weight(1f))
                StatCard("$hatchlings", s.toSea, Modifier.weight(1f))
            }

            // feed
            SectionLabel(s.feed)
            if (nests.isEmpty()) {
                Text(s.noNestsHereBeFirst, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            } else {
                nests.forEach { nest -> FeedItem(nest, s, onOpenNest) }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    val c = caretta
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = c.deep, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = c.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeedItem(nest: Nest, s: AppStrings, onOpenNest: (String) -> Unit) {
    val c = caretta
    val last = nest.updates.lastOrNull()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(16.dp)).clickable { onOpenNest(nest.id) }.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LocalPhoto(
            nest.photos.firstOrNull()?.localUri,
            Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)),
            placeholder = nest.photos.firstOrNull()?.placeholder ?: "🥚",
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(nest.code, color = c.deep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                StatusPill(nest.status, s)
            }
            Text(
                last?.body?.ifBlank { "${s.dayWord} ${nestDay(nest)}" } ?: "${s.dayWord} ${nestDay(nest)}",
                color = c.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
            )
        }
        if (nest.photos.firstOrNull()?.localUri == null) Pill("${s.dayWord} ${nestDay(nest)}", c.sea)
        Text("›", color = c.muted, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}
