package ru.sguardo.app.ui.screens

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import ru.sguardo.app.data.Clock
import ru.sguardo.app.ui.HScroll
import ru.sguardo.app.ui.Ic
import ru.sguardo.app.ui.LocalPerms
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import java.io.File
import kotlin.math.roundToInt

/** Запись голосового: MediaRecorder в AAC (.m4a). */
class VoiceRecorder(private val ctx: Context) {
    private var rec: MediaRecorder? = null
    var file: File? = null; private set
    fun start(): Boolean = try {
        val f = File(ctx.cacheDir, "voice-${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(64_000); r.setAudioSamplingRate(44_100)
        r.setOutputFile(f.absolutePath)
        r.prepare(); r.start()
        rec = r; file = f; true
    } catch (_: Exception) { rec = null; false }
    fun level(): Float = try { (rec?.maxAmplitude ?: 0) / 32767f } catch (_: Exception) { 0f }
    fun stop(): File? = try { rec?.stop(); rec?.release(); rec = null; file } catch (_: Exception) { rec?.release(); rec = null; null }
    fun cancel() { stop(); file?.delete(); file = null }
}

class RecState(val circle: Boolean, val started: Long) {
    var locked by mutableStateOf(false)
    var dx by mutableFloatStateOf(0f)
    var dy by mutableFloatStateOf(0f)
    var cancelled by mutableStateOf(false)
    var level by mutableFloatStateOf(0f)
    var flash by mutableStateOf(false)
    var back by mutableStateOf(false)
}

/** Управление кружком из CameraX: превью, запись в файл, вспышка, смена камеры. */
class CircleCam {
    var preview: PreviewView? = null
    var capture: VideoCapture<Recorder>? = null
    var recording: Recording? = null
    var camera: androidx.camera.core.Camera? = null
    var file: File? = null
    var onDone: ((File?) -> Unit)? = null
    fun torch(on: Boolean) { try { camera?.cameraControl?.enableTorch(on) } catch (_: Exception) {} }
}

const val TEMPLATE_KEY = "tpl"

/** Поле ввода чата (Android): скрепка, текст, шаблоны, «позже»; справа — отправка или микрофон/кружок. */
@Composable
fun Composer(
    convId: String,
    text: MutableState<String>,
    bar: (@Composable () -> Unit)?,
    showTpl: Boolean,
    onTplToggle: () -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onLater: () -> Unit,
    rec: MutableState<RecState?>,
    onRecDone: (RecState, Boolean) -> Unit,
) {
    val repo = LocalRepo.current
    val perms = LocalPerms.current
    var circleMode by remember { mutableStateOf(false) }
    val has = text.value.isNotBlank()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val st = repo.state.value
    Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 10.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AnimatedVisibility(showTpl, enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 }, exit = fadeOut(tween(180))) {
            Box(Modifier.padding(horizontal = 2.dp)) {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(st.templates.size) { i ->
                        val t = st.templates[i]
                        Box(Modifier.height(34.dp).clip(RoundedCornerShape(10.dp)).background(Sg.c.cHigh).drawBehind {}.clickable { text.value = t.text; onTplToggle() }
                            .padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { T(t.text, body(13.5f, FontWeight.Medium), maxLines = 1) }
                    }
                }
            }
        }
        bar?.invoke()
        Box {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).heightIn(min = 52.dp).clip(RoundedCornerShape(28.dp)).background(Sg.c.cHigh)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SmallBtn("clip", onAttach)
                        Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                            if (text.value.isEmpty()) T("Сообщение", body(15.5f), Sg.c.faint)
                            BasicTextField(text.value, { text.value = it; repo.saveDraft(convId, it) }, textStyle = body(15.5f).copy(color = Sg.c.fg), maxLines = 6,
                                cursorBrush = SolidColor(Sg.c.primary), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth())
                        }
                        SmallBtn("bolt", onTplToggle, tint = if (showTpl) Sg.c.primary else Sg.c.muted)
                        SmallBtn("clock", onLater)
                    }
                    rec.value?.let { r -> RecUi(r) { onRecDone(r, false) } }
                }
                // кнопка отправки / микрофона: форма 18 → 26 при тексте
                val radius by animateDpAsState(if (has) 26.dp else 18.dp, label = "r")
                val recNow = rec.value
                Box(Modifier.size(52.dp).graphicsLayer { alpha = if (recNow != null) 0f else 1f }.clip(RoundedCornerShape(radius)).background(Sg.c.primary)
                    .pointerInput(has, circleMode) {
                        if (has) {
                            awaitEachGesture { awaitFirstDown(); val up = waitForUpOrCancellation(); if (up != null) onSend() }
                        } else awaitEachGesture {
                            val down = awaitFirstDown()
                            val upEarly = withTimeoutOrNull(220) { waitForUpOrCancellation() }
                            if (upEarly != null) {
                                circleMode = !circleMode
                                repo.toast(if (circleMode) "Кружок: удерживайте, чтобы записать" else "Голосовое: удерживайте, чтобы записать")
                                return@awaitEachGesture
                            }
                            val r = RecState(circleMode, System.currentTimeMillis())
                            var ok = true
                            if (!repo.demo.value) {
                                ok = if (circleMode) perms.hasCamera() && perms.hasAudio() else perms.hasAudio()
                                if (!ok) { if (circleMode) perms.camera { } else perms.audio { }; repo.toast("Разрешите доступ к ${if (circleMode) "камере и микрофону" else "микрофону"} и нажмите ещё раз"); return@awaitEachGesture }
                            }
                            rec.value = r
                            val sx = down.position.x; val sy = down.position.y
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull() ?: break
                                if (!ch.pressed) break
                                if (!r.locked && !r.cancelled) {
                                    r.dx = minOf(0f, (ch.position.x - sx) / density.density)
                                    r.dy = minOf(0f, (ch.position.y - sy) / density.density)
                                    if (r.dy < -70) { r.locked = true; r.dx = 0f; r.dy = 0f }
                                    else if (r.dx < -120) { r.cancelled = true; onRecDone(r, false); return@awaitEachGesture }
                                }
                                ch.consume()
                            }
                            if (!r.locked && !r.cancelled) onRecDone(r, true)
                        }
                    }, contentAlignment = Alignment.Center) {
                    AnimatedContent(if (has) "send" else if (circleMode) "vcirc" else "mic", transitionSpec = {
                        (scaleIn(spring(dampingRatio = .55f, stiffness = 500f)) + fadeIn()) togetherWith (scaleOut(tween(150)) + fadeOut(tween(150)))
                    }, label = "send") { k -> Ic(k, Sg.c.onPrimary) }
                }
            }
            rec.value?.let { r -> BigMic(r, Modifier.align(Alignment.BottomEnd)) { onRecDone(r, true) } }
        }
    }
}

