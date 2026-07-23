package com.carettafriends.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.content.appStrings
import com.carettafriends.domain.AppState
import com.carettafriends.domain.NestStatus
import com.carettafriends.ui.components.SectionLabel
import com.carettafriends.ui.components.TopBar
import com.carettafriends.ui.theme.caretta
import kotlinx.datetime.LocalDate

/** Season trends: nests found per day, the hatch-window forecast, and excavation outcomes. */
@Composable
fun StatsScreen(state: AppState, onBack: () -> Unit) {
    val c = caretta
    val nests = state.nests
    val today = com.carettafriends.data.today()

    val total = nests.size
    val active = nests.count { it.status == NestStatus.INCUBATING || it.status == NestStatus.HATCHING }
    val hatched = nests.count { it.status == NestStatus.HATCHED || it.status == NestStatus.EXCAVATED }
    val excavatedNests = nests.filter { it.status == NestStatus.EXCAVATED && it.excavation != null }
    val toSea = nests.sumOf { it.excavation?.hatchlingsToSea ?: 0 }

    // Nests found per day (last 14 days).
    val perDay: List<Pair<String, Int>> = (13 downTo 0).map { back ->
        val d = LocalDate.fromEpochDays(today.toEpochDays() - back)
        "${d.dayOfMonth}" to nests.count { it.foundDate == d }
    }

    // Hatch-window forecast: expected emergence = foundDate + incubation, grouped into the next 8 weeks.
    val activeNests = nests.filter { it.status == NestStatus.INCUBATING || it.status == NestStatus.HATCHING }
    val hatchEpoch = activeNests.map { it.foundDate.toEpochDays() + it.incubationDaysEst }
    val forecast: List<Pair<String, Int>> = (0..7).map { w ->
        val start = today.toEpochDays() + w * 7
        "W${w + 1}" to hatchEpoch.count { it in start..(start + 6) }
    }

    // Excavation outcomes.
    val shells = excavatedNests.sumOf { it.excavation!!.shells }
    val unhatched = excavatedNests.sumOf { it.excavation!!.unhatched + it.excavation!!.pipped }

    val s = appStrings(state.profile.language)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(s.trendsTitle, onBack = onBack)
        Column(
            Modifier.padding(horizontal = 15.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // summary tiles
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("$total", s.nestsWord, Modifier.weight(1f))
                StatTile("$active", s.activeWord, Modifier.weight(1f))
                StatTile("$hatched", s.hatchedWord, Modifier.weight(1f))
                StatTile("$toSea", s.toSea, Modifier.weight(1f))
            }

            SectionLabel(s.nestsFound14)
            BarChart(perDay, c.sea)
            if (total == 0) EmptyNote(s.noNestsTrend)

            SectionLabel(s.hatchForecast8)
            BarChart(forecast, c.warn)
            Text(
                s.hatchForecastSub,
                color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium,
            )

            SectionLabel(s.excavationOutcomes)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("${excavatedNests.size}", s.excavatedWord, Modifier.weight(1f))
                StatTile("$shells", s.hatchedEggs, Modifier.weight(1f))
                StatTile("$unhatched", s.unhatchedWord, Modifier.weight(1f))
            }
            if (excavatedNests.isEmpty()) EmptyNote(s.noExcavations)
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val c = caretta
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(c.surface).padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = c.deep, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = c.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, color = caretta.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

/** Simple bar chart drawn on a Canvas — value labels on the tallest bars, x-labels below. */
@Composable
private fun BarChart(data: List<Pair<String, Int>>, color: Color) {
    val c = caretta
    val maxVal = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(12.dp)) {
        Column {
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val n = data.size
                val gap = size.width * 0.02f
                val barW = (size.width - gap * (n - 1)) / n
                data.forEachIndexed { i, (_, v) ->
                    val h = if (v == 0) 2f else (size.height - 6f) * (v.toFloat() / maxVal)
                    val x = i * (barW + gap)
                    drawRoundRect(
                        color = if (v == 0) c.line else color,
                        topLeft = Offset(x, size.height - h),
                        size = Size(barW, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                // show a sparse set of x-labels (first, middle, last) to avoid clutter
                listOf(0, data.size / 2, data.size - 1).distinct().forEach { idx ->
                    data.getOrNull(idx)?.let { Text(it.first, color = c.muted, fontSize = 9.sp) }
                }
            }
        }
    }
}
