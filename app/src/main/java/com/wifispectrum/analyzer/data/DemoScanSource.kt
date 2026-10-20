package com.wifispectrum.analyzer.data

import com.wifispectrum.analyzer.core.ConnectionInfo
import com.wifispectrum.analyzer.core.Band
import com.wifispectrum.analyzer.core.MloLinkInfo
import com.wifispectrum.analyzer.core.WifiGeneration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * A realistic Taipei apartment: a Wi-Fi 7 router with three MLO links, crowded 2.4 GHz,
 * 80/160 MHz neighbours on 5 GHz and 160/320 MHz on 6 GHz. Lets the app be explored
 * (and reviewed) without nearby hardware.
 */
class DemoScanSource(
    seed: Int = 7,
    private val clock: () -> Long = System::currentTimeMillis,
) : ScanSource {

    private val random = Random(seed)
    private val state = MutableStateFlow<ScanSnapshot?>(null)
    private val drift = HashMap<String, Double>()
    private var scans = 0
    private var lastTimestamp = Long.MIN_VALUE

    override val snapshots: StateFlow<ScanSnapshot?> = state
    override val isDemo = true
    override fun start() { if (state.value == null) emit() }
    override fun stop() = Unit
    override fun requestScan(): ScanRequest { emit(); return ScanRequest.STARTED }

    private fun emit() {
        scans++
        val raws = ENVIRONMENT.mapNotNull { base ->
            if (base.ssid == "小明的 iPhone" && scans % 5 == 3) return@mapNotNull null
            val d = (drift[base.bssid!!] ?: 0.0) * 0.8 + random.nextDouble(-1.6, 1.6)
            drift[base.bssid] = d
            base.copy(level = (base.level + d).toInt().coerceIn(-95, -30))
        }
        val aps = ScanMapper.mapAll(raws)
        lastTimestamp = maxOf(clock(), lastTimestamp + 1)
        state.value = ScanSnapshot(aps, connection(), lastTimestamp, fresh = true)
    }

    private fun connection(): ConnectionInfo {
        val wiggle = random.nextInt(-2, 3)
        return ConnectionInfo(
            ssid = "Home-BE", bssid = "04:d9:f5:6e:20:02", rssi = -47 + wiggle, frequencyMhz = 5180,
            linkSpeedMbps = 2882, txSpeedMbps = 2882, rxSpeedMbps = 2401, maxTxSpeedMbps = 5764,
            generation = WifiGeneration.WIFI_7, ipAddress = "192.168.50.123",
            mldAddress = "06:d9:f5:6e:20:00",
            activeLinks = listOf(
                MloLinkInfo(1, Band.GHZ_5, 36, "04:d9:f5:6e:20:02", -47 + wiggle),
                MloLinkInfo(2, Band.GHZ_6, 37, "04:d9:f5:6e:20:03", -54 + wiggle),
            ),
        )
    }

    companion object {
        private const val SAE7 = "[RSN-SAE+SAE_EXT_KEY-CCMP+GCMP-256][ESS][MFPR]"
        private const val WPA2 = "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]"
        private const val MIXED = "[RSN-PSK+SAE-CCMP][ESS][MFPC]"

        private val HOME_LINKS = listOf(
            RawMloLink(0, 1, 6, "04:d9:f5:6e:20:01", -44),
            RawMloLink(1, 2, 36, "04:d9:f5:6e:20:02", -48),
            RawMloLink(2, 8, 37, "04:d9:f5:6e:20:03", -55),
        )
        private const val HOME_MLD = "06:d9:f5:6e:20:00"

        private fun raw(
            bssid: String, ssid: String, level: Int, freq: Int, width: Int, std: Int, caps: String,
            cf0: Int = 0, cf1: Int = 0, mld: String? = null, linkId: Int = -1, links: List<RawMloLink> = emptyList(),
        ) = RawScan(bssid, ssid, level, freq, cf0, cf1, width, std, caps, mld, linkId, links)

        val ENVIRONMENT: List<RawScan> = listOf(
            // Our Wi-Fi 7 router (ASUSTek OUI 04:D9:F5), one MLD, three affiliated links.
            raw("04:d9:f5:6e:20:01", "Home-BE", -44, 2437, 0, 8, SAE7, mld = HOME_MLD, linkId = 0, links = HOME_LINKS),
            raw("04:d9:f5:6e:20:02", "Home-BE", -48, 5180, 3, 8, SAE7, cf0 = 5250, mld = HOME_MLD, linkId = 1, links = HOME_LINKS),
            raw("04:d9:f5:6e:20:03", "Home-BE", -55, 6135, 5, 8, SAE7, cf0 = 6105, mld = HOME_MLD, linkId = 2, links = HOME_LINKS),
            raw("04:d9:f5:6e:20:09", "Home-BE-IoT", -45, 2437, 0, 6, WPA2),
            // 2.4 GHz neighbours.
            raw("00:12:bf:41:8f:3a", "CHT-Wi-Fi-8F3A", -71, 2412, 0, 4, WPA2),
            raw("00:0a:eb:22:91:07", "TP-Link_9107", -78, 2412, 1, 5, WPA2, cf0 = 2422),
            raw("00:0c:6e:88:01:10", "ASUS_88", -66, 2437, 1, 6, MIXED, cf0 = 2447),
            raw("00:9e:c8:5a:21:44", "Xiaomi_5A21", -74, 2462, 0, 6, WPA2),
            raw("00:ad:24:7c:10:02", "dlink-7C10", -84, 2422, 0, 4, WPA2),
            raw("00:01:e6:3b:00:11", "DIRECT-3B-HP LaserJet", -80, 2462, 0, 4, WPA2),
            raw("00:13:49:0f:aa:01", "", -86, 2452, 0, 4, WPA2),
            raw("00:00:00:00:00:00", "invalid", -60, 2437, 0, 4, WPA2),
            // 5 GHz neighbours.
            raw("00:12:bf:41:8f:3b", "CHT-Wi-Fi-8F3A-5G", -76, 5745, 2, 5, WPA2, cf0 = 5775),
            raw("00:0c:6e:88:01:11", "ASUS_88_5G", -69, 5260, 2, 6, MIXED, cf0 = 5290),
            raw("00:09:5b:c0:16:05", "NETGEAR-5G", -80, 5500, 3, 6, WPA2, cf0 = 5570),
            raw("00:15:6d:ab:00:44", "Office-AX", -82, 5220, 2, 6, "[RSN-EAP-CCMP][ESS]", cf0 = 5210),
            raw("8a:2b:3c:4d:5e:6f", "小明的 iPhone", -70, 5765, 2, 6, "[RSN-SAE-CCMP][ESS][MFPR]", cf0 = 5775),
            // 6 GHz neighbours, including a second MLO router.
            raw("00:0c:6e:88:01:12", "ASUS_88_6G", -79, 6455, 3, 6, "[RSN-SAE-CCMP][ESS][MFPR]", cf0 = 6505),
            raw("00:0a:eb:99:70:01", "Neighbor-BE", -77, 5745, 2, 8, SAE7, cf0 = 5775,
                mld = "02:0a:eb:99:70:00", linkId = 0, links = listOf(RawMloLink(0, 2, 149, "00:0a:eb:99:70:01", -77), RawMloLink(1, 8, 197, "00:0a:eb:99:70:02", -83))),
            raw("00:0a:eb:99:70:02", "Neighbor-BE", -83, 6935, 5, 8, SAE7, cf0 = 6905,
                mld = "02:0a:eb:99:70:00", linkId = 1),
            raw("00:13:49:0f:aa:06", "Guest-OWE", -73, 6035, 1, 6, "[RSN-OWE-CCMP][ESS][MFPR]", cf0 = 6045),
        )
    }
}
