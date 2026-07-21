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
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Beach
import com.carettafriends.ui.components.Pill
import com.carettafriends.ui.components.PrimaryButton
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.theme.caretta

@Composable
fun CommunityScreen(state: AppState, onBack: () -> Unit) {
    val c = caretta
    val community = state.community
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // cover header
        Box(
            Modifier.fillMaxWidth().height(104.dp)
                .background(Brush.linearGradient(listOf(c.sea, c.deep))),
        ) {
            // big turtle in the corner
            Text(
                "🐢",
                fontSize = 74.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp),
            )
            // back button on the cover
            Box(
                Modifier.align(Alignment.TopStart).padding(12.dp).size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("‹", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
            // title + tagline
            Column(
                Modifier.align(Alignment.BottomStart).padding(start = 15.dp, bottom = 14.dp, end = 70.dp),
            ) {
                Text(community.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(community.tagline, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
            }
        }

        Column(
            Modifier.padding(horizontal = 15.dp).padding(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryButton("＋ Join community") { }

            SectionLabel("Reach us")
            LinkRow("💬", "WhatsApp chat", "Ask an admin to connect you", c.good)
            LinkRow("🌐", "carettafriends.com", "Official website", c.sea)
            LinkRow("📸", "Instagram", "@gazipasa_caretta_ve_kumzambagi", c.coral)

            SectionLabel("Beach leaders · ответственные")
            state.beaches.filter { it.leaderName != null }.forEach { b -> LeaderRow(b) }
        }
    }
}

@Composable
private fun LinkRow(emoji: String, title: String, subtitle: String, tint: Color) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(15.dp)).clickable { }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 20.sp) }
        Column(Modifier.weight(1f)) {
            Text(title, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = c.muted, fontSize = 12.sp)
        }
        Text("›", color = c.muted, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun LeaderRow(beach: Beach) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(15.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(c.sea.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Text(beach.leaderAvatar, fontSize = 20.sp) }
        Column(Modifier.weight(1f)) {
            Text(beach.leaderName ?: "", color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(beach.name, color = c.muted, fontSize = 12.sp)
        }
        Pill("Leader", c.sea)
    }
}
