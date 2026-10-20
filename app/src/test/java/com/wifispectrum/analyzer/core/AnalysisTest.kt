package com.wifispectrum.analyzer.core

import com.wifispectrum.analyzer.ap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SecurityParserTest {
    private fun p(c: String) = SecurityParser.parse(c)

    @Test fun open() = assertEquals(setOf(Security.OPEN), p("[ESS]"))
    @Test fun empty() = assertEquals(setOf(Security.OPEN), p(""))
    @Test fun owe() = assertEquals(setOf(Security.OWE), p("[RSN-OWE-CCMP][ESS][MFPR]"))
    @Test fun oweTransitionOnOpenBss() = assertEquals(setOf(Security.OPEN), p("[OWE_TRANSITION][ESS]"))
    @Test fun wep() = assertEquals(setOf(Security.WEP), p("[WEP][ESS]"))
    @Test fun wpa2Psk() = assertEquals(setOf(Security.WPA2_PERSONAL), p("[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]"))
    @Test fun wpaMixed() = assertEquals(setOf(Security.WPA, Security.WPA2_PERSONAL), p("[WPA-PSK-CCMP+TKIP][WPA2-PSK-CCMP+TKIP][ESS]"))
    @Test fun wpa3Transition() = assertEquals(setOf(Security.WPA2_PERSONAL, Security.WPA3_PERSONAL), p("[RSN-PSK+SAE-CCMP][ESS][MFPC]"))
    @Test fun wpa3Only() = assertEquals(setOf(Security.WPA3_PERSONAL), p("[RSN-SAE-CCMP][ESS][MFPR]"))
    @Test fun wpa3Wifi7ExtKey() = assertEquals(setOf(Security.WPA3_PERSONAL), p("[RSN-SAE_EXT_KEY-GCMP-256][ESS][MFPR]"))
    @Test fun fastTransition() = assertEquals(setOf(Security.WPA2_PERSONAL, Security.WPA3_PERSONAL), p("[RSN-FT/PSK+FT/SAE-CCMP][ESS]"))
    @Test fun pskSha256() = assertEquals(setOf(Security.WPA2_PERSONAL), p("[RSN-PSK-SHA256-CCMP][ESS]"))
    @Test fun enterprise() = assertEquals(setOf(Security.WPA2_ENTERPRISE), p("[WPA2-EAP-CCMP][RSN-EAP-CCMP][ESS]"))
    @Test fun enterpriseWpa3() = assertEquals(setOf(Security.WPA3_ENTERPRISE), p("[RSN-EAP-SHA256-CCMP][ESS][MFPR]"))
    @Test fun suiteB() = assertEquals(setOf(Security.WPA3_ENTERPRISE_192), p("[RSN-EAP_SUITE_B_192-GCMP-256][ESS][MFPR]"))
    @Test fun lowercaseInput() = assertEquals(setOf(Security.WPA2_PERSONAL), p("[rsn-psk-ccmp]"))
    @Test fun malformedDoesNotThrow() {
        for (s in listOf("[", "]", "[[]]", "[RSN-", "[RSN-]", "[WPA-+++]", "\u0000[RSN-PSK", "[RSN-PSK-CCMP")) p(s)
    }
    @Test fun summaryShowsStrongestFirst() =
        assertEquals("WPA3 個人 / WPA2 個人", SecurityParser.summary(p("[RSN-PSK+SAE-CCMP]")))
}

class GenerationsTest {
    @Test fun mapping() {
        assertEquals(WifiGeneration.WIFI_7, Generations.fromAndroidStandard(8, Band.GHZ_6))
        assertEquals(WifiGeneration.WIFI_6E, Generations.fromAndroidStandard(6, Band.GHZ_6))
        assertEquals(WifiGeneration.WIFI_6, Generations.fromAndroidStandard(6, Band.GHZ_5))
        assertEquals(WifiGeneration.WIFI_5, Generations.fromAndroidStandard(5, Band.GHZ_5))
        assertEquals(WifiGeneration.WIFI_4, Generations.fromAndroidStandard(4, Band.GHZ_2_4))
        assertEquals(WifiGeneration.LEGACY, Generations.fromAndroidStandard(1, Band.GHZ_2_4))
        assertEquals(WifiGeneration.UNKNOWN, Generations.fromAndroidStandard(0, null))
        assertEquals(WifiGeneration.UNKNOWN, Generations.fromAndroidStandard(99, Band.GHZ_5))
    }
}

class SignalTest {
    @Test fun barsAndQuality() {
        assertEquals(4, Signal.bars(-30)); assertEquals("極佳", Signal.quality(-50))
        assertEquals(3, Signal.bars(-51)); assertEquals("良好", Signal.quality(-60))
        assertEquals(2, Signal.bars(-70)); assertEquals(1, Signal.bars(-80))
        assertEquals(0, Signal.bars(-81)); assertEquals("很弱", Signal.quality(-127))
    }

