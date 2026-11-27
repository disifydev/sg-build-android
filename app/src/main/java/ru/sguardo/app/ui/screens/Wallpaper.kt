package ru.sguardo.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sguardo.app.ui.Ic
import ru.sguardo.app.ui.LocalNav
import ru.sguardo.app.ui.LocalPerms
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.Sect
import ru.sguardo.app.ui.SubTopBar
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/** Готовые обои чата: ключ, название, цвета сверху и снизу (светлая / тёмная тема), рисовать ли узор. */
private data class Wall(val key: String, val name: String, val light: List<Long>, val dark: List<Long>, val pattern: Boolean)

private val WALLS = listOf(
    Wall("school", "Тетрадь", listOf(0, 0), listOf(0, 0), true),          // цвета из темы
    Wall("plain", "Без узора", listOf(0, 0), listOf(0, 0), false),
    Wall("mint", "Мята", listOf(0xFFD9F0E6, 0xFFEAF4DF), listOf(0xFF10241F, 0xFF16261C), true),
    Wall("lilac", "Сирень", listOf(0xFFE3DEFB, 0xFFF6DDEE), listOf(0xFF1B1830, 0xFF261A2A), true),
    Wall("peach", "Персик", listOf(0xFFFBE3D3, 0xFFF7D9E1), listOf(0xFF2A1C17, 0xFF2A1820), true),
    Wall("sky", "Небо", listOf(0xFFD8E8FA, 0xFFE4F3F5), listOf(0xFF111C2B, 0xFF102226), true),
    Wall("night", "Ночь", listOf(0xFF2B3550, 0xFF433052), listOf(0xFF0E1222, 0xFF1C1328), false),
)

fun wallpaperFile(ctx: android.content.Context) = File(ctx.filesDir, "chat-wallpaper.jpg")

/**
 * Обои чата, как в Telegram: по умолчанию мягкий градиент и узор из школьных мелочей; в «Ещё → Оформление чата»
 * можно выбрать другой цвет или своё фото. key — показать вариант в предпросмотре, не меняя настройку.
 */
@Composable
fun ChatWallpaper(modifier: Modifier = Modifier, key: String? = null) {
    val repo = LocalRepo.current
    val saved by repo.prefs.chatWall.collectAsState()
    val k = key ?: saved
    val c = Sg.c
    if (k == "custom") {
        val ctx = LocalContext.current
        val ver by repo.prefs.chatWallVer.collectAsState()
        val bmp = remember(ver) { runCatching { android.graphics.BitmapFactory.decodeFile(wallpaperFile(ctx).path)?.asImageBitmap() }.getOrNull() }
        if (bmp != null) {
            Box(modifier.clipToBounds()) {
                Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (c.dark) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)))
            }
            return
        }
    }
    val w = WALLS.firstOrNull { it.key == k } ?: WALLS[0]
    val (top, bottom) = if (w.light[0] == 0L) {
        lerp(c.surface, c.teal, if (c.dark) .07f else .10f) to lerp(c.surface, c.primary, if (c.dark) .03f else .05f)
    } else (if (c.dark) w.dark else w.light).map { Color(it) }.let { it[0] to it[1] }
    val night = w.key == "night"
    val ink = (if (night) Color.White else c.fg).copy(alpha = if (c.dark || night) .045f else .04f)
    Canvas(modifier.clipToBounds()) {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        if (night) stars()
        if (!w.pattern) return@Canvas
        val tile = 150f * density
        val stroke = Stroke(1.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        var row = 0
        var y = -tile * .3f
        while (y < size.height + tile) {
            var x = if (row % 2 == 0) -tile * .2f else tile * .3f
            while (x < size.width + tile) {
                translate(x, y) { doodles(tile, ink, stroke) }
                x += tile
            }
            y += tile * .82f; row++
        }
    }
}

private fun DrawScope.stars() {
    val r = java.util.Random(7)
    repeat(140) {
        val x = r.nextFloat() * size.width; val y = r.nextFloat() * size.height
        drawCircle(Color.White.copy(alpha = .15f + r.nextFloat() * .5f), (0.6f + r.nextFloat() * 1.2f) * density, Offset(x, y))
    }
}

