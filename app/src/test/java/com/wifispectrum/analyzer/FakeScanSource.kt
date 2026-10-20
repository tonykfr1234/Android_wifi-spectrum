package com.wifispectrum.analyzer

import com.wifispectrum.analyzer.core.AccessPoint
import com.wifispectrum.analyzer.data.ScanRequest
import com.wifispectrum.analyzer.data.ScanSnapshot
import com.wifispectrum.analyzer.data.ScanSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Scriptable stand-in for the real Wi-Fi scanner. */
class FakeScanSource(var nextResult: ScanRequest = ScanRequest.STARTED) : ScanSource {
    val state = MutableStateFlow<ScanSnapshot?>(null)
    var requests = 0
    var started = 0
    var stopped = 0
    var aps: List<AccessPoint> = listOf(ap(bssid = "aa:00:00:00:00:01", ssid = "Real"))
    private var clock = 1L

    override val snapshots: StateFlow<ScanSnapshot?> = state
    override val isDemo = false
    override fun start() { started++ }
    override fun stop() { stopped++ }

    override fun requestScan(): ScanRequest {
        requests++
        if (nextResult == ScanRequest.STARTED) emit(aps)
        return nextResult
    }

    fun emit(list: List<AccessPoint>, timestamp: Long = clock++) {
        state.value = ScanSnapshot(list, null, timestamp, fresh = true)
    }
}
