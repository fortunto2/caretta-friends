package com.carettafriends.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun ExcavationScreen(nest: Nest, repo: CarettaRepository, onBack: () -> Unit) {
    val c = caretta
    val seed = nest.excavation

    var shells by remember { mutableStateOf(seed?.shells ?: 47) }
    var unhatched by remember { mutableStateOf(seed?.unhatched ?: 6) }
    var pipped by remember { mutableStateOf(seed?.pipped ?: 2) }
    var inNest by remember { mutableStateOf(seed?.inNest ?: 1) }
    var helpedOut by remember { mutableStateOf(seed?.helpedOut ?: 3) }

    val exc = Excavation(
        shells = shells,
        unhatched = unhatched,
        pipped = pipped,
        inNest = inNest,
        helpedOut = helpedOut,
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Nest excavation", onBack = onBack)
        Column(
            Modifier.padding(horizontal = 15.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${nest.code} · FWC count", color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            // glove-friendly stepper rows
            SectionLabel("Count what you find")
            CountRow("🥚", "Hatched / empty shells", shells, onDec = { if (shells > 0) shells-- }, onInc = { shells++ })
            CountRow("🟤", "Unhatched / whole eggs", unhatched, onDec = { if (unhatched > 0) unhatched-- }, onInc = { unhatched++ })
            CountRow("🐣", "Pipped / in egg", pipped, onDec = { if (pipped > 0) pipped-- }, onInc = { pipped++ })
            CountRow("🕳️", "In nest / stuck", inNest, onDec = { if (inNest > 0) inNest-- }, onInc = { inNest++ })
            CountRow("🐢", "Helped out 🐢 / rescued", helpedOut, onDec = { if (helpedOut > 0) helpedOut-- }, onInc = { helpedOut++ })

            // auto-computed success tiles
            SectionLabel("Success")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SuccessTile("${exc.hatchSuccessPct ?: 0}%", "Hatching success", c.good, Modifier.weight(1f))
                SuccessTile("${exc.emergenceSuccessPct ?: 0}%", "Emergence success", c.sea, Modifier.weight(1f))
            }

            // celebratory north-star tile
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(c.sea, c.deep))).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${exc.hatchlingsToSea}", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Text("hatchlings reached the sea 🌊", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                }
            }

            PrimaryButton("Finish & celebrate 🎉") {
                repo.setExcavation(nest.id, exc)
                onBack()
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
