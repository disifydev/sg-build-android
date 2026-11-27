package ru.sguardo.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sguardo.app.data.Repo
import ru.sguardo.app.ui.Ic
import ru.sguardo.app.ui.LocalPerms
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.Overlays
import ru.sguardo.app.ui.Permissions
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import java.io.File

/** Что готовим к отправке: файл с телефона или демо-картинка. */
data class PrepItem(val file: File?, val name: String, val mime: String) {
    val isVideo get() = mime.startsWith("video")
    val isImage get() = mime.startsWith("image") || name.startsWith("demo:")
    val size get() = file?.length() ?: 0L
}

fun prepFromUris(ctx: Context, uris: List<Uri>): List<PrepItem> = uris.mapNotNull { u -> copyToCache(ctx, u)?.let { (f, n, m) -> PrepItem(f, n, m) } }

/**
 * Подготовка к отправке, как в Telegram: фото и видео листаются на чёрном фоне, внизу полоска выбранных
 * (можно убрать лишнее и добавить ещё), подпись и кнопка отправки со счётчиком. Фото по умолчанию сжимаются,
 * чтобы уходили быстрее; «Без сжатия» отправляет оригиналы. Для документов тот же экран списком.
 */
fun openMediaPrep(ov: Overlays, convId: String, items: List<PrepItem>, files: Boolean = false, caption: String = "") {
    if (items.isEmpty()) return
    ov.layer = { MediaPrep(convId, items, files, caption) { ov.layer = null } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaPrep(convId: String, initial: List<PrepItem>, files: Boolean, initialCaption: String, close: () -> Unit) {
    val repo = LocalRepo.current; val perms = LocalPerms.current; val ctx = LocalContext.current
    val items = remember { mutableStateListOf<PrepItem>().also { it.addAll(initial) } }
    var caption by remember { mutableStateOf(initialCaption) }
    var compress by remember { mutableStateOf(true) }
    val pager = rememberPagerState { items.size }
    val scope = rememberCoroutineScope()
    val strip = rememberLazyListState()
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = .85f, stiffness = 420f)) }
    LaunchedEffect(pager.currentPage) { if (items.isNotEmpty()) strip.animateScrollToItem(pager.currentPage.coerceAtMost(items.lastIndex)) }
    BackHandler(onBack = close)
    val dark = !files
    val bg = if (dark) Color(0xFF0B0F0F) else Sg.c.surface
    val fg = if (dark) Color.White else Sg.c.fg
    val add: () -> Unit = {
        if (repo.demo.value) items += PrepItem(null, "demo:${items.size % GAL.size}", "image/jpeg")
        else if (files) perms.pickFiles { us -> items += prepFromUris(ctx, us) }
        else perms.pickImages { us -> items += prepFromUris(ctx, us) }
    }
    val send: () -> Unit = {
        val list = items.toList(); val cap = caption.trim()
        close()
        repo.scope.launch { sendPrepared(repo, ctx, convId, list, cap, compress, files) }
    }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = appear.value; translationY = (1 - appear.value) * 60.dp.toPx() }.background(bg)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}.windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        Column(Modifier.fillMaxSize()) {
            // шапка
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = close), contentAlignment = Alignment.Center) { Ic("x", fg) }
                T(if (files) (if (items.size > 1) "Файлов: ${items.size}" else "Отправить файл") else if (items.size > 1) "${pager.currentPage + 1} из ${items.size}" else "Отправить",
                    body(17f, FontWeight.SemiBold), fg, modifier = Modifier.weight(1f).padding(start = 6.dp))
                if (!files) Box(Modifier.size(48.dp).clip(CircleShape).clickable {
                    val i = pager.currentPage.coerceAtMost(items.lastIndex)
                    if (items.size <= 1) close() else items.removeAt(i)
                }, contentAlignment = Alignment.Center) { Ic("trash", fg) }
            }
            if (files) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEachIndexed { i, it ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Sg.c.cHigh).padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(46.dp).background(Sg.c.teal, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                T(it.name.substringAfterLast('.', "").take(4).uppercase().ifBlank { "ФАЙЛ" }, body(11f, FontWeight.Bold), Sg.c.onTeal)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                T(it.name, body(15f, FontWeight.SemiBold), maxLines = 1)
                                T(if (it.size > 0) fmtSize(it.size) else "файл", body(12.5f), Sg.c.muted)
                            }
                            Box(Modifier.size(44.dp).clip(CircleShape).clickable { if (items.size <= 1) close() else items.removeAt(i) }, contentAlignment = Alignment.Center) { Ic("x", Sg.c.muted, size = 18.dp) }
                        }
                    }
                    Row(Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = add).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Ic("plus", Sg.c.primary, size = 18.dp); Spacer(Modifier.width(8.dp)); T("Добавить файл", body(15f, FontWeight.SemiBold), Sg.c.primary)
                    }
                }
            } else {
                HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), key = { i -> items.getOrNull(i)?.let { it.name + it.file?.path } ?: i }) { i ->
                    val it = items.getOrNull(i) ?: return@HorizontalPager
                    Box(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        if (it.file == null) DemoPhoto(it.name, Modifier.fillMaxWidth().aspectRatio(.8f).clip(RoundedCornerShape(10.dp)))
                        else AsyncImage(it.file, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        if (it.isVideo) Box(Modifier.size(60.dp).background(Color.Black.copy(alpha = .45f), CircleShape), contentAlignment = Alignment.Center) { Ic("play", Color.White, size = 26.dp) }
                    }
                }
                // полоска выбранных и «добавить ещё»
                LazyRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), state = strip, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                    itemsIndexed(items, key = { i, it -> it.name + it.file?.path + i }) { i, it ->
                        val on = i == pager.currentPage
                        val k by animateFloatAsState(if (on) 1f else 0f, tween(180), label = "th")
                        Box(Modifier.size(56.dp).graphicsLayer { scaleX = .9f + .1f * k; scaleY = .9f + .1f * k }.clip(RoundedCornerShape(10.dp))
                            .border(2.dp, Color.White.copy(alpha = k), RoundedCornerShape(10.dp)).clickable { scope.launch { pager.animateScrollToPage(i) } }) {
                            if (it.file == null) DemoPhoto(it.name, Modifier.fillMaxSize()) else AsyncImage(it.file, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            if (it.isVideo) Ic("play", Color.White, size = 16.dp, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    item(key = "add") {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .12f)).clickable(onClick = add), contentAlignment = Alignment.Center) { Ic("plus", Color.White) }
                    }
                }
                if (items.any { it.isImage && !it.isVideo && it.file != null }) Row(Modifier.padding(start = 14.dp, bottom = 6.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = if (compress) .12f else .22f)).clickable { compress = !compress }.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Ic(if (compress) "check" else "image", Color.White, size = 14.dp); Spacer(Modifier.width(6.dp))
                    T(if (compress) "Сжать фото: быстрее отправка" else "Без сжатия: оригинал", body(13f, FontWeight.Medium), Color.White)
                }
            }
            // подпись и отправка
            Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 4.dp), verticalAlignment = Alignment.Bottom) {
                Box(Modifier.weight(1f).heightIn(min = 50.dp).clip(RoundedCornerShape(25.dp)).background(if (dark) Color.White.copy(alpha = .12f) else Sg.c.cHigh).padding(horizontal = 18.dp, vertical = 14.dp)) {
                    if (caption.isEmpty()) T("Добавить подпись…", body(15.5f), fg.copy(alpha = .55f))
                    BasicTextField(caption, { caption = it }, textStyle = body(15.5f).copy(color = fg), maxLines = 5, cursorBrush = SolidColor(if (dark) Color.White else Sg.c.primary),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.width(8.dp))
                Box {
                    Box(Modifier.size(50.dp).clip(CircleShape).background(Sg.c.primary).clickable(onClick = send), contentAlignment = Alignment.Center) { Ic("send", Sg.c.onPrimary) }
                    if (items.size > 1) Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp)) {
                        Box(Modifier.size(22.dp).border(2.dp, bg, CircleShape).clip(CircleShape).background(Sg.c.teal), contentAlignment = Alignment.Center) {
                            T(items.size.toString(), body(11f, FontWeight.Bold), Sg.c.onTeal)
                        }
                    }
                }
            }
        }
    }
}