    @Test fun distance() {
        assertTrue(Signal.estimateDistanceMeters(-40, 2437) < Signal.estimateDistanceMeters(-70, 2437))
        assertTrue(Signal.estimateDistanceMeters(-60, 5180) < Signal.estimateDistanceMeters(-60, 2437))
        assertTrue(Signal.estimateDistanceMeters(-60, 0).isNaN())
    }
}

class MacTest {
    @Test fun locallyAdministered() {
        assertTrue(Mac.isLocallyAdministered("02:00:00:00:00:00"))
        assertTrue(Mac.isLocallyAdministered("f2:8a:6c:19:d0:3e"))
        assertFalse(Mac.isLocallyAdministered("04:d9:f5:3a:11:20"))
        assertFalse(Mac.isLocallyAdministered(""))
        assertFalse(Mac.isLocallyAdministered("zz:zz"))
    }

    @Test fun oui() {
        assertEquals(0x04D9F5, Mac.ouiKey("04:d9:f5:3a:11:20"))
        assertNull(Mac.ouiKey("04:d9"))
    }
}

class VendorDbTest {
    private val db = File("src/main/assets/oui.tsv.gz").inputStream().use { VendorDb.parseGzip(it) }

    @Test fun loadsWholeRegistry() = assertTrue("size ${db.size}", db.size > 30_000)
    @Test fun knownVendor() = assertTrue(db.lookup("00:00:0c:12:34:56")!!.contains("Cisco"))
    @Test fun randomizedMacHasNoVendor() = assertNull(db.lookup("02:00:0c:12:34:56"))
    @Test fun garbage() = assertNull(db.lookup("xyz"))
}

class MloTest {
    @Test fun groupsLinksByMldCaseInsensitive() {
        val aps = listOf(
            ap(bssid = "a0:00:00:00:00:03", ssid = "", freq = 6115, rssi = -60, mld = "AA:BB:CC:00:00:01", linkId = 2),
            ap(bssid = "a0:00:00:00:00:01", ssid = "Home7", freq = 2437, rssi = -45, mld = "aa:bb:cc:00:00:01", linkId = 0,
                links = listOf(MloLinkInfo(1, Band.GHZ_5, 36, null, null), MloLinkInfo(0, Band.GHZ_2_4, 6, null, null))),
            ap(bssid = "a0:00:00:00:00:02", ssid = "Home7", freq = 5180, rssi = -50, mld = "aa:bb:cc:00:00:01", linkId = 1,
                links = listOf(MloLinkInfo(1, Band.GHZ_5, 36, null, null))),
            ap(bssid = "b0:00:00:00:00:01", ssid = "Other", freq = 2412),
        )
        val g = Mlo.group(aps)
        assertEquals(1, g.size)
        val d = g.single()
        assertEquals("Home7", d.ssid)
        assertEquals(listOf(Band.GHZ_2_4, Band.GHZ_5, Band.GHZ_6), d.bands)
        assertEquals(-45, d.bestRssi)
        assertEquals(listOf(0, 1), d.advertisedLinks.map { it.linkId })
    }

    @Test fun noMloNoGroups() = assertTrue(Mlo.group(listOf(ap())).isEmpty())
}

class ChannelRatingTest {
    @Test fun emptyBandIsAllFiveStars() {
        for (b in Band.entries) assertTrue(ChannelRating.score(b, emptyList()).all { it.stars == 5 })
    }

    @Test fun strongCoChannelNeighbourIsOneStar() {
        val s = ChannelRating.score(Band.GHZ_2_4, listOf(ap(freq = 2437, rssi = -40)))
        assertEquals(1, s.first { it.channel == 6 }.stars)
        assertEquals(5, s.first { it.channel == 1 }.stars)
        assertEquals(1, s.first { it.channel == 6 }.apCount)
    }

    @Test fun weakNeighbourBarelyMatters() {
        val s = ChannelRating.score(Band.GHZ_2_4, listOf(ap(freq = 2437, rssi = -88)))
        assertTrue(s.first { it.channel == 6 }.stars >= 4)
    }

    @Test fun recommends11When1And6Busy() {
        val aps = listOf(ap(freq = 2412, rssi = -50), ap(freq = 2437, rssi = -55))
        assertEquals(11, ChannelRating.recommend(Band.GHZ_2_4, aps).first().channel)
        assertTrue(ChannelRating.recommend(Band.GHZ_2_4, aps).all { it.channel in setOf(1, 6, 11) })
    }

