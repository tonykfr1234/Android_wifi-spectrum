package com.wifispectrum.analyzer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wifispectrum.analyzer.UiState
import com.wifispectrum.analyzer.core.AccessPoint
import com.wifispectrum.analyzer.core.ApFilter
import com.wifispectrum.analyzer.core.Band
import com.wifispectrum.analyzer.core.ConnectionInfo
import com.wifispectrum.analyzer.core.Signal
import com.wifispectrum.analyzer.core.SortOrder
import com.wifispectrum.analyzer.core.VendorDb
import com.wifispectrum.analyzer.core.WifiGeneration

@Composable
fun ApListScreen(state: UiState, onOpen: (String) -> Unit, onFilter: (ApFilter) -> Unit) {
    LazyColumn(Modifier.fillMaxWidth().testTag("apList")) {
        state.connection?.let { c -> item(key = "conn") { ConnectionCard(c, state.vendors) } }
        item(key = "filter") { FilterBar(state.filter, state.aps.size, state.visible.size, onFilter) }
        if (state.visible.isEmpty()) {
            item(key = "empty") {
                if (state.aps.isEmpty()) EmptyState("還沒有掃描結果", "按右上角的重新整理開始掃描。")
                else EmptyState("沒有符合條件的網路", "放寬篩選條件，或清除搜尋文字。")
            }
        }
        items(state.visible, key = { it.bssid }) { ap ->
            ApRow(ap, state.vendors, connected = ap.bssid == state.connection?.bssid, onClick = { onOpen(ap.bssid) })
        }
        item(key = "pad") { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ConnectionCard(c: ConnectionInfo, vendors: VendorDb) {
    SectionTitle("目前連線")
    Panel {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.ssid ?: "（名稱需要定位權限）", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(c.bssid, c.bssid?.let(vendors::lookup)).joinToString("  ·  "),
                    color = Palette.Text2, fontSize = 12.sp, fontFamily = Mono, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    c.band?.let { Tag("${it.shortLabel} CH ${c.channel}", Palette.band(it)) }
                    if (c.generation != WifiGeneration.UNKNOWN) Tag(c.generation.label, generationColor(c.generation))
                    if (c.mldAddress != null) Tag("MLO ×${c.activeLinks.size.coerceAtLeast(1)}", Palette.Wifi7, filled = true)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${c.rssi}", color = Palette.signal(c.rssi), fontSize = 28.sp, fontFamily = Mono, fontWeight = FontWeight.Light)
                Text("dBm", color = Palette.Text2, fontSize = 11.sp)
            }
        }
        HorizontalDivider(color = Palette.Line)
        FlowRow(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Metric("傳送", c.txSpeedMbps ?: c.linkSpeedMbps, "Mbps")
            c.rxSpeedMbps?.let { Metric("接收", it, "Mbps") }
            c.maxTxSpeedMbps?.let { Metric("最高", it, "Mbps") }
        }
        c.ipAddress?.let {
            Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                Text("IP 位址", color = Palette.Text2, fontSize = 12.sp, modifier = Modifier.width(56.dp))
                Text(it, fontFamily = Mono, fontSize = 13.sp)
            }
        }
        if (c.activeLinks.isNotEmpty()) {
            HorizontalDivider(color = Palette.Line)
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                for (l in c.activeLinks) {
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Link ${l.linkId}", color = Palette.Text2, fontSize = 12.sp, fontFamily = Mono, modifier = Modifier.width(56.dp))
                        Tag("${l.band?.shortLabel ?: "?"} CH ${l.channel}", Palette.band(l.band))
                        Spacer(Modifier.weight(1f))
                        l.rssi?.let { Text("$it dBm", color = Palette.signal(it), fontSize = 12.sp, fontFamily = Mono) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: Int, unit: String) {
    Column {
        Text(label, color = Palette.Text2, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$value", fontFamily = Mono, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(" $unit", color = Palette.Text2, fontSize = 11.sp)
        }
    }
}

private val WIDTH_STEPS = listOf(0 to "全部頻寬", 40 to "≥40", 80 to "≥80", 160 to "≥160", 320 to "320")

@Composable
private fun FilterBar(f: ApFilter, total: Int, shown: Int, onFilter: (ApFilter) -> Unit) {
    Column(Modifier.padding(top = 10.dp)) {
        Row(
            Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .height(40.dp)
                .background(Palette.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, Palette.Line, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Search, null, tint = Palette.Text2, modifier = Modifier.size(18.dp))
            Box(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                if (f.query.isEmpty()) Text("搜尋 SSID 或 BSSID", color = Palette.Text2, fontSize = 14.sp)
                BasicTextField(
                    value = f.query,
                    onValueChange = { onFilter(f.copy(query = it.take(64))) },
                    singleLine = true,
                    textStyle = TextStyle(color = Palette.Text, fontSize = 14.sp),
                    cursorBrush = SolidColor(Palette.Accent),
                    modifier = Modifier.fillMaxWidth().testTag("search"),
                )
            }
            if (f.query.isNotEmpty()) {
                Icon(
                    Icons.Close, "清除搜尋", tint = Palette.Text2,
                    modifier = Modifier.size(18.dp).clickable { onFilter(f.copy(query = "")) },
                )
            }
        }
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (b in Band.entries) {
                val on = b in f.bands
                Chip(b.label, on, Palette.band(b)) {
                    val next = if (on) f.bands - b else f.bands + b
                    onFilter(f.copy(bands = next.ifEmpty { setOf(b) }))
                }
            }
            Divider()
            val idx = WIDTH_STEPS.indexOfFirst { it.first == f.minWidthMhz }.coerceAtLeast(0)
            Chip(WIDTH_STEPS[idx].second + if (idx > 0) " MHz" else "", idx > 0, Palette.Accent) {
                onFilter(f.copy(minWidthMhz = WIDTH_STEPS[(idx + 1) % WIDTH_STEPS.size].first))
            }
            Chip("Wi-Fi 7", f.wifi7Only, Palette.Wifi7) { onFilter(f.copy(wifi7Only = !f.wifi7Only)) }
            Chip("隱藏網路", !f.hideHidden, Palette.Text2) { onFilter(f.copy(hideHidden = !f.hideHidden)) }
            Divider()
            Chip("排序：${f.sort.label}", false, Palette.Text2) {
                onFilter(f.copy(sort = SortOrder.entries[(f.sort.ordinal + 1) % SortOrder.entries.size]))
            }
        }
        Text(
            if (shown == total) "$total 個基地台" else "顯示 $shown / $total 個基地台",
            color = Palette.Text2, fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp).testTag("count"),
        )
    }
}

@Composable
private fun Divider() = Box(Modifier.width(1.dp).height(20.dp).background(Palette.Line))

@Composable
private fun Chip(text: String, on: Boolean, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Text(
        text,
        color = if (on) color else Palette.Text2,
        fontSize = 13.sp,
        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        modifier = Modifier
            .background(if (on) color.copy(alpha = 0.14f) else Palette.Surface, shape)
            .border(1.dp, if (on) color.copy(alpha = 0.6f) else Palette.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { contentDescription = "$text 篩選${if (on) "已開啟" else "已關閉"}" },
    )
}

@Composable
fun ApRow(ap: AccessPoint, vendors: VendorDb, connected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 14.dp, top = 9.dp, bottom = 9.dp)
            .testTag("ap:${ap.bssid}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(46.dp).background(Palette.trace(ap.traceKey()), RoundedCornerShape(2.dp)))
        Column(Modifier.width(58.dp).padding(start = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${ap.rssi}", color = Palette.signal(ap.rssi), fontFamily = Mono, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            SignalBars(ap.rssi)
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    ap.displaySsid,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (ap.isHidden) Palette.Text2 else Palette.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (connected) {
                    Spacer(Modifier.width(6.dp))
                    Tag("已連線", Palette.Good, filled = true)
                }
            }
            Text(
                ap.bssid + (vendors.lookup(ap.bssid)?.let { "  $it" } ?: if (ap.bssid.isNotEmpty() && com.wifispectrum.analyzer.core.Mac.isLocallyAdministered(ap.bssid)) "  隨機位址" else ""),
                color = Palette.Text2, fontSize = 11.sp, fontFamily = Mono, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                Modifier.padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Tag("${ap.band?.shortLabel ?: "?"} CH ${ap.channel}", Palette.band(ap.band))
                Tag(ap.width.label, if (ap.width.mhz >= 160) Palette.Accent else Palette.Text2)
                if (ap.generation != WifiGeneration.UNKNOWN) Tag(ap.generation.label, generationColor(ap.generation))
                Tag(ap.securityShort(), if (ap.isWeakSecurity()) Palette.Bad else Palette.Text2)
                if (ap.isMlo) Tag("MLO L${ap.mloLinkId ?: "?"}", Palette.Wifi7, filled = true)
            }
        }
        Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End) {
            Text(
                formatDistance(Signal.estimateDistanceMeters(ap.rssi, ap.frequencyMhz)),
                color = Palette.Text2, fontSize = 11.sp, fontFamily = Mono,
            )
        }
    }
    HorizontalDivider(Modifier.padding(start = 81.dp), color = Palette.Grid)
}
