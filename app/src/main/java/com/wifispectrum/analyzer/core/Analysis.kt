package com.wifispectrum.analyzer.core

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object SecurityParser {
    private val token = Regex("\\[([^\\]]*)]")
    private val cipherSuffix = Regex("-(CCMP|TKIP|GCMP|SMS4|NONE|WEP|BIP).*$")

    /** Parses Android's ScanResult.capabilities, e.g. "[RSN-PSK+SAE-CCMP][ESS]". */
    fun parse(capabilities: String): Set<Security> {
        val out = linkedSetOf<Security>()
        for (m in token.findAll(capabilities.uppercase())) {
            val t = m.groupValues[1]
            when {
                t.startsWith("WEP") -> out += Security.WEP
                t.startsWith("WAPI") -> out += Security.WAPI
                t.startsWith("RSN-") || t.startsWith("WPA2-") -> parseKeyMgmt(t.substringAfter('-'), wpa1 = false, out)
                t.startsWith("WPA-") -> parseKeyMgmt(t.substringAfter('-'), wpa1 = true, out)
            }
        }
        if (out.isEmpty()) out += Security.OPEN
        return out
    }

    private fun parseKeyMgmt(body: String, wpa1: Boolean, out: MutableSet<Security>) {
        for (km in body.replace(cipherSuffix, "").split('+')) {
            when {
                "SUITE_B" in km || "SUITE-B" in km -> out += Security.WPA3_ENTERPRISE_192
                "SAE" in km -> out += Security.WPA3_PERSONAL
                "OWE" in km -> out += Security.OWE
                km == "EAP-SHA256" || km == "EAP/SHA256" -> out += Security.WPA3_ENTERPRISE
                "PSK" in km -> out += if (wpa1) Security.WPA else Security.WPA2_PERSONAL
                "EAP" in km || "FILS" in km -> out += if (wpa1) Security.WPA else Security.WPA2_ENTERPRISE
            }
        }
    }

    fun summary(security: Set<Security>): String = security
        .sortedByDescending { it.ordinal }
        .joinToString(" / ") { it.label }
}

object Generations {
    /** Maps android.net.wifi.ScanResult.WIFI_STANDARD_* to a marketing generation. */
    fun fromAndroidStandard(standard: Int, band: Band?): WifiGeneration = when (standard) {
        1 -> WifiGeneration.LEGACY
        4 -> WifiGeneration.WIFI_4
        5 -> WifiGeneration.WIFI_5
        6 -> if (band == Band.GHZ_6) WifiGeneration.WIFI_6E else WifiGeneration.WIFI_6
        7 -> WifiGeneration.WIGIG
        8 -> WifiGeneration.WIFI_7
        else -> WifiGeneration.UNKNOWN
    }
}

object Signal {
    fun quality(rssi: Int): String = when {
        rssi >= -50 -> "極佳"
        rssi >= -60 -> "良好"
        rssi >= -70 -> "普通"
        rssi >= -80 -> "偏弱"
        else -> "很弱"
    }

    /** 0..4 bars, matching the thresholds of [quality]. */
    fun bars(rssi: Int): Int = when {
        rssi >= -50 -> 4
        rssi >= -60 -> 3
        rssi >= -70 -> 2
        rssi >= -80 -> 1
        else -> 0
    }

    /** Free-space path loss estimate. Walls make real distances shorter, so treat it as a rough guide. */
    fun estimateDistanceMeters(rssi: Int, frequencyMhz: Int): Double {
        if (frequencyMhz <= 0) return Double.NaN
        val exp = (27.55 - 20 * log10(frequencyMhz.toDouble()) + abs(rssi)) / 20.0
        return 10.0.pow(exp)
    }
}

object Mac {
    fun normalize(mac: String): String = mac.trim().lowercase()

    /** Randomized / locally administered addresses have bit 1 of the first octet set. */
    fun isLocallyAdministered(mac: String): Boolean {
        val first = mac.take(2).toIntOrNull(16) ?: return false
        return first and 0x02 != 0
    }

    fun ouiKey(mac: String): Int? {
        val hex = mac.filter { it.isLetterOrDigit() }
        if (hex.length < 6) return null
        return hex.substring(0, 6).toIntOrNull(16)
    }
}

data class MloDevice(
    val mldAddress: String,
    val ssid: String,
    val links: List<AccessPoint>,
    val advertisedLinks: List<MloLinkInfo>,
) {
    val bands: List<Band> get() = links.mapNotNull { it.band }.distinct().sorted()
    val bestRssi: Int get() = links.maxOf { it.rssi }
    val totalWidthMhz: Int get() = links.sumOf { it.width.mhz }
}

object Mlo {
    fun group(aps: List<AccessPoint>): List<MloDevice> = aps
        .filter { it.mldAddress != null }
        .groupBy { Mac.normalize(it.mldAddress!!) }
        .map { (mld, links) ->
            val sorted = links.sortedWith(compareBy({ it.band?.ordinal ?: 9 }, { it.frequencyMhz }))
            MloDevice(
                mldAddress = mld,
                ssid = sorted.firstOrNull { !it.isHidden }?.ssid ?: "",
                links = sorted,
                advertisedLinks = links.flatMap { it.mloLinks }.distinctBy { it.linkId }.sortedBy { it.linkId },
            )
        }
        .sortedByDescending { it.bestRssi }
}

