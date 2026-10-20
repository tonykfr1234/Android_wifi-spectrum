package com.wifispectrum.analyzer.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wifispectrum.analyzer.UiState
import com.wifispectrum.analyzer.data.ScanRequest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** Renders every screen to build/screenshots so the layout can be inspected by eye. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w400dp-h860dp-xhdpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun shoot(name: String) {
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun show(state: UiState, history: com.wifispectrum.analyzer.core.SignalHistory? = null) {
        rule.setContent {
            SpectrumTheme { WifiApp(state, AppActions(samples = { b -> history?.samples(b) ?: emptyList() })) }
        }
    }

    @Test fun list() { val (s, h) = Fixtures.demoState(); show(s, h); shoot("01_list") }

    @Test fun graph24() { val (s, h) = Fixtures.demoState(); show(s, h); rule.onNodeWithTag("tab:GRAPH").performClick(); rule.onNodeWithContentDescription("頻段 2.4 GHz").performClick(); shoot("02_graph_24") }
    @Test fun graph5() { val (s, h) = Fixtures.demoState(); show(s, h); rule.onNodeWithTag("tab:GRAPH").performClick(); rule.onNodeWithContentDescription("頻段 5 GHz").performClick(); shoot("03_graph_5") }
    @Test fun graph6() { val (s, h) = Fixtures.demoState(); show(s, h); rule.onNodeWithTag("tab:GRAPH").performClick(); rule.onNodeWithContentDescription("頻段 6 GHz").performClick(); shoot("04_graph_6") }
    @Test fun graph6Zoom() { val (s, h) = Fixtures.demoState(); show(s, h); rule.onNodeWithTag("tab:GRAPH").performClick(); rule.onNodeWithContentDescription("頻段 6 GHz").performClick(); rule.onNodeWithTag("zoom").performClick(); shoot("04b_graph_6_zoom") }
    @Test fun time() { val (s, h) = Fixtures.demoState(); show(s, h); rule.onNodeWithTag("tab:TIME").performClick(); rule.onNodeWithContentDescription("頻段 2.4 GHz").performClick(); shoot("05_time") }
    @Test fun rating() { val (s, h) = Fixtures.demoState(); show(s, h); rule.onNodeWithTag("tab:RATING").performClick(); rule.onNodeWithContentDescription("頻段 2.4 GHz").performClick(); shoot("06_rating") }
    @Test fun mlo() { val (s, h) = Fixtures.demoState(); show(s, h); rule.onNodeWithTag("tab:MLO").performClick(); shoot("07_mlo") }
    @Test fun detail() { val (s, h) = Fixtures.demoState(); show(s, h); rule.onNodeWithTag("apList").performScrollToNode(hasTestTag("ap:04:d9:f5:6e:20:03")); rule.onNodeWithTag("ap:04:d9:f5:6e:20:03").performClick(); shoot("08_detail_mlo_320") }
    @Test fun permission() { show(UiState(isDemo = false, hasPermission = false)); shoot("09_permission") }
    @Test fun throttled() { val (s, h) = Fixtures.demoState(); show(s.copy(lastRequest = ScanRequest.THROTTLED), h); shoot("10_throttled") }
    @Test fun empty() { show(UiState(hasPermission = true)); shoot("11_empty") }
}
