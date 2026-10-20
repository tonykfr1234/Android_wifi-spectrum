package com.wifispectrum.analyzer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wifispectrum.analyzer.UiState
import com.wifispectrum.analyzer.core.AccessPoint
import com.wifispectrum.analyzer.core.Band
import com.wifispectrum.analyzer.core.ChannelRating
import com.wifispectrum.analyzer.core.Channels
import com.wifispectrum.analyzer.core.MloDevice
import com.wifispectrum.analyzer.core.SignalHistory

/** Charts take at most ~45% of the screen so landscape phones still show the axis and controls. */
@Composable
private fun chartHeight(max: Int = 300): androidx.compose.ui.unit.Dp {
    val px = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height
    val dp = with(LocalDensity.current) { px.toDp().value }
    return (dp * 0.45f).toInt().coerceIn(180, max).dp
}

private fun counts(aps: List<AccessPoint>) = Band.entries.associateWith { b -> aps.count { it.band == b } }

/** The connected BSSID plus every other link of the same Wi-Fi 7 MLD. */
fun connectedKeys(state: UiState): Set<String> {
    val c = state.connection ?: return emptySet()
    val out = mutableSetOf<String>()
    c.bssid?.let(out::add)
    c.mldAddress?.let { mld -> state.aps.filter { it.mldAddress == mld }.forEach { out += it.bssid } }
    return out
}

private fun defaultBand(state: UiState): Band = state.connection?.band ?: Band.GHZ_2_4

@Composable
private fun rememberBand(state: UiState): androidx.compose.runtime.MutableState<Band> =
    rememberSaveable { mutableStateOf(defaultBand(state)) }

@Composable
private fun Legend(aps: List<AccessPoint>, connected: String?, onOpen: (String) -> Unit) {
    for (ap in aps) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onOpen(ap.bssid) }
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).background(Palette.trace(ap.traceKey()), CircleShape))
            Text(
                ap.displaySsid + if (ap.bssid == connected) "（已連線）" else "",
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
            )
            Text("CH ${ap.channel} · ${ap.width.label}", color = Palette.Text2, fontSize = 12.sp, fontFamily = Mono)
            Text("${ap.rssi}", color = Palette.signal(ap.rssi), fontSize = 13.sp, fontFamily = Mono, modifier = Modifier.width(44.dp).padding(start = 8.dp))
        }
    }
}

