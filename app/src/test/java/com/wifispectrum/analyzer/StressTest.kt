package com.wifispectrum.analyzer

import com.wifispectrum.analyzer.core.ApFilter
import com.wifispectrum.analyzer.core.Band
import com.wifispectrum.analyzer.core.ChannelRating
import com.wifispectrum.analyzer.core.Channels
import com.wifispectrum.analyzer.core.Mlo
import com.wifispectrum.analyzer.core.SecurityParser
import com.wifispectrum.analyzer.core.SignalHistory
import com.wifispectrum.analyzer.core.SortOrder
import com.wifispectrum.analyzer.core.VendorDb
import com.wifispectrum.analyzer.data.RawMloLink
import com.wifispectrum.analyzer.data.RawScan
import com.wifispectrum.analyzer.data.ScanMapper
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random
import kotlin.system.measureNanoTime

object Gen {
    private val caps = listOf(
        "[ESS]", "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]", "[RSN-PSK+SAE-CCMP][ESS]", "[RSN-SAE_EXT_KEY-GCMP-256][ESS]",
        "[RSN-OWE-CCMP][ESS]", "[WPA2-EAP-CCMP][ESS]", "[WEP][ESS]", "[RSN-EAP_SUITE_B_192-GCMP-256]",
    )
    private val widths = mapOf(Band.GHZ_2_4 to listOf(0, 1), Band.GHZ_5 to listOf(0, 1, 2, 3, 4), Band.GHZ_6 to listOf(0, 1, 2, 3, 5))
    private val mhz = intArrayOf(20, 40, 80, 160, 160, 320)

    fun mac(r: Random, prefix: Int = r.nextInt(0x1000000)): String {
        val b = listOf(prefix shr 16, prefix shr 8 and 0xff, prefix and 0xff, r.nextInt(256), r.nextInt(256), r.nextInt(256))
        return b.joinToString(":") { "%02x".format(it and 0xff) }
    }

    /** A plausible scan result anywhere in the three bands. */
    fun valid(r: Random, mld: String? = null): RawScan {
        val band = Band.entries.random(r)
        val ch = Channels.candidateChannels(band).random(r)
        val f = Channels.frequencyOf(band, ch)
        val w = widths.getValue(band).random(r)
        val cf0 = if (w == 0 || r.nextInt(5) == 0) 0 else Channels.inferCenter(band, f, mhz[w])
        return RawScan(
            bssid = mac(r), ssid = if (r.nextInt(10) == 0) "" else "AP-${r.nextInt(100_000)}",
            level = r.nextInt(-95, -25), frequency = f, centerFreq0 = cf0, centerFreq1 = if (w == 4) 5775 else 0,
            channelWidth = w, wifiStandard = listOf(0, 1, 4, 5, 6, 8).random(r), capabilities = caps.random(r),
            mldAddress = mld, mloLinkId = if (mld != null) r.nextInt(3) else -1,
            mloLinks = if (mld != null) List(3) { RawMloLink(it, listOf(1, 2, 8)[it], 36, mac(r), -60) } else emptyList(),
        )
    }

    fun scan(r: Random, n: Int): List<RawScan> = List(n) { i -> valid(r, mld = if (i % 5 == 0) "02:00:00:00:%02x:%02x".format(i / 256 % 256, i / 5 % 256) else null) }

    fun garbageString(r: Random, max: Int = 40): String =
        String(CharArray(r.nextInt(max)) { listOf('[', ']', '-', '+', '/', '_', ':', '\u0000', '"', 'Z', '測').random(r).takeIf { r.nextBoolean() } ?: ('!' + r.nextInt(90)) })

    fun garbage(r: Random) = RawScan(
        bssid = if (r.nextInt(4) == 0) null else garbageString(r, 20),
        ssid = if (r.nextInt(4) == 0) null else garbageString(r),
        level = r.nextInt(), frequency = listOf(r.nextInt(), r.nextInt(-10, 8000), 2484, 5935, 7115, 0).random(r),
        centerFreq0 = r.nextInt(-100, 9000), centerFreq1 = r.nextInt(-100, 9000),
        channelWidth = r.nextInt(-3, 10), wifiStandard = r.nextInt(-3, 12),
        capabilities = if (r.nextInt(4) == 0) null else garbageString(r, 80),
        mldAddress = if (r.nextBoolean()) garbageString(r, 20) else null, mloLinkId = r.nextInt(-5, 20),
        mloLinks = List(r.nextInt(4)) { RawMloLink(r.nextInt(), r.nextInt(), r.nextInt(), garbageString(r, 20), r.nextInt()) },
    )
}

