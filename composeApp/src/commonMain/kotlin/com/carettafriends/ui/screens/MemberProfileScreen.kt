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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.content.appStrings
import com.carettafriends.content.badgeName
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Badge
import com.carettafriends.domain.Member
import com.carettafriends.domain.MemberRole
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.LocalPhoto
import com.carettafriends.ui.components.Pill
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

/**
 * Read-only profile of any volunteer — reached by tapping their name/avatar anywhere in the app
 * (community team, leaderboard, beach leader, nest finder / timeline author). Stats & badges are
 * derived from their nest activity (V1 keeps no per-member store); tapping a nest opens it.
 */
@Composable
fun MemberProfileScreen(
    member: Member,
    state: AppState,
    onBack: () -> Unit,
    onOpenNest: (String) -> Unit,
    onOpenBeach: (String) -> Unit,
) {
    val c = caretta
    val uri = LocalUriHandler.current
    val s = appStrings(state.profile.language)

    val nests = state.nestsBy(member)
    val hatchlings = nests.sumOf { it.excavation?.hatchlingsToSea ?: 0 }
    val badges = state.earnedBadgesFor(member)
    val homeBeach = member.homeBeach?.let { name -> state.beaches.firstOrNull { it.name == name } }
    val photos = nests
        .sortedByDescending { it.updatedAtMillis }
        .flatMap { n -> n.photos.mapNotNull { ph -> ph.localUri?.let { uriStr -> uriStr to n.id } } }
        .take(9)

    val roleTint = when (member.role) {
        MemberRole.ADMIN -> c.coral
        MemberRole.BEACH_LEADER -> c.sea
        MemberRole.VOLUNTEER -> c.muted
    }
    val roleText = when (member.role) {
        MemberRole.ADMIN -> s.roleAdmin
        MemberRole.BEACH_LEADER -> s.roleLeader
        MemberRole.VOLUNTEER -> s.roleVolunteer
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(member.name, onBack = onBack)
        Column(
            Modifier.padding(horizontal = 15.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // identity — avatar + name + role, home beach chip (tappable)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(roleTint.copy(alpha = 0.85f), c.deep))),
                    contentAlignment = Alignment.Center,
                ) { Text(member.avatar, fontSize = 30.sp) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(member.name, color = c.deep, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Pill(roleText, roleTint)
                }
            }

            if (homeBeach != null) {
                Box(Modifier.clickable { onOpenBeach(homeBeach.id) }) {
                    Pill("🏖️ ${homeBeach.name}", c.sea)
                }
            }

            // impact — hatchlings this volunteer helped reach the sea
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(c.sea, c.deep))).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$hatchlings", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Text(s.hatchlingsReached, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                }
            }

            // stats
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MemberStat("${nests.size} 🥚", s.memberNestsStat, Modifier.weight(1f))
                MemberStat("$hatchlings 🐢", s.memberHatchStat, Modifier.weight(1f))
            }

            // public link (Instagram / blog / website) — the one contact a volunteer chose to share
            if (member.link.isNotBlank()) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(c.surface)
                        .border(1.dp, c.line, RoundedCornerShape(15.dp))
                        .clickable { runCatching { uri.openUri(member.link) } }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(c.sea.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) { Text("🔗", fontSize = 20.sp) }
                    Column(Modifier.weight(1f)) {
                        Text(s.memberLink, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        Text(linkHostShort(member.link), color = c.muted, fontSize = 12.sp)
                    }
                    Text("›", color = c.muted, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // badges — derived from activity (earned only, no greyed-out locks)
            if (badges.isNotEmpty()) {
                SectionLabel(s.badges)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    badges.forEach { b -> MemberBadge(b, s.badgeName(b.code)) }
                }
            }

            // their nests — photo grid (tap → nest) + code chips for photo-less nests
            SectionLabel(s.memberNestsSection)
            if (nests.isEmpty()) {
                Text(s.memberNoNests, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    nests.forEach { n ->
                        Box(Modifier.clickable { onOpenNest(n.id) }) { Pill("🥚 ${n.code}", c.sea) }
                    }
                }
                if (photos.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        photos.chunked(3).forEach { rowPhotos ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowPhotos.forEach { (photoUri, nestId) ->
                                    LocalPhoto(
                                        photoUri,
                                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                                            .clickable { onOpenNest(nestId) },
                                    )
                                }
                                repeat(3 - rowPhotos.size) { Box(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberStat(value: String, label: String, modifier: Modifier = Modifier) {
    val c = caretta
    CarettaCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(value, color = c.deep, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MemberBadge(b: Badge, label: String) {
    val c = caretta
    Column(Modifier.width(68.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(15.dp))
                .background(c.sunlit.copy(alpha = 0.22f)).border(1.dp, c.line, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(b.emoji, fontSize = 22.sp) }
        Text(label, color = c.muted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Compact display host for a shared link (strip scheme/www/trailing slash). */
private fun linkHostShort(link: String): String =
    link.removePrefix("https://").removePrefix("http://").removePrefix("www.").trimEnd('/')
