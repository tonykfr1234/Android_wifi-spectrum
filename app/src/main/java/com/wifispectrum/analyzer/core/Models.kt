package com.wifispectrum.analyzer.core

enum class Band(val label: String, val shortLabel: String) {
    GHZ_2_4("2.4 GHz", "2.4G"),
    GHZ_5("5 GHz", "5G"),
    GHZ_6("6 GHz", "6G"),
}

enum class ChannelWidth(val mhz: Int, val label: String) {
    MHZ_20(20, "20 MHz"),
    MHZ_40(40, "40 MHz"),
    MHZ_80(80, "80 MHz"),
    MHZ_160(160, "160 MHz"),
    MHZ_80_PLUS_80(160, "80+80 MHz"),
    MHZ_320(320, "320 MHz"),
}

enum class WifiGeneration(val label: String, val ieee: String) {
    LEGACY("Wi-Fi 1–3", "802.11a/b/g"),
    WIFI_4("Wi-Fi 4", "802.11n"),
    WIFI_5("Wi-Fi 5", "802.11ac"),
    WIFI_6("Wi-Fi 6", "802.11ax"),
    WIFI_6E("Wi-Fi 6E", "802.11ax"),
    WIFI_7("Wi-Fi 7", "802.11be"),
    WIGIG("WiGig", "802.11ad"),
    UNKNOWN("未知", "—"),
}

enum class Security(val label: String) {
    OPEN("開放"),
    OWE("增強型開放 OWE"),
    WEP("WEP"),
    WPA("WPA"),
    WPA2_PERSONAL("WPA2 個人"),
    WPA3_PERSONAL("WPA3 個人"),
    WPA2_ENTERPRISE("WPA2 企業"),
    WPA3_ENTERPRISE("WPA3 企業"),
    WPA3_ENTERPRISE_192("WPA3 企業 192 位元"),
    WAPI("WAPI"),
}

data class MloLinkInfo(
    val linkId: Int,
    val band: Band?,
    val channel: Int,
    val bssid: String?,
    val rssi: Int?,
)

data class AccessPoint(
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val centerFreq0: Int,
    val centerFreq1: Int,
    val width: ChannelWidth,
    val generation: WifiGeneration,
    val security: Set<Security>,
    val capabilities: String,
    val mldAddress: String? = null,
    val mloLinkId: Int? = null,
    val mloLinks: List<MloLinkInfo> = emptyList(),
) {
    val band: Band? = Channels.bandOf(frequencyMhz)
    val channel: Int = Channels.channelOf(frequencyMhz)
    val spans: List<IntRange> = Channels.occupiedMhz(band, frequencyMhz, width, centerFreq0, centerFreq1)
    val isHidden: Boolean get() = ssid.isEmpty()
    val displaySsid: String get() = ssid.ifEmpty { "（隱藏網路）" }
    val isMlo: Boolean get() = mldAddress != null

    /** Channel number at the middle of the whole occupied block, e.g. 42 for an 80 MHz AP on 36. */
    val centerChannel: Int
        get() = if (width == ChannelWidth.MHZ_80_PLUS_80) channel
        else Channels.channelOf((spans.first().first + spans.first().last) / 2)
}

data class ConnectionInfo(
    val ssid: String?,
    val bssid: String?,
    val rssi: Int,
    val frequencyMhz: Int,
    val linkSpeedMbps: Int,
    val txSpeedMbps: Int?,
    val rxSpeedMbps: Int?,
    val maxTxSpeedMbps: Int?,
    val generation: WifiGeneration,
    val ipAddress: String?,
    val mldAddress: String? = null,
    val activeLinks: List<MloLinkInfo> = emptyList(),
) {
    val band: Band? get() = Channels.bandOf(frequencyMhz)
    val channel: Int get() = Channels.channelOf(frequencyMhz)
}