@Composable
private fun SmallBtn(icon: String, onClick: () -> Unit, tint: Color = Sg.c.muted) {
    Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Ic(icon, tint, size = 16.dp) }
}

/** Полоса записи поверх поля: мигающая точка, время, «Влево — отмена» или «Отмена» на замке. */
@Composable
private fun RecUi(r: RecState, onCancel: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val t0 = r.started
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(50) } }
    val inf = rememberInfiniteTransition(label = "blink")
    val a by inf.animateFloat(1f, .2f, infiniteRepeatable(tween(500), androidx.compose.animation.core.RepeatMode.Reverse), label = "a")
    val sh by inf.animateFloat(0f, -5f, infiniteRepeatable(tween(700), androidx.compose.animation.core.RepeatMode.Reverse), label = "sh")
    val t = (now - t0) / 1000f
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).background(Sg.c.cHigh).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (r.cancelled) Ic("trash", Color(0xFFD93A33), size = 16.dp)
        else Box(Modifier.size(10.dp).graphicsLayer { alpha = a }.background(Color(0xFFE5484D), CircleShape))
        Spacer(Modifier.width(10.dp))
        T("${(t / 60).toInt()}:${"%02d".format((t % 60).toInt())},${((t % 1) * 10).toInt()}", body(15f), modifier = Modifier.width(60.dp))
        if (r.locked) T("Отмена", body(15f, FontWeight.SemiBold), Sg.c.primary, modifier = Modifier.weight(1f).padding(end = 40.dp).clickable(onClick = onCancel), align = androidx.compose.ui.text.style.TextAlign.Center)
        else Row(Modifier.weight(1f).padding(end = 40.dp).offset { IntOffset((r.dx * .7f * density).roundToInt(), 0) }.graphicsLayer { alpha = (1 + r.dx / 140).coerceIn(0f, 1f) },
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Ic("chevL", Sg.c.muted, size = 16.dp, modifier = Modifier.offset(x = sh.dp)); T("Влево — отмена", body(14f), Sg.c.muted, maxLines = 1)
        }
    }
}

