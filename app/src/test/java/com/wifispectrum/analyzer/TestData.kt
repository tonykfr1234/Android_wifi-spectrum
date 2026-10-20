package com.wifispectrum.analyzer

import com.wifispectrum.analyzer.core.AccessPoint
import com.wifispectrum.analyzer.core.ChannelWidth
import com.wifispectrum.analyzer.core.MloLinkInfo
import com.wifispectrum.analyzer.core.SecurityParser
import com.wifispectrum.analyzer.core.WifiGeneration

fun ap(
    bssid: String = "04:d9:f5:00:00:01",
    ssid: String = "Net",
    rssi: Int = -50,
    freq: Int = 2437,
    width: ChannelWidth = ChannelWidth.MHZ_20,
    cf0: Int = 0,
    cf1: Int = 0,
    gen: WifiGeneration = WifiGeneration.WIFI_6,
    caps: String = "[RSN-PSK-CCMP][ESS]",
    mld: String? = null,
    linkId: Int? = null,
    links: List<MloLinkInfo> = emptyList(),
) = AccessPoint(
    bssid = bssid, ssid = ssid, rssi = rssi, frequencyMhz = freq,
    centerFreq0 = cf0, centerFreq1 = cf1, width = width, generation = gen,
    security = SecurityParser.parse(caps), capabilities = caps,
    mldAddress = mld, mloLinkId = linkId, mloLinks = links,
)
