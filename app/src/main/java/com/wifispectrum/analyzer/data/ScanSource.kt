package com.wifispectrum.analyzer.data

import com.wifispectrum.analyzer.core.AccessPoint
import com.wifispectrum.analyzer.core.ConnectionInfo
import kotlinx.coroutines.flow.StateFlow

enum class ScanRequest { STARTED, THROTTLED, WIFI_OFF, LOCATION_OFF, NO_PERMISSION }

data class ScanSnapshot(
    val aps: List<AccessPoint>,
    val connection: ConnectionInfo?,
    val timestampMs: Long,
    /** False when Android returned cached results because the new scan failed or was throttled. */
    val fresh: Boolean,
)

interface ScanSource {
    val snapshots: StateFlow<ScanSnapshot?>
    val isDemo: Boolean
    fun start()
    fun stop()
    fun requestScan(): ScanRequest
}
