package ru.sguardo.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Удаление «пылью», как в Telegram: пузырь снимается в картинку, режется на крошки,
 * крошки слева направо улетают вверх и гаснут, затем сообщение действительно удаляется.
 */
object Dust {
    /** id сообщений, которые сейчас рассыпаются */
    val ids = mutableStateMapOf<String, Boolean>()
    const val MS = 950

    fun run(ids: List<String>, then: () -> Unit) {
        ids.forEach { this.ids[it] = true }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ then(); ids.forEach { this.ids.remove(it) } }, MS.toLong() + 60)
    }
}

private class Grain(val x: Float, val y: Float, val color: Color, val delay: Float, val vx: Float, val vy: Float, val life: Float)

@Composable
fun DustBox(active: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    var grains by remember { mutableStateOf<List<Grain>?>(null) }
    var cell by remember { mutableStateOf(1f) }
    val t = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (!active) { grains = null; t.snapTo(0f); return@LaunchedEffect }
        val bmp = runCatching { layer.toImageBitmap() }.getOrNull()
        if (bmp == null || bmp.width == 0 || bmp.height == 0) { grains = emptyList(); return@LaunchedEffect }
        val w = bmp.width; val h = bmp.height
        val px = IntArray(w * h)
        bmp.readPixels(px, 0, 0, w, h)
        val step = with(density) { 2.6f * this.density }.toInt().coerceAtLeast(2)
        cell = step.toFloat()
        val rnd = Random(w * 31 + h)
        val up = with(density) { 70f * this.density }
        val side = with(density) { 36f * this.density }
        val out = ArrayList<Grain>((w / step) * (h / step))
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val c = px[(y + step / 2).coerceAtMost(h - 1) * w + (x + step / 2).coerceAtMost(w - 1)]
                val a = (c ushr 24) and 0xFF
                if (a > 24) {
                    // волна идёт слева направо с небольшим разбросом
                    val d = x.toFloat() / w * .55f + rnd.nextFloat() * .12f
                    out += Grain(x.toFloat(), y.toFloat(), Color(c), d, (rnd.nextFloat() * 1.3f - .3f) * side, -(.35f + rnd.nextFloat()) * up, .35f + rnd.nextFloat() * .25f)
                }
                x += step
            }
            y += step
        }
        grains = out
        t.snapTo(0f)
        t.animateTo(1f, tween(Dust.MS, easing = LinearEasing))
        delay(50)
    }
    Box(modifier.drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        val g = grains
        if (!active || g == null) { drawLayer(layer); return@drawWithContent }
        val p = t.value
        val sz = Size(cell, cell)
        for (gr in g) {
            val k = ((p - gr.delay) / gr.life).coerceIn(0f, 1f)
            if (k >= 1f) continue
            val e = k * k
            val alpha = (1f - k) * gr.color.alpha
            drawRect(gr.color.copy(alpha = alpha), Offset(gr.x + gr.vx * e, gr.y + gr.vy * e), if (k > 0f) Size(cell * (1f - k * .5f), cell * (1f - k * .5f)) else sz)
        }
    }) { content() }
}

/** Чтобы сообщение не «прилетало» дважды, когда временное заменяется настоящим с сервера. */
object Arrived {
    private val recent = LinkedHashMap<String, Long>()
    fun claim(m: ru.sguardo.app.data.Message): Boolean {
        val now = System.currentTimeMillis()
        recent.entries.removeAll { now - it.value > 20_000 }
        val key = "${m.dir}|${m.text}|${m.att?.kind}|${m.att?.name}"
        if (recent.containsKey(key)) return false
        recent[key] = now; return true
    }
}
