package com.wifispectrum.analyzer.core

import kotlin.math.abs

object Channels {

    fun bandOf(frequencyMhz: Int): Band? = when (frequencyMhz) {
        in 2401..2495 -> Band.GHZ_2_4
        in 4900..5900 -> Band.GHZ_5
        in 5925..7125 -> Band.GHZ_6
        else -> null
    }

    /** Primary 20 MHz channel number, or -1 when the frequency is not on the channel raster. */
    fun channelOf(frequencyMhz: Int): Int {
        val f = frequencyMhz
        return when {
            f == 2484 -> 14
            f in 2412..2472 && (f - 2407) % 5 == 0 -> (f - 2407) / 5
            f in 4910..4980 && f % 5 == 0 -> (f - 4000) / 5
            f in 5000..5895 && f % 5 == 0 -> (f - 5000) / 5
            f == 5935 -> 2
            f in 5955..7115 && (f - 5950) % 5 == 0 -> (f - 5950) / 5
            else -> -1
        }
    }

    fun frequencyOf(band: Band, channel: Int): Int = when (band) {
        Band.GHZ_2_4 -> if (channel == 14) 2484 else 2407 + 5 * channel
        Band.GHZ_5 -> if (channel >= 182) 4000 + 5 * channel else 5000 + 5 * channel
        Band.GHZ_6 -> if (channel == 2) 5935 else 5950 + 5 * channel
    }

    /** 20 MHz channels a radio may be tuned to, in display order. */
    fun candidateChannels(band: Band): List<Int> = when (band) {
        Band.GHZ_2_4 -> (1..13).toList()
        Band.GHZ_5 -> (36..64 step 4) + (100..144 step 4) + (149..177 step 4)
        Band.GHZ_6 -> (1..233 step 4).toList()
    }

    /** Preferred Scanning Channels: 6 GHz clients probe these first, so APs should prefer them. */
    fun isPreferredScanningChannel(channel: Int): Boolean = channel in 5..229 && channel % 16 == 5

    fun displayRangeMhz(band: Band): IntRange = when (band) {
        Band.GHZ_2_4 -> 2397..2477
        Band.GHZ_5 -> 5160..5900
        Band.GHZ_6 -> 5935..7125
    }

    /**
     * Frequency blocks the AP transmits on. Drivers report centerFreq0/1 inconsistently
     * (missing, or the 160 MHz centre in centerFreq1), so bad values fall back to the
     * standard channel plan.
     */
    fun occupiedMhz(
        band: Band?,
        frequencyMhz: Int,
        width: ChannelWidth,
        centerFreq0: Int,
        centerFreq1: Int,
    ): List<IntRange> {
        val f = frequencyMhz
        if (band == null || width == ChannelWidth.MHZ_20) return listOf(f - 10..f + 10)
        if (width == ChannelWidth.MHZ_80_PLUS_80) {
            val a = centerFreq0.takeIf { fits(it, f, 40) } ?: inferCenter(band, f, 80)
            val b = centerFreq1.takeIf { it > 0 && abs(it - a) >= 80 }
            return if (b == null) listOf(a - 40..a + 40)
            else listOf(a - 40..a + 40, b - 40..b + 40).sortedBy { it.first }
        }
        val half = width.mhz / 2
        val center = when {
            width == ChannelWidth.MHZ_160 && centerFreq1 > 0 &&
                abs(centerFreq1 - centerFreq0) == 40 && fits(centerFreq1, f, half) -> centerFreq1
            fits(centerFreq0, f, half) -> centerFreq0
            else -> inferCenter(band, f, width.mhz)
        }
        return listOf(center - half..center + half)
    }

    private fun fits(center: Int, primary: Int, half: Int) =
        center > 0 && abs(center - primary) <= half - 10

    internal fun inferCenter(band: Band, primary: Int, widthMhz: Int): Int {
        val ch = channelOf(primary)
        if (ch < 0 || widthMhz <= 20) return primary
        val k = widthMhz / 20
        return when (band) {
            Band.GHZ_2_4 -> if (ch <= 7) primary + 10 else primary - 10
            Band.GHZ_5 -> {
                val base = if (ch >= 149) 149 else 36
                if (ch < base || ch >= 182) return primary
                val start = base + 4 * k * ((ch - base) / 4 / k)
                frequencyOf(band, start + 2 * (k - 1))
            }
            Band.GHZ_6 -> {
                if (ch == 2) return primary
                val start = 1 + 4 * k * ((ch - 1) / 4 / k)
                frequencyOf(band, start + 2 * (k - 1))
            }
        }
    }
}