class StressTest {

    @Test fun thousandApsPerScanStaysFast() {
        val r = Random(42)
        val history = SignalHistory()
        val filter = ApFilter(minWidthMhz = 40, sort = SortOrder.CHANNEL)
        val scans = List(300) { Gen.scan(r, 1_000) }
        repeat(20) { process(scans[it], history, filter) }
        val nanos = scans.sumOf { s -> measureNanoTime { process(s, history, filter) } }
        val avgMs = nanos / scans.size / 1e6
        println("1000 APs/scan: avg %.2f ms over %d scans".format(avgMs, scans.size))
        assertTrue("avg $avgMs ms", avgMs < 150)
        assertTrue(history.totalSamples() <= history.trackedCount * 120)
    }

    private fun process(raw: List<RawScan>, history: SignalHistory, filter: ApFilter) {
        val aps = ScanMapper.mapAll(raw)
        history.record(aps)
        Mlo.group(aps)
        for (b in Band.entries) ChannelRating.score(b, aps)
        filter.apply(aps)
    }

    @Test fun historyMemoryIsBoundedUnderChurn() {
        val r = Random(7)
        val history = SignalHistory(maxSamples = 120, evictAfterMissedScans = 30)
        var peak = 0
        repeat(20_000) {
            history.record(ScanMapper.mapAll(List(50) { Gen.valid(r) }))
            peak = maxOf(peak, history.trackedCount)
        }
        println("churn: peak tracked $peak, final ${history.trackedCount}, samples ${history.totalSamples()}")
        assertTrue("peak $peak", peak <= 50 * 31)
        assertTrue(history.totalSamples() <= 50 * 31 * 120)
    }

    @Test fun stableApsKeepFixedSampleCount() {
        val r = Random(9)
        val fixed = ScanMapper.mapAll(List(200) { Gen.valid(r) })
        val history = SignalHistory(maxSamples = 120)
        repeat(10_000) { history.record(fixed) }
        assertTrue(history.totalSamples() == fixed.size * 120)
    }

    @Test fun garbageInputNeverCrashes() {
        val r = Random(1234)
        var kept = 0
        repeat(200_000) {
            val ap = ScanMapper.toAccessPoint(Gen.garbage(r)) ?: return@repeat
            kept++
            assertTrue(ap.spans.isNotEmpty())
            assertTrue(ap.spans.all { s -> s.first < s.last })
            assertTrue(ap.rssi in -127..0)
            ap.centerChannel
            ap.displaySsid
        }
        println("fuzz: $kept of 200000 garbage scans produced an AP")
        assertTrue(kept > 0)
    }

    @Test fun garbageThroughWholePipeline() {
        val r = Random(99)
        val history = SignalHistory()
        repeat(2_000) {
            val aps = ScanMapper.mapAll(List(r.nextInt(60)) { if (r.nextBoolean()) Gen.garbage(r) else Gen.valid(r) })
            history.record(aps)
            Mlo.group(aps)
            for (b in Band.entries) {
                ChannelRating.score(b, aps)
                ChannelRating.recommend(b, aps)
            }
            ApFilter(query = Gen.garbageString(r, 6)).apply(aps)
        }
    }

    @Test fun securityParserFuzz() {
        val r = Random(5)
        repeat(100_000) { assertTrue(SecurityParser.parse(Gen.garbageString(r, 120)).isNotEmpty()) }
    }

    @Test fun vendorLookupFuzz() {
        val db = File("src/main/assets/oui.tsv.gz").inputStream().use(VendorDb::parseGzip)
        val r = Random(6)
        repeat(100_000) { db.lookup(Gen.garbageString(r, 30)) }
        val t = measureNanoTime { repeat(100_000) { db.lookup(Gen.mac(r)) } }
        println("vendor lookup: %.0f ns each".format(t / 100_000.0))
    }
}
