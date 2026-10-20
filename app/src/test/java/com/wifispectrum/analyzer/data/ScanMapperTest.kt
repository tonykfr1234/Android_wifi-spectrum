package com.wifispectrum.analyzer.data

import com.wifispectrum.analyzer.core.Band
import com.wifispectrum.analyzer.core.ChannelWidth
import com.wifispectrum.analyzer.core.Mlo
import com.wifispectrum.analyzer.core.Security
import com.wifispectrum.analyzer.core.WifiGeneration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanMapperTest {
    private fun raw(
        bssid: String? = "AA:BB:CC:00:00:01", ssid: String? = "\"Home\"", level: Int = -50, freq: Int = 5180,
        width: Int = 2, std: Int = 6, caps: String? = "[RSN-PSK-CCMP]", mld: String? = null, linkId: Int = -1,
        links: List<RawMloLink> = emptyList(),
    ) = RawScan(bssid, ssid, level, freq, 5210, 0, width, std, caps, mld, linkId, links)

    @Test fun widths() {
        assertEquals(
            listOf(ChannelWidth.MHZ_20, ChannelWidth.MHZ_40, ChannelWidth.MHZ_80, ChannelWidth.MHZ_160, ChannelWidth.MHZ_80_PLUS_80, ChannelWidth.MHZ_320, ChannelWidth.MHZ_20),
            listOf(0, 1, 2, 3, 4, 5, 99).map(ScanMapper::width),
        )
    }

    @Test fun bands() {
        assertEquals(listOf(Band.GHZ_2_4, Band.GHZ_5, Band.GHZ_6, null, null), listOf(1, 2, 8, 16, 0).map(ScanMapper::band))
    }

    @Test fun ssidCleanup() {
        assertEquals("Home", ScanMapper.cleanSsid("\"Home\""))
        assertEquals("", ScanMapper.cleanSsid("<unknown ssid>"))
        assertEquals("", ScanMapper.cleanSsid(null))
        assertEquals("a\"b", ScanMapper.cleanSsid("a\"b"))
        assertEquals("\"", ScanMapper.cleanSsid("\""))
        assertEquals("ab", ScanMapper.cleanSsid("a\u0000b"))
        assertEquals("咖啡廳 Free", ScanMapper.cleanSsid("咖啡廳 Free"))
    }

    @Test fun macCleanup() {
        assertEquals("aa:bb:cc:00:00:01", ScanMapper.cleanMac(" AA:BB:CC:00:00:01 "))
        assertNull(ScanMapper.cleanMac("00:00:00:00:00:00"))
        assertNull(ScanMapper.cleanMac("02:00:00:00:00:00"))
        assertNull(ScanMapper.cleanMac(""))
        assertNull(ScanMapper.cleanMac(null))
    }

    @Test fun fullMapping() {
        val a = ScanMapper.toAccessPoint(raw())!!
        assertEquals("aa:bb:cc:00:00:01", a.bssid)
        assertEquals("Home", a.ssid)
        assertEquals(Band.GHZ_5, a.band)
        assertEquals(36, a.channel)
        assertEquals(ChannelWidth.MHZ_80, a.width)
        assertEquals(WifiGeneration.WIFI_6, a.generation)
        assertEquals(setOf(Security.WPA2_PERSONAL), a.security)
        assertNull(a.mldAddress)
    }

    @Test fun rejectsUnusableEntries() {
        assertNull(ScanMapper.toAccessPoint(raw(bssid = null)))
        assertNull(ScanMapper.toAccessPoint(raw(bssid = "02:00:00:00:00:00")))
        assertNull(ScanMapper.toAccessPoint(raw(freq = 0)))
        assertNull(ScanMapper.toAccessPoint(raw(freq = -1)))
    }

    @Test fun clampsRssiAndToleratesNulls() {
        assertEquals(0, ScanMapper.toAccessPoint(raw(level = 50))!!.rssi)
        assertEquals(-127, ScanMapper.toAccessPoint(raw(level = Int.MIN_VALUE))!!.rssi)
        val a = ScanMapper.toAccessPoint(raw(ssid = null, caps = null))!!
        assertEquals("", a.ssid)
        assertEquals(setOf(Security.OPEN), a.security)
    }

    @Test fun mloFieldsOnlyWhenMldPresent() {
        val links = listOf(RawMloLink(0, 1, 6, "AA:BB:CC:00:00:02", -45), RawMloLink(1, 8, 37, null, 0))
        val a = ScanMapper.toAccessPoint(raw(mld = "AA:BB:CC:00:00:00", linkId = 1, links = links, std = 8))!!
        assertEquals("aa:bb:cc:00:00:00", a.mldAddress)
        assertEquals(1, a.mloLinkId)
        assertEquals(2, a.mloLinks.size)
        assertEquals(Band.GHZ_6, a.mloLinks[1].band)
        assertNull("rssi 0 means unknown", a.mloLinks[1].rssi)
        val b = ScanMapper.toAccessPoint(raw(mld = "00:00:00:00:00:00", linkId = 1, links = links))!!
        assertNull(b.mldAddress)
        assertNull(b.mloLinkId)
        assertTrue(b.mloLinks.isEmpty())
    }

    @Test fun dedupesKeepingStrongest() {
        val aps = ScanMapper.mapAll(listOf(raw(level = -70), raw(bssid = "aa:bb:cc:00:00:01", level = -40), raw(bssid = "aa:bb:cc:00:00:09")))
        assertEquals(2, aps.size)
        assertEquals(-40, aps.first { it.bssid == "aa:bb:cc:00:00:01" }.rssi)
    }
}

class DemoScanSourceTest {
    @Test fun environmentCoversEveryBandWidthAndMlo() {
        val s = DemoScanSource(clock = { 1_000L })
        s.start()
        val snap = s.snapshots.value!!
        val aps = snap.aps
        assertEquals(setOf(Band.GHZ_2_4, Band.GHZ_5, Band.GHZ_6), aps.mapNotNull { it.band }.toSet())
        assertTrue(aps.map { it.width }.containsAll(listOf(ChannelWidth.MHZ_20, ChannelWidth.MHZ_40, ChannelWidth.MHZ_80, ChannelWidth.MHZ_160, ChannelWidth.MHZ_320)))
        assertEquals("invalid BSSID dropped", 0, aps.count { it.ssid == "invalid" })
        val mlo = Mlo.group(aps)
        assertEquals(2, mlo.size)
        assertEquals(3, mlo.first { it.ssid == "Home-BE" }.links.size)
        assertEquals(WifiGeneration.WIFI_7, snap.connection!!.generation)
        assertEquals(2, snap.connection!!.activeLinks.size)
    }

    @Test fun rssiStaysInRangeOverManyScans() {
        val s = DemoScanSource()
        repeat(2_000) {
            assertEquals(ScanRequest.STARTED, s.requestScan())
            assertTrue(s.snapshots.value!!.aps.all { it.rssi in -95..-30 })
        }
    }

    @Test fun hotspotComesAndGoes() {
        val s = DemoScanSource()
        val seen = (1..10).map { s.requestScan(); s.snapshots.value!!.aps.any { it.ssid == "小明的 iPhone" } }
        assertTrue(seen.contains(true) && seen.contains(false))
    }
}
