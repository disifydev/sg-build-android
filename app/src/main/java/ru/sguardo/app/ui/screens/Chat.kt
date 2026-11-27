package ru.sguardo.app.ui.screens

import androidx.compose.ui.draw.blur
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.mutableStateListOf
import android.app.DatePickerDialog
import android.app.DownloadManager
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.sguardo.app.data.Clock
import ru.sguardo.app.data.Conversation
import ru.sguardo.app.data.Fmt
import ru.sguardo.app.data.Message
import ru.sguardo.app.data.Repo
import ru.sguardo.app.data.parseIso
import ru.sguardo.app.ui.Avatar
import ru.sguardo.app.ui.DialogBtn
import ru.sguardo.app.ui.DialogSpec
import ru.sguardo.app.ui.EaseM3
import ru.sguardo.app.ui.EmptyState
import ru.sguardo.app.ui.IconBtn
import ru.sguardo.app.ui.Ic
import ru.sguardo.app.ui.LocalNav
import ru.sguardo.app.ui.LocalOv
import ru.sguardo.app.ui.LocalPerms
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.Nav
import ru.sguardo.app.ui.Nets
import ru.sguardo.app.ui.Overlays
import ru.sguardo.app.ui.SheetTitle
import ru.sguardo.app.ui.SubTopBar
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.Tile
import ru.sguardo.app.ui.shadowCompat
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

private val RX = listOf("👍", "❤️", "🔥", "🙏", "😂", "👌", "🤝")