/** Большой микрофон и замочек (как .bigmic и .lockp). */
@Composable
private fun BigMic(r: RecState, modifier: Modifier, onSend: () -> Unit) {
    val ctx = LocalContext.current
    val lvl = remember { Animatable(0f) }
    LaunchedEffect(r) {
        while (true) { val target = if (r.level > 0) r.level else (Math.random() * Math.random() * 1.6).toFloat(); lvl.animateTo(target.coerceAtMost(1f), tween(90)); delay(60) }
    }
    val inf = rememberInfiniteTransition(label = "lock")
    val up by inf.animateFloat(0f, -4f, infiniteRepeatable(tween(600), androidx.compose.animation.core.RepeatMode.Reverse), label = "up")
    Box(modifier.offset(22.dp, 22.dp)) {
        // замок над кнопкой
        val lockH by animateDpAsState(if (r.locked) 42.dp else 84.dp, label = "lh")
        Column(Modifier.offset(x = 25.dp, y = (-(if (r.locked) 112 else 96)).dp + (r.dy * .75f).coerceAtLeast(-56f).dp).size(42.dp, lockH)
            .clip(RoundedCornerShape(21.dp)).background(Sg.c.cHigh), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceAround) {
            Ic(if (r.locked) "lock" else "lockO", Sg.c.fg, size = 16.dp)
            if (!r.locked) Ic("chevU", Sg.c.fg, size = 13.dp, modifier = Modifier.offset(y = up.dp))
        }
        Box(Modifier.size(92.dp).offset { IntOffset((r.dx * .55f * density).roundToInt(), (r.dy * .6f * density).roundToInt()) }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(116.dp).graphicsLayer { scaleX = 1f + lvl.value * .6f; scaleY = 1f + lvl.value * .6f }.background(Sg.c.teal.copy(alpha = .24f), CircleShape))
            Box(Modifier.size(92.dp).clip(CircleShape).background(Sg.c.primary).clickable(enabled = r.locked, onClick = onSend), contentAlignment = Alignment.Center) {
                Ic(if (r.locked) "send" else if (r.circle) "vcirc" else "mic", Sg.c.onPrimary, size = 28.dp)
            }
        }
    }
}

/** Запись кружка: круглое превью камеры, кольцо прогресса до 60 с, вспышка и смена камеры. */
@OptIn(androidx.camera.video.ExperimentalPersistentRecording::class)
@Composable
fun CircleRecorder(r: RecState, cam: CircleCam, demo: Boolean) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val t0 = r.started
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(50) } }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = .7f, stiffness = 300f)) }
    val flip = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val vc = remember { VideoCapture.withOutput(Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.SD)).build()) }
    if (!demo) DisposableEffect(r.back) {
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            try {
                val provider = future.get()
                val preview = androidx.camera.core.Preview.Builder().build().also { p -> cam.preview?.let { pv -> p.setSurfaceProvider(pv.surfaceProvider) } }
                provider.unbindAll()
                // та же VideoCapture и «постоянная» запись: смена камеры не прерывает кружок
                cam.camera = provider.bindToLifecycle(owner, if (r.back) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA, preview, vc)
                cam.capture = vc
                if (cam.recording == null) {
                    val f = File(ctx.cacheDir, "circle-${System.currentTimeMillis()}.mp4"); cam.file = f
                    var pending = vc.output.prepareRecording(ctx, FileOutputOptions.Builder(f).build())
                    if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) pending = pending.withAudioEnabled()
                    pending = pending.asPersistentRecording()
                    cam.recording = pending.start(ContextCompat.getMainExecutor(ctx)) { e -> if (e is VideoRecordEvent.Finalize) cam.onDone?.invoke(if (e.hasError()) null else f) }
                }
                if (r.flash) cam.torch(true)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(ctx))
        onDispose { }
    }
    if (!demo) DisposableEffect(Unit) {
        onDispose {
            // даём записи завершиться и отпускаем камеру
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { ProcessCameraProvider.getInstance(ctx).get().unbindAll() } catch (_: Exception) {}
            }, 1500)
        }
    }
    Box(Modifier.fillMaxSize().background(if (r.flash) Color(0x99FFFAF0) else Color(0x80060C0C)).clickable(enabled = false) {}) {
        val t = (now - t0) / 1000f
        Column(Modifier.fillMaxWidth().padding(top = 110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(276.dp).graphicsLayer { scaleX = (.12f + .88f * appear.value) * (1 - flip.value); scaleY = .12f + .88f * appear.value }
                    .let { if (r.flash) it.drawBehind { drawCircle(Color.White.copy(alpha = .95f), size.minDimension / 2 + 16.dp.toPx()) } else it }
                    .clip(CircleShape)) {
                    if (demo) DemoCam(r.back, Modifier.fillMaxSize())
                    else AndroidView({ c -> PreviewView(c).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; cam.preview = this } }, Modifier.fillMaxSize())
                }
                Canvas(Modifier.size(300.dp)) {
                    drawArc(Color.White.copy(alpha = .95f), -90f, 360f * (t / 60f).coerceAtMost(1f), false, Offset(2.dp.toPx(), 2.dp.toPx()),
                        Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                CamBtn("bolt", r.flash) { r.flash = !r.flash; cam.torch(r.flash) }
                CamBtn("flip", false) {
                    scope.launch { flip.animateTo(1f, tween(170)); r.back = !r.back; flip.animateTo(0f, tween(260)) }
                }
            }
            Spacer(Modifier.height(18.dp))
            T(if (r.locked) "Запись идёт. Нажмите на стрелку, чтобы отправить" else "Смахните вверх, чтобы записывать без рук", body(13.5f),
                if (r.flash) Color(0xFF14211F) else Color.White.copy(alpha = .88f), modifier = Modifier.padding(horizontal = 30.dp), align = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun CamBtn(icon: String, on: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(54.dp).clip(CircleShape).background(if (on) Color.White else Color.White.copy(alpha = .18f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Ic(icon, if (on) Color(0xFF111111) else Color.White)
    }
}
