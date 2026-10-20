package com.wifispectrum.analyzer.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** Google Play phone screenshots: 1080x2160 (2:1, the maximum ratio Play accepts). */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h820dp-420dpi")
class StoreScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun shoot(name: String) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        File("build/store").mkdirs()
        File("build/store/$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun show() {
        val (s, h) = Fixtures.demoState()
        rule.setContent { SpectrumTheme { WifiApp(s, AppActions(samples = h::samples)) } }
    }

    @Test fun s1List() { show(); shoot("phone_1_list") }
    @Test fun s2Spectrum() { show(); rule.onNodeWithTag("tab:GRAPH").performClick(); rule.onNodeWithContentDescription("頻段 2.4 GHz").performClick(); shoot("phone_2_spectrum_24") }
    @Test fun s3Six() { show(); rule.onNodeWithTag("tab:GRAPH").performClick(); rule.onNodeWithContentDescription("頻段 6 GHz").performClick(); shoot("phone_3_spectrum_6") }
    @Test fun s4Mlo() { show(); rule.onNodeWithTag("tab:MLO").performClick(); shoot("phone_4_mlo") }
    @Test fun s5Rating() { show(); rule.onNodeWithTag("tab:RATING").performClick(); rule.onNodeWithContentDescription("頻段 2.4 GHz").performClick(); shoot("phone_5_rating") }
    @Test fun s6Detail() {
        show()
        rule.onNodeWithTag("apList").performScrollToNode(hasTestTag("ap:04:d9:f5:6e:20:03"))
        rule.onNodeWithTag("ap:04:d9:f5:6e:20:03").performClick()
        shoot("phone_6_detail")
    }
}