private sealed class Row2 { data class Day(val label: String, val key: String) : Row2(); data class Msg(val m: Message, val prev: Message?, val next: Message? = null) : Row2() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(id: String, initialRec: RecState? = null, initialReply: String? = null) {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current; val perms = LocalPerms.current
    val ctx = LocalContext.current
    val st by repo.state.collectAsState()
    val history by repo.history.collectAsState()
    val pendingAll by repo.pending.collectAsState()
    val typingAll by repo.typing.collectAsState()
    val presence by repo.presence.collectAsState()
    val scheduledAll by repo.scheduled.collectAsState()
    val online by repo.online.collectAsState()
    val conv = st.conversations.firstOrNull { it.id == id }
    if (conv == null) {
        Column(Modifier.fillMaxSize().background(Sg.c.surface)) { SubTopBar("Чат", { nav.back() }); EmptyState("chat", "Чат не найден", "Возможно, его удалили или он в корзине") }
        return
    }
    val contact = repo.contact(conv.contactId)
    val msgs = remember(st, history, pendingAll) { repo.messages(id) }
    val text = remember(id) { mutableStateOf(repo.drafts.value[id] ?: "") }
    var reply by remember(id) { mutableStateOf(initialReply) }
    var edit by remember(id) { mutableStateOf<String?>(null) }
    var sel by remember(id) { mutableStateOf<Set<String>?>(null) }
    var tpl by remember(id) { mutableStateOf(false) }
    var hidden by remember { mutableStateOf<String?>(null) }
    val rec = remember { mutableStateOf(initialRec) }
    val cam = remember { CircleCam() }
    val voice = remember { VoiceRecorder(ctx.applicationContext) }
    val bounds = remember { mutableStateMapOf<String, Float>() }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val backX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val widthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    LaunchedEffect(id) { repo.markRead(id); repo.loadHistory(id); repo.loadScheduled(id) }
    var firstScroll by remember(id) { mutableStateOf(true) }
    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) { if (firstScroll) list.scrollToItem(0) else if (list.firstVisibleItemIndex < 3) list.animateScrollToItem(0) }
        firstScroll = false; repo.markRead(id)
    }
    // старые сообщения при прокрутке вверх
    LaunchedEffect(list.firstVisibleItemIndex, msgs.size) {
        val lastVisible = list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (repo.hasMore.value[id] == true && lastVisible >= list.layoutInfo.totalItemsCount - 3) repo.loadHistory(id, older = true)
    }
    // запись голосового
    LaunchedEffect(rec.value) {
        val r = rec.value ?: return@LaunchedEffect
        if (!r.circle && !FAKE_DEVICES) {
            val why = voice.start()
            if (why != null) { repo.toast("Запись не началась: $why"); rec.value = null; return@LaunchedEffect }
            var lp = false
            while (rec.value === r) {
                if (r.paused != lp) { lp = r.paused; if (lp) voice.pause() else voice.resume() }
                r.level = if (r.paused) 0f else voice.level(); r.levels.add(r.level); if (r.levels.size > 40) r.levels.removeAt(0); kotlinx.coroutines.delay(90)
            }
        } else {
            // кружок: пауза записи камеры; волна условная
            var lp = false
            while (rec.value === r) {
                if (r.paused != lp) { lp = r.paused; runCatching { if (lp) cam.recording?.pause() else cam.recording?.resume() } }
                r.level = if (r.paused) 0f else (Math.random() * Math.random() * 1.4).toFloat(); r.levels.add(r.level); if (r.levels.size > 40) r.levels.removeAt(0); kotlinx.coroutines.delay(90)
            }
        }
    }
    val finishRec: (RecState, Boolean) -> Unit = { r, send ->
        rec.value = null
        val secs = r.elapsed() / 1000f
        if (FAKE_DEVICES) {
            if (send && secs >= .7f) repo.sendFile(id, File(""), if (r.circle) "circle.mp4" else "voice.m4a", "", if (r.circle) "video" else "audio", r.circle, secs.roundToInt().coerceAtLeast(1))
            else if (send) repo.toast(if (r.circle) "Удерживайте, чтобы записать кружок" else "Удерживайте, чтобы записать голосовое")
        } else if (r.circle) {
            cam.onDone = { f -> if (send && f != null && secs >= .7f) repo.sendFile(id, f, "Кружок.mp4", "video/mp4", "video", true, secs.roundToInt()) else f?.delete(); cam.recording = null; cam.onDone = null }
            cam.recording?.stop() ?: run { cam.onDone = null }
            cam.torch(false)
        } else {
            if (send && secs >= .7f) {
                val f = voice.stop()
                if (f != null) repo.sendFile(id, f, "Голосовое.m4a", "audio/mp4", "audio", false, secs.roundToInt())
                else repo.toast("Голосовое не отправлено: ${voice.error ?: "запись не сохранилась"}")
            }
            else { voice.cancel(); if (send) repo.toast("Удерживайте, чтобы записать голосовое") }
        }
    }
    val doSend: () -> Unit = {
        val v = text.value.trim()
        if (v.isNotEmpty()) {
            if (edit != null) { repo.editMessage(id, edit!!, v); edit = null }
            else { repo.send(id, v, reply); reply = null }
            text.value = ""; repo.saveDraft(id, ""); tpl = false
        }
    }
    val rows = remember(msgs) {
        val out = mutableListOf<Row2>()
        var lastDay = ""
        msgs.forEachIndexed { i, m ->
            val d = Fmt.dayChip(m.time)
            if (d != lastDay) { out += Row2.Day(d, "d-$i-$d"); lastDay = d }
            out += Row2.Msg(m, msgs.getOrNull(i - 1), msgs.getOrNull(i + 1))
        }
        out.reversed()
    }
    val k = (backX.value / widthPx * 2.2f).coerceIn(0f, 1f)
    val onBackDrag: (Float) -> Unit = { dx -> scope.launch { backX.snapTo((backX.value + dx).coerceAtLeast(0f)) } }
    val onBackEnd: () -> Unit = {
        if (backX.value > widthPx * .28f) nav.back() else scope.launch { backX.animateTo(0f, tween(300, easing = EaseM3)) }
    }
    if (sel != null) BackHandler { sel = null }

    // сообщения, которые уже были при открытии чата, не анимируем
    val seenIds = remember(id) { msgs.map { it.id }.toHashSet() }
    val hub = remember(id) { PlayerHub() }
    androidx.compose.runtime.CompositionLocalProvider(LocalPlayer provides hub) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .25f * k))) {
        Column(Modifier.fillMaxSize().graphicsLayer { translationX = k * 22 * density.density; scaleX = 1 - .1f * k; scaleY = 1 - .1f * k }
            .clip(RoundedCornerShape((k * 30).dp)).background(Sg.c.surface).imePadding()
            .then(if (hidden != null && android.os.Build.VERSION.SDK_INT >= 31) Modifier.blur(14.dp) else Modifier)) {
            // шапка
            Row(Modifier.fillMaxWidth().background(Sg.c.c).windowInsetsPadding(WindowInsets.statusBars).height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (sel != null) {
                    IconBtn("x", { sel = null })
                    T(sel!!.size.toString(), body(20f, FontWeight.Bold), modifier = Modifier.weight(1f).padding(start = 8.dp))
                    IconBtn("fwd", { if (sel!!.isEmpty()) repo.toast("Выберите сообщения") else openForward(ov, repo, id, sel!!.toList()) { sel = null } })
                    IconBtn("trash", { if (sel!!.isEmpty()) repo.toast("Выберите сообщения") else askDeleteMsgs(ov, repo, id, sel!!.toList(), msgs) { sel = null } })
                } else {
                    IconBtn("arrowL", { nav.back() })
                    Row(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(12.dp)).clickable { nav.go("profile/$id") }.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        val bookName = repo.bookName(contact)
                        Avatar(if (bookName == null && contact?.phone != null && contact.name == contact.phone) contact.phone else repo.nameOf(contact), 40.dp, conv.network, ring = Sg.c.c)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            T(repo.nameOf(contact), body(15f, FontWeight.SemiBold), maxLines = 1)
                            val tp = typingAll[id]; val pr = presence[id]
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when {
                                    tp != null -> T(if (tp.first == "recording") "записывает…" else "печатает…", body(11.5f), Sg.c.teal, maxLines = 1)
                                    pr?.online == true -> T("в сети", body(11.5f), Sg.c.teal, maxLines = 1)
                                    bookName != null -> { Ic("book", Sg.c.muted, size = 13.dp); Spacer(Modifier.width(4.dp)); T("в контактах · ${contact?.phone ?: contact?.handle ?: ""}", body(11.5f), Sg.c.muted, maxLines = 1) }
                                    else -> T(Nets.name(conv.network) + ((contact?.handle ?: contact?.phone)?.let { " · $it" } ?: "") +
                                        (pr?.lastSeen?.let { " · " + Fmt.lastSeen(parseIso(it)) } ?: ""), body(11.5f), Sg.c.muted, maxLines = 1)
                                }
                            }
                        }
                    }
                    ChatMenu(conv, inChat = true, onSelect = { sel = emptySet() })
                }
            }
            // закреплённое сообщение
            val ovC = Sg.c.ov; val tealC = Sg.c.teal
            val pinned = conv.pins.firstNotNullOfOrNull { pid -> msgs.firstOrNull { it.id == pid } }
            if (pinned != null && sel == null) Row(Modifier.fillMaxWidth().height(50.dp).background(Sg.c.cLow).drawBehind {
                drawRect(ovC, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx()))
            }.clickable {
                val idx = rows.indexOfFirst { it is Row2.Msg && it.m.id == pinned.id }; if (idx >= 0) scope.launch { list.animateScrollToItem(idx) }
            }.padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).drawBehind { drawRect(tealC, size = Size(3.dp.toPx(), size.height)) }.padding(start = 11.dp)) {
                    T("Закреплённое сообщение", body(12f, FontWeight.SemiBold), Sg.c.teal)
                    T(pinned.snippet, body(13.5f), maxLines = 1)
                }
                IconBtn("x", { repo.pinMessage(id, pinned.id, false) }, size = 36.dp, iconSize = 16.dp)
            }
            val sch = scheduledAll[id].orEmpty()
            if (sch.isNotEmpty() && sel == null) Row(Modifier.fillMaxWidth().background(Sg.c.cLow).clickable { openScheduledList(ov, repo, id) }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Ic("clock", Sg.c.teal, size = 16.dp); Spacer(Modifier.width(8.dp))
                T("Запланировано: ${sch.size}", body(13.5f, FontWeight.SemiBold), Sg.c.teal, modifier = Modifier.weight(1f))
                if (sch.any { it.status == "failed" }) T("Не отправлено", body(12.5f, FontWeight.SemiBold), Sg.c.danger)
            }
            // лента
            Box(Modifier.weight(1f).fillMaxWidth().pointerInput(Unit) {
                detectHorizontalDragGestures(onDragEnd = onBackEnd, onDragCancel = onBackEnd) { ch, dx -> if (dx > 0 || backX.value > 0) { ch.consume(); onBackDrag(dx) } }
            }) {
                ChatWallpaper(Modifier.matchParentSize())
                LazyColumn(Modifier.fillMaxSize(), state = list, reverseLayout = true, contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (typingAll[id] != null) item(key = "typing") { Row(Modifier.fillMaxWidth()) { TypingBubble() } }
                    items(rows, key = { r -> when (r) { is Row2.Day -> r.key; is Row2.Msg -> r.m.id } }) { r ->
                        when (r) {
                            is Row2.Day -> Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                T(r.label, body(12f, FontWeight.SemiBold), Sg.c.muted, modifier = Modifier.background(Sg.c.cHigh, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 5.dp))
                            }
                            is Row2.Msg -> MsgRow(r.m, r.prev, r.next, conv, sel, hidden == r.m.id, fresh = r.m.id !in seenIds && kotlin.math.abs(Clock.now() - r.m.time) < 60_000,
                                onTap = { if (sel != null) sel = sel!!.let { s -> if (r.m.id in s) s - r.m.id else s + r.m.id } },
                                onLong = {
                                    if (sel == null) {
                                        hidden = r.m.id
                                        ov.layer = { MsgContext(r.m, conv, bounds[r.m.id] ?: 300f, close = { ov.layer = null; hidden = null },
                                            onReply = { reply = r.m.id; edit = null }, onEdit = { edit = r.m.id; reply = null; text.value = r.m.text }, onSelect = { sel = setOf(r.m.id) }, msgs = msgs) }
                                    }
                                },
                                onReply = { reply = r.m.id; edit = null },
                                onBackDrag = onBackDrag, onBackEnd = onBackEnd,
                                onQuote = { qid -> val idx = rows.indexOfFirst { it is Row2.Msg && it.m.id == qid }; if (idx >= 0) scope.launch { list.animateScrollToItem(idx) } else repo.toast("Сообщение удалено") },
                                onBounds = { y -> bounds[r.m.id] = y })
                        }
                    }
                    if (!online) item { Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Row(Modifier.background(Sg.c.cHigh, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Ic("wifiOff", Sg.c.muted, size = 13.dp); Spacer(Modifier.width(4.dp)); T("нет сети: показываем сохранённое", body(12f, FontWeight.SemiBold), Sg.c.muted)
                        } } }
                }
            }
            if (sel == null) Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                val barMsg = (edit ?: reply)?.let { mid -> msgs.firstOrNull { it.id == mid } }
                val isEdit = edit != null
                val barC: (@Composable () -> Unit)? = if (barMsg != null) { {
                    CBar(if (isEdit) "edit" else "reply", if (isEdit) "Редактирование" else if (barMsg.isOut) "Ответ на своё сообщение" else repo.nameOf(contact), barMsg.snippet) {
                        if (isEdit) text.value = ""; reply = null; edit = null }
                } } else null
                Composer(id, text,
                    bar = barC,
                    showTpl = tpl, onTplToggle = { tpl = !tpl }, onSend = doSend,
                    onAttach = { openAttach(ov, repo, perms, ctx, id, onText = { t -> text.value = t; repo.saveDraft(id, t) }) { tpl = true } },
                    onLater = { if (text.value.isBlank()) repo.toast("Сначала напишите сообщение") else openLater(ov, repo, ctx, id, text.value.trim(), reply) { text.value = ""; repo.saveDraft(id, ""); reply = null } },
                    rec = rec, onRecDone = finishRec, editing = isEdit)
            }
        }
        rec.value?.let { r -> if (r.circle) CircleRecorder(r, cam, FAKE_DEVICES, onFail = { why -> repo.toast(why); cam.recording?.stop(); cam.onDone = null; rec.value = null },
            onCancel = { finishRec(r, false) }, onSend = { finishRec(r, true) }) }
    }
    }
}


