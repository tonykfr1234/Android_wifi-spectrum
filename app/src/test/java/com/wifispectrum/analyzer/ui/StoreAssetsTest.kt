package com.wifispectrum.analyzer.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wifispectrum.analyzer.R
import com.wifispectrum.analyzer.core.Band
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** Google Play listing art: 512x512 icon and 1024x500 feature graphic. */
@RunWith(AndroidJUnit4::class)
class StoreAssetsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun shoot(name: String, w: Int, h: Int) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        val out = Bitmap.createBitmap(bmp, 0, 0, w, h)
        File("build/store").mkdirs()
        File("build/store/$name.png").outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Config(qualifiers = "w512dp-h512dp-mdpi")
    @Test fun icon512() {
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Palette.Bg), contentAlignment = Alignment.Center) {
                Image(
                    painterResource(R.drawable.ic_launcher_foreground), null,
                    Modifier.fillMaxSize().graphicsLayer(scaleX = 1.45f, scaleY = 1.45f, translationY = 30f),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        shoot("icon_512", 512, 512)
    }

    @Config(qualifiers = "w1024dp-h500dp-mdpi")
    @Test fun featureGraphic() {
        val (s, _) = Fixtures.demoState()
        rule.setContent {
            SpectrumTheme {
                Row(Modifier.fillMaxSize().background(Palette.Bg).padding(36.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(380.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Wi-Fi 頻譜分析儀", fontSize = 46.sp, fontWeight = FontWeight.Bold, color = Palette.Text)
                        Text("2.4 · 5 · 6 GHz 全頻段", fontSize = 24.sp, color = Palette.Accent)
                        Text("20–320 MHz 頻寬 · Wi-Fi 7 MLO", fontSize = 24.sp, color = Palette.Wifi7)
                        Text("頻道圖 · 訊號趨勢 · 頻道評分", fontSize = 20.sp, color = Palette.Text2)
                    }
                    Box(Modifier.weight(1f)) {
                        Panel { SpectrumChart(Band.GHZ_2_4, s.aps, setOf("04:d9:f5:6e:20:01"), zoomed = false, height = 380.dp, modifier = Modifier.padding(vertical = 8.dp)) }
                    }
                }
            }
        }
        shoot("feature_1024x500", 1024, 500)
    }
}
