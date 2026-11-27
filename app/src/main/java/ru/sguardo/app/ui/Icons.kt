package ru.sguardo.app.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Иконки макета (константа IC): контурные, 24×24, скруглённые концы.
 * Элемент: "P:путь" | "R:x,y,w,h,rx" | "C:cx,cy,r"; после буквы флаги f — залить, n — без обводки.
 */
private val IC: Map<String, List<String>> = mapOf(
    "grid" to listOf("R:3,3,7.5,7.5,2.2", "R:13.5,3,7.5,7.5,2.2", "R:3,13.5,7.5,7.5,2.2", "R:13.5,13.5,7.5,7.5,2.2"),
    "chat" to listOf("P:M21 11.5a8.5 8.5 0 0 1-12.6 7.4L3 20.5l1.6-5A8.5 8.5 0 1 1 21 11.5z"),
    "users" to listOf("P:M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", "C:9,7,4", "P:M22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8"),
    "card" to listOf("R:2,5,20,14,3", "P:M2 10h20M6 15h4"),
    "cal" to listOf("R:3,4,18,18,3", "P:M16 2v4M8 2v4M3 10h18"),
    "send" to listOf("P:M22 2 11 13M22 2l-7 20-4-9-9-4z"),
    "shield" to listOf("P:M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"),
    "ell" to listOf("C:12,12,9.5", "Cf:7.8,12,.9", "Cf:12,12,.9", "Cf:16.2,12,.9"),
    "more" to listOf("Cfn:5,12,1.7", "Cfn:12,12,1.7", "Cfn:19,12,1.7"),
    "morev" to listOf("Cfn:12,5,1.7", "Cfn:12,12,1.7", "Cfn:12,19,1.7"),
    "search" to listOf("C:11,11,7", "P:m20.5 20.5-4.2-4.2"),
    "bell" to listOf("P:M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "P:M10.3 21a1.94 1.94 0 0 0 3.4 0"),
    "plus" to listOf("P:M12 5v14M5 12h14"),
    "chevL" to listOf("P:m15 18-6-6 6-6"), "chevR" to listOf("P:m9 18 6-6-6-6"), "chevD" to listOf("P:m6 9 6 6 6-6"),
    "chevU" to listOf("P:m18 15-6-6-6 6"),
    "arrowL" to listOf("P:M20 12H4M11 19l-7-7 7-7"),
    "mic" to listOf("R:9,2,6,12,3", "P:M19 10v1a7 7 0 0 1-14 0v-1M12 18v4"),
    "clip" to listOf("P:m21.4 11-9.2 9.2a6 6 0 0 1-8.5-8.5l8.6-8.6A4 4 0 1 1 18 8.8l-8.6 8.6a2 2 0 0 1-2.8-2.8l8.5-8.5"),
    "clock" to listOf("C:12,12,9.5", "P:M12 6.5V12l3.5 2"),
    "bolt" to listOf("P:M13 2 3 14h9l-1 8 10-12h-9z"),
    "check" to listOf("P:M20 6 9 17l-5-5"),
    "checks" to listOf("P:M17 7 6.5 17.5 2 13M22 7.5 13 16.5l-1-1"),
    "x" to listOf("P:M18 6 6 18M6 6l12 12"),
    "wifiOff" to listOf("P:m2 2 20 20M8.5 16.5a5 5 0 0 1 7 0M2 8.8a15 15 0 0 1 4.2-2.6M10.7 5c4-.4 8.1.9 11.3 3.8M16.9 11.3a10 10 0 0 1 2.2 1.6M5 13a10 10 0 0 1 5.2-2.8M12 20h.01"),
    "alert" to listOf("C:12,12,9.5", "P:M12 7.5v5M12 16.2h.01"),
    "refresh" to listOf("P:M3 12a9 9 0 0 1 15.4-6.4L21 8M21 3v5h-5M21 12a9 9 0 0 1-15.4 6.4L3 16M3 21v-5h5"),
    "userPlus" to listOf("P:M15 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", "C:8.5,7,4", "P:M19 8v6M22 11h-6"),
    "user" to listOf("C:12,8,4", "P:M20 21a8 8 0 0 0-16 0"),
    "hour" to listOf("P:M5 22h14M5 2h14M17 22v-4.2a2 2 0 0 0-.6-1.4L12 12l-4.4 4.4a2 2 0 0 0-.6 1.4V22M7 2v4.2a2 2 0 0 0 .6 1.4L12 12l4.4-4.4a2 2 0 0 0 .6-1.4V2"),
    "link" to listOf("P:M10 13a5 5 0 0 0 7.5.5l3-3a5 5 0 0 0-7-7l-1.8 1.7M14 11a5 5 0 0 0-7.5-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7"),
    "edit" to listOf("P:M17 3a2.8 2.8 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5z"),
    "play" to listOf("Pf:M8 5.5v13l11-6.5z"),
    "pause" to listOf("Rfn:6.5,5,4,14,1", "Rfn:13.5,5,4,14,1"),
    "book" to listOf("P:M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20", "C:12,9,2.5", "P:M8.5 14.5a3.5 3.5 0 0 1 7 0"),
    "smile" to listOf("C:12,12,9.5", "P:M8 14.5s1.5 2 4 2 4-2 4-2M9 9.5h.01M15 9.5h.01"),
    "trend" to listOf("P:m22 7-8.5 8.5-5-5L2 17", "P:M16 7h6v6"),
    "inbox" to listOf("P:M22 12h-6l-2 3h-4l-2-3H2", "P:M5.5 5.1 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.5-6.9A2 2 0 0 0 16.8 4H7.2a2 2 0 0 0-1.7 1.1z"),
    "wallet" to listOf("P:M19 7V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-4", "P:M21 11h-5a2 2 0 0 0 0 4h5z"),
    "msgSq" to listOf("P:M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"),
    "pin" to listOf("P:M12 17v5M9 10.8V4h6v6.8l3 3.2H6z"),
    "ext" to listOf("P:M15 3h6v6M10 14 21 3M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"),
    "mail" to listOf("R:2,4,20,16,3", "P:m22 7-10 6L2 7"),
    "moon" to listOf("P:M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9z"),
    "phone" to listOf("R:6,2,12,20,3", "P:M11 18h2"),
    "laptop" to listOf("R:4,4,16,11,2", "P:M2 19h20"),
    "star" to listOf("P:m12 2 3 6.5 7 .8-5.2 4.8 1.4 7L12 17.5 5.8 21l1.4-7L2 9.3l7-.8z"),
    "bellOff" to listOf("P:M8.7 3A6 6 0 0 1 18 8c0 2 .2 3.7.6 5M17 17H3s3-2 3-9c0-.6.1-1.2.3-1.7M10.3 21a1.94 1.94 0 0 0 3.4 0M2 2l20 20"),
    "archive" to listOf("R:2,3,20,5,1.5", "P:M4 8v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8M10 12h4"),
    "trash" to listOf("P:M3 6h18M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"),
    "reply" to listOf("P:M9 17 4 12l5-5", "P:M20 18v-2a4 4 0 0 0-4-4H4"),
    "copy" to listOf("R:9,9,13,13,2", "P:M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"),
    "fwd" to listOf("P:m15 17 5-5-5-5", "P:M4 18v-2a4 4 0 0 1 4-4h12"),
    "selc" to listOf("C:12,12,9.5", "P:m8 12 3 3 5-6"),
    "save" to listOf("P:M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3"),
    "lock" to listOf("R:4,11,16,11,2.5", "P:M8 11V7a4 4 0 0 1 8 0v4"),
    "lockO" to listOf("R:4,11,16,11,2.5", "P:M8 11V7a4 4 0 0 1 7.8-1.3"),
    "flip" to listOf("P:M20 5h-3.2L15 3H9L7.2 5H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2z", "P:M9 12.5a3 3 0 0 0 5.6 1.5M15 11.5a3 3 0 0 0-5.6-1.5M14.6 16v-2h-2M9.4 8v2h2"),
    "vcirc" to listOf("C:12,12,9.5", "R:6.5,9.5,7.5,5,1.3", "P:m14 11.3 3.5-1.8v5l-3.5-1.8"),
    "heart" to listOf("P:M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7z"),
    "leaf" to listOf("P:M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z", "P:M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"),
    "cloud" to listOf("P:M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9z"),
    "sun" to listOf("C:12,12,4", "P:M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"),
    "camera" to listOf("P:M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z", "C:12,13,3.5"),
    "image" to listOf("R:3,3,18,18,3", "C:9,9,2", "P:m21 15-5-5L5 21"),
    "file" to listOf("P:M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z", "P:M14 2v6h6"),
    "sliders" to listOf("P:M4 6h9M17 6h3M4 12h3M11 12h9M4 18h11M19 18h1", "C:15,6,2", "C:9,12,2", "C:17,18,2"),
    "grip" to listOf("Cfn:9,6,1.3", "Cfn:15,6,1.3", "Cfn:9,12,1.3", "Cfn:15,12,1.3", "Cfn:9,18,1.3", "Cfn:15,18,1.3"),
)

private fun f(v: String) = v.toFloat()
private fun rectPath(x: Float, y: Float, w: Float, h: Float, r: Float): String {
    val rr = minOf(r, w / 2, h / 2)
    return "M${x + rr} ${y}H${x + w - rr}A$rr $rr 0 0 1 ${x + w} ${y + rr}V${y + h - rr}A$rr $rr 0 0 1 ${x + w - rr} ${y + h}" +
        "H${x + rr}A$rr $rr 0 0 1 $x ${y + h - rr}V${y + rr}A$rr $rr 0 0 1 ${x + rr} ${y}Z"
}
private fun circlePath(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}A$r $r 0 1 0 ${cx + r} ${cy}A$r $r 0 1 0 ${cx - r} ${cy}Z"

private val cache = HashMap<String, ImageVector>()

fun icon(name: String, stroke: Float = 1.9f): ImageVector = cache.getOrPut("$name/$stroke") {
    val b = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
    for (el in IC[name] ?: emptyList()) {
        val head = el.substringBefore(':')
        val data = el.substringAfter(':')
        val fill = head.contains('f')
        val noStroke = head.contains('n')
        val d = when (head[0]) {
            'R' -> data.split(',').let { rectPath(f(it[0]), f(it[1]), f(it[2]), f(it[3]), f(it[4])) }
            'C' -> data.split(',').let { circlePath(f(it[0]), f(it[1]), f(it[2])) }
            else -> data
        }
        b.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = if (fill) SolidColor(Color.Black) else null,
            stroke = if (noStroke) null else SolidColor(Color.Black),
            strokeLineWidth = stroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    b.build()
}

/** Иконка макета нужного размера; sm = 16dp (обводка 2), xs = 13dp (обводка 2.2). */
@Composable
fun Ic(name: String, tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val stroke = when {
        size <= 14.dp -> 2.2f
        size <= 18.dp -> 2f
        else -> 1.9f
    }
    Icon(icon(name, stroke), contentDescription = null, tint = tint, modifier = modifier.size(size))
}
