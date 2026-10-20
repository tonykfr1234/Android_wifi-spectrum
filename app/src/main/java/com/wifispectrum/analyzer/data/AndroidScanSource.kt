package com.wifispectrum.analyzer.data

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.MloLink
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.wifispectrum.analyzer.core.ConnectionInfo
import com.wifispectrum.analyzer.core.Generations
import com.wifispectrum.analyzer.core.Channels
import com.wifispectrum.analyzer.core.MloLinkInfo
import com.wifispectrum.analyzer.core.WifiGeneration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address

class AndroidScanSource(
    context: Context,
    private val scope: () -> CoroutineScope,
    private val worker: CoroutineDispatcher = Dispatchers.Default,
) : ScanSource {

    private val app = context.applicationContext
    private val wifi = app.getSystemService(WifiManager::class.java)
    private val location = app.getSystemService(LocationManager::class.java)
    private val connectivity = app.getSystemService(ConnectivityManager::class.java)
    private val state = MutableStateFlow<ScanSnapshot?>(null)
    private var registered = false

    override val snapshots: StateFlow<ScanSnapshot?> = state
    override val isDemo = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, true)
            publish(updated)
        }
    }

    override fun start() {
        if (registered) return
        ContextCompat.registerReceiver(
            app, receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registered = true
        if (hasPermission()) publish(fresh = false)
    }

    override fun stop() {
        if (!registered) return
        runCatching { app.unregisterReceiver(receiver) }
        registered = false
    }

    fun hasPermission(): Boolean {
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        return if (Build.VERSION.SDK_INT >= 33) fine && granted(Manifest.permission.NEARBY_WIFI_DEVICES) else fine
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(app, p) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    override fun requestScan(): ScanRequest {
        if (!hasPermission()) return ScanRequest.NO_PERMISSION
        if (wifi == null || !wifi.isWifiEnabled) return ScanRequest.WIFI_OFF
        if (location == null || !LocationManagerCompat.isLocationEnabled(location)) return ScanRequest.LOCATION_OFF
        val started = runCatching { wifi.startScan() }.getOrDefault(false)
        if (!started) publish(fresh = false)
        return if (started) ScanRequest.STARTED else ScanRequest.THROTTLED
    }

    private fun publish(fresh: Boolean) {
        scope().launch(worker) {
            val results = try {
                wifi?.scanResults.orEmpty()
            } catch (_: SecurityException) {
                emptyList()
            }
            val aps = ScanMapper.mapAll(results.map(::toRaw))
            state.value = ScanSnapshot(aps, readConnection(), System.currentTimeMillis(), fresh)
        }
    }

    @Suppress("DEPRECATION")
    private fun toRaw(r: ScanResult): RawScan {
        var mld: String? = null
        var linkId = -1
        var links = emptyList<RawMloLink>()
        if (Build.VERSION.SDK_INT >= 33) {
            mld = r.apMldMacAddress?.toString()
            linkId = r.apMloLinkId
            links = r.affiliatedMloLinks.orEmpty().map(::toRawLink)
        }
        return RawScan(
            bssid = r.BSSID,
            ssid = r.SSID,
            level = r.level,
            frequency = r.frequency,
            centerFreq0 = r.centerFreq0,
            centerFreq1 = r.centerFreq1,
            channelWidth = r.channelWidth,
            wifiStandard = if (Build.VERSION.SDK_INT >= 30) r.wifiStandard else 0,
            capabilities = r.capabilities,
            mldAddress = mld,
            mloLinkId = linkId,
            mloLinks = links,
        )
    }

    private fun toRawLink(l: MloLink): RawMloLink {
        if (Build.VERSION.SDK_INT < 33) return RawMloLink(-1, 0, 0, null, 0)
        val rssi = if (Build.VERSION.SDK_INT >= 34) l.rssi else 0
        return RawMloLink(l.linkId, l.band, l.channel, l.apMacAddress?.toString(), rssi)
    }

    @Suppress("DEPRECATION")
    private fun readConnection(): ConnectionInfo? {
        val info: WifiInfo = runCatching { wifi?.connectionInfo }.getOrNull() ?: return null
        val bssid = ScanMapper.cleanMac(info.bssid) ?: return null
        if (info.frequency <= 0) return null
        val band = Channels.bandOf(info.frequency)
        var mld: String? = null
        var links = emptyList<MloLinkInfo>()
        if (Build.VERSION.SDK_INT >= 33) mld = ScanMapper.cleanMac(info.apMldMacAddress?.toString())
        if (Build.VERSION.SDK_INT >= 34) {
            links = info.associatedMloLinks.orEmpty().map {
                MloLinkInfo(it.linkId, ScanMapper.band(it.band), it.channel, ScanMapper.cleanMac(it.apMacAddress?.toString()), it.rssi)
            }
        }
        return ConnectionInfo(
            ssid = ScanMapper.cleanSsid(info.ssid).ifEmpty { null },
            bssid = bssid,
            rssi = info.rssi,
            frequencyMhz = info.frequency,
            linkSpeedMbps = info.linkSpeed,
            txSpeedMbps = if (Build.VERSION.SDK_INT >= 29) info.txLinkSpeedMbps.takeIf { it > 0 } else null,
            rxSpeedMbps = if (Build.VERSION.SDK_INT >= 29) info.rxLinkSpeedMbps.takeIf { it > 0 } else null,
            maxTxSpeedMbps = if (Build.VERSION.SDK_INT >= 30) info.maxSupportedTxLinkSpeedMbps.takeIf { it > 0 } else null,
            generation = if (Build.VERSION.SDK_INT >= 30) Generations.fromAndroidStandard(info.wifiStandard, band) else WifiGeneration.UNKNOWN,
            ipAddress = ipv4(),
            mldAddress = mld,
            activeLinks = links,
        )
    }

    private fun ipv4(): String? = runCatching {
        val net = connectivity?.activeNetwork ?: return null
        connectivity.getLinkProperties(net)?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { it is Inet4Address }
            ?.hostAddress
    }.getOrNull()
}