/** Плашка ответа/правки над полем (.cbar). */
@Composable
fun CBar(kind: String, title: String, text: String, onClose: () -> Unit) {
    val teal = Sg.c.teal
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 0.dp, top = 2.dp, bottom = 0.dp), verticalAlignment = Alignment.CenterVertically) {
        Ic(if (kind == "edit") "edit" else "reply", teal, size = 16.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).drawBehind { drawRect(teal, size = Size(3.dp.toPx(), size.height)) }.padding(start = 11.dp)) {
            T(title, body(13f, FontWeight.SemiBold), teal, maxLines = 1)
            T(text, body(13f), Sg.c.muted, maxLines = 1)
        }
        IconBtn("x", onClose, size = 36.dp, iconSize = 16.dp, tint = Sg.c.muted)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MsgRow(m: Message, prev: Message?, next: Message?, conv: Conversation, sel: Set<String>?, hidden: Boolean, fresh: Boolean, onTap: () -> Unit, onLong: () -> Unit, onReply: () -> Unit,
                   onBackDrag: (Float) -> Unit, onBackEnd: () -> Unit, onQuote: (String) -> Unit, onBounds: (Float) -> Unit) {
    // новое сообщение «прилетает» снизу, как в Telegram (своё — от поля ввода справа, входящее — слева)
    val anim = LocalChatStyle.current.anim
    val enter = remember { Animatable(if (anim && fresh && Arrived.claim(m)) 0f else 1f) }
    LaunchedEffect(Unit) { if (enter.value < 1f) enter.animateTo(1f, spring(dampingRatio = .72f, stiffness = 420f)) }
    val x = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var mode by remember { mutableStateOf<String?>(null) }
    val picked = sel != null && m.id in sel
    Box(Modifier.fillMaxWidth().onGloballyPositioned { onBounds(it.positionInRoot().y) }
        .pointerInput(m.id, sel == null) {
            if (sel != null) return@pointerInput
            var total = 0f
            detectHorizontalDragGestures(
                onDragStart = { total = 0f; mode = null },
                onDragEnd = {
                    if (mode == "reply") { if (x.value < -45 * density.density) onReply(); scope.launch { x.animateTo(0f, spring(dampingRatio = .6f, stiffness = 500f)) } }
                    else if (mode == "back") onBackEnd()
                    mode = null
                },
                onDragCancel = { scope.launch { x.animateTo(0f) }; if (mode == "back") onBackEnd(); mode = null },
            ) { ch, dx ->
                total += dx
                if (mode == null) mode = if (total < 0) "reply" else "back"
                ch.consume()
                if (mode == "reply") {
                    var nx = minOf(0f, x.value + dx); val lim = -60 * density.density
                    if (nx < lim) nx = x.value + dx * .3f
                    scope.launch { x.snapTo(minOf(0f, nx)) }
                } else onBackDrag(dx)
            }
        }
        .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap, onLongClick = onLong)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (sel != null) {
                Box(Modifier.padding(end = 10.dp).size(24.dp).clip(CircleShape).background(if (picked) Sg.c.teal else Color.Transparent)
                    .border(1.6.dp, if (picked) Sg.c.teal else Sg.c.faint, CircleShape), contentAlignment = Alignment.Center) { if (picked) Ic("check", Sg.c.onTeal, size = 13.dp) }
            }
            Box(Modifier.weight(1f).offset { IntOffset(x.value.roundToInt(), 0) }.graphicsLayer {
                val e = enter.value
                if (e < 1f) {
                    translationY = (1f - e) * 56.dp.toPx(); alpha = e.coerceIn(0f, 1f)
                    scaleX = .86f + .14f * e; scaleY = .86f + .14f * e
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (m.isOut) 1f else 0f, 1f)
                }
            }) { DustBox(Dust.ids[m.id] == true) { Bubble(m, conv, prev, hidden = hidden, next = next, onQuote = onQuote) } }
        }
        val ro = (-x.value / (55 * density.density)).coerceIn(0f, 1f)
        if (ro > 0f) Box(Modifier.align(Alignment.CenterEnd).padding(end = 2.dp).size(32.dp).graphicsLayer { alpha = ro; scaleX = .4f + ro * .6f; scaleY = .4f + ro * .6f }
            .background(Sg.c.cHigh, CircleShape), contentAlignment = Alignment.Center) { Ic("reply", Sg.c.fg, size = 16.dp) }
    }
}

