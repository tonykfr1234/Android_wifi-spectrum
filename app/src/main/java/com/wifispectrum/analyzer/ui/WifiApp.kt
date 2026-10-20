package com.wifispectrum.analyzer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wifispectrum.analyzer.ScanViewModel
import com.wifispectrum.analyzer.UiState
import com.wifispectrum.analyzer.core.AccessPoint
import com.wifispectrum.analyzer.core.ApFilter
import com.wifispectrum.analyzer.core.SignalHistory
import com.wifispectrum.analyzer.data.ScanRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Tab(val label: String, val icon: ImageVector) {
    LIST("網路", Icons.List),
    GRAPH("頻譜", Icons.Spectrum),
    TIME("趨勢", Icons.Timeline),
    RATING("評分", Icons.Star),
    MLO("MLO", Icons.Mlo),
}

class AppActions(
    val scanNow: () -> Unit = {},
    val setAutoScan: (Boolean) -> Unit = {},
    val setInterval: (Int) -> Unit = {},
    val setFilter: (ApFilter) -> Unit = {},
    val setDemo: (Boolean) -> Unit = {},
    val requestPermission: () -> Unit = {},
    val openWifiSettings: () -> Unit = {},
    val openLocationSettings: () -> Unit = {},
    val openAppSettings: () -> Unit = {},
    val samples: (String) -> List<SignalHistory.Sample> = { emptyList() },
)

