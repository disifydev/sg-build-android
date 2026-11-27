package ru.sguardo.app.ui.screens

import androidx.compose.ui.graphics.drawOutline
import androidx.compose.foundation.layout.heightIn
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.sguardo.app.data.Conversation
import ru.sguardo.app.data.Fixture
import ru.sguardo.app.data.Fmt
import ru.sguardo.app.data.Message
import ru.sguardo.app.ui.Ic
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.LocalOv
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import kotlin.math.abs
import kotlin.math.sin

val GAL = listOf(
    listOf(0xFFF2C7B0, 0xFFC46F5A), listOf(0xFFC3DAE3, 0xFF4E7F8F), listOf(0xFFF0DDA8, 0xFFB88A2E),
    listOf(0xFFD9D0EC, 0xFF6B5A9A), listOf(0xFFBFE0CF, 0xFF2E7A5A), listOf(0xFFEFC0BC, 0xFF8E3A3A),
).map { l -> Brush.linearGradient(l.map { Color(it) }) }

/** Демо-фото: «тетрадь в линейку» (NOTE_BG) или градиент из галереи. */
@Composable
fun DemoPhoto(name: String?, modifier: Modifier) {
    val g = name?.removePrefix("demo:")?.toIntOrNull()
    if (g != null) { Box(modifier.background(GAL[g % GAL.size])); return }
    Canvas(modifier.background(Brush.linearGradient(listOf(Color(0xFFF6F0E3), Color(0xFFE6D9C2))))) {
        var y = 15.dp.toPx()
        while (y < size.height) { drawLine(Color(0x404664A0), Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += 16.dp.toPx() }
    }
}

/** «Камера» для кружка в демо (как .cam в макете). */
@Composable
fun DemoCam(back: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        if (back) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFA7C2C8), Color(0xFFE6DFD0))))
            drawRect(Color(0xFF6F5A47), Offset(0f, h * .64f), Size(w, h * .36f))
            drawOval(Color(0xFFF7F1E4), Offset(w * .72f - w * .18f, h * .34f - h * .26f), Size(w * .36f, h * .52f))
            drawOval(Color(0xFFC4572F), Offset(w * .3f - w * .12f, h * .6f - h * .08f), Size(w * .24f, h * .16f))
        } else {
            drawRect(Brush.linearGradient(listOf(Color(0xFFD9CCB6), Color(0xFF8FA9A4))))
            drawOval(Color(0xFF2E5256), Offset(w * .5f - w * .45f, h * 1.04f - h * .3f), Size(w * .9f, h * .6f))
            drawOval(Color(0xFF3B2A22), Offset(w * .5f - w * .23f, h * .4f - h * .27f), Size(w * .46f, h * .54f))
            drawOval(Color(0xFFE2BFA4), Offset(w * .5f - w * .17f, h * .47f - h * .22f), Size(w * .34f, h * .44f))
        }
    }
}

/**
 * Пузырь сообщения: текст, голосовое, фото, файл, кружок. Время настоящее, у своих — галочки.
 * compact — для предпросмотра (без интерактива).
 */
@Composable
fun Bubble(m: Message, c: Conversation, prev: Message?, compact: Boolean = false, hidden: Boolean = false, next: Message? = null, onQuote: (String) -> Unit = {}) {
    val out = m.isOut
    val gap = prev != null && prev.isOut != m.isOut
    // группа как в Telegram: подряд от одного отправителя в пределах 5 минут — внутренние углы меньше, хвостик только у последнего
    val top = sameGroup(prev, m)
    val bottom = sameGroup(m, next)
    Row(Modifier.fillMaxWidth().padding(top = if (gap) 8.dp else 0.dp, start = if (out) 0.dp else 6.dp, end = if (out) 6.dp else 0.dp).graphicsLayer { alpha = if (hidden) 0f else 1f },
        horizontalArrangement = if (out) Arrangement.End else Arrangement.Start) {
        BubbleBody(m, c, compact, onQuote, top = top, bottom = bottom || next == null && compact)
    }
}

fun sameGroup(a: Message?, b: Message?): Boolean =
    a != null && b != null && a.isOut == b.isOut && kotlin.math.abs(b.time - a.time) < 5 * 60_000 && Fmt.dayChip(a.time) == Fmt.dayChip(b.time)