/** Меню чата (⋮): профиль, открыть в сети, закрепить, звук, выбрать, удалить. */
@Composable
fun ChatMenu(conv: Conversation, inChat: Boolean, onSelect: () -> Unit = {}) {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconBtn("morev", { open = true })
        DropdownMenu(open, { open = false }, containerColor = Sg.c.c, shape = RoundedCornerShape(6.dp)) {
            if (inChat) MenuItem("Профиль", "user") { open = false; nav.go("profile/${conv.id}") }
            MenuItem("Открыть в ${Nets.name(conv.network)}", "ext") { open = false; openInNetwork(ctx, repo, conv) }
            MenuItem(if (conv.pinnedAt != null) "Открепить" else "Закрепить", "pin") { open = false; repo.setConvFlag(conv.id, "pinned", conv.pinnedAt == null, if (conv.pinnedAt == null) "Чат закреплён" else "Чат откреплён") }
            MenuItem(if (conv.muted) "Включить уведомления" else "Выключить уведомления", if (conv.muted) "bell" else "bellOff") {
                open = false; repo.setConvFlag(conv.id, "muted", !conv.muted, if (conv.muted) "Уведомления из чата включены" else "Уведомления из чата выключены")
            }
            if (inChat) MenuItem("Выбрать сообщения", "selc") { open = false; onSelect() }
            MenuItem("Удалить чат", "trash", danger = true) { open = false; askDeleteChat(repo, ov, conv.id) { nav.back() } }
        }
    }
}

