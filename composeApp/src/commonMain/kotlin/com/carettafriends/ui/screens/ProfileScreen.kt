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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.content.appStrings
import com.carettafriends.content.badgeName
import com.carettafriends.data.CarettaRepository
import com.carettafriends.data.platformShare
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Badge
import com.carettafriends.domain.MemberRole
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.LocalPhoto
import com.carettafriends.ui.components.Pill
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

@Composable
fun ProfileScreen(
    repo: CarettaRepository,
    state: AppState,
    onOpenCommunity: () -> Unit,
    onOpenStats: () -> Unit = {},
    onOpenNest: (String) -> Unit = {},
    onOpenBeach: (String) -> Unit = {},
    onAddNest: () -> Unit = {},
) {
    val c = caretta
    val p = state.profile
    val s = appStrings(p.language)
    var editingName by remember { mutableStateOf(false) }
    var pickingBeach by remember { mutableStateOf(false) }
    var pickingLang by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var deletedNotice by remember { mutableStateOf(false) }
    var feedRange by remember { mutableStateOf(FeedRange.ALL) }
    var showMenu by remember { mutableStateOf(false) }

    // "Beaches you've been to" — where this volunteer has patrolled or logged a nest.
    val visitedIds = (state.patrols.map { it.beachId } + state.nests.filter { it.foundBy == p.displayName }.map { it.beachId }).toSet()
    val visited = state.beaches.filter { it.id in visitedIds }
    val home = p.homeBeachId?.let { state.beach(it) }
    val earnedBadges = state.badges.filter { it.earned }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // "⋮" overflow tucks away the non-essential actions (share today's report) so the header stays clean.
        TopBar(s.profile, trailing = {
            Box {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(c.surface)
                        .border(1.dp, c.line, RoundedCornerShape(11.dp)).clickable { showMenu = true },
                    contentAlignment = Alignment.Center,
                ) { Text("⋮", color = c.deep, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("📤 ${s.shareDay}") },
                        onClick = { showMenu = false; platformShare(todayReportText(state, s)) },
                    )
                }
            }
        })
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
                ) {
                    // A photo (once added) replaces the emoji; tap the name row to edit for now.
                    if (p.photoPath != null) {
                        LocalPhoto(p.photoPath, Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)))
                    } else {
                        Text(p.avatar, fontSize = 32.sp)
                    }
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(p.displayName, color = c.deep, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("✎", color = c.muted, fontSize = 15.sp)
                    }
                    val roleText = when (p.memberRole) {
                        MemberRole.ADMIN -> s.roleAdmin
                        MemberRole.BEACH_LEADER -> s.roleLeader
                        MemberRole.VOLUNTEER -> s.roleVolunteer
                    }
                    Text(roleText, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (editingName) {
                var draft by remember { mutableStateOf(p.displayName) }
                AlertDialog(
                    onDismissRequest = { editingName = false },
                    title = { Text(s.yourName) },
                    text = {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it.take(40) },
                            singleLine = true,
                            label = { Text(s.displayName) },
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { repo.setDisplayName(draft); editingName = false }) { Text(s.save) }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingName = false }) { Text(s.cancel) }
                    },
                )
            }

            // account — anonymous volunteers get a nudge to save their work under an email.
            if (state.accountEmail == null) {
                AccountCta(s.saveAccount, s.saveAccountSub) { showAuth = true }
            } else {
                SignedInRow(s.signedIn, state.accountEmail!!)
            }
            if (showAuth) {
                EmailAuthDialog(repo, appStrings(p.language), onDismiss = { showAuth = false })
            }

            // impact tile — when it's still 0, the whole tile is a tappable "log your first nest" CTA.
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(c.sea, c.deep)))
                    .then(if (p.hatchlingsReached == 0) Modifier.clickable { onAddNest() } else Modifier)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (p.hatchlingsReached > 0) {
                        Text("${p.hatchlingsReached}", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                        Text(s.hatchlingsReached, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    } else {
                        Text("🐣", fontSize = 30.sp)
                        Text(s.impactZeroCta, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
            // share my impact — motivational "I'm a volunteer" card via the OS share sheet.
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(c.coral.copy(alpha = 0.14f)).border(1.dp, c.coral.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable { platformShare(s.shareText.replace("%d", p.hatchlingsReached.toString())) }
                    .padding(13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(s.shareImpact, color = c.coral, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }

            // stats
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("🔥 ${p.streakDays}", s.dayStreak, Modifier.weight(1f))
                StatTile("${p.kmWalked.toInt()} km", s.walked, Modifier.weight(1f))
                StatTile("${p.patrols}", s.patrols, Modifier.weight(1f))
            }

            // community — shown directly (a volunteer usually belongs to 1–3), tap to open.
            SectionLabel(s.community)
            CommunityCard(
                s = s,
                name = state.community.name,
                tagline = state.community.taglineFor(p.language),
                members = state.members.size,
                onOpen = onOpenCommunity,
            )

            // my beach — one pinned home beach + everywhere you've patrolled.
            SectionLabel(s.myBeach)
            HomeBeachCard(
                title = home?.let { "${it.leaderAvatar} ${it.name}" } ?: s.freeVolunteer,
                sub = if (home != null) s.yourHomeBeach else s.patrolClosest,
                change = s.change,
                onChange = { pickingBeach = true },
            )
            if (visited.isNotEmpty()) {
                Text(s.patrolled, color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    visited.forEach { b -> Box(Modifier.clickable { onOpenBeach(b.id) }) { Pill("🏖️ ${b.name}", c.sea) } }
                }
            }
            if (pickingBeach) {
                AlertDialog(
                    onDismissRequest = { pickingBeach = false },
                    title = { Text(s.homeBeach) },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BeachPickRow(s.freeVolunteer, p.homeBeachId == null) {
                                repo.setHomeBeach(null); pickingBeach = false
                            }
                            state.beaches.forEach { b ->
                                BeachPickRow("${b.leaderAvatar} ${b.name}", p.homeBeachId == b.id) {
                                    repo.setHomeBeach(b.id); pickingBeach = false
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { pickingBeach = false }) { Text(s.done) } },
                )
            }

            // badges — only the ones actually earned (no confusing greyed-out locks).
            SectionLabel(s.badges)
            if (earnedBadges.isEmpty()) {
                Text(
                    s.noBadges,
                    color = c.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    earnedBadges.forEach { b -> BadgeCell(s, b) }
                }
            }

            // Activity — your trail (photo + what happened per nest), the same feed used on member,
            // beach and community screens. Time-filtered (today/week/month/all) + a "share today" report.
            val feed = activityFeed(state, s, member = state.meAsMember, range = feedRange)
            SectionLabel(s.activityTitle)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RangeChip(s.today, feedRange == FeedRange.TODAY) { feedRange = FeedRange.TODAY }
                RangeChip(s.rangeWeek, feedRange == FeedRange.WEEK) { feedRange = FeedRange.WEEK }
                RangeChip(s.rangeMonth, feedRange == FeedRange.MONTH) { feedRange = FeedRange.MONTH }
                RangeChip(s.filterAll, feedRange == FeedRange.ALL) { feedRange = FeedRange.ALL }
            }
            if (feed.isNotEmpty()) {
                ActivityFeed(feed, onOpenNest)
            } else {
                Text(s.activityEmpty, color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            // trends / charts
            ProfileNavRow(s.trends, onOpenStats)
            // language — switch the whole interface (RU / TR / EN).
            ProfileNavRow(s.language) { pickingLang = true }
            if (pickingLang) {
                AlertDialog(
                    onDismissRequest = { pickingLang = false },
                    title = { Text(s.language.trim()) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LANGUAGES.forEach { (code, label) ->
                                BeachPickRow(label, p.language.equals(code, ignoreCase = true)) {
                                    repo.setLanguage(code); pickingLang = false
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { pickingLang = false }) { Text(s.close) } },
                )
            }

            // Account deletion — App Store guideline 5.1.1(v): any app that lets you create an
            // account must let you delete it from inside the app. Field records survive; see
            // CarettaRepository.deleteAccount.
            DangerNavRow(s.deleteAccount) { confirmingDelete = true }
            if (confirmingDelete) {
                AlertDialog(
                    onDismissRequest = { if (!deleting) confirmingDelete = false },
                    title = { Text(s.deleteAccountTitle) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(s.deleteAccountBody, color = c.muted, fontSize = 13.sp)
                            deleteError?.let {
                                Text(it, color = c.risk, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = !deleting,
                            onClick = {
                                deleting = true
                                deleteError = null
                                repo.deleteAccount { err ->
                                    deleting = false
                                    if (err == null) {
                                        confirmingDelete = false
                                        deletedNotice = true
                                    } else {
                                        deleteError = err
                                    }
                                }
                            },
                        ) { Text(if (deleting) "…" else s.deleteAccountCta, color = c.risk) }
                    },
                    dismissButton = {
                        TextButton(enabled = !deleting, onClick = { confirmingDelete = false }) { Text(s.cancel) }
                    },
                )
            }
            if (deletedNotice) {
                AlertDialog(
                    onDismissRequest = { deletedNotice = false },
                    title = { Text(s.deleteAccountDone) },
                    confirmButton = { TextButton(onClick = { deletedNotice = false }) { Text(s.close) } },
                )
            }
        }
    }
}

private val LANGUAGES = listOf("en" to "🇬🇧 English", "ru" to "🇷🇺 Русский", "tr" to "🇹🇷 Türkçe")

@Composable
private fun CommunityCard(s: com.carettafriends.content.AppStrings, name: String, tagline: String, members: Int, onOpen: () -> Unit) {
    val c = caretta
    CarettaCard(modifier = Modifier.clickable { onOpen() }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(c.good, c.deep))),
                contentAlignment = Alignment.Center,
            ) { Text("🐢", fontSize = 22.sp) }
            Column(Modifier.weight(1f)) {
                Text(name, color = c.deep, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    tagline.ifBlank { s.yourCommunity } + if (members > 0) " · $members ${s.volunteersWord}" else "",
                    color = c.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
            }
            Text("›", color = c.muted, fontSize = 18.sp)
        }
    }
}

@Composable
private fun HomeBeachCard(title: String, sub: String, change: String, onChange: () -> Unit) {
    val c = caretta
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(13.dp)).clickable { onChange() }.padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(sub, color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Text(change, color = c.sea, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun BeachPickRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
            .background(if (selected) c.sea.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() }.padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (selected) Text("✓", color = c.sea, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
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

/** Same row as [ProfileNavRow] but reads as destructive — used for account deletion. */
@Composable
private fun DangerNavRow(label: String, onClick: () -> Unit) {
    val c = caretta
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(13.dp)).clickable { onClick() }.padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = c.risk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text("›", color = c.muted, fontSize = 18.sp)
        }
    }
}

@Composable
private fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = caretta
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) c.sea else c.surface)
            .border(1.dp, if (selected) c.sea else c.line, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (selected) Color.White else c.ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
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
private fun AccountCta(title: String, sub: String, onClick: () -> Unit) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(c.sunlit.copy(alpha = 0.18f)).border(1.dp, c.sunlit, RoundedCornerShape(14.dp))
            .clickable { onClick() }.padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("🔒", fontSize = 20.sp)
        Column(Modifier.weight(1f)) {
            Text(title, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(sub, color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Text("›", color = c.muted, fontSize = 18.sp)
    }
}

@Composable
private fun SignedInRow(label: String, email: String) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(c.good.copy(alpha = 0.14f)).border(1.dp, c.good.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("✓", color = c.good, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Column(Modifier.weight(1f)) {
            Text(label, color = c.deep, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(email, color = c.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EmailAuthDialog(repo: CarettaRepository, s: com.carettafriends.content.AppStrings, onDismiss: () -> Unit) {
    val c = caretta
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var signIn by remember { mutableStateOf(false) } // false = create/save, true = sign in
    val valid = email.contains("@") && email.contains(".") && password.length >= 6

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (signIn) s.signIn else s.saveAccount) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (signIn) s.welcomeBack else s.saveAccountBody,
                    color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim(); error = null },
                    singleLine = true,
                    label = { Text(s.email) },
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    singleLine = true,
                    label = { Text(s.passwordHint) },
                    visualTransformation = PasswordVisualTransformation(),
                )
                error?.let { Text(it, color = c.coral, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Text(
                    if (signIn) s.newHere else s.haveAccount,
                    color = c.sea,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable(enabled = !busy) { signIn = !signIn; error = null },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && valid,
                onClick = {
                    busy = true; error = null
                    val cb: (String?) -> Unit = { err -> busy = false; if (err == null) onDismiss() else error = err }
                    if (signIn) repo.signInEmail(email, password, cb) else repo.linkEmail(email, password, cb)
                },
            ) { Text(if (busy) "…" else if (signIn) s.signIn else s.save) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(s.cancel) } },
    )
}

@Composable
private fun BadgeCell(s: com.carettafriends.content.AppStrings, b: Badge) {
    val c = caretta
    val label = s.badgeName(b.code)
    Column(Modifier.width(68.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(15.dp))
                .background(c.sunlit.copy(alpha = 0.22f)).border(1.dp, c.line, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(b.emoji, fontSize = 22.sp) }
        Text(label, color = c.muted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
