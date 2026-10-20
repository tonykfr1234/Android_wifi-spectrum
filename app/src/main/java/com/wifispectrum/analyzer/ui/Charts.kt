package com.wifispectrum.analyzer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wifispectrum.analyzer.core.AccessPoint
import com.wifispectrum.analyzer.core.Band
import com.wifispectrum.analyzer.core.Channels
import com.wifispectrum.analyzer.core.SignalHistory

const val DBM_TOP = -20
const val DBM_BOTTOM = -100
private val GUTTER = 34.dp
private val AXIS = 22.dp

private fun DrawScope.yOf(dbm: Int, top: Float, bottom: Float): Float {
    val v = dbm.coerceIn(DBM_BOTTOM, DBM_TOP)
    return top + (DBM_TOP - v).toFloat() / (DBM_TOP - DBM_BOTTOM) * (bottom - top)
}

private fun DrawScope.dbmGrid(tm: TextMeasurer, left: Float, top: Float, right: Float, bottom: Float) {
    val label = TextStyle(color = Palette.Text2, fontSize = 10.sp, fontFamily = Mono)
    for (d in DBM_TOP downTo DBM_BOTTOM step 10) {
        val y = yOf(d, top, bottom)
        drawLine(Palette.Grid, Offset(left, y), Offset(right, y), 1f)
        val r = tm.measure("$d", label)
        drawText(r, topLeft = Offset(left - r.size.width - 6f, y - r.size.height / 2f))
    }
}

/**
 * Channel spectrum: one parabola per AP whose apex sits at its RSSI and whose base spans its channel width.
 * [zoomed] switches 5/6 GHz from a fit-to-width overview to a scrollable close-up.
 */