fun openInNetwork(ctx: Context, repo: Repo, conv: Conversation) {
    val c = repo.contact(conv.contactId)
    val h = c?.handle?.removePrefix("@")
    val digits = c?.phone?.filter { it.isDigit() }
    val url = when (conv.network) {
        "telegram" -> h?.let { "https://t.me/$it" }
        "whatsapp" -> digits?.let { "https://wa.me/$it" }
        "instagram" -> h?.let { "https://instagram.com/$it" }
        "vk" -> h?.let { "https://vk.com/$it" }
        "tiktok" -> h?.let { "https://www.tiktok.com/@$it" }
        "threads" -> h?.let { "https://www.threads.net/@$it" }
        else -> null
    }
    if (url == null) { repo.toast("Откройте приложение ${Nets.name(conv.network)}"); return }
    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { repo.toast("Не нашли приложение") }
}

/** Долгое нажатие на сообщение: реакции, копия пузыря и меню действий. */
@Composable
private fun MsgContext(m: Message, conv: Conversation, topY: Float, close: () -> Unit, onReply: () -> Unit, onEdit: () -> Unit, onSelect: () -> Unit, msgs: List<Message>) {
    val repo = LocalRepo.current; val ov = LocalOv.current
    val ctx = LocalContext.current
    val density = LocalDensity.current
    BackHandler(onBack = close)
    val reacts = repo.demo.value || conv.network == "telegram" || conv.network == "whatsapp"
    val items = buildList<Triple<String, String, () -> Unit>> {
        add(Triple("Ответить", "reply") { close(); onReply() })
        if (m.kind == "text") add(Triple("Скопировать", "copy") {
            close(); (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("text", m.text)); repo.toast("Текст скопирован")
        }) else add(Triple(when (m.kind) { "photo" -> "Сохранить фото"; "circle", "video" -> "Сохранить видео"; "voice" -> "Сохранить аудио"; else -> "Сохранить файл" }, "save") {
            close(); saveAttachment(ctx, repo, m)
        })
        if (m.isOut && m.kind == "text") add(Triple("Изменить", "edit") { close(); onEdit() })
        val isPinned = m.id in conv.pins
        add(Triple(if (isPinned) "Открепить" else "Закрепить", "pin") {
            close()
            // как в Telegram: в Telegram можно закрепить у обоих или только у себя
            if (!isPinned && conv.network == "telegram") ov.confirm(DialogSpec("Закрепить сообщение?", "«У обоих»: закреп увидит и ${repo.nameOfConv(conv)} в Telegram.", buttons = listOf(
                DialogBtn("Только у меня") { repo.pinMessage(conv.id, m.id, true, both = false) },
                DialogBtn("У обоих") { repo.pinMessage(conv.id, m.id, true, both = true) })))
            else repo.pinMessage(conv.id, m.id, !isPinned)
        })
        add(Triple("Переслать", "fwd") { close(); openForward(ov, repo, conv.id, listOf(m.id)) {} })
        add(Triple("Удалить", "trash") { close(); askDeleteMsgs(ov, repo, conv.id, listOf(m.id), msgs) {} })
        add(Triple("Выбрать", "selc") { close(); onSelect() })
    }
    val screenH = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val menuH = with(density) { (items.size * 48 + 16 + (if (m.isOut) 38 else 0)).dp.toPx() }
    val rxH = with(density) { 60.dp.toPx() }
    val want = topY - rxH
    val y = want.coerceAtMost(screenH - menuH - rxH - with(density) { 220.dp.toPx() }).coerceAtLeast(with(density) { 70.dp.toPx() })
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (android.os.Build.VERSION.SDK_INT >= 31) .38f else .55f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close)) {
        Column(Modifier.fillMaxWidth().offset { IntOffset(0, y.roundToInt()) }.padding(horizontal = 12.dp),
            horizontalAlignment = if (m.isOut) Alignment.End else Alignment.Start, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (reacts) Row(Modifier.shadowCompat(4.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(Sg.c.cHigh).padding(horizontal = 6.dp, vertical = 5.dp)) {
                RX.forEachIndexed { i, e ->
                    val s = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { kotlinx.coroutines.delay(i * 28L); s.animateTo(1f, spring(dampingRatio = .45f, stiffness = 500f)) }
                    val mine = m.reactions?.any { it.mine && it.emoji == e } == true
                    Box(Modifier.size(41.dp).graphicsLayer { scaleX = s.value; scaleY = s.value }.clip(CircleShape).background(if (mine) Sg.c.teal.copy(alpha = .2f) else Color.Transparent)
                        .clickable { close(); repo.react(conv.id, m.id, if (mine) null else e) }, contentAlignment = Alignment.Center) { T(e, body(24f)) }
                }
            }
            val appear = remember { Animatable(.96f) }
            LaunchedEffect(Unit) { appear.animateTo(1.04f, tween(240)); appear.animateTo(1f, tween(200)) }
            Box(Modifier.graphicsLayer { scaleX = appear.value; scaleY = appear.value }) { BubbleBody(m, conv, compact = false) }
            val ms = remember { Animatable(.5f) }
            LaunchedEffect(Unit) { ms.animateTo(1f, spring(dampingRatio = .6f, stiffness = 400f)) }
            Column(Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max).widthIn(min = 236.dp).graphicsLayer { scaleX = ms.value; scaleY = ms.value; alpha = ((ms.value - .5f) * 2).coerceIn(0f, 1f)
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (m.isOut) 1f else 0f, 0f) }
                .shadowCompat(10.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Sg.c.cHigh).padding(vertical = 6.dp)) {
                if (m.isOut && !m.pending) Row(Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Ic("checks", Sg.c.muted, size = 13.dp); Spacer(Modifier.width(6.dp))
                    T("Отправлено ${if (Clock.local(m.time).toLocalDate() == Clock.today()) "сегодня" else Fmt.dayMonth(m.time)} в ${Fmt.hm(m.time)}", body(12.5f), Sg.c.muted)
                }
                items.forEach { (t, ic, fn) ->
                    // «Выбрать» отдельно, под чертой, как в Telegram
                    if (ic == "selc") androidx.compose.material3.HorizontalDivider(color = Sg.c.hair, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    PvItem(t, ic, danger = ic == "trash", onClick = fn)
                }
            }
        }
    }
}