@Composable
fun ChannelGraphScreen(state: UiState, onOpen: (String) -> Unit) {
    var band by rememberBand(state)
    var zoomed by rememberSaveable { mutableStateOf(false) }
    val inBand = state.aps.filter { it.band == band }.sortedByDescending { it.rssi }
    val highlight = remember(state.aps, state.connection) { connectedKeys(state) }
    LazyColumn(Modifier.fillMaxWidth().testTag("graphScreen")) {
        item { BandSelector(band, counts(state.aps), { band = it }) }
        item {
            Panel {
                if (band != Band.GHZ_2_4) {
                    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (zoomed) "放大檢視，可左右滑動" else "整個頻段總覽", color = Palette.Text2, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        androidx.compose.material3.TextButton(onClick = { zoomed = !zoomed }, modifier = Modifier.testTag("zoom")) {
                            Text(if (zoomed) "總覽" else "放大", color = Palette.Accent, fontSize = 13.sp)
                        }
                    }
                }
                SpectrumChart(band, state.aps, highlight = highlight, zoomed = zoomed, modifier = Modifier.padding(vertical = 8.dp), height = chartHeight())
            }
        }
        item {
            Text(
                when (band) {
                    Band.GHZ_2_4 -> "2.4 GHz 只有 1、6、11 三個互不重疊的頻道。曲線底部寬度就是頻寬，粗線是你連線中的基地台。"
                    Band.GHZ_5 -> "160 MHz 會同時佔用 8 個頻道。52–144 是 DFS 頻道。"
                    Band.GHZ_6 -> "紫色號碼是 PSC 頻道，手機會優先掃描。320 MHz 只有 Wi-Fi 7 支援。"
                },
                color = Palette.Text2, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (inBand.isEmpty()) item { EmptyState("這個頻段沒有基地台", "手機或附近的路由器可能不支援 ${band.label}。") }
        else item { SectionTitle("圖例 · ${inBand.size} 個") }
        item { Legend(inBand.take(40), state.connection?.bssid, onOpen) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun TimeGraphScreen(state: UiState, samples: (String) -> List<SignalHistory.Sample>, onOpen: (String) -> Unit) {
    var band by rememberBand(state)
    val top = state.aps.filter { it.band == band }.sortedByDescending { it.rssi }.take(10)
    val series = remember(state.scanCount, band) {
        top.map { Series(it.bssid, it.displaySsid, Palette.trace(it.traceKey()), samples(it.bssid)) }
    }
    LazyColumn(Modifier.fillMaxWidth().testTag("timeScreen")) {
        item { BandSelector(band, counts(state.aps), { band = it }) }
        item {
            Panel { SignalChart(series, state.scanCount, modifier = Modifier.padding(vertical = 8.dp), height = chartHeight(260)) }
        }
        item {
            Text(
                "最近 60 次掃描，顯示訊號最強的 10 個基地台。線條中斷代表那幾次沒掃到。",
                color = Palette.Text2, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (top.isEmpty()) item { EmptyState("這個頻段沒有基地台", "換個頻段看看。") }
        item { Legend(top, state.connection?.bssid, onOpen) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun RatingScreen(state: UiState) {
    var band by rememberBand(state)
    val scores = remember(state.aps, band) { ChannelRating.score(band, state.aps) }
    val best = remember(state.aps, band) { ChannelRating.recommend(band, state.aps) }
    val maxI = (scores.maxOfOrNull { it.interference } ?: 0.0).coerceAtLeast(0.6)
    LazyColumn(Modifier.fillMaxWidth().testTag("ratingScreen")) {
        item { BandSelector(band, counts(state.aps), { band = it }) }
        item { SectionTitle("建議頻道") }
        item { BestChannels(best) }
        item {
            Text(
                when (band) {
                    Band.GHZ_2_4 -> "2.4 GHz 只建議 1、6、11，用其他頻道會同時干擾兩邊。"
                    Band.GHZ_5 -> "52–144 是 DFS 頻道，偵測到雷達時路由器會暫停並換頻道。"
                    Band.GHZ_6 -> "優先選 PSC 頻道（5、21、37…），手機才找得到你的 6 GHz 網路。"
                },
                color = Palette.Text2, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        item { SectionTitle("全部頻道") }
        items(scores, key = { it.channel }) { s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .testTag("ch:${s.channel}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${s.channel}", fontFamily = Mono, fontSize = 14.sp, modifier = Modifier.width(38.dp))
                Box(Modifier.width(16.dp)) {
                    if (band == Band.GHZ_6 && Channels.isPreferredScanningChannel(s.channel)) Text("P", color = Palette.Band6, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    if (band == Band.GHZ_5 && s.channel in 52..144) Text("D", color = Palette.Warn, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.weight(1f).height(8.dp).background(Palette.Grid, RoundedCornerShape(4.dp))) {
                    val frac = (s.interference / maxI).toFloat().coerceIn(0f, 1f)
                    if (frac > 0f) Box(
                        Modifier
                            .fillMaxWidth(frac)
                            .height(8.dp)
                            .background(if (s.stars >= 4) Palette.Good else if (s.stars == 3) Palette.Warn else Palette.Bad, RoundedCornerShape(4.dp)),
                    )
                }
                Text("${s.apCount}", color = Palette.Text2, fontFamily = Mono, fontSize = 12.sp, modifier = Modifier.width(30.dp).padding(start = 8.dp))
                Stars(s.stars)
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun BestChannels(best: List<com.wifispectrum.analyzer.core.ChannelScore>) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val narrow = maxWidth / LocalDensity.current.fontScale < 340.dp
        if (narrow) {
            Panel {
                best.forEachIndexed { i, s ->
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp).testTag("best:${s.channel}"), verticalAlignment = Alignment.CenterVertically) {
                        Text("${s.channel}", fontFamily = Mono, fontSize = 22.sp, color = if (i == 0) Palette.Good else Palette.Text, modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Stars(s.stars)
                            Text("${s.apCount} 個重疊", color = Palette.Text2, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                best.forEachIndexed { i, s ->
                    Column(
                        Modifier
                            .weight(1f)
                            .background(if (i == 0) Palette.Good.copy(alpha = 0.10f) else Palette.Surface, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                            .testTag("best:${s.channel}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(if (i == 0) "最佳" else "第 ${i + 1}", color = Palette.Text2, fontSize = 11.sp)
                        Text("${s.channel}", fontFamily = Mono, fontSize = 30.sp, fontWeight = FontWeight.Light, maxLines = 1, color = if (i == 0) Palette.Good else Palette.Text)
                        Stars(s.stars)
                        Text("${s.apCount} 個重疊", color = Palette.Text2, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun MloScreen(state: UiState, onOpen: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxWidth().testTag("mloScreen")) {
        item {
            Text(
                "Wi-Fi 7 的多連結操作（MLO）讓一台路由器同時用 2.4、5、6 GHz 多條連結傳資料。同一台裝置（MLD）的各條連結在這裡合併顯示。",
                color = Palette.Text2, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        val c = state.connection
        if (c?.mldAddress != null) {
            item { SectionTitle("目前連線的 MLD") }
            item {
                Panel {
                    KeyValue("SSID", c.ssid ?: "—", mono = false)
                    KeyValue("MLD 位址", c.mldAddress)
                    KeyValue("使用中連結", if (c.activeLinks.isEmpty()) "需要 Android 14 以上" else "${c.activeLinks.size} 條", mono = false)
                    for (l in c.activeLinks) KeyValue("Link ${l.linkId}", "${l.band?.label ?: "?"} · CH ${l.channel}" + (l.rssi?.let { " · $it dBm" } ?: ""))
                }
            }
        }
        item { SectionTitle("附近的 MLO 裝置 · ${state.mlo.size}") }
        if (state.mlo.isEmpty()) {
            item {
                EmptyState(
                    "附近沒有偵測到 MLO 基地台",
                    "需要 Wi-Fi 7 路由器，而且這支手機要是 Android 13 以上、支援 Wi-Fi 7，系統才會回報 MLO 資訊。",
                )
            }
        }
        items(state.mlo, key = { it.mldAddress }) { d -> MloCard(d, state, onOpen) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MloCard(d: MloDevice, state: UiState, onOpen: (String) -> Unit) {
    val color = Palette.trace(d.mldAddress)
    Column(Modifier.padding(bottom = 10.dp).testTag("mlo:${d.mldAddress}")) {
        Panel {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(d.ssid.ifEmpty { "（隱藏網路）" }, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "MLD ${d.mldAddress}" + (d.links.firstNotNullOfOrNull { state.vendors.lookup(it.bssid) }?.let { " · $it" } ?: ""),
                        color = Palette.Text2, fontSize = 11.sp, fontFamily = Mono, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${d.links.size} 條連結", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.Wifi7)
                    Text("合計 ${d.totalWidthMhz} MHz", color = Palette.Text2, fontSize = 11.sp, fontFamily = Mono)
                }
            }
            HorizontalDivider(color = Palette.Line)
            for (l in d.links) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(l.bssid) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("L${l.mloLinkId ?: "?"}", color = Palette.Text2, fontFamily = Mono, fontSize = 12.sp, modifier = Modifier.width(28.dp))
                    Tag("${l.band?.shortLabel ?: "?"} CH ${l.channel}", Palette.band(l.band))
                    Spacer(Modifier.width(6.dp))
                    Tag(l.width.label, if (l.width.mhz >= 160) Palette.Accent else Palette.Text2)
                    Text(l.bssid, color = Palette.Text2, fontFamily = Mono, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    Text("${l.rssi}", color = Palette.signal(l.rssi), fontFamily = Mono, fontSize = 13.sp)
                }
            }
            val seen = d.links.mapNotNull { it.mloLinkId }.toSet()
            val missing = d.advertisedLinks.filter { it.linkId !in seen }
            if (missing.isNotEmpty()) {
                HorizontalDivider(color = Palette.Line)
                for (m in missing) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("L${m.linkId}", color = Palette.Text2, fontFamily = Mono, fontSize = 12.sp, modifier = Modifier.width(28.dp))
                        Tag("${m.band?.shortLabel ?: "?"} CH ${m.channel}", Palette.Text2)
                        Text("已宣告，這次沒掃到", color = Palette.Text2, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
