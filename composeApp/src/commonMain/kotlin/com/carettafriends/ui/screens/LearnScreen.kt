package com.carettafriends.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.carettafriends.content.GuideContent
import com.carettafriends.content.GuideMeta
import com.carettafriends.content.L10n
import com.carettafriends.data.CarettaRepository
import com.mikepenz.markdown.m3.Markdown
import com.carettafriends.domain.AppState
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

private val lblDidYouKnow = L10n("А ТЫ ЗНАЛ?", "BİLİYOR MUYDUN?", "DID YOU KNOW?")
private val lblTapAnother = L10n("нажми для следующего →", "başkası için dokun →", "tap for another →")
private val lblGuide = L10n("Гайд · что делать", "Rehber · ne yapmalı", "Guide · what to do")
private val lblVisit = L10n("Открыть carettafriends.com", "carettafriends.com'u aç", "Visit carettafriends.com")
private val lblVisitSub = L10n("Новости, гайды и как помочь", "Haberler, rehberler ve nasıl yardım edilir", "News, guides & how to help")

@Composable
fun LearnScreen(state: AppState, repo: CarettaRepository) {
    val c = caretta
    val lang = state.profile.language
    val uriHandler = LocalUriHandler.current
    var factIndex by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf<String?>(null) }
    val facts = GuideContent.facts
    val fact = if (facts.isNotEmpty()) facts[factIndex % facts.size] else null

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Learn")
        Column(
            Modifier.padding(horizontal = 15.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // language switcher
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GuideContent.languages.forEach { (code, label) ->
                    val on = code == lang
                    Box(
                        Modifier.clip(CircleShape).background(if (on) c.sea else c.surface)
                            .border(1.dp, c.line, CircleShape)
                            .clickable { repo.setLanguage(code) }
                            .padding(horizontal = 15.dp, vertical = 7.dp),
                    ) {
                        Text(label, color = if (on) Color.White else c.muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // fact hero — tap to cycle
            if (fact != null) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 140.dp).clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(c.sunlit, c.coral)))
                        .clickable { if (facts.size > 1) factIndex = (factIndex + 1) % facts.size },
                ) {
                    Text("🌡️", fontSize = 96.sp, color = Color.White.copy(alpha = 0.22f), modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp))
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(lblDidYouKnow.get(lang), color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                        Text(fact.get(lang), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
                        if (facts.size > 1) Text(lblTapAnother.get(lang), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // guide — expandable baked-in articles
            SectionLabel(lblGuide.get(lang))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GuideContent.articles.forEach { doc ->
                    ArticleCard(doc, lang, expanded == doc.id) { expanded = if (expanded == doc.id) null else doc.id }
                }
            }

            // one link to the site
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(c.sea, c.deep)))
                    .clickable { uriHandler.openUri(GuideContent.siteUrl) }
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🌐", fontSize = 20.sp)
                    Column(Modifier.weight(1f)) {
                        Text(lblVisit.get(lang), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(lblVisitSub.get(lang), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                    Text("→", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun ArticleCard(meta: GuideMeta, lang: String, expanded: Boolean, onToggle: () -> Unit) {
    val c = caretta
    CarettaCard(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onToggle() }.animateContentSize()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(c.sand), contentAlignment = Alignment.Center) {
                    Text(meta.emoji, fontSize = 22.sp)
                }
                Text(meta.title.get(lang), color = c.deep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp, modifier = Modifier.weight(1f))
                Text(if (expanded) "▾" else "›", color = c.muted, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (expanded) {
                var body by remember(meta.id, lang) { mutableStateOf<String?>(null) }
                LaunchedEffect(meta.id, lang) { body = GuideContent.loadArticle(meta.id, lang) }
                val text = body
                if (text == null) {
                    Text("…", color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                } else {
                    Markdown(content = text, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