fun saveAttachment(ctx: Context, repo: Repo, m: Message) {
    val done = when (m.kind) { "photo" -> "Фото сохранено в загрузки"; "circle", "video" -> "Видео сохранено в загрузки"; "voice" -> "Аудио сохранено в загрузки"; else -> "Файл сохранён в загрузки" }
    if (repo.demo.value) { repo.toast(done); return }
    val url = repo.api.media(m.att?.url) ?: run { repo.toast("Файл недоступен"); return }
    try {
        val req = DownloadManager.Request(Uri.parse(url)).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, m.att?.name ?: "sguardo-${m.id}")
        if (repo.api.isOwn(url)) req.addRequestHeader("Cookie", repo.api.cookies.loadForRequest(repo.api.url("/")).joinToString("; ") { "${it.name}=${it.value}" })
        (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
        repo.toast("Сохраняем в загрузки")
    } catch (_: Exception) { repo.toast("Не получилось сохранить") }
}

fun askDeleteMsgs(ov: Overlays, repo: Repo, convId: String, ids: List<String>, msgs: List<Message>, after: () -> Unit) {
    val own = msgs.filter { it.id in ids }.all { it.isOut }
    ov.confirm(DialogSpec(if (ids.size > 1) "Удалить ${ids.size} ${Fmt.plural(ids.size, "сообщение", "сообщения", "сообщений")}?" else "Удалить сообщение?",
        "Сообщение пропадёт из этого чата.", check = if (own) "Удалить и у собеседника" else null,
        buttons = listOf(DialogBtn("Удалить", danger = true) { both -> after(); if (repo.prefs.animations.value) Dust.run(ids) { repo.deleteMessages(convId, ids, both) } else repo.deleteMessages(convId, ids, both) })))
}

