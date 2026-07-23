package com.carettafriends.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.carettafriends.content.appStrings
import com.carettafriends.data.CarettaRepository
import com.carettafriends.domain.Excavation
import com.carettafriends.domain.Nest
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

    val exc = Excavation(
        shells = shells,
        unhatched = unhatched,
        pipped = pipped,
        inNest = inNest,
        helpedOut = helpedOut,
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(s.excavationTitle, onBack = onBack)
        Column(
            Modifier.padding(horizontal = 15.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${nest.code} · ${s.excFinalCount}", color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            // glove-friendly stepper rows
            SectionLabel(s.excCountWhatYouFind)
            CountRow("🥚", s.excShells, shells, onDec = { if (shells > 0) shells-- }, onInc = { shells++ })
            CountRow("🟤", s.excUnhatched, unhatched, onDec = { if (unhatched > 0) unhatched-- }, onInc = { unhatched++ })
            CountRow("🐣", s.excPipped, pipped, onDec = { if (pipped > 0) pipped-- }, onInc = { pipped++ })
            CountRow("🕳️", s.excInNest, inNest, onDec = { if (inNest > 0) inNest-- }, onInc = { inNest++ })
            CountRow("🐢", s.excHelpedOut, helpedOut, onDec = { if (helpedOut > 0) helpedOut-- }, onInc = { helpedOut++ })

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

@Composable
private fun CountRow(emoji: String, label: String, value: Int, onDec: () -> Unit, onInc: () -> Unit) {
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
            Stepper(value = value, onDec = onDec, onInc = onInc)
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
