package com.wifispectrum.analyzer.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wifispectrum.analyzer.Gen
import com.wifispectrum.analyzer.UiState
import com.wifispectrum.analyzer.core.Mlo
import com.wifispectrum.analyzer.core.SignalHistory
import com.wifispectrum.analyzer.data.ScanMapper
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class UiStressTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun bigState(n: Int): Pair<UiState, SignalHistory> {
        val r = Random(3)
        val history = SignalHistory()
        var aps = ScanMapper.mapAll(Gen.scan(r, n))
        repeat(60) { aps = aps.map { it.copy(rssi = (it.rssi + r.nextInt(-2, 3)).coerceIn(-95, -25)) }; history.record(aps) }
        return UiState(
            aps = aps, visible = aps.sortedByDescending { it.rssi }, mlo = Mlo.group(aps),
            hasPermission = true, scanCount = history.scanCount, vendors = Fixtures.vendors,
        ) to history
    }

    private fun shoot(name: String) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        File("build/screenshots").mkdirs()
        File("build/screenshots/$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Config(qualifiers = "w400dp-h860dp")
    @Test fun thousandApsAcrossEveryScreen() {
        val (s, h) = bigState(1_000)
        val timings = linkedMapOf<String, Long>()
        timings["first frame"] = measureTimeMillis {
            rule.setContent { SpectrumTheme { WifiApp(s, AppActions(samples = h::samples)) } }
            rule.waitForIdle()
        }
        val last = s.visible.last().bssid
        timings["scroll to last of 1000"] = measureTimeMillis {
            rule.onNodeWithTag("apList").performScrollToNode(hasTestTag("ap:$last"))
            rule.onNodeWithTag("ap:$last").assertIsDisplayed()
        }
        for (tab in listOf(Tab.GRAPH, Tab.TIME, Tab.RATING)) {
            rule.onNodeWithTag("tab:${tab.name}").performClick()
            for (b in listOf("2.4 GHz", "5 GHz", "6 GHz")) {
                timings["${tab.label} $b"] = measureTimeMillis {
                    rule.onNodeWithContentDescription("頻段 $b").performClick()
                    rule.waitForIdle()
                }
            }
        }
        rule.onNodeWithTag("tab:GRAPH").performClick()
        shoot("stress_graph_1000")
        rule.onNodeWithTag("tab:MLO").performClick()
        timings["MLO ${s.mlo.size} devices"] = measureTimeMillis { rule.waitForIdle() }
        timings.forEach { (k, v) -> println("UI stress: %-28s %5d ms".format(k, v)) }
        assertTrue(timings.values.all { it < 15_000 })
    }

    @Config(qualifiers = "w320dp-h640dp", fontScale = 2.0f)
    @Test fun smallScreenHugeFont() {
        val (s, h) = Fixtures.demoState()
        rule.setContent { SpectrumTheme { WifiApp(s, AppActions(samples = h::samples)) } }
        shoot("small_font2_list")
        rule.onNodeWithTag("tab:RATING").performClick()
        shoot("small_font2_rating")
        rule.onNodeWithTag("tab:MLO").performClick()
        shoot("small_font2_mlo")
    }

    @Config(qualifiers = "w840dp-h400dp")
    @Test fun landscapeTablet() {
        val (s, h) = Fixtures.demoState()
        rule.setContent { SpectrumTheme { WifiApp(s, AppActions(samples = h::samples)) } }
        rule.onNodeWithTag("tab:GRAPH").performClick()
        shoot("landscape_graph")
    }
}
