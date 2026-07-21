package com.carettafriends.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Badge
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.Pill
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

@Composable
fun ProfileScreen(state: AppState, onOpenCommunity: () -> Unit) {
    val c = caretta
    val p = state.profile
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Profile")
        Column(Modifier.padding(horizontal = 15.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(62.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(c.sunlit, c.coral))),
                    contentAlignment = Alignment.Center,
                ) { Text(p.avatar, fontSize = 32.sp) }
                Column {
                    Text(p.displayName, color = c.deep, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(p.role, color = c.muted, fontSize = 12.sp)
                }
            }
            // impact tile
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(c.sea, c.deep))).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${p.hatchlingsReached}", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Text("hatchlings reached the sea 🌊", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                }
            }
            // stats
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("🔥 ${p.streakDays}", "day streak", Modifier.weight(1f))
                StatTile("${p.kmWalked.toInt()} km", "walked", Modifier.weight(1f))
                StatTile("${p.patrols}", "patrols", Modifier.weight(1f))
            }
            // badges
            SectionLabel("Badges")
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                state.badges.forEach { b -> BadgeCell(b, Modifier.weight(1f)) }
            }
            // leaderboard (de-emphasised)
            SectionLabel("Beach board · optional")
            CarettaCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Column {
                    LeaderRow("1", "🦊", "Mert", "28", top = true)
                    LeaderRow("2", "🐢", "${p.displayName} (you)", "24", top = false)
                }
            }
            // community
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(c.surface)
                    .border(1.dp, c.line, RoundedCornerShape(13.dp)).clickable { onOpenCommunity() }
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🐢  Community", color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Text("›", color = c.muted, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val c = caretta
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(14.dp)).padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = c.deep, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = c.muted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BadgeCell(b: Badge, modifier: Modifier = Modifier) {
    val c = caretta
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(15.dp))
                .background(if (b.earned) c.sunlit.copy(alpha = 0.22f) else c.line.copy(alpha = 0.4f))
                .border(1.dp, c.line, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(b.emoji, fontSize = 22.sp) }
        Text(b.name, color = c.muted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LeaderRow(rank: String, avatar: String, name: String, score: String, top: Boolean) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(rank, color = if (top) c.sunlit else c.muted, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Text(avatar, fontSize = 16.sp)
        Text(name, color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(score, color = c.sea, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}
