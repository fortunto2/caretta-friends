package com.carettafriends.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.carettafriends.content.AppStrings
import com.carettafriends.content.appStrings
import com.carettafriends.data.CarettaRepository
import com.carettafriends.data.platformSharePdf
import com.carettafriends.data.today
import com.carettafriends.domain.Community
import com.carettafriends.domain.Excavation
import com.carettafriends.domain.Nest
import com.carettafriends.ui.components.GhostButton
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import com.carettafriends.ui.components.CarettaCard
import com.carettafriends.ui.components.PrimaryButton
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.Stepper
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta

@Composable
fun ExcavationScreen(nest: Nest, repo: CarettaRepository, lang: String, onBack: () -> Unit) {
    val c = caretta
    val s = appStrings(lang)
    val seed = nest.excavation

    // Start at 0 — volunteers count up what they actually find (no demo pre-fill).
    var shells by remember { mutableStateOf(seed?.shells ?: 0) }
    var unhatched by remember { mutableStateOf(seed?.unhatched ?: 0) }
    var pipped by remember { mutableStateOf(seed?.pipped ?: 0) }
    var inNest by remember { mutableStateOf(seed?.inNest ?: 0) }
    var helpedOut by remember { mutableStateOf(seed?.helpedOut ?: 0) }
    var celebrating by remember { mutableStateOf(false) }
    // Official record fields (tutanak): who did it (defaults to the person filling this in) + the date.
    var team by remember { mutableStateOf(seed?.team?.ifBlank { null } ?: repo.state.value.profile.displayName) }
    var excDate by remember { mutableStateOf(seed?.excavatedOn ?: today()) }

    val exc = Excavation(
        shells = shells,
        unhatched = unhatched,
        pipped = pipped,
        inNest = inNest,
        helpedOut = helpedOut,
        excavatedOn = excDate,
        team = team,
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(s.excavationTitle, onBack = onBack)
        Column(
            Modifier.padding(horizontal = 15.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${nest.code} · ${s.excFinalCount}", color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            // Regulatory reminder — excavation is coordinated with the community's authority (country-
            // specific: Türkiye = DKMP), done with the authorized team, recorded for the official protocol.
            val community = repo.state.value.community
            val authority = community.authorityName.ifBlank { s.authorityGeneric }
            val uriHandler = LocalUriHandler.current
            CarettaCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⚖️ ${s.excAuthNote.replace("%s", authority)}", color = c.deep, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    if (community.authorityUrl.isNotBlank()) {
                        Text(
                            "${community.authorityName} ›",
                            color = c.sea, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.clickable { runCatching { uriHandler.openUri(community.authorityUrl) } },
                        )
                    }
                }
            }

            // glove-friendly stepper rows
            SectionLabel(s.excCountWhatYouFind)
            CountRow("🥚", s.excShells, shells, onDec = { if (shells > 0) shells-- }, onInc = { shells++ }, onSet = { shells = it })
            CountRow("🟤", s.excUnhatched, unhatched, onDec = { if (unhatched > 0) unhatched-- }, onInc = { unhatched++ }, onSet = { unhatched = it })
            CountRow("🐣", s.excPipped, pipped, onDec = { if (pipped > 0) pipped-- }, onInc = { pipped++ }, onSet = { pipped = it })
            CountRow("🕳️", s.excInNest, inNest, onDec = { if (inNest > 0) inNest-- }, onInc = { inNest++ }, onSet = { inNest = it })
            CountRow("🐢", s.excHelpedOut, helpedOut, onDec = { if (helpedOut > 0) helpedOut-- }, onInc = { helpedOut++ }, onSet = { helpedOut = it })

            // auto-computed success tiles
            SectionLabel(s.excSuccessLabel)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SuccessTile("${exc.hatchSuccessPct ?: 0}%", s.excHatchingSuccess, c.good, Modifier.weight(1f))
                SuccessTile("${exc.emergenceSuccessPct ?: 0}%", s.excEmergenceSuccess, c.sea, Modifier.weight(1f))
            }

            // celebratory north-star tile
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(c.sea, c.deep))).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${exc.hatchlingsToSea}", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Text(s.hatchlingsReached, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    if (exc.hatchlingsToSea == 0) {
                        Text(s.excZeroHint, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Official record (tutanak): who + when, then export a PDF to hand to the coordinator.
            SectionLabel(s.excReportTitle)
            OutlinedTextField(
                value = team,
                onValueChange = { team = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(s.excTeamLabel) },
                singleLine = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.excDateLabel, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                DateStepBtn("‹") { excDate = excDate.minus(DatePeriod(days = 1)) }
                Text(
                    "${excDate.dayOfMonth} ${s.months[excDate.month.ordinal]}",
                    color = c.deep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                )
                DateStepBtn("›") { if (excDate < today()) excDate = excDate.plus(DatePeriod(days = 1)) }
            }
            GhostButton("📄 ${s.excReportBtn}", modifier = Modifier.fillMaxWidth()) {
                val beachName = repo.state.value.beach(nest.beachId)?.name ?: ""
                val (title, lines) = excavationReport(nest, exc, repo.state.value.community, s, beachName)
                platformSharePdf("caretta_${nest.code}", title, lines)
            }

            PrimaryButton(s.excFinish) {
                repo.setExcavation(nest.id, exc)
                celebrating = true
            }
        }
    }

    // Celebratory send-off — this is the emotional peak (nest excavated, babies to the sea).
    if (celebrating) {
        Dialog(onDismissRequest = { onBack() }) {
            Box(
                Modifier.clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(c.sea, c.deep))).padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (exc.hatchlingsToSea > 0) {
                        // A real payoff — hatchlings made it to the sea.
                        Text("🎉", fontSize = 46.sp)
                        Text("${exc.hatchlingsToSea}", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold)
                        Text(s.hatchlingsReached, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    } else {
                        // A failed / predated nest — no confetti; thank the volunteer for the record.
                        Text("🐢", fontSize = 46.sp)
                        Text(s.excThanksRecord, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(6.dp))
                    PrimaryButton(s.done) { onBack() }
                }
            }
        }
    }
}

