package com.wifispectrum.analyzer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.wifispectrum.analyzer.core.VendorDb
import com.wifispectrum.analyzer.data.AndroidScanSource
import com.wifispectrum.analyzer.data.DemoScanSource
import com.wifispectrum.analyzer.ui.AppActions
import com.wifispectrum.analyzer.ui.SpectrumTheme
import com.wifispectrum.analyzer.ui.WifiApp

class MainActivity : ComponentActivity() {

    private val vm: ScanViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = applicationContext
                val hasWifi = packageManager.hasSystemFeature("android.hardware.wifi")
                lateinit var model: ScanViewModel
                val real = if (hasWifi) AndroidScanSource(app, scope = { model.viewModelScope }) else null
                model = ScanViewModel(
                    realSource = real,
                    demoFactory = { DemoScanSource() },
                    permissionCheck = { real?.hasPermission() ?: false },
                    vendorLoader = { app.assets.open("oui.tsv.gz").use(VendorDb::parseGzip) },
                )
                return model as T
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.onPermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = vm.onStart()
            override fun onStop(owner: LifecycleOwner) = vm.onStop()
        })
        val actions = AppActions(
            scanNow = vm::scanNow,
            setAutoScan = vm::setAutoScan,
            setInterval = vm::setInterval,
            setFilter = vm::setFilter,
            setDemo = vm::setDemo,
            requestPermission = { permissionLauncher.launch(requiredPermissions()) },
            openWifiSettings = { openSettings(wifiSettingsIntent()) },
            openLocationSettings = { openSettings(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
            openAppSettings = {
                openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
            },
            samples = vm::samples,
        )
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            SpectrumTheme { WifiApp(state, actions) }
        }
    }

    private fun requiredPermissions(): Array<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }.toTypedArray()

    private fun wifiSettingsIntent() =
        if (Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_WIFI) else Intent(Settings.ACTION_WIFI_SETTINGS)

    private fun openSettings(intent: Intent) {
        runCatching { startActivity(intent) }.onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) } }
    }
}
