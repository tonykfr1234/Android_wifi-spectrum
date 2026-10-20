package com.wifispectrum.analyzer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wifispectrum.analyzer.core.AccessPoint
import com.wifispectrum.analyzer.core.Band
import com.wifispectrum.analyzer.core.Security
import com.wifispectrum.analyzer.core.Signal
import com.wifispectrum.analyzer.core.WifiGeneration

@Composable
fun SignalBars(rssi: Int, modifier: Modifier = Modifier) {
    val bars = Signal.bars(rssi)
    val color = Palette.signal(rssi)
    Canvas(modifier.size(18.dp, 14.dp).semantics { contentDescription = "訊號 $bars 格" }) {
        val w = size.width / 4f
        for (i in 0 until 4) {
            val h = size.height * (i + 1) / 4f
            drawRoundRect(
                color = if (i < bars) color else Palette.Line,
                topLeft = Offset(i * w + 1f, size.height - h),
                size = Size(w - 3f, h),
                cornerRadius = CornerRadius(1.5f),
            )
        }
    }
}

@Composable
fun Tag(text: String, color: Color = Palette.Text2, filled: Boolean = false) {
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = text,
        color = if (filled) Palette.Bg else color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = Mono,
        maxLines = 1,
        modifier = Modifier
            .background(if (filled) color else color.copy(alpha = 0.10f), shape)
            .border(1.dp, color.copy(alpha = if (filled) 1f else 0.45f), shape)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Palette.Text2,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .background(Palette.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, Palette.Line, RoundedCornerShape(10.dp)),
    ) { content() }
}

@Composable
fun KeyValue(label: String, value: String, mono: Boolean = true, valueColor: Color = Palette.Text) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, color = Palette.Text2, fontSize = 13.sp, modifier = Modifier.width(104.dp))
        Text(
            value,
            color = valueColor,
            fontSize = 13.sp,
            fontFamily = if (mono) Mono else null,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun Stars(count: Int, modifier: Modifier = Modifier) {
    Text(
        "★".repeat(count) + "☆".repeat(5 - count),
        color = when {
            count >= 4 -> Palette.Good
            count >= 3 -> Palette.Warn
            else -> Palette.Bad
        },
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        modifier = modifier.semantics { contentDescription = "$count 顆星" },
    )
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(body, color = Palette.Text2, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun BandSelector(selected: Band, counts: Map<Band, Int>, onSelect: (Band) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
    val narrow = maxWidth / LocalDensity.current.fontScale < 340.dp
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(Palette.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, Palette.Line, RoundedCornerShape(10.dp))
            .padding(3.dp),
    ) {
        for (b in Band.entries) {
            val on = b == selected
            Box(
                Modifier
                    .weight(1f)
                    .background(if (on) Palette.band(b).copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { onSelect(b) }
                    .padding(vertical = 8.dp)
                    .semantics { contentDescription = "頻段 ${b.label}" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${if (narrow) b.shortLabel else b.label}  ${counts[b] ?: 0}",
                    color = if (on) Palette.band(b) else Palette.Text2,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp,
                    fontFamily = Mono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    }
}

fun AccessPoint.securityShort(): String {
    val s = security
    return when {
        Security.WPA3_ENTERPRISE_192 in s || Security.WPA3_ENTERPRISE in s -> "WPA3-E"
        Security.WPA2_ENTERPRISE in s -> "WPA2-E"
        Security.WPA3_PERSONAL in s && Security.WPA2_PERSONAL in s -> "WPA2/3"
        Security.WPA3_PERSONAL in s -> "WPA3"
        Security.WPA2_PERSONAL in s -> "WPA2"
        Security.WPA in s -> "WPA"
        Security.WEP in s -> "WEP"
        Security.OWE in s -> "OWE"
        Security.WAPI in s -> "WAPI"
        else -> "開放"
    }
}

fun AccessPoint.isWeakSecurity(): Boolean =
    Security.OPEN in security || Security.WEP in security || (Security.WPA in security && Security.WPA2_PERSONAL !in security)

fun generationColor(g: WifiGeneration): Color = when (g) {
    WifiGeneration.WIFI_7 -> Palette.Wifi7
    WifiGeneration.WIFI_6E, WifiGeneration.WIFI_6 -> Palette.Accent
    WifiGeneration.WIFI_5 -> Palette.Good
    else -> Palette.Text2
}

fun formatDistance(meters: Double): String = when {
    meters.isNaN() -> "—"
    meters < 10 -> "≈ %.1f m".format(meters)
    meters < 1000 -> "≈ %.0f m".format(meters)
    else -> "> 1 km"
}

fun AccessPoint.traceKey(): String = mldAddress ?: bssid
