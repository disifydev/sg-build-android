package ru.sguardo.app.ui.screens

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ru.sguardo.app.ui.LocalOv
import ru.sguardo.app.ui.LocalPerms
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.SheetTitle
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import java.io.File

/** Фото или видео в сетке вложений; demo — номер картинки в демо. */
data class GItem(val uri: Uri?, val video: Boolean, val dur: Long, val demo: Int? = null) { val key get() = uri?.toString() ?: "demo:$demo" }

/** Последние фото и видео телефона (как лента в «Вложениях» Telegram). */
fun loadRecent(ctx: Context, limit: Int = 90): List<GItem> = try {
    val base = MediaStore.Files.getContentUri("external")
    val proj = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Video.VideoColumns.DURATION)
    val sel = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
    val order = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
    val cursor = if (Build.VERSION.SDK_INT >= 30) ctx.contentResolver.query(base, proj, Bundle().apply {
        putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, sel)
        putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, order)
        putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
    }, null) else ctx.contentResolver.query(base, proj, sel, null, "$order LIMIT $limit")
    val out = ArrayList<GItem>()
    cursor?.use { c ->
        while (c.moveToNext()) {
            val id = c.getLong(0); val video = c.getInt(1) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            val uri = ContentUris.withAppendedId(if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            out += GItem(uri, video, if (video) c.getLong(2) else 0L)
        }
    }
    out
} catch (_: Exception) { emptyList() }

private suspend fun toPrep(ctx: Context, l: List<GItem>): List<PrepItem> = withContext(Dispatchers.IO) {
    l.mapNotNull { g -> if (g.demo != null) PrepItem(null, "demo:${g.demo}", "image/jpeg") else g.uri?.let { u -> copyToCache(ctx, u)?.let { (f, n, m) -> PrepItem(f, n, m) } } }
}

/**
 * Лист «Вложения» как в Telegram: сверху сетка — плитка камеры и последние фото и видео с кружками выбора (номер по порядку).
 * Нажатие на фото открывает подготовку, нажатие на кружок выбирает. Когда что-то выбрано, внизу подпись и «Отправить» со счётчиком,
 * иначе круглые кнопки «Галерея», «Файл», «Оплата», «Шаблон».
 */