private fun DrawScope.doodles(t: Float, ink: Color, s: Stroke) {
    val u = t / 150f
    rotate(-12f, Offset(30 * u, 28 * u)) {
        drawRoundRect(ink, Offset(14 * u, 16 * u), Size(32 * u, 24 * u), androidx.compose.ui.geometry.CornerRadius(3 * u), style = s)
        drawLine(ink, Offset(30 * u, 16 * u), Offset(30 * u, 40 * u), s.width, StrokeCap.Round)
    }
    rotate(38f, Offset(100 * u, 30 * u)) {
        drawRoundRect(ink, Offset(84 * u, 25 * u), Size(30 * u, 9 * u), androidx.compose.ui.geometry.CornerRadius(2 * u), style = s)
        val p = Path().apply { moveTo(84 * u, 25 * u); lineTo(76 * u, 29.5f * u); lineTo(84 * u, 34 * u) }
        drawPath(p, ink, style = s)
    }
    drawPath(star(Offset(62 * u, 78 * u), 10 * u, 4.3f * u), ink, style = s)
    drawPath(Path().apply { moveTo(112 * u, 82 * u); lineTo(119 * u, 89 * u); lineTo(133 * u, 73 * u) }, ink, style = s)
    drawCircle(ink, 5 * u, Offset(20 * u, 92 * u), style = s)
    drawCircle(ink, 2.4f * u, Offset(138 * u, 128 * u))
    rotate(-20f, Offset(40 * u, 128 * u)) {
        drawRoundRect(ink, Offset(20 * u, 122 * u), Size(42 * u, 11 * u), androidx.compose.ui.geometry.CornerRadius(2 * u), style = s)
        for (i in 1..4) drawLine(ink, Offset((20 + i * 8.4f) * u, 122 * u), Offset((20 + i * 8.4f) * u, (if (i % 2 == 0) 128 else 126) * u), s.width * .8f, StrokeCap.Round)
    }
    drawLine(ink, Offset(98 * u, 118 * u), Offset(98 * u, 132 * u), s.width, StrokeCap.Round)
    drawLine(ink, Offset(91 * u, 125 * u), Offset(105 * u, 125 * u), s.width, StrokeCap.Round)
}

private fun star(c: Offset, r: Float, r2: Float) = Path().apply {
    for (i in 0 until 10) {
        val a = Math.PI / 5 * i - Math.PI / 2
        val rr = if (i % 2 == 0) r else r2
        val p = Offset(c.x + (cos(a) * rr).toFloat(), c.y + (sin(a) * rr).toFloat())
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/** «Ещё → Оформление чата»: предпросмотр с пузырями, готовые обои и своё фото. */
@Composable
fun ChatStyleScreen() {
    val repo = LocalRepo.current; val nav = LocalNav.current; val perms = LocalPerms.current; val ctx = LocalContext.current
    val cur by repo.prefs.chatWall.collectAsState()
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        SubTopBar("Обои для чатов", { nav.back() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Box(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(24.dp))) {
                ChatWallpaper(Modifier.fillMaxSize())
                Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                    SampleBubble("Добрый день! Можно перенести занятие на четверг?", false)
                    SampleBubble("Да, давайте в 17:00", true)
                }
            }
            Sect("Обои", top = 18.dp)
            val tiles = listOf("custom") + WALLS.map { it.key }
            tiles.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { k ->
                        val on = k == cur
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().aspectRatio(.72f).clip(RoundedCornerShape(16.dp))
                                .border(if (on) 2.5.dp else 1.dp, if (on) Sg.c.primary else Sg.c.hair, RoundedCornerShape(16.dp))
                                .clickable {
                                    if (k == "custom") perms.pickImage { uri ->
                                        if (uri == null) return@pickImage
                                        repo.scope.launch {
                                            val ok = withContext(Dispatchers.IO) {
                                                runCatching {
                                                    val tmp = copyToCache(ctx, uri)?.first ?: return@runCatching false
                                                    compressImage(ctx, tmp).copyTo(wallpaperFile(ctx), overwrite = true); true
                                                }.getOrDefault(false)
                                            }
                                            if (ok) { repo.prefs.setChatWall("custom"); repo.prefs.bumpChatWall(); repo.toast("Обои установлены") } else repo.toast("Не получилось прочитать фото")
                                        }
                                    } else repo.prefs.setChatWall(k)
                                }) {
                                if (k == "custom" && !wallpaperFile(ctx).exists()) Box(Modifier.fillMaxSize().background(Sg.c.cHigh), contentAlignment = Alignment.Center) { Ic("image", Sg.c.primary, size = 28.dp) }
                                else ChatWallpaper(Modifier.fillMaxSize(), key = k)
                                if (on) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp).clip(CircleShape).background(Sg.c.primary), contentAlignment = Alignment.Center) { Ic("check", Sg.c.onPrimary, size = 13.dp) }
                            }
                            Spacer(Modifier.height(5.dp))
                            T(if (k == "custom") "Своё фото" else WALLS.first { it.key == k }.name, body(12.5f), if (on) Sg.c.primary else Sg.c.muted, maxLines = 1)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SampleBubble(text: String, out: Boolean) {
    val shape = bubbleShape(out, top = false, bottom = false, big = LocalChatStyle.current.radius)
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = if (out) Arrangement.End else Arrangement.Start) {
        Box(Modifier.widthIn(max = 240.dp).bubbleBg(if (out) chatOut() else Sg.c.cHigh, out, shape, true).clip(shape).padding(horizontal = 12.dp, vertical = 8.dp)) {
            T(text, body(14.5f * LocalChatStyle.current.text), if (out) chatOutFg() else Sg.c.fg)
        }
    }
}
