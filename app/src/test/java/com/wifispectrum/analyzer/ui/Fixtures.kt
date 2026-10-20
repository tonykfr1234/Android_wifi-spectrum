package com.wifispectrum.analyzer.ui

import com.wifispectrum.analyzer.UiState
import com.wifispectrum.analyzer.core.Mlo
import com.wifispectrum.analyzer.core.SignalHistory
import com.wifispectrum.analyzer.core.VendorDb
import com.wifispectrum.analyzer.data.DemoScanSource
import java.io.File

object Fixtures {
    val vendors: VendorDb by lazy { File("src/main/assets/oui.tsv.gz").inputStream().use(VendorDb::parseGzip) }

    /** Demo environment after [scans] scans, with a filled signal history. */
    fun demoState(scans: Int = 40): Pair<UiState, SignalHistory> {
        val src = DemoScanSource(seed = 11, clock = { 1_760_000_000_000L })
        val history = SignalHistory()
        src.start()
        history.record(src.snapshots.value!!.aps)
        repeat(scans - 1) { src.requestScan(); history.record(src.snapshots.value!!.aps) }
        val snap = src.snapshots.value!!
        val state = UiState(
            aps = snap.aps, visible = snap.aps.sortedByDescending { it.rssi }, connection = snap.connection,
            mlo = Mlo.group(snap.aps), lastScanMs = snap.timestampMs, isDemo = false, hasPermission = true,
            scanCount = history.scanCount, vendors = vendors,
        )
        return state to history
    }
}
