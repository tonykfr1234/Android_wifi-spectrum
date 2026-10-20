package com.wifispectrum.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifispectrum.analyzer.core.AccessPoint
import com.wifispectrum.analyzer.core.ApFilter
import com.wifispectrum.analyzer.core.ConnectionInfo
import com.wifispectrum.analyzer.core.Mlo
import com.wifispectrum.analyzer.core.MloDevice
import com.wifispectrum.analyzer.core.SignalHistory
import com.wifispectrum.analyzer.core.VendorDb
import com.wifispectrum.analyzer.data.ScanRequest
import com.wifispectrum.analyzer.data.ScanSnapshot
import com.wifispectrum.analyzer.data.ScanSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UiState(
    val aps: List<AccessPoint> = emptyList(),
    val visible: List<AccessPoint> = emptyList(),
    val connection: ConnectionInfo? = null,
    val mlo: List<MloDevice> = emptyList(),
    val filter: ApFilter = ApFilter(),
    val lastScanMs: Long? = null,
    val lastRequest: ScanRequest? = null,
    val fresh: Boolean = true,
    val isDemo: Boolean = false,
    val canUseRealScanner: Boolean = true,
    val hasPermission: Boolean = false,
    val autoScan: Boolean = true,
    val intervalSec: Int = 30,
    val scanCount: Long = 0,
    val vendors: VendorDb = VendorDb.EMPTY,
) {
    val needsPermission: Boolean get() = !isDemo && !hasPermission
}

class ScanViewModel(
    private val realSource: ScanSource?,
    demoFactory: () -> ScanSource,
    private val permissionCheck: () -> Boolean,
    vendorLoader: suspend () -> VendorDb,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val history: SignalHistory = SignalHistory(),
) : ViewModel() {

    private val demo by lazy(demoFactory)
    private var source: ScanSource = realSource ?: demo
    private val _state = MutableStateFlow(
        UiState(
            isDemo = source.isDemo,
            canUseRealScanner = realSource != null,
            hasPermission = permissionCheck(),
            intervalSec = if (source.isDemo) DEMO_INTERVAL else REAL_INTERVAL,
        ),
    )
    val state: StateFlow<UiState> = _state

    private var collectJob: Job? = null
    private var loopJob: Job? = null
    private var active = false
    private var lastTimestamp: Long? = null

    init {
        viewModelScope.launch(ioDispatcher) {
            val db = runCatching { vendorLoader() }.getOrDefault(VendorDb.EMPTY)
            _state.update { it.copy(vendors = db) }
        }
    }

    fun onStart() {
        active = true
        _state.update { it.copy(hasPermission = permissionCheck()) }
        source.start()
        collect()
        restartLoop()
    }

    fun onStop() {
        active = false
        loopJob?.cancel()
        source.stop()
    }

    fun scanNow() {
        val result = if (!source.isDemo && !permissionCheck()) ScanRequest.NO_PERMISSION else source.requestScan()
        _state.update { it.copy(lastRequest = result, hasPermission = permissionCheck()) }
    }

    fun setAutoScan(on: Boolean) {
        _state.update { it.copy(autoScan = on) }
        restartLoop()
    }

    fun setInterval(seconds: Int) {
        _state.update { it.copy(intervalSec = seconds.coerceIn(2, 600)) }
        restartLoop()
    }

    fun setFilter(filter: ApFilter) {
        _state.update { it.copy(filter = filter, visible = filter.apply(it.aps)) }
    }

    fun setDemo(on: Boolean) {
        if (on == source.isDemo) return
        val next = if (on) demo else realSource ?: return
        source.stop()
        collectJob?.cancel()
        history.clear()
        lastTimestamp = null
        source = next
        _state.update {
            it.copy(
                aps = emptyList(), visible = emptyList(), connection = null, mlo = emptyList(),
                lastScanMs = null, lastRequest = null, scanCount = 0, isDemo = on,
                intervalSec = if (on) DEMO_INTERVAL else REAL_INTERVAL,
            )
        }
        if (active) {
            source.start()
            collect()
            restartLoop()
        }
    }

    fun onPermissionResult() {
        val granted = permissionCheck()
        _state.update { it.copy(hasPermission = granted) }
        if (granted && active) {
            source.start()
            restartLoop()
        }
    }

    fun samples(bssid: String): List<SignalHistory.Sample> = history.samples(bssid)

    private fun collect() {
        if (collectJob?.isActive == true) return
        collectJob = viewModelScope.launch {
            source.snapshots.filterNotNull().collect(::onSnapshot)
        }
    }

    private fun onSnapshot(s: ScanSnapshot) {
        if (s.timestampMs == lastTimestamp) return
        lastTimestamp = s.timestampMs
        history.record(s.aps)
        val mlo = Mlo.group(s.aps)
        _state.update {
            it.copy(
                aps = s.aps,
                visible = it.filter.apply(s.aps),
                connection = s.connection,
                mlo = mlo,
                lastScanMs = s.timestampMs,
                fresh = s.fresh,
                scanCount = history.scanCount,
            )
        }
    }

    private fun restartLoop() {
        loopJob?.cancel()
        val st = _state.value
        if (!active || !st.autoScan || st.needsPermission) return
        loopJob = viewModelScope.launch {
            while (isActive) {
                scanNow()
                delay(_state.value.intervalSec * 1000L)
            }
        }
    }

    companion object {
        /** Android lets a foreground app scan 4 times per 2 minutes, so 30 s never hits the throttle. */
        const val REAL_INTERVAL = 30
        const val DEMO_INTERVAL = 3
        val INTERVALS = listOf(3, 5, 10, 30, 60)
    }
}