@Composable
fun SpectrumChart(
    band: Band,
    aps: List<AccessPoint>,
    highlight: Set<String>,
    zoomed: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
) {
    val tm = rememberTextMeasurer()
    val range = Channels.displayRangeMhz(band)
    val spanMhz = range.last - range.first
    val inBand = aps.filter { it.band == band }.sortedBy { it.rssi }
    val strongest = inBand.lastOrNull()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val fitWidth = maxWidth - GUTTER
        val zoomDpPerMhz = when (band) {
            Band.GHZ_2_4 -> null
            Band.GHZ_5 -> 2.2f
            Band.GHZ_6 -> 1.6f
        }
        val plotWidth = if (zoomed && zoomDpPerMhz != null) (spanMhz * zoomDpPerMhz).dp else fitWidth
        val scroll = rememberScrollState()
        LaunchedEffect(band, zoomed, strongest == null) {
            if (!zoomed) { scroll.scrollTo(0); return@LaunchedEffect }
            val target = inBand.lastOrNull { it.bssid in highlight } ?: strongest ?: return@LaunchedEffect
            val px = with(density) { ((target.frequencyMhz - range.first) * (zoomDpPerMhz ?: 0f)).dp.toPx() }
            scroll.scrollTo((px - with(density) { fitWidth.toPx() } / 2f).toInt().coerceAtLeast(0))
        }
        androidx.compose.foundation.layout.Row {
            Canvas(Modifier.width(GUTTER).height(height)) {
                val top = 8.dp.toPx()
                val bottom = size.height - AXIS.toPx()
                val label = TextStyle(color = Palette.Text2, fontSize = 10.sp, fontFamily = Mono)
                for (d in DBM_TOP downTo DBM_BOTTOM step 10) {
                    val r = tm.measure("$d", label)
                    drawText(r, topLeft = Offset(size.width - r.size.width - 6f, yOf(d, top, bottom) - r.size.height / 2f))
                }
            }
            Canvas(
                Modifier
                    .horizontalScroll(scroll, enabled = zoomed)
                    .width(plotWidth)
                    .height(height)
                    .testTag("spectrum:${band.name}")
                    .semantics { contentDescription = "${band.label} 頻道圖，${inBand.size} 個基地台" },
            ) {
                val top = 8.dp.toPx()
                val bottom = size.height - AXIS.toPx()
                val w = size.width
                fun x(mhz: Int) = (mhz - range.first).toFloat() / spanMhz * w
                for (d in DBM_TOP downTo DBM_BOTTOM step 10) {
                    val y = yOf(d, top, bottom)
                    drawLine(Palette.Grid, Offset(0f, y), Offset(w, y), 1f)
                }
                drawChannelAxis(tm, band, ::x, top, bottom)
                drawLine(Palette.Line, Offset(0f, bottom), Offset(w, bottom), 1.5f)

                for (ap in inBand) {
                    val color = Palette.trace(ap.traceKey())
                    val apexY = yOf(ap.rssi, top, bottom)
                    val hl = ap.bssid in highlight
                    for (span in ap.spans) {
                        val x0 = x(span.first)
                        val x1 = x(span.last)
                        val path = Path().apply {
                            moveTo(x0, bottom)
                            quadraticTo((x0 + x1) / 2f, 2 * apexY - bottom, x1, bottom)
                        }
                        drawPath(path, color.copy(alpha = if (hl) 0.30f else 0.13f))
                        drawPath(path, color, style = Stroke(width = (if (hl) 3.5.dp else 1.6.dp).toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
                    }
                }
                val pad = 4.dp.toPx()
                val labels = mutableListOf<Rect>()
                for (ap in inBand.asReversed()) {
                    val color = Palette.trace(ap.traceKey())
                    val apexY = yOf(ap.rssi, top, bottom)
                    val span = ap.spans.first()
                    val cx = x((span.first + span.last) / 2)
                    val r = tm.measure(
                        ap.displaySsid,
                        TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        constraints = androidx.compose.ui.unit.Constraints(maxWidth = 120.dp.roundToPx()),
                    )
                    val left = (cx - r.size.width / 2f).coerceIn(0f, (w - r.size.width).coerceAtLeast(0f))
                    val rect = Rect(left, apexY - r.size.height - 2f, left + r.size.width, apexY - 2f)
                    val padded = Rect(rect.left - pad, rect.top - pad, rect.right + pad, rect.bottom + pad)
                    if (labels.none { it.overlaps(padded) } && rect.top >= 0f) {
                        labels += padded
                        drawText(r, topLeft = rect.topLeft)
                    }
                }
            }
        }
    }
}

/** Channel numbers along the bottom; PSC channels get priority and labels never overlap. */
private fun DrawScope.drawChannelAxis(tm: TextMeasurer, band: Band, x: (Int) -> Float, top: Float, bottom: Float) {
    val tick = TextStyle(color = Palette.Text2, fontSize = 10.sp, fontFamily = Mono)
    val pscTick = tick.copy(color = Palette.Band6, fontWeight = FontWeight.SemiBold)
    val channels = Channels.candidateChannels(band)
    val gap = 6.dp.toPx()
    val dash = PathEffect.dashPathEffect(floatArrayOf(3f, 5f))
    val minGridPx = 6.dp.toPx()
    var lastGrid = Float.NEGATIVE_INFINITY
    for (ch in channels) {
        val cx = x(Channels.frequencyOf(band, ch))
        if (cx - lastGrid >= minGridPx) {
            drawLine(Palette.Grid, Offset(cx, top), Offset(cx, bottom), 1f, pathEffect = dash)
            lastGrid = cx
        }
    }
    val ordered = channels.sortedByDescending { band == Band.GHZ_6 && Channels.isPreferredScanningChannel(it) }
    val taken = mutableListOf<ClosedFloatingPointRange<Float>>()
    for (ch in ordered) {
        val psc = band == Band.GHZ_6 && Channels.isPreferredScanningChannel(ch)
        val r = tm.measure("$ch", if (psc) pscTick else tick)
        val cx = x(Channels.frequencyOf(band, ch))
        val l = (cx - r.size.width / 2f).coerceIn(0f, (size.width - r.size.width).coerceAtLeast(0f))
        val span = (l - gap)..(l + r.size.width + gap)
        if (taken.any { it.start < span.endInclusive && span.start < it.endInclusive }) continue
        taken += span
        drawText(r, topLeft = Offset(l, bottom + 4f))
    }
}

data class Series(val key: String, val label: String, val color: Color, val samples: List<SignalHistory.Sample>)

/** RSSI over the last [window] scans; gaps where an AP was not seen are left open. */
@Composable
fun SignalChart(series: List<Series>, latestScan: Long, modifier: Modifier = Modifier, window: Int = 60, height: Dp = 260.dp) {
    val tm = rememberTextMeasurer()
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .testTag("signalChart")
            .semantics { contentDescription = "訊號時間圖，${series.size} 條線" },
    ) {
        val left = GUTTER.toPx()
        val top = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val bottom = size.height - AXIS.toPx()
        dbmGrid(tm, left, top, right, bottom)
        drawLine(Palette.Line, Offset(left, bottom), Offset(right, bottom), 1.5f)
        val first = latestScan - window + 1
        fun x(scan: Long) = left + (scan - first).toFloat() / (window - 1).coerceAtLeast(1) * (right - left)
        val tick = TextStyle(color = Palette.Text2, fontSize = 10.sp, fontFamily = Mono)
        for (ago in listOf(window - 1, (window - 1) / 2, 0)) {
            val r = tm.measure(if (ago == 0) "現在" else "-$ago", tick)
            val cx = x(latestScan - ago)
            drawText(r, topLeft = Offset((cx - r.size.width / 2f).coerceIn(left, right - r.size.width), bottom + 4f))
        }
        for (s in series) {
            val pts = s.samples.filter { it.scan >= first }
            if (pts.isEmpty()) continue
            val path = Path()
            var prev: Long? = null
            for (p in pts) {
                val px = x(p.scan)
                val py = yOf(p.rssi, top, bottom)
                if (prev == null || p.scan != prev + 1) path.moveTo(px, py) else path.lineTo(px, py)
                prev = p.scan
            }
            drawPath(path, s.color, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
            val last = pts.last()
            if (last.scan == latestScan) drawCircle(s.color, 3.5.dp.toPx(), Offset(x(last.scan), yOf(last.rssi, top, bottom)))
        }
    }
}
