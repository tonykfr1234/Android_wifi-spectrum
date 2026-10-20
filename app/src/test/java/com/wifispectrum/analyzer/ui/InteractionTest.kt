package com.wifispectrum.analyzer.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wifispectrum.analyzer.FakeScanSource
import com.wifispectrum.analyzer.ScanViewModel
import com.wifispectrum.analyzer.data.DemoScanSource
import com.wifispectrum.analyzer.data.ScanRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w400dp-h860dp")
class InteractionTest {
    @get:Rule val rule = createComposeRule()

    private var permission = true
    private val calls = mutableListOf<String>()
    private var vm: ScanViewModel? = null

    @After fun tearDown() { vm?.onStop() }

    private fun actions(m: ScanViewModel) = AppActions(
        scanNow = m::scanNow, setAutoScan = m::setAutoScan, setInterval = m::setInterval,
        setFilter = m::setFilter, setDemo = m::setDemo,
        requestPermission = { calls += "perm"; permission = true; m.onPermissionResult() },
        openWifiSettings = { calls += "wifi" }, openLocationSettings = { calls += "loc" },
        openAppSettings = { calls += "app" },
        samples = m::samples,
    )

    private fun launch(real: FakeScanSource? = null): ScanViewModel {
        val m = ScanViewModel(real, { DemoScanSource(seed = 5) }, { permission }, { Fixtures.vendors })
        m.setAutoScan(false)
        m.onStart()
        vm = m
        rule.setContent {
            val s by m.state.collectAsState()
            SpectrumTheme { WifiApp(s, actions(m)) }
        }
        rule.waitForIdle()
        return m
    }

