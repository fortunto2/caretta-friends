package com.carettafriends.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.domain.NestStatus
import com.carettafriends.ui.theme.caretta

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = caretta.muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.6.sp,
    )
}

@Composable
fun Pill(text: String, fg: Color, bg: Color = fg.copy(alpha = 0.15f), modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun StatusPill(status: NestStatus, s: com.carettafriends.content.AppStrings) {
    val c = caretta
    val (label, color) = when (status) {
        NestStatus.INCUBATING -> s.statusIncubating to c.sea
        NestStatus.HATCHING -> s.statusHatchingSoon to c.warn
        NestStatus.HATCHED -> s.statusHatched to c.good
        NestStatus.EXCAVATED -> s.statusExcavated to c.good
        NestStatus.PREDATED -> s.statusPredated to c.risk
        NestStatus.WASHED_OVER -> s.statusWashedOver to c.risk
        NestStatus.POACHED -> s.statusPoached to c.risk
        NestStatus.LOST -> s.statusLost to c.muted
        NestStatus.FALSE_CRAWL -> s.statusFalseCrawl to c.muted
    }
    Pill(label, color)
}

@Composable
fun CarettaCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(14.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(caretta.surface)
            .border(1.dp, caretta.line, RoundedCornerShape(18.dp))
            .padding(padding),
    ) { content() }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = caretta
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (enabled) c.coral else c.muted.copy(alpha = 0.4f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = caretta
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(c.surface)
            .border(1.5.dp, c.line, RoundedCornerShape(13.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = c.sea, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Circular incubation progress ring: "Day X / ~total". */
@Composable
fun CountdownRing(day: Int, total: Int, dayLabel: String = "DAY", modifier: Modifier = Modifier) {
    val c = caretta
    val progress = (day.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
    Box(modifier = modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(92.dp)) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = c.line,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = c.sea,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$day", color = c.deep, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("$dayLabel / ~$total", color = c.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Predicted sex ratio as a RANGE (honest about uncertainty). */
@Composable
fun SexRangeBar(femaleLow: Int, femaleHigh: Int, modifier: Modifier = Modifier) {
    val c = caretta
    val femaleMid = ((femaleLow + femaleHigh) / 2).coerceIn(1, 99)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(CircleShape),
    ) {
        Box(
            modifier = Modifier
                .weight((100 - femaleMid).toFloat())
                .fillMaxWidth()
                .background(c.male),
            contentAlignment = Alignment.Center,
        ) { Text("♂", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
        Box(
            modifier = Modifier
                .weight(femaleMid.toFloat())
                .background(c.female),
            contentAlignment = Alignment.Center,
        ) {
            Text("♀ $femaleLow–$femaleHigh%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** Big +/- stepper for excavation counts (glove-friendly). */
@Composable
fun Stepper(value: Int, onDec: () -> Unit, onInc: () -> Unit, onSet: ((Int) -> Unit)? = null) {
    val c = caretta
    var editing by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepBtn("−", onDec)
        // Tap the number to type a value directly (no tapping "+" 100 times).
        Text(
            "$value", color = c.deep, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .then(if (onSet != null) Modifier.clickable { editing = true } else Modifier)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        StepBtn("+", onInc)
    }
    if (editing && onSet != null) {
        // The current value opens SELECTED, so the first digit typed REPLACES it.
        //
        // Prefilled and unselected, the field appended instead — and silently: a volunteer with 12
        // shells counted who tapped the number and typed "3" stored 123. Every count on this screen
        // is typed that way, so one careless tap could put a three-digit clutch into an official
        // record. The field also takes focus itself; needing a second tap to raise the keyboard is
        // what made people type without looking.
        val focus = remember { FocusRequester() }
        var field by remember {
            val start = value.toString()
            mutableStateOf(TextFieldValue(start, selection = TextRange(0, start.length)))
        }
        AlertDialog(
            onDismissRequest = { editing = false },
            confirmButton = {
                TextButton(onClick = { onSet(field.text.toIntOrNull()?.coerceAtLeast(0) ?: value); editing = false }) {
                    Text("✓", color = c.sea, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text("✕", color = c.muted, fontSize = 18.sp) }
            },
            text = {
                OutlinedTextField(
                    value = field,
                    onValueChange = { new -> field = new.copy(text = new.text.filter { it.isDigit() }.take(4)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.focusRequester(focus),
                )
                LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            },
        )
    }
}

@Composable
private fun StepBtn(sym: String, onClick: () -> Unit) {
    val c = caretta
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(c.sand)
            .border(1.5.dp, c.line, RoundedCornerShape(11.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(sym, color = c.sea, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
}

private fun Modifier.widthIn(): Modifier = this // keeps tabular width simple

data class NavItem(val key: String, val emoji: String, val label: String)

/** Bottom nav — always visible. The first half of [items], then a prominent centre "+" (add nest),
 *  then the rest. */
@Composable
fun BottomBar(current: String, items: List<NavItem>, onSelect: (String) -> Unit, onAdd: () -> Unit) {
    val c = caretta
    val half = (items.size + 1) / 2
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .border(width = 1.dp, color = c.line)
            .padding(top = 8.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.take(half).forEach { NavTab(it, current, onSelect) }
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(c.coral)
                .clickable { onAdd() },
            contentAlignment = Alignment.Center,
        ) { Text("＋", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold) }
        items.drop(half).forEach { NavTab(it, current, onSelect) }
    }
}

@Composable
private fun NavTab(item: NavItem, current: String, onSelect: (String) -> Unit) {
    val c = caretta
    val on = item.key == current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onSelect(item.key) }.padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(item.emoji, fontSize = 18.sp)
        Text(item.label, color = if (on) c.sea else c.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, trailing: @Composable (RowScope.() -> Unit)? = null) {
    val c = caretta
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                    .background(c.surface).border(1.dp, c.line, RoundedCornerShape(11.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("‹", color = c.deep, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
            Box(Modifier.size(10.dp))
        }
        Text(title, color = c.deep, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        if (trailing != null) trailing()
    }
}

@Composable
fun EmptyHint(emoji: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(emoji, fontSize = 40.sp)
        Text(text, color = caretta.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}