/** Форма пузыря: большие углы снаружи группы, маленькие внутри, у последнего — угол под хвостик. */
fun bubbleShape(out: Boolean, top: Boolean, bottom: Boolean, big: androidx.compose.ui.unit.Dp = 18.dp): RoundedCornerShape {
    val small = minOf(6.dp, big); val tail = minOf(4.dp, big)
    val sideTop = if (top) small else big
    val sideBottom = if (bottom) small else tail
    return if (out) RoundedCornerShape(big, sideTop, sideBottom, big) else RoundedCornerShape(sideTop, big, big, sideBottom)
}

/** Фон пузыря с хвостиком у последнего в группе (хвостик рисуется за краем, контент обрезается формой без него). */
fun Modifier.bubbleBg(bg: Color, out: Boolean, shape: RoundedCornerShape, tail: Boolean): Modifier = this.drawBehind {
    val o = shape.createOutline(size, layoutDirection, this)
    drawOutline(o, bg)
    if (tail) {
        val w = size.width; val h = size.height; val d = density
        val p = androidx.compose.ui.graphics.Path()
        if (out) {
            p.moveTo(w - 1 * d, h - 17 * d)
            p.cubicTo(w - 1 * d, h - 6 * d, w + 2 * d, h - 1.5f * d, w + 7 * d, h)
            p.lineTo(w - 12 * d, h); p.close()
        } else {
            p.moveTo(1 * d, h - 17 * d)
            p.cubicTo(1 * d, h - 6 * d, -2 * d, h - 1.5f * d, -7 * d, h)
            p.lineTo(12 * d, h); p.close()
        }
        drawPath(p, bg)
    }
}

@Composable
fun BubbleBody(m: Message, c: Conversation, compact: Boolean = false, onQuote: (String) -> Unit = {}, top: Boolean = false, bottom: Boolean = false) {
    val out = m.isOut
    val bg = if (out) chatOut() else Sg.c.cHigh
    val fg = if (out) chatOutFg() else Sg.c.fg
    val shape = bubbleShape(out, top, bottom, LocalChatStyle.current.radius)
    val tail = !bottom
    val reaction = m.reactions?.firstOrNull()?.emoji
    when (m.kind) {
        "circle" -> CircleMsg(m, compact)
        "voice" -> Column(Modifier.widthIn(max = 290.dp).bubbleBg(bg, out, shape, tail).clip(shape).padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)) {
            VoiceRow(m, out, fg)
            if (reaction != null) ReactionChip(reaction, fg)
            Row(Modifier.align(Alignment.End)) { Meta(m, fg) }
        }
        "photo", "video" -> Column(Modifier.width(250.dp).bubbleBg(bg, out, shape, tail).clip(shape).padding(3.dp)) {
            val repo = LocalRepo.current; val ov = LocalOv.current; val ctx = LocalContext.current
            val url = imageUrl(repo, m.att)
            val demoPic = url == null || m.att?.name?.startsWith("demo:") == true
            val open = Modifier.clickable(enabled = !compact) {
                if (m.kind == "video") openAttachment(ctx, repo, m.att)
                else ov.layer = { PhotoViewer(if (demoPic) null else url, m.att?.name) { ov.layer = null } }
            }
            Box {
                if (demoPic) DemoPhoto(m.att?.name, Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(15.dp)).then(open))
                else AsyncImage(url, null, Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp).clip(RoundedCornerShape(15.dp)).background(Sg.c.hair).then(open), contentScale = ContentScale.Crop)
                if (m.kind == "video") Box(Modifier.align(Alignment.Center).size(48.dp).background(Color.Black.copy(alpha = .35f), CircleShape), contentAlignment = Alignment.Center) { Ic("play", Color.White) }
            }
            Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 2.dp)) {
                if (m.text.isNotBlank()) TextWithMeta(m.text, fg, m) else Row(Modifier.align(Alignment.End)) { Meta(m, fg) }
                if (reaction != null) ReactionChip(reaction, fg)
            }
        }
        else -> Column(Modifier.widthIn(max = 300.dp).bubbleBg(bg, out, shape, tail).clip(shape).padding(start = 11.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)) {
            if (m.forwardedFrom != null) T("Переслано от ${m.forwardedFrom}", body(12f, FontWeight.SemiBold), fg.copy(alpha = .75f), modifier = Modifier.padding(bottom = 3.dp))
            m.replyTo?.let { r ->
                val repo = LocalRepo.current
                Column(Modifier.padding(top = 2.dp, bottom = 6.dp).clip(RoundedCornerShape(6.dp)).background(fg.copy(alpha = .1f)).clickable(enabled = !compact) { onQuote(r.id) }
                    .drawBehind { drawRect(fg, size = Size(3.dp.toPx(), size.height)) }.padding(start = 11.dp, end = 8.dp, top = 3.dp, bottom = 3.dp)) {
                    T(if (r.dir == "out") "Вы" else repo.nameOfConv(c), body(12.5f, FontWeight.Bold), fg)
                    T(r.text, body(13f), fg.copy(alpha = .8f), maxLines = 1, modifier = Modifier.widthIn(max = 220.dp))
                }
            }
            if (m.kind == "file") FileRow(m, out, fg)
            if (m.kind == "file" && m.text.isBlank()) Row(Modifier.align(Alignment.End)) { Meta(m, fg) }
            else if (m.text.isNotBlank()) TextWithMeta(m.text, fg, m)
            if (reaction != null) ReactionChip(reaction, fg)
        }
    }
}

