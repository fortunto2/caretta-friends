package com.carettafriends.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carettafriends.ui.theme.caretta

/**
 * In-app camera mock (design-spec 4.9). Visual-only — no real CameraX.
 * Always dark regardless of the app theme (night-patrol friendly).
 */
@Composable
fun CameraScreen(lang: String, onBack: () -> Unit, onCaptured: () -> Unit) {
    val c = caretta
    val s = com.carettafriends.content.appStrings(lang)
    // fixed dark chrome so this screen stays dark in any app theme
    val chrome = Color(0xFF06100F)
    val glass = Color.White.copy(alpha = 0.14f)
    val ready = Color(0xFF7CE0A6)

    var flash by remember { mutableStateOf("Auto") }   // Auto → On → Off
    var frontFacing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(chrome)) {
        // ── viewfinder ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF13383E), Color(0xFF0A2024), Color(0xFF040D0C)),
                    ),
                ),
        ) {
            // rule-of-thirds grid overlay
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val col = Color.White.copy(alpha = 0.12f)
                val sw = 1.dp.toPx()
                drawLine(col, Offset(w / 3f, 0f), Offset(w / 3f, h), sw)
                drawLine(col, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), sw)
                drawLine(col, Offset(0f, h / 3f), Offset(w, h / 3f), sw)
                drawLine(col, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), sw)
            }

            // subject — a big centered egg with a soft glow + hint
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(c.sunlit.copy(alpha = 0.22f), Color.Transparent),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) { Text("🥚", fontSize = 108.sp) }
                Spacer(Modifier.height(10.dp))
                Text(
                    s.cameraFrameHint,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            // top controls: ✕  ·  ⚡︎ flash
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(glass)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) { Text("✕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }

                Box(
                    modifier = Modifier.clip(CircleShape)
                        .background(if (flash == "Off") glass else c.coral.copy(alpha = 0.9f))
                        .clickable {
                            flash = when (flash) {
                                "Auto" -> "On"
                                "On" -> "Off"
                                else -> "Auto"
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("⚡︎ $flash", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // GPS + EXIF readiness badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 66.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    "📍 GPS + EXIF ready · ±4 m",
                    color = ready,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        // ── shutter bar ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color(0xFF030807))
                .padding(horizontal = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // gallery thumbnail
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(c.sea, c.deep)))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("🖼️", fontSize = 22.sp) }

            // shutter — big round white button
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onCaptured() },
                )
            }

            // flip front/back camera
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(glass)
                    .clickable { frontFacing = !frontFacing },
                contentAlignment = Alignment.Center,
            ) { Text("⟲", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
}
