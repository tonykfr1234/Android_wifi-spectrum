package com.wifispectrum.analyzer.data

import android.Manifest
import android.app.Application
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wifispectrum.analyzer.core.Band
import com.wifispectrum.analyzer.core.ChannelWidth
import com.wifispectrum.analyzer.core.Security
import com.wifispectrum.analyzer.core.WifiGeneration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Runs the real WifiManager adapter on every Android version the app supports. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Config(sdk = [26, 29, 30, 33, 34, 35, 36])
class AndroidScanSourceTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val wifi = app.getSystemService(WifiManager::class.java)
    private val location = app.getSystemService(LocationManager::class.java)
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private lateinit var source: AndroidScanSource

    @Before fun setUp() {
        // androidx declares this signature permission in the merged manifest and Android grants it at
        // install time; Robolectric does not auto-grant app-defined permissions.
        shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        source = AndroidScanSource(app, { scope }, Dispatchers.Unconfined)
        @Suppress("DEPRECATION") wifi.setWifiEnabled(true)
        shadowOf(location).setLocationEnabled(true)
    }

    @After fun tearDown() = source.stop()

    private fun grant() {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.NEARBY_WIFI_DEVICES
        shadowOf(app).grantPermissions(*perms.toTypedArray())
    }

    @Suppress("DEPRECATION")
    private fun result(bssid: String, ssid: String, freq: Int, level: Int, width: Int, cf0: Int, caps: String) = ScanResult().apply {
        BSSID = bssid; SSID = ssid; frequency = freq; this.level = level
        channelWidth = width; centerFreq0 = cf0; capabilities = caps
    }

    private fun deliverBroadcast(updated: Boolean = true) {
        app.sendBroadcast(Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION).putExtra(WifiManager.EXTRA_RESULTS_UPDATED, updated))
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun withoutPermissionNothingIsRead() {
        assertFalse(source.hasPermission())
        assertEquals(ScanRequest.NO_PERMISSION, source.requestScan())
        source.start()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(null, source.snapshots.value)
    }

    @Test fun coarseLocationAloneIsNotEnough() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        assertFalse(source.hasPermission())
    }

    @Test fun wifiOff() {
        grant()
        @Suppress("DEPRECATION") wifi.setWifiEnabled(false)
        assertEquals(ScanRequest.WIFI_OFF, source.requestScan())
    }

    @Test fun locationOff() {
        grant()
        shadowOf(location).setLocationEnabled(false)
        assertEquals(ScanRequest.LOCATION_OFF, source.requestScan())
    }

    @Test fun throttledScanPublishesCachedResults() {
        grant()
        source.start()
        shadowOf(wifi).setScanResults(listOf(result("aa:bb:cc:00:00:01", "\"Cached\"", 2437, -60, 0, 0, "[RSN-PSK-CCMP]")))
        shadowOf(wifi).setStartScanSucceeds(false)
        assertEquals(ScanRequest.THROTTLED, source.requestScan())
        val snap = source.snapshots.value
        assertNotNull(snap)
        assertFalse(snap!!.fresh)
        assertEquals("Cached", snap.aps.single().ssid)
    }

    @Test fun successfulScanDeliversMappedResults() {
        grant()
        source.start()
        shadowOf(wifi).setStartScanSucceeds(true)
        shadowOf(wifi).setScanResults(
            listOf(
                result("AA:BB:CC:00:00:01", "\"Home\"", 5180, -48, ScanResult.CHANNEL_WIDTH_160MHZ, 5250, "[RSN-PSK+SAE-CCMP][ESS]"),
                result("aa:bb:cc:00:00:02", "Home6", 6135, -55, 5, 6105, "[RSN-SAE_EXT_KEY-GCMP-256]"),
                result("02:00:00:00:00:00", "redacted", 2437, -40, 0, 0, "[ESS]"),
                result("aa:bb:cc:00:00:03", "", 2412, -90, 0, 0, "[ESS]"),
            ),
        )
        assertEquals(ScanRequest.STARTED, source.requestScan())
        deliverBroadcast()
        val aps = source.snapshots.value!!.aps.associateBy { it.bssid }
        assertEquals(3, aps.size)
        val home = aps.getValue("aa:bb:cc:00:00:01")
        assertEquals("Home", home.ssid)
        assertEquals(Band.GHZ_5, home.band)
        assertEquals(ChannelWidth.MHZ_160, home.width)
        assertEquals(listOf(5170..5330), home.spans)
        assertEquals(setOf(Security.WPA2_PERSONAL, Security.WPA3_PERSONAL), home.security)
        val six = aps.getValue("aa:bb:cc:00:00:02")
        assertEquals(ChannelWidth.MHZ_320, six.width)
        assertEquals(Band.GHZ_6, six.band)
        assertTrue(aps.getValue("aa:bb:cc:00:00:03").isHidden)
        if (Build.VERSION.SDK_INT < 30) assertEquals(WifiGeneration.UNKNOWN, home.generation)
    }

    @Test fun staleBroadcastIsMarkedNotFresh() {
        grant()
        source.start()
        shadowOf(wifi).setScanResults(listOf(result("aa:bb:cc:00:00:01", "X", 2437, -60, 0, 0, "")))
        deliverBroadcast(updated = false)
        assertFalse(source.snapshots.value!!.fresh)
    }

    @Test fun startAndStopAreIdempotent() {
        grant()
        repeat(3) { source.start() }
        repeat(3) { source.stop() }
        source.start()
        shadowOf(wifi).setScanResults(emptyList())
        deliverBroadcast()
        assertTrue(source.snapshots.value!!.aps.isEmpty())
    }

    @Test fun stoppedSourceIgnoresBroadcasts() {
        grant()
        source.start()
        source.stop()
        val before = source.snapshots.value
        shadowOf(wifi).setScanResults(listOf(result("aa:bb:cc:00:00:09", "Late", 2437, -60, 0, 0, "")))
        deliverBroadcast()
        assertEquals(before, source.snapshots.value)
    }
}
