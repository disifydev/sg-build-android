package ru.sguardo.app.ui.screens

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import ru.sguardo.app.ui.theme.Sg
import kotlin.math.cos
import kotlin.math.sin

/**
 * Обои чата, как в Telegram: мягкий градиент и еле заметный узор из школьных мелочей
 * (книга, карандаш, звезда, линейка, галочка, кружки). Рисуется один раз на холсте, без картинок.
 */
@Composable
fun ChatWallpaper(modifier: Modifier = Modifier) {
    val c = Sg.c
    val top = lerp(c.surface, c.teal, if (c.dark) .07f else .10f)
    val bottom = lerp(c.surface, c.primary, if (c.dark) .03f else .05f)
    val ink = c.fg.copy(alpha = if (c.dark) .045f else .04f)
    Canvas(modifier.clipToBounds()) {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
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

private fun DrawScope.doodles(t: Float, ink: Color, s: Stroke) {
    val u = t / 150f
    // книга
    rotate(-12f, Offset(30 * u, 28 * u)) {
        drawRoundRect(ink, Offset(14 * u, 16 * u), Size(32 * u, 24 * u), androidx.compose.ui.geometry.CornerRadius(3 * u), style = s)
        drawLine(ink, Offset(30 * u, 16 * u), Offset(30 * u, 40 * u), s.width, StrokeCap.Round)
    }
    // карандаш
    rotate(38f, Offset(100 * u, 30 * u)) {
        drawRoundRect(ink, Offset(84 * u, 25 * u), Size(30 * u, 9 * u), androidx.compose.ui.geometry.CornerRadius(2 * u), style = s)
        val p = Path().apply { moveTo(84 * u, 25 * u); lineTo(76 * u, 29.5f * u); lineTo(84 * u, 34 * u) }
        drawPath(p, ink, style = s)
    }
    // звезда
    drawPath(star(Offset(62 * u, 78 * u), 10 * u, 4.3f * u), ink, style = s)
    // галочка
    drawPath(Path().apply { moveTo(112 * u, 82 * u); lineTo(119 * u, 89 * u); lineTo(133 * u, 73 * u) }, ink, style = s)
    // кружки
    drawCircle(ink, 5 * u, Offset(20 * u, 92 * u), style = s)
    drawCircle(ink, 2.4f * u, Offset(138 * u, 128 * u))
    // линейка
    rotate(-20f, Offset(40 * u, 128 * u)) {
        drawRoundRect(ink, Offset(20 * u, 122 * u), Size(42 * u, 11 * u), androidx.compose.ui.geometry.CornerRadius(2 * u), style = s)
        for (i in 1..4) drawLine(ink, Offset((20 + i * 8.4f) * u, 122 * u), Offset((20 + i * 8.4f) * u, (if (i % 2 == 0) 128 else 126) * u), s.width * .8f, StrokeCap.Round)
    }
    // плюс
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
