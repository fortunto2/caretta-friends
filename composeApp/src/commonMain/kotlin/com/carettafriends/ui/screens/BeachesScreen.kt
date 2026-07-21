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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.data.nestDay
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Beach
import com.carettafriends.domain.Nest
import com.carettafriends.domain.NestStatus
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.EmptyHint
import com.carettafriends.ui.components.Pill
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.StatusPill
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

@Composable
fun BeachesScreen(state: AppState, onOpenNest: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Beaches")
        Column(
            Modifier.padding(horizontal = 15.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel("Your beaches 🏖️")
            if (state.beaches.isEmpty()) {
                EmptyHint("🏖️", "No beaches assigned yet.\nPick your community to get started.")
            } else {
                state.beaches.forEach { beach ->
                    val nests = state.nests.filter { it.beachId == beach.id }
                    BeachGroup(
                        beach = beach,
                        nests = nests,
                        active = nests.count { it.status == NestStatus.INCUBATING || it.status == NestStatus.HATCHING },
                        hatchingSoon = nests.count { it.status == NestStatus.HATCHING },
                        patrolled = state.patrols.any { it.beachId == beach.id },
                        onOpenNest = onOpenNest,
                    )
                }
            }
        }
    }
}

@Composable
private fun BeachGroup(
    beach: Beach,
    nests: List<Nest>,
    active: Int,
    hatchingSoon: Int,
    patrolled: Boolean,
    onOpenNest: (String) -> Unit,
) {
    val c = caretta
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // beach header card
        CarettaCard {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                            .background(Brush.linearGradient(listOf(c.sea, c.deep))),
                        contentAlignment = Alignment.Center,
                    ) { Text("🏖️", fontSize = 24.sp) }
                    Column(Modifier.weight(1f)) {
                        Text(beach.name, color = c.deep, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(beach.city, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (patrolled) Pill("✓ Patrolled today", c.good)
                }
                // beach leader
                if (beach.leaderName != null) {
                    Text(
                        "👑 ${beach.leaderAvatar} ${beach.leaderName}",
                        color = c.ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                // counts
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Pill("🥚 $active active", c.sea)
                    if (hatchingSoon > 0) Pill("🐣 $hatchingSoon hatching soon", c.warn)
                }
            }
        }
        // compact nest rows under the beach card
        if (nests.isEmpty()) {
            Text(
                "No nests logged here yet 🐢",
                color = c.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
        } else {
            nests.forEach { nest -> NestRow(nest, onOpenNest) }
        }
    }
}

@Composable
private fun NestRow(nest: Nest, onOpenNest: (String) -> Unit) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(13.dp))
            .clickable { onOpenNest(nest.id) }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(nest.code, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        StatusPill(nest.status)
        Text("Day ${nestDay(nest)}", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("›", color = c.muted, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
    }
}