@Composable
fun WifiApp(state: UiState, actions: AppActions) {
    var tab by rememberSaveable { mutableStateOf(Tab.LIST) }
    var openBssid by rememberSaveable { mutableStateOf<String?>(null) }
    var lastSeen by remember { mutableStateOf<AccessPoint?>(null) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }

    val openAp = openBssid?.let { b -> state.aps.firstOrNull { it.bssid == b } }
    if (openAp != null) lastSeen = openAp
    BackHandler(enabled = openBssid != null) { openBssid = null }

    Scaffold(
        containerColor = Palette.Bg,
        topBar = {
            TopBar(
                state = state,
                detailTitle = openBssid?.let { (openAp ?: lastSeen)?.displaySsid ?: "基地台" },
                onBack = { openBssid = null },
                actions = actions,
                onDialog = { dialog = it },
            )
        },
        bottomBar = {
            if (openBssid == null && !state.needsPermission) {
                NavigationBar(containerColor = Palette.Surface, tonalElevation = 0.dp) {
                    for (t in Tab.entries) {
                        NavigationBarItem(
                            selected = t == tab,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, null) },
                            label = { Text(t.label, fontSize = 11.sp, maxLines = 1) },
                            modifier = Modifier.testTag("tab:${t.name}"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Palette.Accent,
                                selectedTextColor = Palette.Accent,
                                indicatorColor = Palette.Accent.copy(alpha = 0.14f),
                                unselectedIconColor = Palette.Text2,
                                unselectedTextColor = Palette.Text2,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (state.needsPermission) {
                PermissionScreen(actions)
                return@Column
            }
            StatusBanner(state, actions)
            val detailAp = openBssid?.let { openAp ?: lastSeen }
            when {
                openBssid != null && detailAp != null -> ApDetailScreen(
                    detailAp, state.vendors, connected = detailAp.bssid == state.connection?.bssid,
                    scanCount = state.scanCount, samples = actions.samples,
                )
                openBssid != null -> EmptyState("找不到這個基地台", "它可能已經離開掃描範圍。")
                else -> when (tab) {
                    Tab.LIST -> ApListScreen(state, onOpen = { openBssid = it }, onFilter = actions.setFilter)
                    Tab.GRAPH -> ChannelGraphScreen(state, onOpen = { openBssid = it })
                    Tab.TIME -> TimeGraphScreen(state, actions.samples, onOpen = { openBssid = it })
                    Tab.RATING -> RatingScreen(state)
                    Tab.MLO -> MloScreen(state, onOpen = { openBssid = it })
                }
            }
        }
    }

    when (dialog) {
        "privacy" -> InfoDialog("隱私權政策", PRIVACY_TEXT) { dialog = null }
        "about" -> InfoDialog("關於", ABOUT_TEXT) { dialog = null }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.TAIWAN)

fun statusLine(s: UiState): String {
    val parts = mutableListOf<String>()
    if (s.isDemo) parts += "示範模式"
    parts += "${s.aps.size} 個基地台"
    s.lastScanMs?.let { parts += "更新 ${timeFormat.format(Date(it))}" }
    if (!s.autoScan) parts += "已暫停"
    return parts.joinToString(" · ")
}

@Composable
private fun TopBar(
    state: UiState,
    detailTitle: String?,
    onBack: () -> Unit,
    actions: AppActions,
    onDialog: (String) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.background(Palette.Bg).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (detailTitle != null) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back")) { Icon(Icons.Back, "返回", tint = Palette.Text) }
            } else {
                Spacer(Modifier.padding(start = 12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    detailTitle ?: "Wi-Fi 頻譜分析儀",
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (!state.needsPermission) {
                    Text(statusLine(state), color = if (state.isDemo) Palette.Warn else Palette.Text2, fontSize = 11.sp, maxLines = 1, modifier = Modifier.testTag("status"))
                }
            }
            if (state.needsPermission) return@Row
            IconButton(onClick = actions.scanNow, modifier = Modifier.testTag("scan")) { Icon(Icons.Refresh, "立即掃描", tint = Palette.Text) }
            IconButton(onClick = { actions.setAutoScan(!state.autoScan) }, modifier = Modifier.testTag("auto")) {
                Icon(if (state.autoScan) Icons.Pause else Icons.Play, if (state.autoScan) "暫停自動掃描" else "開始自動掃描", tint = Palette.Text)
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.testTag("menu")) { Icon(Icons.More, "更多選項", tint = Palette.Text) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Palette.Surface2) {
                    Text("自動掃描間隔", color = Palette.Text2, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    for (sec in ScanViewModel.INTERVALS) {
                        val warn = !state.isDemo && sec < ScanViewModel.REAL_INTERVAL
                        DropdownMenuItem(
                            text = { Text((if (sec == state.intervalSec) "● " else "○ ") + "每 $sec 秒" + if (warn) "（會被節流）" else "") },
                            onClick = { actions.setInterval(sec); menu = false },
                        )
                    }
                    HorizontalDivider(color = Palette.Line)
                    if (state.canUseRealScanner) {
                        DropdownMenuItem(
                            text = { Text(if (state.isDemo) "改用真實掃描" else "切換到示範模式") },
                            onClick = { actions.setDemo(!state.isDemo); menu = false },
                            modifier = Modifier.testTag("toggleDemo"),
                        )
                    }
                    DropdownMenuItem(text = { Text("隱私權政策") }, onClick = { onDialog("privacy"); menu = false })
                    DropdownMenuItem(text = { Text("關於") }, onClick = { onDialog("about"); menu = false })
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(state: UiState, actions: AppActions) {
    if (state.isDemo) return
    val (text, button, action) = when (state.lastRequest) {
        ScanRequest.WIFI_OFF -> Triple("Wi-Fi 已關閉，開啟後才能掃描。", "開啟 Wi-Fi", actions.openWifiSettings)
        ScanRequest.LOCATION_OFF -> Triple("定位服務已關閉。Android 規定要開啟定位，App 才拿得到 Wi-Fi 掃描結果。", "開啟定位", actions.openLocationSettings)
        ScanRequest.THROTTLED -> Triple(
            "Android 限制每 2 分鐘最多掃描 4 次，現在顯示的是上次結果。可到「開發人員選項」關閉「Wi-Fi 掃描節流」。",
            null, null,
        )
        else -> return
    }
    Banner(text, if (state.lastRequest == ScanRequest.THROTTLED) Palette.Warn else Palette.Bad, button, action)
}

@Composable
private fun Banner(text: String, color: Color, button: String?, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontSize = 12.sp, modifier = Modifier.weight(1f))
        if (button != null && onClick != null) TextButton(onClick = onClick) { Text(button, color = color) }
    }
}

@Composable
private fun PermissionScreen(actions: AppActions) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("permission"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Wifi, null, tint = Palette.Accent, modifier = Modifier.padding(top = 24.dp).height(48.dp))
        Text("需要權限才能掃描 Wi-Fi", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Android 把附近 Wi-Fi 的掃描結果視為位置資訊，所以要請你允許「附近裝置」和「位置」權限。",
            color = Palette.Text2, fontSize = 14.sp,
        )
        Text(
            "系統詢問位置時，請選「精確位置」。只給「大概位置」的話，Android 不會提供 Wi-Fi 掃描結果。",
            color = Palette.Warn, fontSize = 14.sp,
        )
        Text(
            "這個 App 只在手機上分析頻道、頻寬與訊號，不會儲存、上傳或分享你的位置與掃描資料。",
            color = Palette.Text2, fontSize = 14.sp,
        )
        Button(
            onClick = actions.requestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent, contentColor = Palette.Bg),
            modifier = Modifier.fillMaxWidth().testTag("grant"),
        ) { Text("允許權限並開始掃描") }
        OutlinedButton(onClick = { actions.setDemo(true) }, modifier = Modifier.fillMaxWidth().testTag("demo")) {
            Text("先看示範模式", color = Palette.Text)
        }
        TextButton(onClick = actions.openAppSettings, modifier = Modifier.fillMaxWidth().testTag("appSettings")) {
            Text("沒有跳出詢問？到系統設定開啟權限", color = Palette.Accent, fontSize = 13.sp)
        }
    }
}

@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface2,
        title = { Text(title) },
        text = { Text(body, fontSize = 13.sp, color = Palette.Text2, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉") } },
    )
}

const val PRIVACY_TEXT = """Wi-Fi 頻譜分析儀不收集、不儲存、不傳送任何個人資料。

• 掃描結果（基地台名稱、MAC 位址、訊號強度、頻道）只在你的手機記憶體中分析，關閉 App 即清除。
• App 需要「位置」與「附近裝置」權限，是因為 Android 規定讀取 Wi-Fi 掃描結果必須取得這些權限。App 不會讀取 GPS 座標。
• App 沒有網路連線功能、沒有廣告、沒有分析追蹤 SDK。
• 廠商名稱來自內建的 IEEE 公開登錄表，不需要連網查詢。"""

const val ABOUT_TEXT = """Wi-Fi 頻譜分析儀 1.0

支援 2.4、5、6 GHz 掃描，辨識 20／40／80／160／320 MHz 頻寬與 80+80 MHz，並讀取 Wi-Fi 7 多連結（MLO）資訊。

MLO 與 320 MHz 需要 Android 13 以上、支援 Wi-Fi 7 的手機；連線中的各條 MLO 連結需要 Android 14 以上。

廠商資料：IEEE Registration Authority MA-L 公開登錄表。"""
