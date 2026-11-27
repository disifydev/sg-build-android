package ru.sguardo.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import ru.sguardo.app.data.Attachment
import ru.sguardo.app.data.Repo
import ru.sguardo.app.data.await
import ru.sguardo.app.ui.IconBtn
import java.io.File

/** Один плеер на чат: запуск голосового или кружка останавливает предыдущее. */
@Stable
class PlayerHub {
    var current by mutableStateOf<String?>(null)
    fun claim(id: String) { current = id }
    fun release(id: String) { if (current == id) current = null }
}

val LocalPlayer = staticCompositionLocalOf { PlayerHub() }

/** Фото на весь экран: щипок — зум, двойное нажатие — приблизить, смахнуть вниз — закрыть. */
@Composable
fun PhotoViewer(url: String?, demoName: String?, close: () -> Unit) {
    BackHandler(onBack = close)
    var scale by remember { mutableStateOf(1f) }
    var ox by remember { mutableStateOf(0f) }
    var oy by remember { mutableStateOf(0f) }
    val dismiss = remember { Animatable(0f) }
    val appear = remember { Animatable(.92f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = .8f, stiffness = 600f)) }
    val fade = (1f - kotlin.math.abs(dismiss.value) / 900f).coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = fade))
        .pointerInput(Unit) {
            detectTapGestures(onDoubleTap = { if (scale > 1.05f) { scale = 1f; ox = 0f; oy = 0f } else scale = 2.5f }, onTap = { if (scale <= 1.05f) close() })
        }
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                val ns = (scale * zoom).coerceIn(1f, 5f)
                scale = ns
                if (ns > 1.02f) { ox += pan.x; oy += pan.y } else { ox = 0f; oy = 0f; scope.launch { dismiss.snapTo(dismiss.value + pan.y) } }
            }
        }
        .pointerInput(Unit) {
            // конец жеста: решаем, закрыть или вернуть на место
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do { val e = awaitPointerEvent(PointerEventPass.Final) } while (e.changes.any { it.pressed })
                if (scale <= 1.02f) {
                    if (kotlin.math.abs(dismiss.value) > 220f) close()
                    else scope.launch { dismiss.animateTo(0f, spring(dampingRatio = .75f, stiffness = 500f)) }
                }
            }
        }) {
        val mod = Modifier.fillMaxWidth().align(Alignment.Center).graphicsLayer {
            scaleX = scale * appear.value; scaleY = scale * appear.value; translationX = ox; translationY = oy + dismiss.value
        }
        if (url != null) AsyncImage(url, null, mod.fillMaxSize(), contentScale = ContentScale.Fit)
        else DemoPhoto(demoName, mod.height(340.dp_()))
        Box(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(8.dp_())) { IconBtn("x", close, tint = Color.White) }
    }
}

private fun Int.dp_() = androidx.compose.ui.unit.Dp(this.toFloat())

/** Источник картинки: файл до отправки, наш сервер (с кукой через общий загрузчик) или чужой https. */
fun imageUrl(repo: Repo, a: Attachment?): String? {
    val u = a?.url ?: return null
    if (u.startsWith("file:")) return u
    return repo.api.media(u)
}

/** Открыть файл системным просмотрщиком: скачиваем во временную папку (кука — только своему серверу) и отдаём через FileProvider. */
fun openAttachment(ctx: Context, repo: Repo, a: Attachment?) {
    if (a == null) return
    if (repo.demo.value) { repo.toast("В демо файл не открывается"); return }
    val url = repo.api.media(a.url) ?: run { repo.toast("Файл недоступен"); return }
    repo.toast("Открываем ${a.name ?: "файл"}…")
    repo.scope.launch {
        try {
            val f = withContext(Dispatchers.IO) {
                val client = if (repo.api.isOwn(url)) repo.api.client else repo.api.foreignClient
                client.newCall(Request.Builder().url(url).build()).await().use { r ->
                    if (!r.isSuccessful) throw IllegalStateException("Сервер не отдал файл (${r.code})")
                    val dir = File(ctx.cacheDir, "open").apply { mkdirs() }
                    val out = File(dir, (a.name ?: "file").replace(Regex("[\\\\/:*?\"<>|]"), "_"))
                    r.body!!.byteStream().use { i -> out.outputStream().use { o -> i.copyTo(o) } }
                    out
                }
            }
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
            val mime = a.mime ?: android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"
            val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            try { ctx.startActivity(i) } catch (_: Exception) { repo.toast("На телефоне нет приложения, чтобы открыть этот файл") }
        } catch (e: Exception) { repo.toast("Не получилось открыть файл: ${ru.sguardo.app.data.friendly(e)}") }
    }
}

/** Размер файла на сервере (HEAD), чтобы показать его на плашке. */
suspend fun remoteSize(repo: Repo, a: Attachment?): Long? {
    val url = repo.api.media(a?.url) ?: return null
    if (a?.url?.startsWith("file:") == true) return File(java.net.URI(a.url)).length()
    return try {
        withContext(Dispatchers.IO) {
            val client = if (repo.api.isOwn(url)) repo.api.client else repo.api.foreignClient
            client.newCall(Request.Builder().url(url).head().build()).await().use { it.header("Content-Length")?.toLongOrNull() }
        }
    } catch (_: Exception) { null }
}

fun fmtSize(n: Long): String = when {
    n >= 1024L * 1024 -> String.format("%.1f МБ", n / 1048576.0).replace('.', ',')
    n >= 1024 -> "${n / 1024} КБ"
    else -> "$n Б"
}
