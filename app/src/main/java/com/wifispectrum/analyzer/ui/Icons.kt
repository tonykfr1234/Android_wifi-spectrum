package com.wifispectrum.analyzer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Line icons drawn on a 24-unit grid, so the app needs no icon library. */
object Icons {
    private fun icon(name: String, vararg paths: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        for (d in paths) addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

    val List = icon("list", "M8 6h12", "M8 12h12", "M8 18h12", "M4 6h.01", "M4 12h.01", "M4 18h.01")
    val Spectrum = icon("spectrum", "M2 20h20", "M3 20C5 6 9 6 11 20", "M9 20c2-10 6-10 8 0", "M14 20c1.5-6 4.5-6 6 0")
    val Timeline = icon("timeline", "M3 3v18h18", "M7 15l4-5 3 3 5-7")
    val Star = icon("star", "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z")
    val Mlo = icon("mlo", "M5 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0", "M13 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0", "M12 4v3", "M12 17v3", "M8 4h8", "M8 20h8")
    val Refresh = icon("refresh", "M20 11a8 8 0 1 0-2.3 5.7", "M20 4v7h-7")
    val Pause = icon("pause", "M8 5v14", "M16 5v14")
    val Play = icon("play", "M7 4l13 8-13 8z")
    val More = icon("more", "M12 5h.01", "M12 12h.01", "M12 19h.01")
    val Back = icon("back", "M15 5l-7 7 7 7")
    val Search = icon("search", "M10.5 4a6.5 6.5 0 1 0 0 13a6.5 6.5 0 1 0 0-13", "M20 20l-4.8-4.8")
    val Close = icon("close", "M6 6l12 12", "M18 6L6 18")
    val Wifi = icon("wifi", "M2 9a15 15 0 0 1 20 0", "M5 12.5a10 10 0 0 1 14 0", "M8.5 16a5 5 0 0 1 7 0", "M12 19.5h.01")
}
