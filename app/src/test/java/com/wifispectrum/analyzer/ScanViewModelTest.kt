package com.wifispectrum.analyzer

import com.wifispectrum.analyzer.core.ApFilter
import com.wifispectrum.analyzer.core.Band
import com.wifispectrum.analyzer.core.VendorDb
import com.wifispectrum.analyzer.data.DemoScanSource
import com.wifispectrum.analyzer.data.ScanRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private var permission = true

    private fun vm(real: FakeScanSource? = null, loader: suspend () -> VendorDb = { VendorDb.EMPTY }) = ScanViewModel(
        realSource = real,
        demoFactory = { DemoScanSource(seed = 3) },
        permissionCheck = { permission },
        vendorLoader = loader,
        ioDispatcher = dispatcher,
    )

    /** Starts a VM and always stops it, so a failing assertion cannot leave the scan loop spinning. */
    private fun vmTest(
        real: FakeScanSource? = null,
        loader: suspend () -> VendorDb = { VendorDb.EMPTY },
        body: suspend TestScope.(ScanViewModel) -> Unit,
    ) = runTest(dispatcher, timeout = 30.seconds) {
        val m = vm(real, loader)
        m.onStart()
        runCurrent()
        try { body(m) } finally { m.onStop() }
    }

    @Test fun withoutRealScannerStartsInDemo() = vmTest { m ->
        val s = m.state.value
        assertTrue(s.isDemo)
        assertFalse(s.canUseRealScanner)
        assertEquals(20, s.aps.size)
        assertEquals(2, s.mlo.size)
        assertEquals(ScanViewModel.DEMO_INTERVAL, s.intervalSec)
    }

    @Test fun autoScanFollowsInterval() {
        val real = FakeScanSource()
        vmTest(real) { m ->
        assertEquals(1, real.requests)
        advanceTimeBy(ScanViewModel.REAL_INTERVAL * 1000L * 3 + 1)
        assertEquals(4, real.requests)
        m.setInterval(10)
        runCurrent()
        val before = real.requests
        advanceTimeBy(30_001)
        assertEquals(before + 3, real.requests)
        }
    }

    @Test fun pauseStopsScanning() {
        val real = FakeScanSource()
        vmTest(real) { m ->
        m.setAutoScan(false)
        val before = real.requests
        advanceTimeBy(600_000)
        assertEquals(before, real.requests)
        m.scanNow()
        assertEquals(before + 1, real.requests)
        }
    }

    @Test fun onStopCancelsLoopAndStopsSource() {
        val real = FakeScanSource()
        vmTest(real) { m ->
        m.onStop()
        val before = real.requests
        advanceTimeBy(600_000)
        assertEquals(before, real.requests)
        assertEquals(1, real.stopped)
        }
    }

    @Test fun missingPermissionBlocksScansUntilGranted() {
        permission = false; val real = FakeScanSource()
        vmTest(real) { m ->
        assertTrue(m.state.value.needsPermission)
        assertEquals(0, real.requests)
        m.scanNow()
        assertEquals(ScanRequest.NO_PERMISSION, m.state.value.lastRequest)
        assertEquals(0, real.requests)
        permission = true
        m.onPermissionResult()
        runCurrent()
        assertFalse(m.state.value.needsPermission)
        assertEquals(1, real.requests)
        assertEquals("Real", m.state.value.aps.single().ssid)
        }
    }

    @Test fun throttledAndWifiOffAreReported() {
        val real = FakeScanSource(ScanRequest.THROTTLED)
        vmTest(real) { m ->
        assertEquals(ScanRequest.THROTTLED, m.state.value.lastRequest)
        real.nextResult = ScanRequest.WIFI_OFF
        m.scanNow()
        assertEquals(ScanRequest.WIFI_OFF, m.state.value.lastRequest)
        }
    }

    @Test fun switchingDemoClearsDataAndHistory() {
        val real = FakeScanSource()
        vmTest(real) { m ->
        assertEquals("Real", m.state.value.aps.single().ssid)
        m.setDemo(true)
        runCurrent()
        assertTrue(m.state.value.isDemo)
        assertEquals(20, m.state.value.aps.size)
        assertTrue("history restarted", m.state.value.scanCount in 1L..2L)
        assertTrue(m.samples("aa:00:00:00:00:01").isEmpty())
        m.setDemo(false)
        runCurrent()
        assertFalse(m.state.value.isDemo)
        assertEquals("Real", m.state.value.aps.single().ssid)
        }
    }

    @Test fun cannotLeaveDemoWithoutRealScanner() = vmTest { m ->
        m.setDemo(false)
        assertTrue(m.state.value.isDemo)
    }

    @Test fun filterAppliesToLaterScans() = vmTest { m ->
        m.setAutoScan(false)
        m.setFilter(ApFilter(bands = setOf(Band.GHZ_6)))
        assertTrue(m.state.value.visible.all { it.band == Band.GHZ_6 })
        m.scanNow()
        runCurrent()
        assertTrue(m.state.value.visible.isNotEmpty())
        assertTrue(m.state.value.visible.all { it.band == Band.GHZ_6 })
    }

    @Test fun duplicateSnapshotIsIgnored() {
        val real = FakeScanSource()
        vmTest(real) { m ->
        m.setAutoScan(false)
        val count = m.state.value.scanCount
        real.emit(real.aps, timestamp = 999)
        runCurrent()
        real.emit(real.aps, timestamp = 999)
        runCurrent()
        assertEquals(count + 1, m.state.value.scanCount)
        }
    }

    @Test fun vendorLoaderFailureIsHarmless() = vmTest(loader = { error("asset missing") }) { m ->
        assertEquals(0, m.state.value.vendors.size)
        assertEquals(20, m.state.value.aps.size)
    }

    @Test fun intervalIsClamped() = vmTest { m ->
        m.setInterval(0); assertEquals(2, m.state.value.intervalSec)
        m.setInterval(100_000); assertEquals(600, m.state.value.intervalSec)
    }

    @Test fun stressManySnapshotsWithFilterChanges() {
        val real = FakeScanSource()
        vmTest(real) { m ->
        m.setAutoScan(false)
        val rnd = Random(1)
        val pool = DemoScanSource().also { it.start() }.snapshots.value!!.aps
        repeat(5_000) { i ->
            real.emit(pool.shuffled(rnd).take(rnd.nextInt(pool.size + 1)))
            if (i % 7 == 0) m.setFilter(ApFilter(bands = Band.entries.filter { rnd.nextBoolean() }.toSet().ifEmpty { setOf(Band.GHZ_5) }, minWidthMhz = listOf(0, 40, 80, 160, 320).random(rnd)))
            runCurrent()
            val s = m.state.value
            assertEquals(s.filter.apply(s.aps), s.visible)
        }
        assertEquals(5_001L, m.state.value.scanCount)
        assertTrue(m.history.trackedCount <= pool.size)
        }
    }
}
