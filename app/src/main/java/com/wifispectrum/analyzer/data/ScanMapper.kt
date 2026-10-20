package com.wifispectrum.analyzer.data

import com.wifispectrum.analyzer.core.AccessPoint
import com.wifispectrum.analyzer.core.Band
import com.wifispectrum.analyzer.core.ChannelWidth
import com.wifispectrum.analyzer.core.Channels
import com.wifispectrum.analyzer.core.Generations
import com.wifispectrum.analyzer.core.Mac
import com.wifispectrum.analyzer.core.MloLinkInfo
import com.wifispectrum.analyzer.core.SecurityParser

/** Plain copy of the ScanResult fields we use, so the mapping can be unit-tested without a device. */
data class RawScan(
    val bssid: String?,
    val ssid: String?,
    val level: Int,
    val frequency: Int,
    val centerFreq0: Int,
    val centerFreq1: Int,
    val channelWidth: Int,
    val wifiStandard: Int,
    val capabilities: String?,
    val mldAddress: String? = null,
    val mloLinkId: Int = -1,
    val mloLinks: List<RawMloLink> = emptyList(),
)

data class RawMloLink(val linkId: Int, val band: Int, val channel: Int, val apMac: String?, val rssi: Int)

object ScanMapper {
    private const val NO_MAC = "00:00:00:00:00:00"
    private const val HIDDEN_MAC = "02:00:00:00:00:00"

    fun width(androidWidth: Int): ChannelWidth = when (androidWidth) {
        1 -> ChannelWidth.MHZ_40
        2 -> ChannelWidth.MHZ_80
        3 -> ChannelWidth.MHZ_160
        4 -> ChannelWidth.MHZ_80_PLUS_80
        5 -> ChannelWidth.MHZ_320
        else -> ChannelWidth.MHZ_20
    }

    fun band(wifiScannerBand: Int): Band? = when (wifiScannerBand) {
        1 -> Band.GHZ_2_4
        2 -> Band.GHZ_5
        8 -> Band.GHZ_6
        else -> null
    }

    fun cleanSsid(raw: String?): String {
        if (raw == null || raw == "<unknown ssid>") return ""
        val s = if (raw.length >= 2 && raw.startsWith('"') && raw.endsWith('"')) raw.substring(1, raw.length - 1) else raw
        return s.filter { it >= ' ' }
    }

    fun cleanMac(raw: String?): String? {
        val m = raw?.let(Mac::normalize) ?: return null
        return if (m.isEmpty() || m == NO_MAC || m == HIDDEN_MAC) null else m
    }

    fun toAccessPoint(r: RawScan): AccessPoint? {
        val bssid = cleanMac(r.bssid) ?: return null
        if (r.frequency <= 0) return null
        val band = Channels.bandOf(r.frequency)
        val caps = r.capabilities.orEmpty()
        val mld = cleanMac(r.mldAddress)
        return AccessPoint(
            bssid = bssid,
            ssid = cleanSsid(r.ssid),
            rssi = r.level.coerceIn(-127, 0),
            frequencyMhz = r.frequency,
            centerFreq0 = r.centerFreq0,
            centerFreq1 = r.centerFreq1,
            width = width(r.channelWidth),
            generation = Generations.fromAndroidStandard(r.wifiStandard, band),
            security = SecurityParser.parse(caps),
            capabilities = caps,
            mldAddress = mld,
            mloLinkId = r.mloLinkId.takeIf { mld != null && it >= 0 },
            mloLinks = if (mld == null) emptyList() else r.mloLinks.map {
                MloLinkInfo(it.linkId, band(it.band), it.channel, cleanMac(it.apMac), it.rssi.takeIf { v -> v in -127..-1 })
            },
        )
    }

    /** Some drivers report the same BSSID twice in one scan; keep the strongest reading. */
    fun mapAll(raw: List<RawScan>): List<AccessPoint> = raw
        .mapNotNull(::toAccessPoint)
        .groupBy { it.bssid }
        .map { (_, v) -> v.maxBy { it.rssi } }
}