    private fun count() = rule.onNodeWithTag("count")
    private val bestCard = SemanticsMatcher("best card") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("best:") == true }

    @Test fun demoListShowsEveryAp() {
        launch()
        count().assertTextEquals("20 個基地台")
    }

    @Test fun searchFiltersAndClears() {
        launch()
        rule.onNodeWithTag("search").performTextInput("home")
        count().assertTextEquals("顯示 4 / 20 個基地台")
        rule.onNodeWithContentDescription("清除搜尋").performClick()
        count().assertTextEquals("20 個基地台")
    }

    @Test fun bandChipHidesBand() {
        val m = launch()
        rule.onNodeWithContentDescription("2.4 GHz 篩選已開啟").performClick()
        count().assertTextEquals("顯示 11 / 20 個基地台")
        assertTrue(m.state.value.visible.none { it.band == com.wifispectrum.analyzer.core.Band.GHZ_2_4 })
    }

    @Test fun lastBandCannotBeTurnedOff() {
        val m = launch()
        rule.onNodeWithContentDescription("2.4 GHz 篩選已開啟").performClick()
        rule.onNodeWithContentDescription("5 GHz 篩選已開啟").performClick()
        rule.onNodeWithContentDescription("6 GHz 篩選已開啟").performClick()
        assertEquals(setOf(com.wifispectrum.analyzer.core.Band.GHZ_6), m.state.value.filter.bands)
    }

    @Test fun wifi7Chip() {
        launch()
        rule.onNodeWithContentDescription("Wi-Fi 7 篩選已關閉").performClick()
        count().assertTextEquals("顯示 5 / 20 個基地台")
    }

    @Test fun widthChipCyclesAllSteps() {
        val m = launch()
        val seen = mutableListOf<Int>()
        repeat(5) {
            rule.onNode(SemanticsMatcher("width chip") {
                it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()?.let { d -> "頻寬" in d || "MHz" in d } == true
            }).performClick()
            seen += m.state.value.filter.minWidthMhz
            assertTrue(m.state.value.visible.all { ap -> ap.width.mhz >= m.state.value.filter.minWidthMhz })
        }
        assertEquals(listOf(40, 80, 160, 320, 0), seen)
    }

    @Test fun openDetailAndGoBack() {
        launch()
        rule.onNodeWithTag("ap:04:d9:f5:6e:20:01").performClick()
        rule.onNodeWithTag("detail").assertIsDisplayed()
        rule.onNodeWithText("MLD 位址").assertExists()
        rule.onNodeWithTag("back").performClick()
        rule.onNodeWithTag("apList").assertIsDisplayed()
    }

    @Test fun everyTabRenders() {
        launch()
        val screens = mapOf(Tab.GRAPH to "graphScreen", Tab.TIME to "timeScreen", Tab.RATING to "ratingScreen", Tab.MLO to "mloScreen", Tab.LIST to "apList")
        for ((tab, tag) in screens) {
            rule.onNodeWithTag("tab:${tab.name}").performClick()
            rule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }

    @Test fun everyBandOnEveryChartTab() {
        launch()
        for (tab in listOf(Tab.GRAPH, Tab.TIME, Tab.RATING)) {
            rule.onNodeWithTag("tab:${tab.name}").performClick()
            for (b in listOf("2.4 GHz", "5 GHz", "6 GHz")) {
                rule.onNodeWithContentDescription("頻段 $b").performClick()
                rule.waitForIdle()
            }
        }
        rule.onNodeWithTag("tab:RATING").performClick()
        rule.onAllNodes(bestCard).assertCountEquals(3)
    }

    @Test fun mloScreenGroupsBothRouters() {
        launch()
        rule.onNodeWithTag("tab:MLO").performClick()
        rule.onNodeWithTag("mlo:06:d9:f5:6e:20:00").assertExists()
        rule.onNodeWithTag("mloScreen").performScrollToNode(hasTestTag("mlo:02:0a:eb:99:70:00"))
        rule.onNodeWithTag("mlo:02:0a:eb:99:70:00").assertIsDisplayed()
    }

    @Test fun graphZoomToggles() {
        launch()
        rule.onNodeWithTag("tab:GRAPH").performClick()
        rule.onNodeWithContentDescription("頻段 5 GHz").performClick()
        rule.onNodeWithText("整個頻段總覽").assertExists()
        rule.onNodeWithTag("zoom").performClick()
        rule.onNodeWithText("放大檢視，可左右滑動").assertExists()
        rule.onNodeWithTag("zoom").performClick()
        rule.onNodeWithText("整個頻段總覽").assertExists()
    }

    @Test fun rapidRefreshTaps() {
        val m = launch()
        val start = m.state.value.scanCount
        repeat(200) { rule.onNodeWithTag("scan").performClick() }
        rule.waitForIdle()
        assertEquals(start + 200, m.state.value.scanCount)
        rule.onNodeWithTag("apList").assertIsDisplayed()
    }

    @Test fun pauseShowsInStatus() {
        val m = launch()
        m.setAutoScan(true)
        rule.waitForIdle()
        rule.onNodeWithTag("auto").performClick()
        rule.onNodeWithTag("status").assertTextContains("已暫停", substring = true)
    }

    @Test fun permissionGrantFlow() {
        permission = false
        launch(FakeScanSource())
        rule.onNodeWithTag("permission").assertIsDisplayed()
        rule.onNodeWithTag("grant").performClick()
        assertEquals(listOf("perm"), calls)
        rule.onNodeWithTag("permission").assertDoesNotExist()
        rule.onNodeWithTag("scan").performClick()
        count().assertTextEquals("1 個基地台")
    }

    @Test fun permanentlyDeniedPermissionCanOpenAppSettings() {
        permission = false
        launch(FakeScanSource())
        rule.onNodeWithText("精確位置", substring = true).assertExists()
        rule.onNodeWithTag("appSettings").performScrollTo().performClick()
        assertEquals(listOf("app"), calls)
    }

    @Test fun permissionDemoFlow() {
        permission = false
        launch(FakeScanSource())
        rule.onNodeWithTag("demo").performClick()
        count().assertTextEquals("20 個基地台")
        rule.onNodeWithTag("status").assertTextContains("示範模式", substring = true)
    }

    @Test fun throttledBanner() {
        launch(FakeScanSource(ScanRequest.THROTTLED)).scanNow()
        rule.onNodeWithTag("banner").assertIsDisplayed()
        rule.onNodeWithText("Wi-Fi 掃描節流", substring = true).assertIsDisplayed()
    }

    @Test fun wifiOffBannerOpensSettings() {
        launch(FakeScanSource(ScanRequest.WIFI_OFF)).scanNow()
        rule.onNodeWithText("開啟 Wi-Fi").performClick()
        assertEquals(listOf("wifi"), calls)
    }

    @Test fun locationOffBannerOpensSettings() {
        launch(FakeScanSource(ScanRequest.LOCATION_OFF)).scanNow()
        rule.onNodeWithText("開啟定位").performClick()
        assertEquals(listOf("loc"), calls)
    }

    @Test fun noBannerWhenScanStarted() {
        launch(FakeScanSource()).scanNow()
        rule.onNodeWithTag("banner").assertDoesNotExist()
    }

    @Test fun menuSwitchesBetweenRealAndDemo() {
        val m = launch(FakeScanSource())
        rule.onNodeWithTag("menu").performClick()
        rule.onNodeWithText("切換到示範模式").performClick()
        assertTrue(m.state.value.isDemo)
        count().assertTextEquals("20 個基地台")
        rule.onNodeWithTag("menu").performClick()
        rule.onNodeWithText("改用真實掃描").performClick()
        rule.onNodeWithTag("scan").performClick()
        count().assertTextEquals("1 個基地台")
    }

    @Test fun menuChangesInterval() {
        val m = launch()
        rule.onNodeWithTag("menu").performClick()
        rule.onNodeWithText("○ 每 10 秒").performClick()
        assertEquals(10, m.state.value.intervalSec)
    }

    @Test fun privacyDialog() {
        launch()
        rule.onNodeWithTag("menu").performClick()
        rule.onNodeWithText("隱私權政策").performClick()
        rule.onNodeWithText("不收集、不儲存、不傳送任何個人資料", substring = true).assertExists()
        rule.onNodeWithText("關閉").performClick()
        rule.onNodeWithText("不收集、不儲存、不傳送任何個人資料", substring = true).assertDoesNotExist()
    }

    @Test fun detailSurvivesApLeavingRange() {
        launch()
        val tag = "ap:8a:2b:3c:4d:5e:6f"
        rule.onNodeWithTag("apList").performScrollToNode(hasTestTag(tag))
        rule.onNodeWithTag(tag).performClick()
        repeat(2) { rule.onNodeWithTag("scan").performClick() }
        rule.waitForIdle()
        assertTrue("hotspot is out of range on scan 3", vm!!.state.value.aps.none { it.bssid == "8a:2b:3c:4d:5e:6f" })
        rule.onNodeWithTag("detail").assertIsDisplayed()
        rule.onAllNodesWithText("小明的 iPhone").assertCountEquals(2)
    }

    @Test fun hiddenNetworkLabel() {
        launch()
        rule.onNodeWithTag("apList").performScrollToNode(hasTestTag("ap:00:13:49:0f:aa:01"))
        rule.onNodeWithText("（隱藏網路）").assertIsDisplayed()
    }

    @Test fun selectedTabSurvivesRecreation() {
        val m = ScanViewModel(null, { DemoScanSource(seed = 5) }, { true }, { Fixtures.vendors })
        m.setAutoScan(false)
        m.onStart()
        vm = m
        val restore = StateRestorationTester(rule)
        restore.setContent {
            val s by m.state.collectAsState()
            SpectrumTheme { WifiApp(s, actions(m)) }
        }
        rule.onNodeWithTag("tab:RATING").performClick()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("ratingScreen").assertIsDisplayed()
    }

    @Test fun everyApRowIsReachable() {
        val m = launch()
        for (ap in m.state.value.visible) {
            rule.onNodeWithTag("apList").performScrollToNode(hasTestTag("ap:${ap.bssid}"))
            rule.onAllNodesWithTag("ap:${ap.bssid}").assertCountEquals(1)
        }
    }
}