/** ~5-decimal (~1 m) coordinate for the report. */
private fun trim5(v: Double): String = ((v * 100000).toLong() / 100000.0).toString()

/** Build the excavation record (tutanak): a title + "Label: value" lines for the PDF export. */
private fun excavationReport(
    nest: Nest,
    exc: Excavation,
    community: Community,
    s: AppStrings,
    beachName: String,
): Pair<String, List<String>> {
    val d = exc.excavatedOn
    val dateStr = d?.let { "${it.dayOfMonth} ${s.months[it.month.ordinal]} ${it.year}" } ?: ""
    val authority = community.authorityName.ifBlank { s.authorityGeneric }
    val lines = listOf(
        nest.code + if (beachName.isNotBlank()) " · $beachName" else "",
        "${trim5(nest.point.lat)}, ${trim5(nest.point.lng)}",
        "${s.excDateLabel}: $dateStr",
        "${s.excTeamLabel}: ${exc.team}",
        "",
        "${s.excShells}: ${exc.shells}",
        "${s.excUnhatched}: ${exc.unhatched}",
        "${s.excPipped}: ${exc.pipped}",
        "${s.excInNest}: ${exc.inNest}",
        "${s.excHelpedOut}: ${exc.helpedOut}",
        "",
        "${s.excHatchingSuccess}: ${exc.hatchSuccessPct ?: 0}%",
        "${s.excEmergenceSuccess}: ${exc.emergenceSuccessPct ?: 0}%",
        "${s.hatchlingsReached}: ${exc.hatchlingsToSea}",
        "",
        "${s.excReportCoord}: $authority",
        "Caretta Friends",
    )
    return s.excReportTitle to lines
}

@Composable
private fun DateStepBtn(label: String, onClick: () -> Unit) {
    val c = caretta
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(c.sand).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = c.sea, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun CountRow(emoji: String, label: String, value: Int, onDec: () -> Unit, onInc: () -> Unit, onSet: (Int) -> Unit) {
    val c = caretta
    CarettaCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(emoji, fontSize = 22.sp)
            Text(
                label,
                color = c.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Stepper(value = value, onDec = onDec, onInc = onInc, onSet = onSet)
        }
    }
}

@Composable
private fun SuccessTile(value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    val c = caretta
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(tint.copy(alpha = 0.14f))
            .padding(vertical = 14.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = tint, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
