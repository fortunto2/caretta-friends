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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.domain.AppState
import com.carettafriends.domain.GuideArticle
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

@Composable
fun LearnScreen(state: AppState) {
    val c = caretta
    val uriHandler = LocalUriHandler.current
    var factIndex by remember { mutableStateOf(0) }
    val facts = state.facts
    val fact = if (facts.isNotEmpty()) facts[factIndex % facts.size] else null

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Learn")
        Column(
            Modifier.padding(horizontal = 15.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // hero "Did you know?" — tap to cycle
            if (fact != null) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 150.dp).clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(c.sunlit, c.coral)))
                        .clickable { if (facts.size > 1) factIndex = (factIndex + 1) % facts.size },
                ) {
                    Text(
                        fact.emoji, fontSize = 96.sp, color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 2.dp),
                    )
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("DID YOU KNOW?", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                        Text(fact.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                        if (facts.size > 1) Text("tap for another →", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // guide — tappable rows opening the source URL (no per-row site chip)
            SectionLabel("Guide · what to do")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.guide.forEach { article ->
                    GuideRow(article) { uriHandler.openUri(article.sourceUrl) }
                }
            }

            // ONE link to the site (not repeated per row)
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(c.sea, c.deep)))
                    .clickable { uriHandler.openUri(state.community.websiteUrl) }
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🌐", fontSize = 20.sp)
                    Column(Modifier.weight(1f)) {
                        Text("Visit carettafriends.com", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text("News, guides & how to help", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                    Text("→", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // AI-TODO: multilingual (RU/TR/EN) content + auto-fetch latest posts from carettafriends.com (Ktor)
        }
    }
}

@Composable
private fun GuideRow(article: GuideArticle, onClick: () -> Unit) {
    val c = caretta
    CarettaCard(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(c.sand),
                contentAlignment = Alignment.Center,
            ) { Text(article.emoji, fontSize = 22.sp) }
            Text(
                article.title, color = c.deep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                lineHeight = 19.sp, modifier = Modifier.weight(1f),
            )
            Text("›", color = c.muted, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