/** Отправка подготовленного: подпись у первого, фото по желанию сжимаются. */
suspend fun sendPrepared(repo: Repo, ctx: Context, convId: String, list: List<PrepItem>, cap: String, compress: Boolean, files: Boolean) {
    if (list.isEmpty()) { repo.toast("Не получилось прочитать файл"); return }
    list.forEachIndexed { i, it ->
        val c = if (i == 0) cap else ""
        if (it.file == null) { withContext(Dispatchers.Main) { repo.sendFile(convId, File(""), it.name, "", if (files || !it.isImage) "file" else "image", caption = c) }; return@forEachIndexed }
        val f = if (!files && compress && it.isImage && !it.isVideo) withContext(Dispatchers.IO) { compressImage(ctx, it.file) } else it.file
        val (name, mime) = if (f !== it.file) it.name.substringBeforeLast('.') + ".jpg" to "image/jpeg" else it.name to it.mime
        val kind = when { files -> "file"; it.isVideo -> "video"; it.isImage -> "image"; else -> "file" }
        withContext(Dispatchers.Main) { repo.sendFile(convId, f, name, mime, kind, caption = c) }
    }
}

/** Сжатие фото до 1920 px по длинной стороне, JPEG 85; поворот по EXIF учитывает ImageDecoder. Если не вышло меньше — оригинал. */
fun compressImage(ctx: Context, src: File): File = try {
    val max = 1920
    val bmp: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
        android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(src)) { d, info, _ ->
            val w = info.size.width; val h = info.size.height; val k = maxOf(w, h).toFloat() / max
            if (k > 1f) d.setTargetSize((w / k).toInt(), (h / k).toInt())
            d.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeFile(src.path, o)
        var s = 1; while (maxOf(o.outWidth, o.outHeight) / (s * 2) >= max) s *= 2
        BitmapFactory.decodeFile(src.path, BitmapFactory.Options().apply { inSampleSize = s })
    }
    val out = File(ctx.cacheDir, "img-${System.currentTimeMillis()}.jpg")
    out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    if (out.length() in 1 until src.length()) out else { out.delete(); src }
} catch (_: Exception) { src }
