package com.wifispectrum.analyzer.core

import com.wifispectrum.analyzer.ap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelsTest {

    @Test fun `2_4 GHz channels`() {
        assertEquals(1, Channels.channelOf(2412))
        assertEquals(6, Channels.channelOf(2437))
        assertEquals(11, Channels.channelOf(2462))
        assertEquals(13, Channels.channelOf(2472))
        assertEquals(14, Channels.channelOf(2484))
        assertEquals(Band.GHZ_2_4, Channels.bandOf(2412))
    }

    @Test fun `5 GHz channels including 4_9 GHz and UNII-4`() {
        assertEquals(36, Channels.channelOf(5180))
        assertEquals(165, Channels.channelOf(5825))
        assertEquals(177, Channels.channelOf(5885))
        assertEquals(184, Channels.channelOf(4920))
        assertEquals(Band.GHZ_5, Channels.bandOf(5885))
        assertEquals(Band.GHZ_5, Channels.bandOf(4920))
    }

    @Test fun `6 GHz channels including channel 2`() {
        assertEquals(2, Channels.channelOf(5935))
        assertEquals(1, Channels.channelOf(5955))
        assertEquals(33, Channels.channelOf(6115))
        assertEquals(233, Channels.channelOf(7115))
        assertEquals(Band.GHZ_6, Channels.bandOf(5935))
        assertEquals(Band.GHZ_6, Channels.bandOf(7115))
    }

    @Test fun `off-raster and out-of-band frequencies`() {
        assertEquals(-1, Channels.channelOf(2413))
        assertEquals(-1, Channels.channelOf(0))
        assertEquals(-1, Channels.channelOf(-5))
        assertEquals(-1, Channels.channelOf(5940))
        assertNull(Channels.bandOf(60480))
        assertNull(Channels.bandOf(0))
    }

    @Test fun `frequency and channel round-trip for every candidate`() {
        for (band in Band.entries) for (ch in Channels.candidateChannels(band)) {
            val f = Channels.frequencyOf(band, ch)
            assertEquals("$band ch$ch", ch, Channels.channelOf(f))
            assertEquals("$band ch$ch", band, Channels.bandOf(f))
        }
        assertEquals(2, Channels.channelOf(Channels.frequencyOf(Band.GHZ_6, 2)))
        assertEquals(14, Channels.channelOf(Channels.frequencyOf(Band.GHZ_2_4, 14)))
    }

    @Test fun `preferred scanning channels`() {
        val psc = (1..233).filter { Channels.isPreferredScanningChannel(it) }
        assertEquals(15, psc.size)
        assertEquals(5, psc.first())
        assertEquals(229, psc.last())
        assertFalse(Channels.isPreferredScanningChannel(1))
    }

    @Test fun `20 MHz span`() {
        assertEquals(listOf(2427..2447), ap(freq = 2437).spans)
    }

    @Test fun `2_4 GHz 40 MHz with and without center frequency`() {
        assertEquals(listOf(2412..2452), ap(freq = 2422, width = ChannelWidth.MHZ_40, cf0 = 2432).spans)
        assertEquals(listOf(2402..2442), ap(freq = 2412, width = ChannelWidth.MHZ_40).spans)
        assertEquals(listOf(2432..2472), ap(freq = 2462, width = ChannelWidth.MHZ_40).spans)
    }

    @Test fun `5 GHz 80 and 160 MHz`() {
        assertEquals(listOf(5170..5250), ap(freq = 5180, width = ChannelWidth.MHZ_80, cf0 = 5210).spans)
        assertEquals(listOf(5170..5250), ap(freq = 5220, width = ChannelWidth.MHZ_80).spans)
        assertEquals(listOf(5170..5330), ap(freq = 5180, width = ChannelWidth.MHZ_160, cf0 = 5250).spans)
        assertEquals(42, ap(freq = 5180, width = ChannelWidth.MHZ_80, cf0 = 5210).centerChannel)
    }

    @Test fun `160 MHz reported VHT-style with the full centre in centerFreq1`() {
        val a = ap(freq = 5180, width = ChannelWidth.MHZ_160, cf0 = 5210, cf1 = 5250)
        assertEquals(listOf(5170..5330), a.spans)
        assertEquals(50, a.centerChannel)
    }

    @Test fun `80+80 MHz has two segments`() {
        val a = ap(freq = 5180, width = ChannelWidth.MHZ_80_PLUS_80, cf0 = 5210, cf1 = 5775)
        assertEquals(listOf(5170..5250, 5735..5815), a.spans)
    }

    @Test fun `80+80 without a usable second segment falls back to one block`() {
        val a = ap(freq = 5180, width = ChannelWidth.MHZ_80_PLUS_80, cf0 = 5210, cf1 = 0)
        assertEquals(listOf(5170..5250), a.spans)
    }

    @Test fun `6 GHz 320 MHz`() {
        assertEquals(listOf(5945..6265), ap(freq = 5955, width = ChannelWidth.MHZ_320, cf0 = 6105).spans)
        assertEquals(listOf(5945..6265), ap(freq = 6115, width = ChannelWidth.MHZ_320).spans)
        assertEquals(31, ap(freq = 5955, width = ChannelWidth.MHZ_320, cf0 = 6105).centerChannel)
    }

    @Test fun `6 GHz 160 MHz inferred block`() {
        assertEquals(listOf(6105..6265), ap(freq = 6135, width = ChannelWidth.MHZ_160).spans)
    }

    @Test fun `garbage centre frequency falls back to the channel plan`() {
        assertEquals(listOf(5170..5250), ap(freq = 5180, width = ChannelWidth.MHZ_80, cf0 = 9999).spans)
        assertEquals(listOf(5170..5250), ap(freq = 5180, width = ChannelWidth.MHZ_80, cf0 = -1).spans)
    }

    @Test fun `out-of-band AP does not crash`() {
        val a = ap(freq = 60480, width = ChannelWidth.MHZ_160)
        assertEquals(listOf(60470..60490), a.spans)
        assertNull(a.band)
        assertEquals(-1, a.channel)
    }

    @Test fun `every inferred span contains its primary channel and has the right width`() {
        val widths = mapOf(
            Band.GHZ_2_4 to listOf(ChannelWidth.MHZ_20, ChannelWidth.MHZ_40),
            Band.GHZ_5 to listOf(ChannelWidth.MHZ_20, ChannelWidth.MHZ_40, ChannelWidth.MHZ_80, ChannelWidth.MHZ_160),
            Band.GHZ_6 to ChannelWidth.entries.filter { it != ChannelWidth.MHZ_80_PLUS_80 },
        )
        for ((band, ws) in widths) for (ch in Channels.candidateChannels(band)) for (w in ws) {
            val f = Channels.frequencyOf(band, ch)
            val span = ap(freq = f, width = w).spans.single()
            assertTrue("$band ch$ch $w $span", f - 10 >= span.first && f + 10 <= span.last)
            assertEquals("$band ch$ch $w", w.mhz, span.last - span.first)
        }
    }
}