fun openForward(ov: Overlays, repo: Repo, fromConv: String, ids: List<String>, after: () -> Unit) {
    ov.openSheet {
        SheetTitle(if (ids.size > 1) "Переслать ${ids.size} ${Fmt.plural(ids.size, "сообщение", "сообщения", "сообщений")}" else "Переслать сообщение")
        val st = repo.state.value
        sortConvs(st.conversations.filter { !it.archived && !it.trashed && it.id != fromConv }).forEach { c ->
            Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).clickable { ov.closeSheet(); repo.forward(fromConv, ids, c.id); after() }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Avatar(repo.nameOfConv(c), 36.dp, c.network, ring = Sg.c.cLow); Spacer(Modifier.width(12.dp)); T(repo.nameOfConv(c), body(15f), maxLines = 1)
            }
        }
    }
}

/** «Отправить позже»: быстрые варианты и выбор даты и времени. */
fun openLater(ov: Overlays, repo: Repo, ctx: Context, convId: String, text: String, replyTo: String?, after: () -> Unit) {
    val today = Clock.today()
    val nowH = Clock.local(Clock.now()).hour
    val opts = buildList {
        if (nowH < 19) add(Triple("Сегодня вечером", "19:00", Clock.at(today, 19, 0)))
        add(Triple("Завтра утром", "10:00", Clock.at(today.plusDays(1), 10, 0)))
        add(Triple("В понедельник", "09:00", Clock.at(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)), 9, 0)))
    }
    ov.openSheet {
        SheetTitle("Отправить позже")
        opts.forEach { (t, hm, at) ->
            Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).clickable { ov.closeSheet(); repo.schedule(convId, text, at, "${t.lowercase()} в $hm", replyTo); after() }
                .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Ic("clock", Sg.c.fg, size = 16.dp); Spacer(Modifier.width(12.dp)); T(t, body(15f), modifier = Modifier.weight(1f)); T(hm, body(13f), Sg.c.muted)
            }
        }
        Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).clickable {
            ov.closeSheet()
            val z = Clock.local(Clock.now())
            DatePickerDialog(ctx, { _, y, mo, d ->
                TimePickerDialog(ctx, { _, h, mi ->
                    val at = Clock.at(LocalDate.of(y, mo + 1, d), h, mi)
                    if (at <= Clock.now()) repo.toast("Это время уже прошло") else { repo.schedule(convId, text, at, "${d} ${Fmt.dayMonth(at).substringAfter(' ')} в ${"%02d:%02d".format(h, mi)}", replyTo); after() }
                }, 10, 0, true).show()
            }, z.year, z.monthValue - 1, z.dayOfMonth).apply { datePicker.minDate = Clock.now() }.show()
        }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Ic("cal", Sg.c.fg, size = 16.dp); Spacer(Modifier.width(12.dp)); T("Выбрать дату и время", body(15f))
        }
    }
}