@Composable
fun ReactionChip(e: String, fg: Color) {
    val s = remember { Animatable(0.4f) }
    LaunchedEffect(e) { s.animateTo(1f, spring(dampingRatio = .45f, stiffness = 500f)) }
    Box(Modifier.padding(top = 5.dp).graphicsLayer { scaleX = s.value; scaleY = s.value }.height(24.dp).background(fg.copy(alpha = .15f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center) { T(e, body(14f)) }
}

/** Время и галочки (.mt). */
@Composable
fun Meta(m: Message, fg: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer { alpha = .65f }) {
        if (m.edited) T("изм. ", body(11f).copy(fontStyle = FontStyle.Normal), fg)
        T(Fmt.hm(m.time), body(11f), fg)
        if (m.isOut) { Spacer(Modifier.width(3.dp)); Ic(if (m.pending) "clock" else "checks", fg, size = 13.dp) }
    }
}

/** Текст с временем в конце последней строки, если помещается (как в Telegram). */
@Composable
fun TextWithMeta(text: String, fg: Color, m: Message) {
    var tl: TextLayoutResult? = null
    Layout(content = {
        androidx.compose.material3.Text(text, style = body(15f * LocalChatStyle.current.text).copy(lineHeight = (20.4f * LocalChatStyle.current.text).sp), color = fg, onTextLayout = { tl = it })
        Meta(m, fg)
    }) { ms, cons ->
        val tp = ms[0].measure(cons.copy(minWidth = 0))
        val mp = ms[1].measure(Constraints())
        val lr = tl
        val gapPx = 10.dp.roundToPx()
        val lastW = lr?.let { it.getLineRight(it.lineCount - 1) - it.getLineLeft(it.lineCount - 1) }?.toInt() ?: tp.width
        val fits = lastW + gapPx + mp.width <= cons.maxWidth
        val w = if (fits) maxOf(tp.width, lastW + gapPx + mp.width) else maxOf(tp.width, mp.width)
        val h = if (fits) maxOf(tp.height, mp.height) else tp.height + mp.height
        layout(w, h) {
            tp.place(0, 0)
            mp.place(w - mp.width, h - mp.height + if (fits) 2.dp.roundToPx() else 0)
        }
    }
}

@Composable
private fun FileRow(m: Message, out: Boolean, fg: Color) {
    val repo = LocalRepo.current; val ctx = LocalContext.current
    val name = m.att?.name ?: "Файл"
    val ext = name.substringAfterLast('.', "").uppercase()
    val remote by produceState<String?>(Fixture.sizes[name], m.att?.url) { if (value == null && !repo.demo.value) value = remoteSize(repo, m.att)?.let { fmtSize(it) } }
    Row(Modifier.widthIn(min = 210.dp).clip(RoundedCornerShape(12.dp)).clickable { openAttachment(ctx, repo, m.att) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(if (out) chatOutFg() else Sg.c.teal, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Ic("file", if (out) chatOut() else Sg.c.onTeal) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.widthIn(max = 200.dp)) {
            T(name, body(14f, FontWeight.SemiBold), fg, maxLines = 1)
            T(listOfNotNull(remote, ext.ifBlank { null }).joinToString(" · ").ifBlank { "файл" }, body(12f), fg.copy(alpha = .7f))
        }
    }
}

private fun barH(k: Int, seed: Int) = 25f + abs(sin(k * 1.7 + seed)).toFloat() * 70f

/** Голосовое: кнопка, волна, длительность; проигрывание через MediaPlayer (кука сессии — только своему серверу). */
@Composable
fun VoiceRow(m: Message, out: Boolean, fg: Color) {
    val repo = LocalRepo.current
    val ctx = LocalContext.current
    val len = (m.att?.duration ?: 0.0).toInt().coerceAtLeast(1)
    var playing by remember { mutableStateOf(false) }
    var pos by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
    val hub = LocalPlayer.current
    DisposableEffect(Unit) { onDispose { player.value?.release(); hub.release(m.id) } }
    // включили другое голосовое или кружок — это ставим на паузу
    LaunchedEffect(hub.current) { if (hub.current != m.id && playing) { playing = false; player.value?.pause() } }
    LaunchedEffect(playing) {
        while (playing) {
            val p = player.value
            if (p != null) { pos = p.currentPosition / 1000f; if (!p.isPlaying && pos > 0 && p.currentPosition >= p.duration - 50) { playing = false; pos = 0f } }
            else { pos += .1f; if (pos >= len) { playing = false; pos = 0f; hub.release(m.id) } }
            delay(100)
        }
    }
    val seed = m.id.hashCode() % 97
    val wave = m.att?.waveform
    Row(Modifier.widthIn(min = 210.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(if (out) chatOutFg() else Sg.c.teal).clickable {
            if (playing) { playing = false; player.value?.pause(); hub.release(m.id); return@clickable }
            hub.claim(m.id)
            val url = imageUrl(repo, m.att)
            if (url != null && (!repo.demo.value || url.startsWith("file:"))) {
                if (player.value == null) {
                    val mp = MediaPlayer()
                    try {
                        val headers = if (repo.api.isOwn(url)) mapOf("Cookie" to repo.api.cookies.loadForRequest(repo.api.url("/")).joinToString("; ") { "${it.name}=${it.value}" }) else emptyMap()
                        mp.setDataSource(ctx, Uri.parse(url), headers)
                        mp.setOnPreparedListener { it.start(); playing = true }
                        mp.setOnCompletionListener { playing = false; pos = 0f; hub.release(m.id) }
                        mp.setOnErrorListener { _, _, _ -> playing = false; repo.toast("Не получилось воспроизвести голосовое"); true }
                        mp.prepareAsync(); player.value = mp
                    } catch (_: Exception) { repo.toast("Не получилось включить голосовое") }
                } else { player.value?.start(); playing = true }
            } else playing = true
        }, contentAlignment = Alignment.Center) { Ic(if (playing) "pause" else "play", if (out) chatOut() else Sg.c.onTeal, size = 16.dp) }
        Spacer(Modifier.width(10.dp))
        Row(Modifier.weight(1f).height(28.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            val n = 26
            repeat(n) { k ->
                val hv = if (wave != null && wave.isNotEmpty()) 15f + wave[(k * wave.size / n).coerceAtMost(wave.size - 1)] / 31f * 85f else barH(k, seed)
                val on = playing || pos > 0 && k.toFloat() / n < pos / len
                Box(Modifier.weight(1f).fillMaxHeight(hv / 100f).clip(RoundedCornerShape(2.dp)).background(fg.copy(alpha = if (on && k.toFloat() / n < pos / len) 1f else .35f)))
            }
        }
        Spacer(Modifier.width(10.dp))
        T(if (playing || pos > 0) Fmt.dur(pos.toInt()) else Fmt.dur(len), body(11f), fg.copy(alpha = .7f), modifier = Modifier.width(30.dp))
    }
}

/** Кружок как в Telegram: по нажатию растёт и играет со звуком, по краю бежит кольцо, повторное нажатие — пауза. */
@Composable
fun CircleMsg(m: Message, compact: Boolean) {
    val repo = LocalRepo.current
    val hub = LocalPlayer.current
    val len = (m.att?.duration ?: 1.0).toInt().coerceAtLeast(1)
    var started by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    val prog = remember { Animatable(0f) }
    val video = remember { mutableStateOf<VideoView?>(null) }
    val scope = rememberCoroutineScope()
    val size by animateDpAsState(if (compact) 140.dp else if (started) 244.dp else 200.dp, spring(dampingRatio = .7f, stiffness = 500f), label = "cv")
    val url = imageUrl(repo, m.att)
    val real = url != null && (!repo.demo.value || url.startsWith("file:"))
    fun stop() { playing = false; started = false; video.value?.pause(); hub.release(m.id); scope.launch { prog.snapTo(0f) } }
    LaunchedEffect(hub.current) { if (hub.current != m.id && started) { playing = false; started = false; video.value?.pause(); prog.snapTo(0f) } }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        if (!real) {
            prog.animateTo(1f, tween(((1f - prog.value) * len * 1000).toInt().coerceAtLeast(1), easing = LinearEasing))
            stop(); return@LaunchedEffect
        }
        while (playing) {
            val v = video.value
            if (v != null && v.duration > 0) prog.snapTo(v.currentPosition / v.duration.toFloat())
            delay(50)
        }
    }
    Box(Modifier.size(size).clip(CircleShape).clickable(enabled = !compact) {
        when {
            !started -> { hub.claim(m.id); started = true; playing = true; video.value?.start() }
            playing -> { playing = false; video.value?.pause() }
            else -> { hub.claim(m.id); playing = true; video.value?.start() }
        }
    }) {
        if (real && started) {
            AndroidView({ ctx -> VideoView(ctx).apply {
                val headers = if (repo.api.isOwn(url!!)) mapOf("Cookie" to repo.api.cookies.loadForRequest(repo.api.url("/")).joinToString("; ") { "${it.name}=${it.value}" }) else emptyMap()
                setVideoURI(Uri.parse(url), headers)
                setOnPreparedListener { it.isLooping = false; if (playing) start() }
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ -> stop(); repo.toast("Не получилось воспроизвести кружок"); true }
                video.value = this
            } }, Modifier.fillMaxSize(), onRelease = { video.value = null })
        } else if (real) {
            AsyncImage(url, null, Modifier.fillMaxSize().background(Sg.c.hair), contentScale = ContentScale.Crop)
        } else DemoCam(false, Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            if (prog.value > 0f) drawArc(Color.White, -90f, 360f * prog.value, false, Offset(3f, 3f), Size(this.size.width - 6f, this.size.height - 6f), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        }
        if (!playing) Box(Modifier.align(Alignment.Center).size(48.dp).background(Color.Black.copy(alpha = .35f), CircleShape), contentAlignment = Alignment.Center) { Ic("play", Color.White) }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).height(22.dp).background(Color.Black.copy(alpha = .4f), RoundedCornerShape(11.dp)).padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically) {
            T(if (started) Fmt.dur((prog.value * len).toInt()) else Fmt.dur(len), body(11.5f), Color.White)
            if (m.isOut) { Spacer(Modifier.width(3.dp)); Ic(if (m.pending) "clock" else "checks", Color.White, size = 13.dp) }
        }
        m.reactions?.firstOrNull()?.let { r ->
            Box(Modifier.align(Alignment.BottomEnd).offset((-18).dp, (-16).dp).height(24.dp).background(Color.Black.copy(alpha = .4f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) { T(r.emoji, body(14f)) }
        }
    }
}

/** «Печатает…»: три точки в пузыре. */
@Composable
fun TypingBubble() {
    val inf = rememberInfiniteTransition(label = "bob")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "t")
    Row(Modifier.clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)).background(Sg.c.cHigh).padding(horizontal = 14.dp, vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { i ->
            val ph = ((t - i * .14f) + 1f) % 1f
            val up = if (ph < .3f) ph / .3f else if (ph < .6f) 1 - (ph - .3f) / .3f else 0f
            Box(Modifier.size(7.dp).offset(y = (-5 * up).dp).graphicsLayer { alpha = .5f + .5f * up }.background(Sg.c.muted, CircleShape))
        }
    }
}

fun overflowSafe(s: String) = s.replace("\n", " ")
val Ellipsis = TextOverflow.Ellipsis
