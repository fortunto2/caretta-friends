package com.carettafriends.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Extended palette from design-spec.md (sand + sea + coral sunset). */
data class CarettaColors(
    val sand: Color,
    val surface: Color,
    val sea: Color,
    val deep: Color,
    val coral: Color,
    val sunlit: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val good: Color,
    val warn: Color,
    val risk: Color,
    val male: Color,
    val female: Color,
    val isDark: Boolean,
)

val LightCaretta = CarettaColors(
    sand = Color(0xFFFBF6EC),
    surface = Color(0xFFFFFFFF),
    sea = Color(0xFF0F7A82),
    deep = Color(0xFF0B3B3F),
    coral = Color(0xFFFF7A59),
    sunlit = Color(0xFFF6C453),
    ink = Color(0xFF22302E),
    muted = Color(0xFF6E827E),
    line = Color(0xFFE7E0D2),
    good = Color(0xFF4FA96A),
    warn = Color(0xFFE8A93C),
    risk = Color(0xFFE4574B),
    male = Color(0xFF2E86AB),
    female = Color(0xFFE86A8C),
    isDark = false,
)

val DarkCaretta = CarettaColors(
    sand = Color(0xFF0B1F22),
    surface = Color(0xFF122E32),
    sea = Color(0xFF3BB7BE),
    deep = Color(0xFFEAF3EF),
    coral = Color(0xFFFF8A6B),
    sunlit = Color(0xFFF6C453),
    ink = Color(0xFFE7F0EC),
    muted = Color(0xFF8AA39E),
    line = Color(0xFF1E4147),
    good = Color(0xFF5FCB80),
    warn = Color(0xFFF6C453),
    risk = Color(0xFFFF7367),
    male = Color(0xFF5AB3D6),
    female = Color(0xFFFF9DB6),
    isDark = true,
)

val LocalCaretta = staticCompositionLocalOf { LightCaretta }

/** Convenience accessor for the extended palette. */
val caretta: CarettaColors
    @Composable get() = LocalCaretta.current

private val rounded = FontFamily.Default // SF Pro Rounded / Baloo 2 via resources later

val CarettaTypography = Typography(
    displaySmall = TextStyle(fontFamily = rounded, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = rounded, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = rounded, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = rounded, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = rounded, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = rounded, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = rounded, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp),
)

val CarettaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun CarettaTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (dark) DarkCaretta else LightCaretta
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.sea,
            secondary = c.coral,
            background = c.sand,
            surface = c.surface,
            onPrimary = Color.White,
            onBackground = c.ink,
            onSurface = c.ink,
        )
    } else {
        lightColorScheme(
            primary = c.sea,
            secondary = c.coral,
            background = c.sand,
            surface = c.surface,
            onPrimary = Color.White,
            onBackground = c.ink,
            onSurface = c.ink,
        )
    }
    CompositionLocalProvider(LocalCaretta provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = CarettaTypography,
            shapes = CarettaShapes,
            content = content,
        )
    }
}