    @Test fun wideChannelAffectsEveryCoveredChannel() {
        val s = ChannelRating.score(Band.GHZ_5, listOf(ap(freq = 5180, width = com.wifispectrum.analyzer.core.ChannelWidth.MHZ_40, rssi = -50)))
        assertEquals(1, s.first { it.channel == 36 }.apCount)
        assertEquals(1, s.first { it.channel == 40 }.apCount)
        assertEquals(0, s.first { it.channel == 44 }.apCount)
    }

    @Test fun sixGhzPrefersPsc() {
        val r = ChannelRating.recommend(Band.GHZ_6, emptyList())
        assertEquals(listOf(5, 21, 37), r.map { it.channel })
    }

    @Test fun fiveGhzPrefersNonDfsOnTies() {
        assertEquals(listOf(36, 40, 44), ChannelRating.recommend(Band.GHZ_5, emptyList()).map { it.channel })
        val busyLow = listOf(ap(freq = 5180, width = com.wifispectrum.analyzer.core.ChannelWidth.MHZ_80, rssi = -50))
        assertEquals(listOf(149, 153, 157), ChannelRating.recommend(Band.GHZ_5, busyLow).map { it.channel })
    }

    @Test fun fiveGhzStillPicksDfsWhenItIsClearlyQuieter() {
        val aps = (listOf(5180, 5200, 5220, 5240, 5745, 5765, 5785, 5805, 5825)).map { ap(bssid = "b$it", freq = it, rssi = -45) }
        val best = ChannelRating.recommend(Band.GHZ_5, aps).first()
        assertTrue(best.channel in 52..144)
    }

    @Test fun otherBandsIgnored() {
        val s = ChannelRating.score(Band.GHZ_5, listOf(ap(freq = 2437, rssi = -30)))
        assertTrue(s.all { it.apCount == 0 })
    }
}

class SignalHistoryTest {
    @Test fun keepsAtMostMaxSamples() {
        val h = SignalHistory(maxSamples = 5)
        repeat(20) { h.record(listOf(ap(rssi = -40 - it))) }
        val s = h.samples(ap().bssid)
        assertEquals(5, s.size)
        assertEquals(-59, s.last().rssi)
    }

    @Test fun evictsVanishedAps() {
        val h = SignalHistory(evictAfterMissedScans = 3)
        h.record(listOf(ap(bssid = "x")))
        repeat(3) { h.record(emptyList()) }
        assertEquals(1, h.trackedCount)
        h.record(emptyList())
        assertEquals(0, h.trackedCount)
        assertTrue(h.samples("x").isEmpty())
    }

    @Test fun clear() {
        val h = SignalHistory()
        h.record(listOf(ap()))
        h.clear()
        assertEquals(0, h.trackedCount)
        assertEquals(0L, h.scanCount)
    }
}

class ApFilterTest {
    private val aps = listOf(
        ap(bssid = "01", ssid = "Alpha", freq = 2412, rssi = -70),
        ap(bssid = "02", ssid = "beta", freq = 5180, rssi = -40, width = com.wifispectrum.analyzer.core.ChannelWidth.MHZ_80),
        ap(bssid = "03", ssid = "", freq = 6115, rssi = -60, width = com.wifispectrum.analyzer.core.ChannelWidth.MHZ_320, gen = WifiGeneration.WIFI_7),
    )

    @Test fun defaultSortsBySignal() = assertEquals(listOf("02", "03", "01"), ApFilter().apply(aps).map { it.bssid })
    @Test fun bandFilter() = assertEquals(listOf("01"), ApFilter(bands = setOf(Band.GHZ_2_4)).apply(aps).map { it.bssid })
    @Test fun minWidth() = assertEquals(listOf("02", "03"), ApFilter(minWidthMhz = 80).apply(aps).map { it.bssid })
    @Test fun wifi7() = assertEquals(listOf("03"), ApFilter(wifi7Only = true).apply(aps).map { it.bssid })
    @Test fun hidden() = assertEquals(listOf("02", "01"), ApFilter(hideHidden = true).apply(aps).map { it.bssid })
    @Test fun queryIsCaseInsensitive() = assertEquals(listOf("02"), ApFilter(query = " BETA ").apply(aps).map { it.bssid })
    @Test fun queryMatchesBssid() = assertEquals(listOf("03"), ApFilter(query = "03").apply(aps).map { it.bssid })
    @Test fun sortBySsidPutsHiddenLast() = assertEquals(listOf("01", "02", "03"), ApFilter(sort = SortOrder.SSID).apply(aps).map { it.bssid })
    @Test fun sortByChannel() = assertEquals(listOf("01", "02", "03"), ApFilter(sort = SortOrder.CHANNEL).apply(aps).map { it.bssid })
    @Test fun isDefault() {
        assertTrue(ApFilter(sort = SortOrder.SSID).isDefault)
        assertFalse(ApFilter(wifi7Only = true).isDefault)
    }
}
