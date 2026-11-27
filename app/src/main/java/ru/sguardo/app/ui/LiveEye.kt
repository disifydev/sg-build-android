package ru.sguardo.app.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

private const val COLS = 9
private const val ROWS = 7

/**
 * Живой 3D-глаз Sguardo: кадры отрендерены заранее (logo/eyeframes.py, 9×7 направлений взгляда),
 * взгляд переводится быстрыми скачками (как саккады у живого глаза), без наложения кадров. Сам водит взглядом; если задан look (x, y от -1 до 1, y вниз),
 * смотрит туда. Вокруг вращается стеклянное кольцо, задняя половина за глазом, передняя перед ним.
 */
@Composable
fun LiveEye(size: Dp, look: Offset? = null, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    fun load(c: Int, r: Int): ImageBitmap? = runCatching {
        val id = ctx.resources.getIdentifier("eye_g_${c}_$r", "drawable", ctx.packageName)
        BitmapFactory.decodeResource(ctx.resources, id)?.asImageBitmap()
    }.getOrNull()
    val all = android.os.Build.FINGERPRINT == "robolectric"
    // центральный кадр сразу (чтобы глаз был с первого кадра), остальные следом в фоне; в снимках — все сразу
    var frames by remember { mutableStateOf<List<ImageBitmap?>>(List(ROWS * COLS) { i -> if (all || i == (ROWS / 2) * COLS + COLS / 2) load(i % COLS, i / COLS) else null }) }
    LaunchedEffect(Unit) {
        if (frames.all { it != null }) return@LaunchedEffect
        frames = withContext(Dispatchers.IO) { List(ROWS * COLS) { i -> frames[i] ?: load(i % COLS, i / COLS) } }
    }
    val gx = remember { Animatable(0f) }
    val gy = remember { Animatable(0f) }
    // взгляд: по цели или сам по себе — то медленно ведёт, то быстро переводит, как живой
    LaunchedEffect(look) {
        if (look != null) {
            launch { gx.animateTo(look.x.coerceIn(-1f, 1f), spring(dampingRatio = 1f, stiffness = 700f)) }
            gy.animateTo(look.y.coerceIn(-1f, 1f), spring(dampingRatio = 1f, stiffness = 700f))
            return@LaunchedEffect
        }
        val rnd = Random(System.nanoTime())
        while (true) {
            // чаще короткий взгляд рядом, иногда далеко; между скачками глаз «замирает» и разглядывает
            val far = rnd.nextFloat() < .4f
            val tx = if (far) (rnd.nextFloat() * 2 - 1) * .9f else (gx.value + (rnd.nextFloat() * 2 - 1) * .35f).coerceIn(-.9f, .9f)
            val ty = if (far) (rnd.nextFloat() * 2 - 1) * .8f else (gy.value + (rnd.nextFloat() * 2 - 1) * .3f).coerceIn(-.8f, .8f)
            val spec = spring<Float>(dampingRatio = 1f, stiffness = if (far) 600f else 900f)
            launch { gx.animateTo(tx, spec) }
            gy.animateTo(ty, spec)
            delay(450L + rnd.nextLong(1500))
        }
    }
    val inf = rememberInfiniteTransition(label = "eye")
    val spin by inf.animateFloat(0f, (Math.PI * 2).toFloat(), infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")
    val bob by inf.animateFloat(0f, (Math.PI * 2).toFloat(), infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "bob")
    Canvas(modifier.size(size)) {
        val w = size.toPx()
        val eyeD = w * .62f
        val cx = w / 2; val cy = w / 2 + sin(bob) * w * .025f
        val ringR = eyeD / 2 * 1.42f
        val tilt = .42f + sin(bob) * .05f
        ring(cx, cy, ringR, tilt, spin, front = false, w = eyeD * .045f)
        val f = frames
        val center = f[(ROWS / 2) * COLS + COLS / 2]
        if (center != null) {
            val fx = (gx.value + 1) / 2 * (COLS - 1); val fy = (gy.value + 1) / 2 * (ROWS - 1)
            val c0 = kotlin.math.round(fx).toInt().coerceIn(0, COLS - 1); val r0 = kotlin.math.round(fy).toInt().coerceIn(0, ROWS - 1)
            val main = f[r0 * COLS + c0] ?: center
            val dst = IntSize(eyeD.toInt(), eyeD.toInt()); val off = IntOffset((cx - eyeD / 2).toInt(), (cy - eyeD / 2).toInt())
            drawImage(main, dstOffset = off, dstSize = dst, filterQuality = androidx.compose.ui.graphics.FilterQuality.High)
        } else {
            drawCircle(Color(0xFFF3F1FF), eyeD / 2, Offset(cx, cy))
        }
        ring(cx, cy, ringR, tilt, spin, front = true, w = eyeD * .045f)
    }
}

/** Кольцо как орбита: нормаль наклонена к зрителю и прецессирует; рисуем заднюю или переднюю половину. */
private fun DrawScope.ring(cx: Float, cy: Float, r: Float, tilt: Float, spin: Float, front: Boolean, w: Float) {
    // нормаль кольца
    val nx = sin(tilt) * cos(spin) * .55f; val ny = cos(tilt); val nz = sin(tilt) * sin(spin) * .55f + .35f
    val nl = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)
    val n = floatArrayOf(nx / nl, ny / nl, nz / nl)
    // базис в плоскости кольца
    var e1 = floatArrayOf(n[2], 0f, -n[0]); var l1 = kotlin.math.sqrt(e1[0] * e1[0] + e1[2] * e1[2]); if (l1 < 1e-4f) { e1 = floatArrayOf(1f, 0f, 0f); l1 = 1f }
    e1 = floatArrayOf(e1[0] / l1, 0f, e1[2] / l1)
    val e2 = floatArrayOf(n[1] * e1[2] - n[2] * e1[1], n[2] * e1[0] - n[0] * e1[2], n[0] * e1[1] - n[1] * e1[0])
    val steps = 120
    var px = 0f; var py = 0f; var pz = 0f
    for (i in 0..steps) {
        val a = (i.toFloat() / steps) * (Math.PI * 2).toFloat()
        val x = (cos(a) * e1[0] + sin(a) * e2[0]) * r
        val y = (cos(a) * e1[1] + sin(a) * e2[1]) * r
        val z = (cos(a) * e1[2] + sin(a) * e2[2])
        if (i > 0) {
            val mz = (z + pz) / 2
            if ((mz >= 0) == front) {
                val light = (.72f + .28f * mz).coerceIn(0f, 1f)
                val col = Color(.80f * light + .18f, .78f * light + .18f, .98f, if (front) .95f else .55f)
                drawLine(col, Offset(cx + px, cy - py), Offset(cx + x, cy - y), w, StrokeCap.Round)
            }
        }
        px = x; py = y; pz = z
    }
    // блик на передней дуге
    if (front) {
        val a = 1.1f
        val x = (cos(a) * e1[0] + sin(a) * e2[0]) * r; val y = (cos(a) * e1[1] + sin(a) * e2[1]) * r
        val z = cos(a) * e1[2] + sin(a) * e2[2]
        if (z > 0) drawCircle(Color.White.copy(alpha = .55f * z), w * .6f, Offset(cx + x, cy - y))
    }
}
