package com.carettafriends.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.content.appStrings
import com.carettafriends.domain.AppState
import com.carettafriends.ui.theme.caretta
import kotlinx.coroutines.launch

/** First-run intro — 3 friendly pages: what it is · what you can do · why it matters. Localized. */
@Composable
fun OnboardingScreen(state: AppState, onDone: () -> Unit) {
    val c = caretta
    val s = appStrings(state.profile.language)
    val pages = listOf(
        Triple("🐢", s.ob1Title, s.ob1Body),
        Triple("📍", s.ob2Title, s.ob2Body),
        Triple("🙌", s.ob3Title, s.ob3Body),
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.sea, c.deep)))) {
        // Skip (top-right)
        Text(
            s.obSkip,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 20.dp)
                .clip(RoundedCornerShape(10.dp)).clickable { onDone() }.padding(8.dp),
        )

        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val (emoji, title, body) = pages[page]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 34.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(emoji, fontSize = 92.sp)
                Spacer(Modifier.height(26.dp))
                Text(
                    title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    body,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }
        }

        // Bottom controls: page dots + Next / Let's go
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    val on = i == pager.currentPage
                    val dot by animateColorAsState(if (on) Color.White else Color.White.copy(alpha = 0.35f))
                    Box(Modifier.size(if (on) 10.dp else 8.dp).clip(CircleShape).background(dot))
                }
            }
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier.clip(RoundedCornerShape(16.dp)).background(c.coral)
                    .clickable {
                        if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                    .padding(horizontal = 44.dp, vertical = 15.dp),
            ) {
                Text(
                    if (last) s.obStart else s.obNext,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}
