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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.content.AppStrings
import com.carettafriends.content.appStrings
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Community
import com.carettafriends.domain.CommunityKind
import com.carettafriends.domain.Member
import com.carettafriends.domain.MemberRole
import com.carettafriends.ui.components.Pill
import com.carettafriends.ui.components.PrimaryButton
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.theme.caretta

@Composable
fun CommunityScreen(community: Community, state: AppState, onBack: () -> Unit) {
    val c = caretta
    val uri = LocalUriHandler.current
    val s = appStrings(state.profile.language)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // cover header
        Box(
            Modifier.fillMaxWidth().height(104.dp)
                .background(Brush.linearGradient(listOf(c.sea, c.deep))),
        ) {
            Text("🐢", fontSize = 74.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp))
            Box(
                Modifier.align(Alignment.TopStart).padding(12.dp).size(34.dp)
                    .clip(RoundedCornerShape(11.dp)).background(Color.White.copy(alpha = 0.22f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("‹", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 15.dp, bottom = 14.dp, end = 70.dp)) {
                Text(community.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(community.taglineFor(state.profile.language), color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
            }
        }

        Column(
            Modifier.padding(horizontal = 15.dp).padding(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Type badge (community / NGO / university / official) + who they're affiliated with.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(kindLabel(community.kind, s), kindColor(community.kind))
                if (community.affiliation.isNotBlank()) {
                    Text(community.affiliation, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Short human summary (who they are, what they do, how long).
            val localizedDesc = community.descriptionFor(state.profile.language)
            if (localizedDesc.isNotBlank()) {
                Text(localizedDesc, color = c.deep, fontSize = 13.sp, lineHeight = 18.sp)
            }

            if (community.claimed) {
                // Join = message the org on WhatsApp (an admin adds you). No public self-join.
                PrimaryButton(s.joinWhatsapp) { uri.openUri(community.whatsappUrl) }
                Text(
                    s.joinSub,
                    color = c.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )

                // ── Team (admins & volunteers) ──────────────────────────
                SectionLabel(s.team)
                state.members.sortedBy { it.role.ordinal }.forEach { m -> MemberRow(m, s) }
                MemberRow(
                    Member(
                        "you", "${state.profile.displayName} (${s.youWord})", state.profile.memberRole,
                        state.profile.avatar, link = state.profile.link,
                    ),
                    s,
                )
            } else {
                // STUB: a real local group not on Caretta Friends yet — show contacts + a claim CTA.
                Text(
                    if (community.nearArea.isNotBlank()) "${s.localGroupNear} ${community.nearArea}" else s.localGroup,
                    color = c.sea, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                )
                Text(
                    s.stubNotOnApp + s.stubClaim,
                    color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                )
                PrimaryButton(s.weRunGroup) { uri.openUri(state.community.whatsappUrl) }
            }

            // ── Reach them (tappable) — WhatsApp / call first, then the rest ──
            SectionLabel(s.reachThem)
            val digits = community.phone.filter { ch -> ch.isDigit() || ch == '+' }
            if (community.whatsappUrl.isNotBlank()) {
                LinkRow("💬", s.reachWhatsapp, linkHost(community.whatsappUrl), c.good) { uri.openUri(community.whatsappUrl) }
            }
            if (community.phone.isNotBlank()) {
                LinkRow("📞", s.callWord, community.phone, c.sea) { runCatching { uri.openUri("tel:$digits") } }
            }
            if (community.email.isNotBlank()) {
                LinkRow("✉️", s.email, community.email, c.deep) { runCatching { uri.openUri("mailto:${community.email}") } }
            }
            if (community.instagramUrl.isNotBlank()) {
                LinkRow("📸", s.instagramWord, linkHost(community.instagramUrl), c.coral) { uri.openUri(community.instagramUrl) }
            }
            if (community.websiteUrl.isNotBlank()) {
                LinkRow("🌐", s.websiteWord, linkHost(community.websiteUrl), c.sea) { uri.openUri(community.websiteUrl) }
            }
            if (community.adminContact.isNotBlank()) {
                LinkRow("📇", s.contactWord, community.adminContact, c.deep) {
                    if (community.adminContact.startsWith("http")) uri.openUri(community.adminContact)
                }
            }

            if (!community.claimed) return@Column

            // ── Top volunteers — by nests found & hatchlings freed ──────
            SectionLabel(s.topVolunteers)
            val board = state.nests
                .groupBy { it.foundBy }
                .map { (name, ns) -> Ranked(name, ns.size, ns.sumOf { it.excavation?.hatchlingsToSea ?: 0 }) }
                .sortedWith(compareByDescending<Ranked> { it.nests }.thenByDescending { it.hatchlings })
            if (board.isEmpty()) {
                Text(
                    s.noNestsBeFirst,
                    color = c.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                board.take(10).forEachIndexed { i, r -> BoardRow(i + 1, r) }
            }
        }
    }
}

private data class Ranked(val name: String, val nests: Int, val hatchlings: Int)

private fun kindLabel(k: CommunityKind, s: AppStrings): String = when (k) {
    CommunityKind.COMMUNITY -> s.kindCommunity
    CommunityKind.NGO -> s.kindNgo
    CommunityKind.UNIVERSITY -> s.kindUniversity
    CommunityKind.OFFICIAL -> s.kindOfficial
}

private fun kindColor(k: CommunityKind): Color = when (k) {
    CommunityKind.COMMUNITY -> Color(0xFF2E9E5B)
    CommunityKind.NGO -> Color(0xFF0F7A82)
    CommunityKind.UNIVERSITY -> Color(0xFFF97316)
    CommunityKind.OFFICIAL -> Color(0xFF123B40)
}

private fun roleLabel(role: MemberRole, s: AppStrings): String = when (role) {
    MemberRole.ADMIN -> s.roleAdmin
    MemberRole.BEACH_LEADER -> s.roleLeader
    MemberRole.VOLUNTEER -> s.roleVolunteer
}

@Composable
private fun MemberRow(m: Member, s: AppStrings) {
    val c = caretta
    val uri = LocalUriHandler.current
    val hasLink = m.link.isNotBlank()
    val tint = when (m.role) {
        MemberRole.ADMIN -> c.coral
        MemberRole.BEACH_LEADER -> c.sea
        MemberRole.VOLUNTEER -> c.muted
    }
    val base = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(c.surface)
        .border(1.dp, c.line, RoundedCornerShape(15.dp))
    Row(
        (if (hasLink) base.clickable { runCatching { uri.openUri(m.link) } } else base).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Text(m.avatar, fontSize = 20.sp) }
        Column(Modifier.weight(1f)) {
            Text(m.name, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            when {
                hasLink -> Text("🔗 ${linkHost(m.link)}", color = c.sea, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                m.note.isNotBlank() -> Text(m.note, color = c.muted, fontSize = 12.sp)
            }
        }
        Pill(roleLabel(m.role, s), tint)
    }
}

/** Trim a URL/handle to a compact display host (e.g. "instagram.com/foo" → "instagram.com/foo"). */
private fun linkHost(link: String): String =
    link.removePrefix("https://").removePrefix("http://").removePrefix("www.").trimEnd('/')

@Composable
private fun BoardRow(rank: Int, r: Ranked) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("$rank", color = if (rank == 1) c.sunlit else c.muted, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Text(r.name, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("${r.nests} 🥚", color = c.sea, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        if (r.hatchlings > 0) Text("${r.hatchlings} 🐢", color = c.good, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun LinkRow(emoji: String, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(15.dp)).clickable { onClick() }
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
