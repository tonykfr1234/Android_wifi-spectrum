package com.wifispectrum.analyzer.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wifispectrum.analyzer.ScanViewModel
import com.wifispectrum.analyzer.data.DemoScanSource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * Renders the frames behind docs/media/wifi-spectrum-demo.{gif,mp4}: the real app UI driven by the
 * built-in synthetic environment, with bilingual captions. scripts/generate_demo.py assembles them.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w360dp-h760dp-hdpi")
class DemoMediaTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private var caption by mutableStateOf("")
    private var frame = 0
    private val out = File("build/demo").apply { deleteRecursively(); mkdirs() }

    private fun hold(text: String, frames: Int = 1) {
        caption = text
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        repeat(frames) {
            File(out, "frame_%03d.png".format(frame++)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun renderDemoFrames() {
        val vm = ScanViewModel(null, { DemoScanSource(seed = 21) }, { true }, { Fixtures.vendors })
        vm.setAutoScan(false)
        vm.onStart()
        rule.setContent {
            val s by vm.state.collectAsState()
            SpectrumTheme {
                Column(Modifier.fillMaxSize().background(Palette.Bg)) {
                    Box(Modifier.weight(1f)) { WifiApp(s, AppActions(scanNow = vm::scanNow, setFilter = vm::setFilter, samples = vm::samples)) }
                    Text(
                        caption, color = Palette.Text, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth().background(Palette.Surface2).padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
        repeat(12) { vm.scanNow() }
        hold("示範資料：模擬的台北公寓\nSynthetic demo environment, not a real scan", 3)
        hold("同時掃描 2.4／5／6 GHz\nScan 2.4 / 5 / 6 GHz together", 3)
        rule.onNodeWithTag("tab:GRAPH").performClick()
        rule.onNodeWithContentDescription("頻段 2.4 GHz").performClick()
        hold("曲線寬度＝實際頻寬，粗線是連線中的基地台\nCurve width = channel width; bold = connected", 4)
        rule.onNodeWithContentDescription("頻段 5 GHz").performClick()
        hold("5 GHz 總覽：160 MHz 佔用 8 個頻道\n5 GHz overview: 160 MHz spans 8 channels", 3)
        rule.onNodeWithContentDescription("頻段 6 GHz").performClick()
        hold("6 GHz：320 MHz 與 PSC 頻道\n6 GHz: 320 MHz and PSC channels", 3)
        rule.onNodeWithTag("zoom").performClick()
        hold("放大檢視，可左右滑動\nZoom in and scroll", 3)
        rule.onNodeWithTag("tab:TIME").performClick()
        rule.onNodeWithContentDescription("頻段 2.4 GHz").performClick()
        repeat(8) {
            repeat(3) { vm.scanNow() }
            hold("訊號趨勢：最近 60 次掃描\nSignal trend over the last 60 scans")
        }
        rule.onNodeWithTag("tab:RATING").performClick()
        rule.onNodeWithContentDescription("頻段 2.4 GHz").performClick()
        hold("頻道評分：2.4 GHz 只推薦 1／6／11\nChannel rating: only 1/6/11 on 2.4 GHz", 4)
        rule.onNodeWithTag("tab:MLO").performClick()
        hold("Wi-Fi 7 MLO：同一台路由器的連結合併顯示\nWi-Fi 7 MLO links grouped by MLD", 4)
        rule.onNodeWithTag("tab:LIST").performClick()
        rule.onNodeWithTag("apList").performScrollToNode(hasTestTag("ap:04:d9:f5:6e:20:03"))
        rule.onNodeWithTag("ap:04:d9:f5:6e:20:03").performClick()
        hold("詳細資料：6 GHz 320 MHz 涵蓋 16 個頻道\nDetail: 320 MHz on 6 GHz covers 16 channels", 4)
        vm.onStop()
    }
}
