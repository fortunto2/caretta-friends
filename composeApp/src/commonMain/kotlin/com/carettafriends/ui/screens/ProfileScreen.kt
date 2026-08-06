package com.carettafriends.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.content.appStrings
import com.carettafriends.content.badgeName
import com.carettafriends.data.CarettaRepository
import com.carettafriends.data.platformShare
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Badge
import com.carettafriends.domain.MemberRole
import com.carettafriends.ui.DialogAction
import com.carettafriends.ui.DialogStyle
import com.carettafriends.ui.PlatformChoiceDialog
import com.carettafriends.ui.PlatformTextPrompt
import com.carettafriends.ui.choiceAction
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

    // Everything this volunteer has recorded, resolved by owner id (see AppState.nestsBy). Kept
    // behind remember: it scans every nest, and this screen recomposes on every keystroke in a dialog.
    val myNests = remember(state.nests, state.profile) {
        state.nestsBy(state.meAsMember).sortedByDescending { it.foundDate }
    }
    // "Beaches you've been to" — where this volunteer has patrolled or logged a nest.
    val visitedIds = (state.patrols.map { it.beachId } + myNests.map { it.beachId }).toSet()
    val visited = state.beaches.filter { it.id in visitedIds }
    val home = p.homeBeachId?.let { state.beach(it) }
    // Derived from real activity — the seeded badge list is all-unearned, so filtering IT always
    // produced an empty section with a "no badges yet" line under it.
    val earnedBadges = remember(myNests) { state.earnedBadgesFrom(myNests) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // "⋮" overflow tucks away the sharing actions so the screen itself stays about the volunteer.
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
                    DropdownMenuItem(
                        text = { Text("🐢 ${s.shareImpact}") },
                        onClick = {
                            showMenu = false
                            platformShare(s.shareText.replace("%d", p.hatchlingsReached.toString()))
                        },
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
                // Quiet single-tone avatar. A gradient here competed with the cards below it —
                // on a screen that is mostly a list, the header should recede, not shout.
                Box(
                    Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(c.sea.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    // A photo (once added) replaces the emoji; tap the name row to edit for now.
                    if (p.photoPath != null) {
                        LocalPhoto(p.photoPath, Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)))
                    } else {
                        Text(p.avatar, fontSize = 28.sp)
                    }
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(p.displayName, color = c.deep, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("✎", color = c.muted, fontSize = 15.sp)
                    }
                    // Until they type a name they're the same "Volunteer" as everyone else — which is
                    // what put other people's finds under their name on the community leaderboard.
                    // Said in the accent colour, not an alarm colour: it's a suggestion, not a fault.
                    if (!p.nameSet) {
                        Text(s.nameNudge, color = c.sea, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    } else {
                        val roleText = when (p.memberRole) {
                            MemberRole.ADMIN -> s.roleAdmin
                            MemberRole.BEACH_LEADER -> s.roleLeader
                            MemberRole.VOLUNTEER -> s.roleVolunteer
                        }
                        Text(roleText, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (editingName) {
                PlatformTextPrompt(
                    title = s.yourName,
                    initial = p.displayName,
                    placeholder = s.displayName,
                    confirmLabel = s.save,
                    cancelLabel = s.cancel,
                ) { entered ->
                    editingName = false
                    entered?.let { repo.setDisplayName(it) }
                }
            }

            if (showAuth) {
                EmailAuthDialog(repo, appStrings(p.language), onDismiss = { showAuth = false })
            }

            // ── My nests — the first thing a volunteer opens this screen to check ──────────
            // (It used to be missing entirely: the only trace of your work was an activity feed
            // that showed other people's entries and buried your own.)
            SectionLabel("${s.myNests}${if (myNests.isNotEmpty()) " · ${myNests.size}" else ""}")
            if (myNests.isEmpty()) {
                // The empty state IS the call to action, so a brand-new profile doesn't need a
                // separate full-width banner shouting the same thing above it.
                EmptyNestsCta(s.myNestsEmpty, s.impactZeroCta, onAddNest)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    myNests.take(6).forEach { n -> NestListItem(n, s, onOpenNest) }
                }
            }

            // Numbers, only once there are any: hatchlings first (the one that means something),
            // then the walking counters. A row of zeros is noise, and the old full-bleed gradient
            // tile made "0" the loudest thing on the screen.
            val stats = buildList {
                if (p.hatchlingsReached > 0) add("🐣 ${p.hatchlingsReached}" to s.hatchlingsReached)
                if (p.streakDays > 0) add("🔥 ${p.streakDays}" to s.dayStreak)
                if (p.kmWalked >= 1) add("${p.kmWalked.toInt()} km" to s.walked)
                if (p.patrols > 0) add("${p.patrols}" to s.patrols)
            }
            if (stats.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.take(3).forEach { (value, label) -> StatTile(value, label, Modifier.weight(1f)) }
                }
            }
            if (pickingBeach) {
                PlatformChoiceDialog(
                    title = s.homeBeach,
                    actions = listOf(
                        choiceAction(s.freeVolunteer, p.homeBeachId == null) { repo.setHomeBeach(null) },
                    ) + state.beaches.map { b ->
                        choiceAction(b.name, p.homeBeachId == b.id) { repo.setHomeBeach(b.id) }
                    } + DialogAction(s.cancel, DialogStyle.CANCEL),
                    onDismiss = { pickingBeach = false },
                )
            }

            // badges — only once something has actually been earned (no empty shelf, no grey locks).
            if (earnedBadges.isNotEmpty()) {
                SectionLabel(s.badges)
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
                // "today" reads as a word inside sentences elsewhere; as a chip it sits next to
                // Week/Month/All and has to match them.
                RangeChip(s.today.replaceFirstChar { it.uppercase() }, feedRange == FeedRange.TODAY) {
                    feedRange = FeedRange.TODAY
                }
                RangeChip(s.rangeWeek, feedRange == FeedRange.WEEK) { feedRange = FeedRange.WEEK }
                RangeChip(s.rangeMonth, feedRange == FeedRange.MONTH) { feedRange = FeedRange.MONTH }
                RangeChip(s.filterAll, feedRange == FeedRange.ALL) { feedRange = FeedRange.ALL }
            }
            if (feed.isNotEmpty()) {
                ActivityFeed(feed, onOpenNest)
            } else {
                Text(s.activityEmpty, color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            // ── Community — who you're doing this with ────────────────────────────────────
            SectionLabel(s.community)
            ProfileNavRow("🐢 ${state.community.name}", onClick = onOpenCommunity)
            if (visited.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    visited.forEach { b -> Box(Modifier.clickable { onOpenBeach(b.id) }) { Pill("🏖️ ${b.name}", c.sea) } }
                }
            }

            // ── Settings — everything you touch once, gathered at the bottom ───────────────
            // The account row lives here too, marked with a small amber dot while the work is only
            // on this phone. It used to be a full yellow banner near the top, which read as an
            // error and pushed the volunteer's own nests below the fold.
            SectionLabel(s.settingsTitle)
            if (state.accountEmail == null) {
                ProfileNavRow("🔒 ${s.saveAccount}", accent = c.warn) { showAuth = true }
            } else {
                // Signed in: state, not an action — so no chevron inviting a tap that does nothing.
                ProfileNavRow(state.accountEmail!!, accent = c.good, showArrow = false) { }
            }
            // my beach — one pinned home beach (patrol wherever's closest by default).
            ProfileNavRow(home?.let { "🏖️ ${it.name}" } ?: "🏖️ ${s.myBeach}") { pickingBeach = true }
            // trends / charts
            ProfileNavRow(s.trends, onClick = onOpenStats)
            // language — switch the whole interface (RU / TR / EN).
            ProfileNavRow(s.language) { pickingLang = true }
            if (pickingLang) {
                PlatformChoiceDialog(
                    title = s.language.trim(),
                    actions = LANGUAGES.map { (code, label) ->
                        choiceAction(label, p.language.equals(code, ignoreCase = true)) { repo.setLanguage(code) }
                    } + DialogAction(s.cancel, DialogStyle.CANCEL),
                    onDismiss = { pickingLang = false },
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
                PlatformChoiceDialog(
                    title = s.deleteAccountDone,
                    actions = listOf(DialogAction(s.close, DialogStyle.CANCEL)),
                    onDismiss = { deletedNotice = false },
                )
            }
        }
    }
}

private val LANGUAGES = listOf("en" to "🇬🇧 English", "ru" to "🇷🇺 Русский", "tr" to "🇹🇷 Türkçe")

@Composable
private fun ProfileNavRow(
    label: String,
    accent: Color? = null,
    showArrow: Boolean = true,
    onClick: () -> Unit,
) {
    val c = caretta
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(13.dp)).clickable { onClick() }.padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (accent != null) Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
            Text(
                label, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 1, modifier = Modifier.weight(1f),
            )
            if (showArrow) Text("›", color = c.muted, fontSize = 18.sp)
        }
    }
}

/** Empty "my nests" state: the invitation to log the first one, stated once and quietly. */
@Composable
private fun EmptyNestsCta(line: String, cta: String, onAddNest: () -> Unit) {
    val c = caretta
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.sea.copy(alpha = 0.07f))
            .border(1.dp, c.sea.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable { onAddNest() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text("🥚", fontSize = 22.sp)
        Column(Modifier.weight(1f)) {
            Text(cta, color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(line, color = c.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        }
        Text("›", color = c.sea, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
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
