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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.carettafriends.content.AppStrings
import com.carettafriends.content.appStrings
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Beach
import com.carettafriends.domain.GeoPoint
import com.carettafriends.domain.distanceLabel
import com.carettafriends.domain.distanceMeters
import com.carettafriends.ui.theme.caretta
import kotlinx.coroutines.launch

/** First-run intro — 3 friendly pages: what it is · what you can do · why it matters. Localized. */
@Composable
fun OnboardingScreen(state: AppState, onDone: () -> Unit, onPickBeach: (String) -> Unit = {}) {
    val c = caretta
    val s = appStrings(state.profile.language)
    val pages = listOf(
        Triple("🐢", s.ob1Title, s.ob1Body),
        Triple("📍", s.ob2Title, s.ob2Body),
        Triple("🙌", s.ob3Title, s.ob3Body),
    )
    // The nearest beaches, so the last page can ask "which one is yours?" instead of leaving the
    // volunteer to find that setting later. Beaches are discovered from OSM asynchronously at start,
    // so the list may still be filling — the page says so rather than showing nothing.
    val me = state.deviceLocation
    val nearby = remember(state.beaches, me) {
        val ordered = if (me != null) state.beaches.sortedBy { distanceMeters(me, it.center) } else state.beaches
        ordered.take(6)
    }
    val pageCount = pages.size + 1              // + "your beach"
    val pager = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pageCount - 1

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.sea, c.deep)))
            .safeDrawingPadding(),
    ) {
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
            if (page == pages.size) {
                BeachPickPage(s, nearby, state, me) { beachId ->
                    onPickBeach(beachId)
                    onDone()
                }
                return@HorizontalPager
            }
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
                (0 until pageCount).forEach { i ->
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
                    if (last) s.obSkipBeach else s.obNext,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

/**
 * Last onboarding page: which beach is yours.
 *
 * Asked here because it's the one setting that shapes everything after it — the beach decides the
 * community, the list order and what "my beach" means — and a volunteer who has to hunt for it in
 * settings simply never sets it. Nearest first, skippable, changeable later in the profile.
 */
@Composable
private fun BeachPickPage(
    s: AppStrings,
    beaches: List<Beach>,
    state: AppState,
    me: GeoPoint?,
    onPick: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🏖️", fontSize = 64.sp)
        Spacer(Modifier.height(18.dp))
        Text(
            s.obBeachTitle,
            color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, lineHeight = 30.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            s.obBeachBody,
            color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp,
            textAlign = TextAlign.Center, lineHeight = 20.sp,
        )
        Spacer(Modifier.height(20.dp))
        if (beaches.isEmpty()) {
            Text(
                s.findingBeaches,
                color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            )
            return@Column
        }
        Column(
            Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            beaches.forEach { b ->
                val community = state.allCommunities.firstOrNull { it.id == b.communityId }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable { onPick(b.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("🏖️", fontSize = 20.sp)
                    Column(Modifier.weight(1f)) {
                        Text(b.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        val sub = listOfNotNull(
                            community?.name,
                            me?.let { distanceLabel(distanceMeters(it, b.center)) },
                        ).joinToString(" · ")
                        if (sub.isNotBlank()) {
                            Text(sub, color = Color.White.copy(alpha = 0.75f), fontSize = 11.5.sp)
                        }
                    }
                    Text("›", color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)
                }
            }
        }
    }
}
