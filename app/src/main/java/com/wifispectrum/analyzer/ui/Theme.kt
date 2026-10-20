package com.wifispectrum.analyzer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wifispectrum.analyzer.core.Band
import kotlin.math.abs

/** Instrument-panel palette: near-black glass, hairline grid, saturated traces. */
object Palette {
    val Bg = Color(0xFF0B0F14)
    val Surface = Color(0xFF121820)
    val Surface2 = Color(0xFF1A222C)
    val Line = Color(0xFF263140)
    val Grid = Color(0xFF1C2530)
    val Text = Color(0xFFE6EDF3)
    val Text2 = Color(0xFF8B98A8)
    val Accent = Color(0xFF2BC4FF)
    val Good = Color(0xFF3CE38A)
    val Warn = Color(0xFFFFB020)
    val Bad = Color(0xFFFF5A5A)
    val Wifi7 = Color(0xFFC08BFF)

    val Band24 = Color(0xFF3CE38A)
    val Band5 = Color(0xFF2BC4FF)
    val Band6 = Color(0xFFC08BFF)

    private val traces = listOf(
        0xFF2BC4FF, 0xFF3CE38A, 0xFFFF4FA3, 0xFFFFB020, 0xFFC08BFF, 0xFF00E5C0,
        0xFFFF7A45, 0xFF7C9CFF, 0xFFE8E04A, 0xFFFF5A5A, 0xFF62D2FF, 0xFFB4F05A,
    ).map { Color(it) }

    fun trace(key: String): Color = traces[abs(key.hashCode()) % traces.size]

    fun band(b: Band?): Color = when (b) {
        Band.GHZ_2_4 -> Band24
        Band.GHZ_5 -> Band5
        Band.GHZ_6 -> Band6
        null -> Text2
    }

    fun signal(rssi: Int): Color = when {
        rssi >= -60 -> Good
        rssi >= -70 -> Warn
        else -> Bad
    }
}

val Mono = FontFamily.Monospace

private val scheme = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Palette.Bg,
    secondary = Palette.Good,
    background = Palette.Bg,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.Surface2,
    onSurfaceVariant = Palette.Text2,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.Surface2,
    surfaceContainerHighest = Palette.Surface2,
    outline = Palette.Line,
    outlineVariant = Palette.Grid,
    error = Palette.Bad,
)

private val type = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

@Composable
fun SpectrumTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
