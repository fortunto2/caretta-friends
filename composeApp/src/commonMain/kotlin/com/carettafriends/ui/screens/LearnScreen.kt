package com.carettafriends.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.domain.AppState
import com.carettafriends.domain.GuideArticle
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.EmptyHint
import com.carettafriends.ui.components.Pill
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

@Composable
fun LearnScreen(state: AppState) {
    val c = caretta
    // cycle facts on tap of the hero card
    var factIndex by remember { mutableStateOf(0) }
    val facts = state.facts
    val fact = if (facts.isNotEmpty()) facts[factIndex % facts.size] else null

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Learn")
        Column(
            Modifier.padding(horizontal = 15.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // hero "Did you know?" card — tap to cycle facts
            if (fact != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(c.sunlit, c.coral)))
                        .clickable { if (facts.size > 1) factIndex = (factIndex + 1) % facts.size },
                ) {
                    // large faded emoji tucked in the corner
                    Text(
                        fact.emoji,
                        fontSize = 96.sp,
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 2.dp),
                    )
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "DID YOU KNOW?",
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                        )
                        Text(
                            fact.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 26.sp,
                        )
                        if (facts.size > 1) {
                            Text(
                                "tap for another →",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // guide list → carettafriends.com articles
            SectionLabel("Guide · what to do")
            if (state.guide.isEmpty()) {
                EmptyHint("📖", "No guides yet — check carettafriends.com")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.guide.forEach { article -> GuideRow(article) }
                }
            }
        }
    }
}

@Composable
private fun GuideRow(article: GuideArticle) {
    val c = caretta
    CarettaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(c.sand),
                contentAlignment = Alignment.Center,
            ) { Text(article.emoji, fontSize = 22.sp) }
            Text(
                article.title,
                color = c.deep,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 19.sp,
                modifier = Modifier.weight(1f),
            )
            Pill("🔗 ${siteOf(article.sourceUrl)}", c.sea)
        }
    }
}

/** Short host label from a source URL for the small "site" chip. */
private fun siteOf(url: String): String {
    val noScheme = url.substringAfter("://", url)
    val host = noScheme.substringBefore('/').removePrefix("www.")
    return host.ifBlank { "site" }
}
