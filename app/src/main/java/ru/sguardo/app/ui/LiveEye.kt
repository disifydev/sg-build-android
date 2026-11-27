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

private const val COLS = 7
private const val ROWS = 5

/**
 * Живой 3D-глаз Sguardo: кадры отрендерены заранее (logo/eyeframes.py, 7×5 направлений взгляда),
 * между соседними кадрами плавный переход. Сам водит взглядом; если задан look (x, y от -1 до 1, y вниз),
 * смотрит туда. Вокруг вращается стеклянное кольцо, задняя половина за глазом, передняя перед ним.
 */
@Composable
fun LiveEye(size: Dp, look: Offset? = null, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var frames by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    LaunchedEffect(Unit) {
        frames = withContext(Dispatchers.IO) {
            runCatching {
                (0 until ROWS).flatMap { r -> (0 until COLS).map { c ->
                    val id = ctx.resources.getIdentifier("eye_g_${c}_$r", "drawable", ctx.packageName)
                    BitmapFactory.decodeResource(ctx.resources, id).asImageBitmap()
                } }
            }.getOrNull()
        }
    }
    val gx = remember { Animatable(0f) }
    val gy = remember { Animatable(0f) }
    // взгляд: по цели или сам по себе — то медленно ведёт, то быстро переводит, как живой
    LaunchedEffect(look) {
        if (look != null) {
            launch { gx.animateTo(look.x.coerceIn(-1f, 1f), spring(dampingRatio = .7f, stiffness = 120f)) }
            gy.animateTo(look.y.coerceIn(-1f, 1f), spring(dampingRatio = .7f, stiffness = 120f))
            return@LaunchedEffect
        }
        val rnd = Random(System.nanoTime())
        while (true) {
            val quick = rnd.nextFloat() < .35f
            val tx = (rnd.nextFloat() * 2 - 1) * .85f; val ty = (rnd.nextFloat() * 2 - 1) * .75f
            val spec = if (quick) spring<Float>(dampingRatio = .75f, stiffness = 260f) else spring(dampingRatio = .9f, stiffness = 28f)
            launch { gx.animateTo(tx, spec) }
            gy.animateTo(ty, spec)
            delay((if (quick) 500L else 900L) + rnd.nextLong(1600))
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
        if (f != null) {
            val fx = (gx.value + 1) / 2 * (COLS - 1); val fy = (gy.value + 1) / 2 * (ROWS - 1)
            val c0 = floor(fx).toInt().coerceIn(0, COLS - 1); val r0 = floor(fy).toInt().coerceIn(0, ROWS - 1)
            val c1 = (c0 + 1).coerceAtMost(COLS - 1); val r1 = (r0 + 1).coerceAtMost(ROWS - 1)
            val ax = fx - c0; val ay = fy - r0
            // ближайший кадр полностью, соседний по сильнейшей оси — с прозрачностью
            val main = f[r0 * COLS + c0]
            val dst = IntSize(eyeD.toInt(), eyeD.toInt()); val off = IntOffset((cx - eyeD / 2).toInt(), (cy - eyeD / 2).toInt())
            drawImage(main, dstOffset = off, dstSize = dst)
            if (ax >= ay && ax > .02f) drawImage(f[r0 * COLS + c1], dstOffset = off, dstSize = dst, alpha = ax)
            else if (ay > .02f) drawImage(f[r1 * COLS + c0], dstOffset = off, dstSize = dst, alpha = ay)
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
