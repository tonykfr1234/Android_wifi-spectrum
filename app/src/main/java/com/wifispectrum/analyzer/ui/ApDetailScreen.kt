package com.wifispectrum.analyzer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wifispectrum.analyzer.core.AccessPoint
import com.wifispectrum.analyzer.core.Band
import com.wifispectrum.analyzer.core.ChannelWidth
import com.wifispectrum.analyzer.core.Channels
import com.wifispectrum.analyzer.core.Mac
import com.wifispectrum.analyzer.core.SecurityParser
import com.wifispectrum.analyzer.core.Signal
import com.wifispectrum.analyzer.core.SignalHistory
import com.wifispectrum.analyzer.core.VendorDb
import com.wifispectrum.analyzer.core.WifiGeneration

@Composable
fun ApDetailScreen(
    ap: AccessPoint,
    vendors: VendorDb,
    connected: Boolean,
    scanCount: Long,
    samples: (String) -> List<SignalHistory.Sample>,
) {
    val series = remember(scanCount, ap.bssid) {
        listOf(Series(ap.bssid, ap.displaySsid, Palette.trace(ap.traceKey()), samples(ap.bssid)))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("detail"),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ap.displaySsid, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = if (ap.isHidden) Palette.Text2 else Palette.Text)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag("${ap.band?.label ?: "未知頻段"}", Palette.band(ap.band))
                    if (ap.generation != WifiGeneration.UNKNOWN) Tag(ap.generation.label, generationColor(ap.generation))
                    if (connected) Tag("已連線", Palette.Good, filled = true)
                    if (ap.isMlo) Tag("MLO", Palette.Wifi7, filled = true)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${ap.rssi}", color = Palette.signal(ap.rssi), fontSize = 40.sp, fontFamily = Mono, fontWeight = FontWeight.Light)
                Text("dBm · ${Signal.quality(ap.rssi)}", color = Palette.Text2, fontSize = 12.sp)
            }
        }

        Panel { SignalChart(series, scanCount, modifier = Modifier.padding(vertical = 8.dp), height = 180.dp) }

        SectionTitle("識別")
        Panel {
            KeyValue("BSSID", ap.bssid)
            KeyValue("廠商", vendors.lookup(ap.bssid) ?: if (Mac.isLocallyAdministered(ap.bssid)) "隨機 / 本地管理位址" else "未登錄", mono = false)
            KeyValue("SSID 狀態", if (ap.isHidden) "隱藏（不廣播名稱）" else "公開廣播", mono = false)
        }

        SectionTitle("頻道")
        Panel {
            KeyValue("主頻道", "${ap.channel}  (${ap.frequencyMhz} MHz)")
            KeyValue("頻寬", ap.width.label)
            if (ap.width != ChannelWidth.MHZ_20 && ap.width != ChannelWidth.MHZ_80_PLUS_80) KeyValue("中心頻道", "${ap.centerChannel}")
            KeyValue("佔用範圍", ap.spans.joinToString("\n") { "${it.first}–${it.last} MHz" })
            val covered = ap.band?.let { b ->
                Channels.candidateChannels(b).filter { ch ->
                    val f = Channels.frequencyOf(b, ch)
                    ap.spans.any { f - 10 >= it.first && f + 10 <= it.last }
                }
            }.orEmpty()
            if (covered.size > 1) KeyValue("涵蓋頻道", covered.joinToString(", "))
            if (ap.band == Band.GHZ_6) KeyValue("PSC 頻道", if (Channels.isPreferredScanningChannel(ap.channel)) "是" else "否", mono = false)
            if (ap.band == Band.GHZ_5 && ap.channel in 52..144) KeyValue("DFS", "是，偵測到雷達時會換頻道", mono = false)
        }

        SectionTitle("訊號與標準")
        Panel {
            KeyValue("訊號品質", "${Signal.quality(ap.rssi)}（${Signal.bars(ap.rssi)}/4 格）", mono = false)
            KeyValue("估計距離", formatDistance(Signal.estimateDistanceMeters(ap.rssi, ap.frequencyMhz)) + "（無遮蔽估算）", mono = false)
            KeyValue("Wi-Fi 標準", if (ap.generation == WifiGeneration.UNKNOWN) "系統未回報（需要 Android 11 以上）" else "${ap.generation.label} · ${ap.generation.ieee}", mono = false)
        }

        SectionTitle("安全性")
        Panel {
            KeyValue("加密", SecurityParser.summary(ap.security), mono = false, valueColor = if (ap.isWeakSecurity()) Palette.Bad else Palette.Text)
            KeyValue("原始能力", ap.capabilities.ifEmpty { "—" })
        }

        if (ap.isMlo) {
            SectionTitle("多連結（MLO）")
            Panel {
                KeyValue("MLD 位址", ap.mldAddress ?: "—")
                KeyValue("本連結 ID", ap.mloLinkId?.toString() ?: "—")
                if (ap.mloLinks.isEmpty()) KeyValue("關聯連結", "基地台未宣告", mono = false)
                for (l in ap.mloLinks) {
                    KeyValue(
                        "Link ${l.linkId}" + if (l.linkId == ap.mloLinkId) "（本條）" else "",
                        "${l.band?.label ?: "?"} · CH ${l.channel}" + (l.bssid?.let { "\n$it" } ?: ""),
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