fun openScheduledList(ov: Overlays, repo: Repo, convId: String) {
    ov.openSheet {
        SheetTitle("Запланировано")
        val items = repo.scheduled.collectAsState().value[convId].orEmpty()
        if (items.isEmpty()) T("Ничего не запланировано", body(14f), Sg.c.muted)
        items.forEach { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T(s.text.ifBlank { "Вложение" }, body(15f), maxLines = 2)
                    val at = parseIso(s.sendAt)
                    T(if (s.status == "failed") "Не отправлено: ${s.error ?: "ошибка сети"}" else "${Fmt.dayMonth(at)} в ${Fmt.hm(at)}", body(12.5f), if (s.status == "failed") Sg.c.danger else Sg.c.muted)
                }
                T("Отменить", body(14f, FontWeight.SemiBold), Sg.c.danger, modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { repo.cancelScheduled(convId, s.id) }.padding(10.dp))
            }
        }
    }
}

/** Вложения как в Telegram: сетка последних фото с камерой и выбором нескольких, внизу круглые кнопки. */
fun openAttach(ov: Overlays, repo: Repo, perms: ru.sguardo.app.ui.Permissions, ctx: Context, convId: String, onText: (String) -> Unit = {}, onTemplate: () -> Unit) {
    ov.openSheet { AttachSheet(convId, onTemplate, onText) }
}

@Composable
fun AttachBtn(icon: String, text: String, color: Color, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by androidx.compose.animation.core.animateFloatAsState(if (pressed) .9f else 1f, spring(dampingRatio = .5f, stiffness = 600f), label = "ab")
    Column(Modifier.width(76.dp).clip(RoundedCornerShape(16.dp)).clickable(interactionSource = src, indication = null, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(CircleShape).background(color), contentAlignment = Alignment.Center) { Ic(icon, Color.White) }
        Spacer(Modifier.height(6.dp))
        T(text, body(13f, FontWeight.Medium), Sg.c.fg)
    }
}

fun copyToCache(ctx: Context, uri: Uri): Triple<File, String, String>? = try {
    var name = "file"
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) name = c.getString(0) ?: name }
    val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
    val f = File(ctx.cacheDir, "up-${System.currentTimeMillis()}-$name")
    ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
    Triple(f, name, mime)
} catch (_: Exception) { null }


/** Предпросмотр фото перед отправкой: тот же экран подготовки, что и для нескольких. */
fun openPhotoPreview(ov: Overlays, repo: Repo, convId: String, file: Triple<File, String, String>?, demoName: String?) {
    openMediaPrep(ov, convId, listOf(if (file != null) PrepItem(file.first, file.second, file.third) else PrepItem(null, demoName ?: "demo:0", "image/jpeg")))
}