@Composable
fun ColumnScope.AttachSheet(convId: String, onTemplate: () -> Unit) {
    val repo = LocalRepo.current; val ov = LocalOv.current; val perms = LocalPerms.current; val ctx = LocalContext.current
    val demo = repo.demo.value
    var allowed by remember { mutableStateOf(demo || perms.hasGallery()) }
    var media by remember { mutableStateOf<List<GItem>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    val picked = remember { mutableStateListOf<GItem>() }
    var caption by remember { mutableStateOf("") }
    LaunchedEffect(allowed, reload) {
        media = if (demo) GAL.indices.map { GItem(null, false, 0, it) } else if (allowed) withContext(Dispatchers.IO) { loadRecent(ctx) } else emptyList()
    }
    val openPrep: (List<GItem>, GItem?) -> Unit = { sel, first ->
        val l = if (sel.isEmpty()) listOfNotNull(first) else (listOfNotNull(first?.takeIf { it !in sel }) + sel)
        repo.scope.launch {
            val items = toPrep(ctx, l)
            if (items.isEmpty()) repo.toast("Не получилось прочитать файл") else { ov.closeSheet(); openMediaPrep(ov, convId, items, caption = caption) }
        }
    }
    SheetTitle(if (picked.isEmpty()) "Вложения" else "Выбрано: ${picked.size}")
    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxWidth().height(if (allowed && media.isEmpty() && !demo) 130.dp else 330.dp).clip(RoundedCornerShape(16.dp)),
        horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        item(key = "cam") {
            Box(Modifier.aspectRatio(1f).background(Color(0xFF1C2524)).clickable {
                if (demo) { repo.toast("В демо камера для фото не открывается"); return@clickable }
                perms.takePhoto { f -> if (f != null) { ov.closeSheet(); openMediaPrep(ov, convId, listOf(PrepItem(f, f.name, "image/jpeg"))) } }
            }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Ic("camera", Color.White, size = 28.dp); Spacer(Modifier.height(4.dp)); T("Камера", body(12f, FontWeight.Medium), Color.White.copy(alpha = .85f)) }
            }
        }
        if (!allowed) item(key = "ask", span = { GridItemSpan(2) }) {
            Box(Modifier.aspectRatio(2f).background(Sg.c.cHigh).clickable { perms.gallery { ok -> allowed = ok || perms.hasGallery(); if (!allowed) repo.toast("Без доступа можно выбрать фото кнопкой «Галерея» ниже") } }
                .padding(12.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Ic("image", Sg.c.primary, size = 24.dp); Spacer(Modifier.height(6.dp))
                    T("Показать последние фото", body(13.5f, FontWeight.SemiBold), Sg.c.primary)
                    T("нужен доступ к фото", body(12f), Sg.c.muted)
                }
            }
        }
        items(media, key = { it.key }) { g ->
            val n = picked.indexOf(g)
            val k by animateFloatAsState(if (n >= 0) 1f else 0f, spring(dampingRatio = .6f, stiffness = 500f), label = "pk")
            Box(Modifier.aspectRatio(1f).clickable { openPrep(picked.toList(), g) }) {
                Box(Modifier.fillMaxSize().graphicsLayer { scaleX = 1f - .1f * k; scaleY = 1f - .1f * k }.clip(RoundedCornerShape((k * 10).dp))) {
                    if (g.demo != null) Box(Modifier.fillMaxSize().background(GAL[g.demo % GAL.size]))
                    else AsyncImage(g.uri, null, Modifier.fillMaxSize().background(Sg.c.hair), contentScale = ContentScale.Crop)
                }
                if (g.video) Row(Modifier.align(Alignment.BottomStart).padding(6.dp).background(Color.Black.copy(alpha = .45f), RoundedCornerShape(8.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Ic("play", Color.White, size = 10.dp); Spacer(Modifier.width(3.dp)); T(ru.sguardo.app.data.Fmt.dur((g.dur / 1000).toInt()), body(11f, FontWeight.Medium), Color.White)
                }
                // кружок выбора: большая зона нажатия в углу
                Box(Modifier.align(Alignment.TopEnd).size(40.dp).clickable { if (n >= 0) picked.remove(g) else picked.add(g) }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(if (n >= 0) Sg.c.teal else Color.Black.copy(alpha = .2f)).border(1.6.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center) { if (n >= 0) T("${n + 1}", body(12f, FontWeight.Bold), Sg.c.onTeal) }
                }
            }
        }
        if (allowed && media.isEmpty() && !demo) item(key = "empty", span = { GridItemSpan(2) }) {
            Box(Modifier.aspectRatio(2f), contentAlignment = Alignment.Center) { T("Фото пока нет", body(13.5f), Sg.c.muted) }
        }
    }
    Spacer(Modifier.height(14.dp))
    AnimatedContent(picked.isNotEmpty(), label = "att") { many ->
        if (many) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(Sg.c.cHigh).padding(horizontal = 18.dp, vertical = 13.dp)) {
                if (caption.isEmpty()) T("Добавить подпись…", body(15f), Sg.c.faint)
                BasicTextField(caption, { caption = it }, textStyle = body(15f).copy(color = Sg.c.fg), maxLines = 4, cursorBrush = SolidColor(Sg.c.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.width(8.dp))
            Box {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Sg.c.primary).clickable {
                    val sel = picked.toList(); val cap = caption.trim()
                    ov.closeSheet()
                    repo.scope.launch { sendPrepared(repo, ctx, convId, toPrep(ctx, sel), cap, compress = true, files = false) }
                }, contentAlignment = Alignment.Center) { Ic("send", Sg.c.onPrimary) }
                Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).size(22.dp).border(2.dp, Sg.c.c, CircleShape).clip(CircleShape).background(Sg.c.teal), contentAlignment = Alignment.Center) {
                    T(picked.size.toString(), body(11f, FontWeight.Bold), Sg.c.onTeal)
                }
            }
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            AttachBtn("image", "Галерея", Color(0xFF3E8BE8)) {
                if (demo) { openPrep(emptyList(), media.firstOrNull()); return@AttachBtn }
                perms.pickImages { uris -> if (uris.isNotEmpty()) { ov.closeSheet(); repo.scope.launch {
                    val items = withContext(Dispatchers.IO) { prepFromUris(ctx, uris) }
                    if (items.isEmpty()) repo.toast("Не получилось прочитать файл") else openMediaPrep(ov, convId, items)
                } } }
            }
            AttachBtn("file", "Файл", Color(0xFF1FA39A)) {
                if (demo) { ov.closeSheet(); openMediaPrep(ov, convId, listOf(PrepItem(null, "Счёт_27_ноября.pdf", "application/pdf")), files = true); return@AttachBtn }
                perms.pickFiles { uris -> if (uris.isNotEmpty()) { ov.closeSheet(); repo.scope.launch {
                    val items = withContext(Dispatchers.IO) { prepFromUris(ctx, uris) }
                    if (items.isEmpty()) repo.toast("Не получилось прочитать файл") else openMediaPrep(ov, convId, items, files = true)
                } } }
            }
            AttachBtn("card", "Оплата", Color(0xFF3FA45B)) {
                val c = repo.conv(convId)
                if (c == null) repo.toast("Чат не найден") else ov.openSheet { PaySheet(c.contactId, "paid", title = "Записать оплату", lockClient = true) }
            }
            AttachBtn("bolt", "Шаблон", Color(0xFFE79A2E)) { ov.closeSheet(); onTemplate() }
        }
    }
    Spacer(Modifier.height(6.dp))
}
