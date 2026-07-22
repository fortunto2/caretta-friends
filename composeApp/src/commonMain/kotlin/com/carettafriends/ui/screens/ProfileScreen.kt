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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.data.CarettaRepository
import com.carettafriends.domain.AppState
import com.carettafriends.domain.Badge
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
) {
    val c = caretta
    val p = state.profile
    var editingName by remember { mutableStateOf(false) }
    var pickingBeach by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(false) }

    // "Beaches you've been to" — where this volunteer has patrolled or logged a nest.
    val visitedIds = (state.patrols.map { it.beachId } + state.nests.filter { it.foundBy == p.displayName }.map { it.beachId }).toSet()
    val visited = state.beaches.filter { it.id in visitedIds }
    val home = p.homeBeachId?.let { state.beach(it) }
    // Latest photos across this volunteer's nests → an Instagram-style grid.
    val myPhotos = state.nests
        .sortedByDescending { it.updatedAtMillis }
        .flatMap { n -> n.photos.mapNotNull { ph -> ph.localUri?.let { uri -> uri to n.id } } }
        .take(9)
    val earnedBadges = state.badges.filter { it.earned }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Profile")
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
                    Text(p.role, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (editingName) {
                var draft by remember { mutableStateOf(p.displayName) }
                AlertDialog(
                    onDismissRequest = { editingName = false },
                    title = { Text("Your name") },
                    text = {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it.take(40) },
                            singleLine = true,
                            label = { Text("Display name") },
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { repo.setDisplayName(draft); editingName = false }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingName = false }) { Text("Cancel") }
                    },
                )
            }

            // account — anonymous volunteers get a nudge to save their work under an email.
            if (state.accountEmail == null) {
                AccountCta { showAuth = true }
            } else {
                SignedInRow(state.accountEmail!!)
            }
            if (showAuth) {
                EmailAuthDialog(repo, onDismiss = { showAuth = false })
            }

            // impact tile
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(c.sea, c.deep))).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${p.hatchlingsReached}", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Text("hatchlings reached the sea 🌊", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                }
            }
            // stats
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("🔥 ${p.streakDays}", "day streak", Modifier.weight(1f))
                StatTile("${p.kmWalked.toInt()} km", "walked", Modifier.weight(1f))
                StatTile("${p.patrols}", "patrols", Modifier.weight(1f))
            }

            // community — shown directly (a volunteer usually belongs to 1–3), tap to open.
            SectionLabel("Community")
            CommunityCard(
                name = state.community.name,
                tagline = state.community.tagline,
                members = state.members.size,
                onOpen = onOpenCommunity,
            )

            // my beach — one pinned home beach + everywhere you've patrolled.
            SectionLabel("My beach")
            HomeBeachCard(
                title = home?.let { "${it.leaderAvatar} ${it.name}" } ?: "🌊 Free volunteer",
                sub = if (home != null) "Your home beach" else "Patrol wherever's closest",
                onChange = { pickingBeach = true },
            )
            if (visited.isNotEmpty()) {
                Text("Patrolled", color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    visited.forEach { Pill("🏖️ ${it.name}", c.sea) }
                }
            }
            if (pickingBeach) {
                AlertDialog(
                    onDismissRequest = { pickingBeach = false },
                    title = { Text("Home beach") },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BeachPickRow("🌊 Free volunteer (no home beach)", p.homeBeachId == null) {
                                repo.setHomeBeach(null); pickingBeach = false
                            }
                            state.beaches.forEach { b ->
                                BeachPickRow("${b.leaderAvatar} ${b.name}", p.homeBeachId == b.id) {
                                    repo.setHomeBeach(b.id); pickingBeach = false
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { pickingBeach = false }) { Text("Done") } },
                )
            }

            // badges — only the ones actually earned (no confusing greyed-out locks).
            SectionLabel("Badges")
            if (earnedBadges.isEmpty()) {
                Text(
                    "No badges yet — patrol & log nests to earn your first 🐢",
                    color = c.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    earnedBadges.forEach { b -> BadgeCell(b) }
                }
            }

            // my photos — recent nest photos as a square grid (tap opens the nest).
            if (myPhotos.isNotEmpty()) {
                SectionLabel("My photos")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    myPhotos.chunked(3).forEach { rowPhotos ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowPhotos.forEach { (uri, nestId) ->
                                LocalPhoto(
                                    uri,
                                    Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                                        .clickable { onOpenNest(nestId) },
                                )
                            }
                            repeat(3 - rowPhotos.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            // trends / charts
            ProfileNavRow("📊  Trends & charts", onOpenStats)
        }
    }
}

@Composable
private fun CommunityCard(name: String, tagline: String, members: Int, onOpen: () -> Unit) {
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
                    tagline.ifBlank { "Your community" } + if (members > 0) " · $members volunteers" else "",
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
private fun HomeBeachCard(title: String, sub: String, onChange: () -> Unit) {
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
            Text("Change ›", color = c.sea, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
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
private fun AccountCta(onClick: () -> Unit) {
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
            Text("Save your account", color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text("Add an email so your work isn't lost if you change phone.", color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Text("›", color = c.muted, fontSize = 18.sp)
    }
}

@Composable
private fun SignedInRow(email: String) {
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
            Text("Signed in", color = c.deep, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(email, color = c.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EmailAuthDialog(repo: CarettaRepository, onDismiss: () -> Unit) {
    val c = caretta
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var signIn by remember { mutableStateOf(false) } // false = create/save, true = sign in
    val valid = email.contains("@") && email.contains(".") && password.length >= 6

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (signIn) "Sign in" else "Save your account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (signIn) "Welcome back — sign in to load your nests & impact." else "Add an email + password so your work syncs and isn't lost if you change phone.",
                    color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim(); error = null },
                    singleLine = true,
                    label = { Text("Email") },
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    singleLine = true,
                    label = { Text("Password (6+ chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                error?.let { Text(it, color = c.coral, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Text(
                    if (signIn) "New here? Create an account" else "Already have an account? Sign in",
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
            ) { Text(if (busy) "…" else if (signIn) "Sign in" else "Save") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun BadgeCell(b: Badge) {
    val c = caretta
    Column(Modifier.width(68.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(15.dp))
                .background(c.sunlit.copy(alpha = 0.22f)).border(1.dp, c.line, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(b.emoji, fontSize = 22.sp) }
        Text(b.name, color = c.muted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