data class ChannelScore(
    val channel: Int,
    val frequencyMhz: Int,
    val apCount: Int,
    val interference: Double,
    val stars: Int,
    val preferred: Boolean,
)

object ChannelRating {
    /** -30 dBm counts fully, -100 dBm not at all; squared so strong neighbours dominate. */
    fun weight(rssi: Int): Double {
        val x = (rssi + 100).coerceIn(0, 70) / 70.0
        return x * x
    }

    fun score(band: Band, aps: List<AccessPoint>): List<ChannelScore> {
        val inBand = aps.filter { it.band == band }
        return Channels.candidateChannels(band).map { ch ->
            val f = Channels.frequencyOf(band, ch)
            val lo = f - 10
            val hi = f + 10
            var count = 0
            var sum = 0.0
            for (ap in inBand) {
                var overlap = 0
                for (s in ap.spans) overlap += max(0, min(hi, s.last) - max(lo, s.first))
                if (overlap > 0) {
                    count++
                    sum += weight(ap.rssi) * overlap / 20.0
                }
            }
            ChannelScore(ch, f, count, sum, stars(sum), preferred(band, ch))
        }
    }

    /** 2.4 GHz: non-overlapping 1/6/11. 5 GHz: no DFS radar checks and no UNII-4 (not legal everywhere). 6 GHz: PSC. */
    private fun preferred(band: Band, ch: Int) = when (band) {
        Band.GHZ_2_4 -> ch == 1 || ch == 6 || ch == 11
        Band.GHZ_5 -> ch in 36..48 || ch in 149..165
        Band.GHZ_6 -> Channels.isPreferredScanningChannel(ch)
    }

    fun stars(interference: Double): Int = when {
        interference <= 0.01 -> 5
        interference < 0.1 -> 4
        interference < 0.3 -> 3
        interference < 0.6 -> 2
        else -> 1
    }

    /** Best channels first; in 2.4 GHz only 1/6/11, otherwise ties go to preferred channels. */
    fun recommend(band: Band, aps: List<AccessPoint>, count: Int = 3): List<ChannelScore> =
        score(band, aps)
            .filter { band != Band.GHZ_2_4 || it.preferred }
            .sortedWith(compareBy<ChannelScore>({ it.interference }, { !it.preferred }, { it.channel }))
            .take(count)
}

/** Per-BSSID RSSI series with bounded memory: old samples roll off, vanished APs are evicted. */
class SignalHistory(
    private val maxSamples: Int = 120,
    private val evictAfterMissedScans: Int = 30,
) {
    data class Sample(val scan: Long, val rssi: Int)

    private val series = HashMap<String, ArrayDeque<Sample>>()
    private val lastSeen = HashMap<String, Long>()
    var scanCount: Long = 0
        private set

    val trackedCount: Int get() = series.size

    @Synchronized
    fun record(aps: List<AccessPoint>) {
        scanCount++
        for (ap in aps) {
            val q = series.getOrPut(ap.bssid) { ArrayDeque() }
            q.addLast(Sample(scanCount, ap.rssi))
            while (q.size > maxSamples) q.removeFirst()
            lastSeen[ap.bssid] = scanCount
        }
        val it = lastSeen.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (scanCount - e.value > evictAfterMissedScans) {
                series.remove(e.key)
                it.remove()
            }
        }
    }

    @Synchronized
    fun samples(bssid: String): List<Sample> = series[bssid]?.toList() ?: emptyList()

    @Synchronized
    fun totalSamples(): Int = series.values.sumOf { it.size }

    @Synchronized
    fun clear() {
        series.clear()
        lastSeen.clear()
        scanCount = 0
    }
}

enum class SortOrder(val label: String) { SIGNAL("訊號"), SSID("名稱"), CHANNEL("頻道") }

data class ApFilter(
    val bands: Set<Band> = Band.entries.toSet(),
    val minWidthMhz: Int = 0,
    val query: String = "",
    val wifi7Only: Boolean = false,
    val hideHidden: Boolean = false,
    val sort: SortOrder = SortOrder.SIGNAL,
) {
    val isDefault: Boolean get() = this == ApFilter(sort = sort)

    fun apply(aps: List<AccessPoint>): List<AccessPoint> {
        val q = query.trim().lowercase()
        val out = aps.filter { ap ->
            (ap.band == null || ap.band in bands) &&
                ap.width.mhz >= minWidthMhz &&
                (!wifi7Only || ap.generation == WifiGeneration.WIFI_7) &&
                (!hideHidden || !ap.isHidden) &&
                (q.isEmpty() || ap.ssid.lowercase().contains(q) || ap.bssid.contains(q))
        }
        return when (sort) {
            SortOrder.SIGNAL -> out.sortedWith(compareByDescending<AccessPoint> { it.rssi }.thenBy { it.bssid })
            SortOrder.SSID -> out.sortedWith(compareBy<AccessPoint>({ it.isHidden }, { it.ssid.lowercase() }, { it.bssid }))
            SortOrder.CHANNEL -> out.sortedWith(compareBy<AccessPoint>({ it.band?.ordinal ?: 9 }, { it.channel }, { -it.rssi }))
        }
    }
}
