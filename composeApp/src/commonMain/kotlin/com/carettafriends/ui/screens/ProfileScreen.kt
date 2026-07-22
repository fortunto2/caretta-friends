package com.carettafriends.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Badge
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.Pill
import com.carettafriends.data.CarettaRepository
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

@Composable
fun ProfileScreen(
    repo: CarettaRepository,
    state: AppState,
    onOpenCommunity: () -> Unit,
    onOpenStats: () -> Unit = {},
) {
    val c = caretta
    val p = state.profile
    var editingName by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Profile")
        Column(Modifier.padding(horizontal = 15.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // header — tap to set your name (works offline, before any login)
            Row(
                Modifier.clickable { editingName = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(62.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(c.sunlit, c.coral))),
                    contentAlignment = Alignment.Center,
                ) { Text(p.avatar, fontSize = 32.sp) }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(p.displayName, color = c.deep, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("✎", color = c.muted, fontSize = 15.sp)
                    }
                    Text("Tap to set your name", color = c.muted, fontSize = 12.sp)
                }
            }
            if (editingName) {
                var draft by remember { mutableStateOf(p.displayName) }
                AlertDialog(
                    onDismissRequest = { editingName = false },
                    title = { Text("Your name") },
                    text = {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it.take(40) },
                            singleLine = true,
                            label = { Text("Display name") },
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { repo.setDisplayName(draft); editingName = false }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingName = false }) { Text("Cancel") }
                    },
                )
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

            // my beach — optional home beach (most volunteers are free)
            SectionLabel("My beach")
            Text(
                "Free volunteers patrol wherever's closest. Pick a home beach only if you have one.",
                color = c.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeBeachChip("🌊 Free", p.homeBeachId == null) { repo.setHomeBeach(null) }
                state.beaches.forEach { b ->
                    HomeBeachChip("${b.leaderAvatar} ${b.name}", p.homeBeachId == b.id) { repo.setHomeBeach(b.id) }
                }
            }
            // community
            ProfileNavRow("🐢  Community", onOpenCommunity)
            // trends / charts
            ProfileNavRow("📊  Trends & charts", onOpenStats)
        }
    }
}

@Composable
private fun ProfileNavRow(label: String, onClick: () -> Unit) {
    val c = caretta
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(13.dp)).clickable { onClick() }.padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text("›", color = c.muted, fontSize = 18.sp)
        }
    }
}

@Composable
private fun HomeBeachChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = caretta
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) c.sea else c.surface)
            .border(1.dp, if (selected) c.sea else c.line, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.White else c.deep,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
        )
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

