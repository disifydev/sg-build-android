package ru.sguardo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.sguardo.app.ui.theme.Onest
import ru.sguardo.app.ui.theme.Sg

/** Соцсети: подписи и значки из макета (константа NET). */
object Nets {
    val keys = listOf("telegram", "whatsapp", "instagram", "vk", "max", "avito")
    fun name(n: String) = when (n) {
        "telegram" -> "Telegram"; "instagram" -> "Instagram"; "whatsapp" -> "WhatsApp"; "tiktok" -> "TikTok"
        "threads" -> "Threads"; "vk" -> "VK"; "avito" -> "Авито"; "max" -> "MAX"; "profi" -> "Профи"; else -> n
    }
}

private val pathCache = HashMap<String, Path>()
private fun p(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

private const val TG = "M20.7 4.3 3 11.2c-1 .4-1 1.1 0 1.4l4.4 1.4 1.7 5.3c.2.6.5.7 1 .3l2.5-2.1 4.7 3.5c.7.4 1.2.2 1.4-.7l3-14c.3-1.2-.4-1.7-1-1.3zM9.2 14.1l8.5-6.1-6.6 7-.4 3z"
private const val WA = "M12 2.5a9.5 9.5 0 0 0-8.2 14.3L2.5 21.5l4.8-1.3A9.5 9.5 0 1 0 12 2.5zm4.7 13.1c-.2.6-1.2 1.1-1.7 1.1-.4.1-1 .1-1.6-.1a14 14 0 0 1-5.6-4.9c-.4-.6-.8-1.5-.8-2.4 0-.9.5-1.4.7-1.6.2-.2.4-.3.6-.3h.4c.1 0 .3 0 .5.4l.7 1.7c.1.1.1.3 0 .4l-.3.5-.3.4c-.1.1-.2.3-.1.5.1.2.6 1 1.3 1.6.9.8 1.6 1 1.9 1.1.2.1.4.1.5-.1l.7-.9c.2-.2.3-.2.5-.1l1.6.8c.2.1.4.2.4.3.1.1.1.6-.1 1.2z"
private const val TT = "M16.6 2h-3.3v13.4a2.9 2.9 0 1 1-2.9-2.9c.3 0 .6 0 .9.1V9.2a6.3 6.3 0 1 0 5.3 6.2V8.7a7.6 7.6 0 0 0 4.4 1.4V6.8a4.4 4.4 0 0 1-4.4-4.4z"
private const val TH = "M17 8.5C16 5.5 14 4 11.5 4 7.5 4 5 7 5 12s2.5 8 6.5 8c3 0 5.5-1.7 5.5-4.6 0-4.2-6.5-4.6-6.5-1.2 0 1.5 1.3 2.2 2.6 2.1 2-.1 3-1.8 3-4.6"
private const val VK = "M12.9 17.5C6.9 17.5 3.4 13.4 3.3 6.5h3c.1 5 2.4 7.2 4.2 7.6V6.5h2.9v4.3c1.8-.2 3.6-2.2 4.3-4.3h2.8a8.3 8.3 0 0 1-3.8 5.4 8.6 8.6 0 0 1 4.5 5.6h-3.1a5.4 5.4 0 0 0-4.7-3.9v3.9z"
private const val MX = "M5 17V7l7 7 7-7v10"

private fun DrawScope.glyph(net: String) {
    val k = size.minDimension / 24f
    scale(k, k, pivot = Offset.Zero) {
        val w = Color.White
        when (net) {
            "telegram" -> drawPath(p(TG), w)
            "whatsapp" -> drawPath(p(WA), w)
            "tiktok" -> drawPath(p(TT), w)
            "vk" -> drawPath(p(VK), w)
            "threads" -> drawPath(p(TH), w, style = Stroke(2.3f, cap = StrokeCap.Round))
            "max" -> drawPath(p(MX), w, style = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            "instagram" -> {
                drawRoundRect(w, topLeft = Offset(4f, 4f), size = androidx.compose.ui.geometry.Size(16f, 16f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f), style = Stroke(2.4f))
                drawCircle(w, 3.8f, Offset(12f, 12f), style = Stroke(2.4f))
            }
            "avito" -> {
                drawCircle(Color(0xFF00AAFF), 4f, Offset(8f, 8f)); drawCircle(Color(0xFFFF4053), 2.6f, Offset(16.5f, 8.5f))
                drawCircle(Color(0xFF965EEB), 2.6f, Offset(8.5f, 16.5f)); drawCircle(Color(0xFF97CF26), 3.4f, Offset(16f, 16f))
            }
            else -> drawCircle(w, 4f, Offset(12f, 12f))
        }
    }
}

fun netBrush(net: String): Brush = when (net) {
    "telegram" -> Brush.linearGradient(listOf(Color(0xFF29A9EB), Color(0xFF29A9EB)))
    "whatsapp" -> Brush.linearGradient(listOf(Color(0xFF25D366), Color(0xFF25D366)))
    "instagram" -> Brush.radialGradient(
        0f to Color(0xFFFDF497), 0.45f to Color(0xFFFD5949), 0.6f to Color(0xFFD6249F), 0.9f to Color(0xFF285AEB),
        center = Offset(0.3f * 60f, 1.07f * 60f), radius = 80f,
    )
    "tiktok", "threads" -> Brush.linearGradient(listOf(Color(0xFF111111), Color(0xFF111111)))
    "vk" -> Brush.linearGradient(listOf(Color(0xFF0077FF), Color(0xFF0077FF)))
    "avito" -> Brush.linearGradient(listOf(Color.White, Color.White))
    "max" -> Brush.linearGradient(listOf(Color(0xFF3C8CFF), Color(0xFF7B3CF5)))
    "profi" -> Brush.linearGradient(listOf(Color(0xFFFF7A00), Color(0xFFFF7A00)))
    else -> Brush.linearGradient(listOf(Color.Gray, Color.Gray))
}

/** Круглый значок сети. */
@Composable
fun NetDot(net: String, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(netBrush(net)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size * 0.58f)) { glyph(net) }
    }
}

private val PAL = listOf(0xFFE3D9C6, 0xFFC9D9E0, 0xFFF2C7B0, 0xFFD9D0EA, 0xFFC7E2D3, 0xFFF1E0A8, 0xFFEFC4C4, 0xFFD6E3C6).map { Color(it) }
fun colFor(name: String): Color = PAL[name.sumOf { it.code } % PAL.size]
fun initials(name: String) = name.split(Regex("\\s+")).mapNotNull { w -> w.firstOrNull { it.isLetterOrDigit() } }.take(2).joinToString("").uppercase()

/** Аватар: инициалы на пастельном круге, значок сети в углу (как .av и .nb). */
@Composable
fun Avatar(name: String, size: Dp, net: String? = null, color: Color? = null, modifier: Modifier = Modifier, ring: Color = Sg.c.surface) {
    Box(modifier.size(size)) {
        Box(Modifier.size(size).clip(CircleShape).background(color ?: colFor(name)), contentAlignment = Alignment.Center) {
            if (name.startsWith("+")) Ic("user", Color(0xFF14211F), size = size * 0.36f)
            else Text(initials(name), color = Color(0xFF14211F), fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.34f).sp)
        }
        if (net != null) {
            val b = if (size >= 90.dp) 30.dp else 19.dp
            NetDot(net, b, Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).border(2.dp, ring, CircleShape).clip(CircleShape))
        }
    }
}
