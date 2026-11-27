package ru.sguardo.app.ui.screens

import ru.sguardo.app.ui.GROUP_P
import ru.sguardo.app.ui.klsTitle
import ru.sguardo.app.ui.inKls
import ru.sguardo.app.ui.allGroups
import ru.sguardo.app.ui.allClasses
import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.sguardo.app.data.Conversation
import ru.sguardo.app.data.Fmt
import ru.sguardo.app.data.Load
import ru.sguardo.app.data.Repo
import ru.sguardo.app.data.obj
import ru.sguardo.app.ui.Avatar
import ru.sguardo.app.ui.DialogBtn
import ru.sguardo.app.ui.DialogSpec
import ru.sguardo.app.ui.Dot
import ru.sguardo.app.ui.EaseM3
import ru.sguardo.app.ui.EmptyState
import ru.sguardo.app.ui.ErrorState
import ru.sguardo.app.ui.HScroll
import ru.sguardo.app.ui.Ic
import ru.sguardo.app.ui.LocalNav
import ru.sguardo.app.ui.LocalOv
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.LocalUi
import ru.sguardo.app.ui.Nets
import ru.sguardo.app.ui.OfflineBanner
import ru.sguardo.app.ui.Overlays
import ru.sguardo.app.ui.Seg
import ru.sguardo.app.ui.SkelRows
import ru.sguardo.app.ui.SubTopBar
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.TopBar
import ru.sguardo.app.ui.VGap
import ru.sguardo.app.ui.Chip
import ru.sguardo.app.ui.Hint
import ru.sguardo.app.ui.folderDot
import ru.sguardo.app.ui.lastTime
import ru.sguardo.app.ui.shadowCompat
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import kotlin.math.roundToInt

fun lastText(c: Conversation): String {
    val m = c.last ?: return "Нет сообщений"
    return (if (m.isOut) "Вы: " else "") + if (m.kind == "photo" && m.text.isNotBlank()) "Фото, ${m.text}" else m.snippet
}

fun sortConvs(l: List<Conversation>) = l.sortedWith(compareByDescending<Conversation> { it.pinnedAt != null }.thenByDescending { it.pinnedAt ?: "" }.thenByDescending { lastTime(it) })

@Composable
fun MessagesScreen() {
    val repo = LocalRepo.current; val ui = LocalUi.current; val nav = LocalNav.current
    val st by repo.state.collectAsState()
    val load by repo.load.collectAsState()
    val online by repo.online.collectAsState()
    val bookV by repo.book.version.collectAsState()
    val namesOn by repo.prefs.namesFromBook.collectAsState()
    val list = rememberLazyListState()
    LaunchedEffect(ui.scrollTopSignal) { if (ui.scrollTopSignal > 0) list.animateScrollToItem(0) }
    val live = st.conversations.filter { !it.archived && !it.trashed }
    val un = live.count { it.unread > 0 }; val wt = live.count { it.last?.isOut == false }
    val arch = st.conversations.filter { it.archived && !it.trashed }
    val q = ui.q.trim().lowercase()
    val shown = remember(st, ui.mf, ui.net, ui.fold, ui.mkls, q, bookV, namesOn) {
        var cs = live
        if (ui.mf == "unread") cs = cs.filter { it.unread > 0 }
        if (ui.mf == "wait") cs = cs.filter { it.last?.isOut == false }
        if (ui.net != "all") cs = cs.filter { it.network == ui.net }
        if (ui.fold != "all") cs = cs.filter { repo.contact(it.contactId)?.folderId == ui.fold }
        if (ui.mkls != "all") cs = cs.filter { repo.contact(it.contactId)?.inKls(ui.mkls) == true }
        if (q.isNotEmpty()) cs = cs.filter { (repo.nameOfConv(it) + " " + lastText(it)).lowercase().contains(q) }
        sortConvs(cs)
    }
    var found by remember { mutableStateOf<List<ru.sguardo.app.data.SearchItem>>(emptyList()) }
    LaunchedEffect(q) { if (q.length < 2) { found = emptyList(); return@LaunchedEffect }; kotlinx.coroutines.delay(350); found = repo.search(q) }
    val canArch = arch.isNotEmpty() && ui.mf == "all" && q.isEmpty() && ui.net == "all" && ui.fold == "all" && ui.mkls == "all"
    val density = LocalDensity.current
    var pull by remember { mutableFloatStateOf(0f) }
    val archMax = with(density) { 76.dp.toPx() }
    val conn = remember(canArch) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!canArch || ui.archShown) return Offset.Zero
                if (available.y > 0 && source == NestedScrollSource.UserInput) { pull = (pull + available.y * .7f).coerceAtMost(archMax); return Offset(0f, available.y) }
                if (available.y < 0 && pull > 0) { val c = maxOf(available.y, -pull); pull += c; return Offset(0f, c) }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull > 0) { ui.archShown = pull > with(density) { 45.dp.toPx() }; pull = 0f }
                return Velocity.Zero
            }
        }
    }
    LaunchedEffect(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset) {
        if (ui.archShown && (list.firstVisibleItemIndex > 3)) ui.archShown = false
    }
    val scrolled by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 28 } }
    Column(Modifier.fillMaxSize()) {
        TopBar("Сообщения", scrolled) { HeadActions() }
        LazyColumn(Modifier.fillMaxSize().nestedScroll(conn), state = list) {
            item {
                T("все переписки из соцсетей", body(14f), Sg.c.muted, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp))
                if (!online) OfflineBanner()
                SearchField(ui.q, { ui.q = it })
                VGap(10.dp)
                Seg(listOf(Triple("all", "Все", 0), Triple("unread", "Непрочитанные", un), Triple("wait", "Ждут ответа", wt)), ui.mf, { ui.mf = it })
                Row(Modifier.padding(top = 10.dp, bottom = 8.dp)) { FilterChips() }
            }
            when {
                load == Load.Loading -> item { SkelRows(7) }
                load == Load.Error && st.conversations.isEmpty() -> item { ErrorState({ repo.retry() }) }
                st.conversations.isEmpty() -> item { EmptyState("inbox", "Сообщений пока нет", "Переписки из Telegram, WhatsApp, VK, MAX и других сетей появятся здесь, когда клиенты напишут") }
                else -> {
                    if (canArch) item(key = "arch") {
                        val target = if (ui.archShown) archMax else pull
                        val h by animateDpAsState(with(density) { target.toDp() }, if (pull > 0) spring(stiffness = 10_000f) else tween(450, easing = EaseM3), label = "arch")
                        Box(Modifier.fillMaxWidth().height(h).graphicsLayer { alpha = (target / archMax).coerceIn(0f, 1f) }) {
                            Row(Modifier.fillMaxWidth().height(76.dp).clickable { nav.go("archive") }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).background(Sg.c.fg.copy(alpha = .28f), CircleShape), contentAlignment = Alignment.Center) { Ic("archive", Sg.c.surface) }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    T("Архив", body(15.5f, FontWeight.SemiBold))
                                    T(arch.joinToString(", ") { repo.nameOfConv(it) }, body(14f), Sg.c.muted, maxLines = 1)
                                }
                            }
                        }
                    }
                    if (shown.isEmpty() && found.isEmpty()) item { EmptyState("search", "Ничего не нашли", "Попробуйте другое имя или снимите фильтр") }
                    items(shown, key = { it.id }) { c -> ChatRow(c, archived = false, modifier = Modifier.animateItem()) }
                    if (found.isNotEmpty()) {
                        item { T("В сообщениях", body(13f, FontWeight.SemiBold), Sg.c.muted, modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp)) }
                        items(found, key = { "s" + it.messageId }) { f ->
                            Row(Modifier.fillMaxWidth().clickable { nav.chat(f.conversationId) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Avatar(f.name, 40.dp, f.network)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row { T(f.name, body(15f, FontWeight.SemiBold), maxLines = 1, modifier = Modifier.weight(1f)); T(Fmt.listTime(ru.sguardo.app.data.parseIso(f.at)), body(12.5f), Sg.c.muted) }
                                    T((if (f.dir == "out") "Вы: " else "") + f.snippet, body(14f), Sg.c.muted, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
            item { VGap(110.dp) }
        }
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp).background(Sg.c.cHigh, RoundedCornerShape(28.dp)).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Ic("search", Sg.c.muted, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) T("Поиск по имени", body(15f), Sg.c.faint)
            BasicTextField(value, onChange, singleLine = true, textStyle = body(15f).copy(color = Sg.c.fg), cursorBrush = SolidColor(Sg.c.primary), modifier = Modifier.fillMaxWidth())
        }
        if (value.isNotEmpty()) Box(Modifier.size(32.dp).clip(CircleShape).clickable { onChange("") }, contentAlignment = Alignment.Center) { Ic("x", Sg.c.muted, size = 16.dp) }
    }
}

/** «Все аккаунты», «Все папки» и «Все классы» под переключателем. */
@Composable
private fun FilterChips() {
    val repo = LocalRepo.current; val ui = LocalUi.current; val nav = LocalNav.current
    val st by repo.state.collectAsState()
    var netOpen by remember { mutableStateOf(false) }
    var foldOpen by remember { mutableStateOf(false) }
    var klsOpen by remember { mutableStateOf(false) }
    val classes = remember(st) { allClasses(repo, st) }
    val groups = remember(st) { allGroups(repo, st) }
    val nets = (Nets.keys + st.conversations.map { it.network }).distinct()
    val fo = st.folders.firstOrNull { it.id == ui.fold }
    HScroll {
        Box {
            Chip(if (ui.net == "all") "Все аккаунты" else Nets.name(ui.net), on = ui.net != "all", trailingIcon = "chevD") { netOpen = true }
            DropdownMenu(netOpen, { netOpen = false }, containerColor = Sg.c.c, shape = RoundedCornerShape(6.dp)) {
                MenuItem("Все аккаунты", if (ui.net == "all") "check" else "grid") { ui.net = "all"; netOpen = false }
                nets.forEach { n -> MenuItem(Nets.name(n), if (ui.net == n) "check" else null) { ui.net = n; netOpen = false } }
            }
        }
        Box {
            Chip(fo?.name ?: "Все папки", on = fo != null, trailingIcon = "chevD", leading = if (fo != null) { { Dot(folderDot(fo)) } } else null) { foldOpen = true }
            DropdownMenu(foldOpen, { foldOpen = false }, containerColor = Sg.c.c, shape = RoundedCornerShape(6.dp)) {
                MenuItem("Все папки", if (ui.fold == "all") "check" else "grid") { ui.fold = "all"; foldOpen = false }
                st.folders.forEach { f -> MenuItem(f.name, null, dot = folderDot(f), checked = ui.fold == f.id) { ui.fold = f.id; foldOpen = false } }
                HorizontalDivider(color = Sg.c.hair, modifier = Modifier.padding(vertical = 6.dp))
                MenuItem("Настроить папки и классы", "sliders") { foldOpen = false; nav.go("folders") }
            }
        }
        Box {
            Chip(if (ui.mkls == "all") "Все классы" else klsTitle(ui.mkls), on = ui.mkls != "all", trailingIcon = "chevD") { klsOpen = true }
            DropdownMenu(klsOpen, { klsOpen = false }, containerColor = Sg.c.c, shape = RoundedCornerShape(6.dp)) {
                MenuItem("Все классы", if (ui.mkls == "all") "check" else "grid") { ui.mkls = "all"; klsOpen = false }
                classes.forEach { k -> MenuItem(k, if (ui.mkls == k) "check" else null) { ui.mkls = k; klsOpen = false } }
                if (groups.isNotEmpty()) {
                    HorizontalDivider(color = Sg.c.hair, modifier = Modifier.padding(vertical = 6.dp))
                    groups.forEach { g -> val key = GROUP_P + g; MenuItem(g, if (ui.mkls == key) "check" else "users") { ui.mkls = key; klsOpen = false } }
                }
                if (classes.isEmpty() && groups.isEmpty()) MenuItem("Классов пока нет", null) { klsOpen = false; nav.go("folders") }
                HorizontalDivider(color = Sg.c.hair, modifier = Modifier.padding(vertical = 6.dp))
                MenuItem("Настроить папки и классы", "sliders") { klsOpen = false; nav.go("folders") }
            }
        }
    }
}

@Composable
fun MenuItem(text: String, icon: String?, dot: Color? = null, checked: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val col = if (danger) Sg.c.danger else Sg.c.fg
    val lead: (@Composable () -> Unit)? = if (dot != null) { { Dot(dot, modifier = Modifier.padding(horizontal = 4.dp)) } } else if (icon != null) { { Ic(icon, col, size = 16.dp) } } else null
    val trail: (@Composable () -> Unit)? = if (dot != null && checked) { { Ic("check", col, size = 16.dp) } } else null
    DropdownMenuItem(
        text = { T(text, body(15f, if (checked) FontWeight.Bold else FontWeight.Normal), col) },
        leadingIcon = lead,
        trailingIcon = trail,
        onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 210.dp),
    )
}

/** Строка чата: вправо — закрепить; влево — без звука, в архив, удалить; долгое нажатие — предпросмотр. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatRow(c: Conversation, archived: Boolean, modifier: Modifier = Modifier) {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val ctx = LocalContext.current
    val contact = repo.contact(c.contactId)
    val name = repo.nameOf(contact)
    val fromBook = repo.bookName(contact) != null
    val density = LocalDensity.current
    val openW = with(density) { 240.dp.toPx() }
    val armW = with(density) { 70.dp.toPx() }
    val x = remember(c.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var base by remember { mutableFloatStateOf(0f) }
    Box(modifier.fillMaxWidth().height(72.dp)) {
        if (x.value != 0f) Row(Modifier.fillMaxSize()) {
            if (x.value > 0) Box(Modifier.fillMaxHeight().weight(1f).background(Color(0xFF1F7A63)), contentAlignment = Alignment.CenterStart) {
                val armed = x.value > armW
                Column(Modifier.width(96.dp).graphicsLayer { scaleX = if (armed) 1.15f else 1f; scaleY = if (armed) 1.15f else 1f }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Ic("pin", Color.White); T(if (c.pinnedAt != null) "Открепить" else "Закрепить", body(11.5f, FontWeight.SemiBold), Color.White)
                }
            } else {
                Spacer(Modifier.weight(1f))
                SwipeBtn(if (c.muted) "bell" else "bellOff", if (c.muted) "Со звуком" else "Без звука", Color(0xFFD98E2B)) {
                    scope.launch { x.animateTo(0f, tween(300, easing = EaseM3)) }; base = 0f
                    repo.setConvFlag(c.id, "muted", !c.muted, if (c.muted) "Уведомления из чата включены" else "Уведомления из чата выключены")
                }
                SwipeBtn("archive", if (archived) "Вернуть" else "В архив", Color(0xFF7E8B88)) {
                    scope.launch { x.animateTo(0f, tween(300)) }; base = 0f
                    val was = c.archived
                    repo.setConvFlag(c.id, "archived", !was, if (!was) "Чат в архиве. Потяните список вниз, чтобы его открыть" else "Чат вернулся из архива",
                        undo = { repo.setConvFlag(c.id, "archived", was) })
                }
                SwipeBtn("trash", "Удалить", Color(0xFFB3261E)) {
                    scope.launch { x.animateTo(0f, tween(300)) }; base = 0f
                    askDeleteChat(repo, ov, c.id)
                }
            }
        }
        Row(
            Modifier.fillMaxSize().offset { IntOffset(x.value.roundToInt(), 0) }.background(Sg.c.surface)
                .pointerInput(c.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val v = x.value
                            val to = if (base == 0f && v > armW) 0f else if (v < -90f * density.density) -openW else 0f
                            scope.launch { x.animateTo(to, tween(320, easing = EaseM3)) }
                            if (base == 0f && v > armW) repo.setConvFlag(c.id, "pinned", c.pinnedAt == null, if (c.pinnedAt == null) "Чат закреплён наверху" else "Чат откреплён")
                            base = to
                        },
                        onDragCancel = { scope.launch { x.animateTo(base, tween(300)) } },
                    ) { ch, dx ->
                        ch.consume()
                        var nx = x.value + dx
                        if (nx > 80 * density.density) nx = x.value + dx * .25f
                        if (nx < -openW) nx = x.value + dx * .25f
                        if (base == -openW && nx > 0) nx = 0f
                        scope.launch { x.snapTo(nx) }
                    }
                }
                .combinedClickable(onClick = {
                    if (base != 0f) { scope.launch { x.animateTo(0f, tween(300, easing = EaseM3)) }; base = 0f } else nav.chat(c.id)
                }, onLongClick = { ov.layer = { ChatPreview(c.id) { ov.layer = null } } })
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(if (contact?.phone != null && !fromBook && name == contact.phone) contact.phone else name, 48.dp, c.network)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        T(name, body(15.5f, FontWeight.SemiBold), maxLines = 1, modifier = Modifier.weight(1f, false))
                        if (fromBook) { Spacer(Modifier.width(5.dp)); Ic("book", Sg.c.teal.copy(alpha = .8f), size = 13.dp) }
                        if (c.muted) { Spacer(Modifier.width(5.dp)); Ic("bellOff", Sg.c.faint, size = 13.dp) }
                    }
                    Spacer(Modifier.width(8.dp))
                    T(Fmt.listTime(lastTime(c)), body(12.5f), Sg.c.muted)
                }
                Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    val typing by repo.typing.collectAsState()
                    val tp = typing[c.id]
                    if (tp != null) T(if (tp.first == "recording") "записывает…" else "печатает…", body(14f), Sg.c.teal, modifier = Modifier.weight(1f), maxLines = 1)
                    else T(lastText(c), body(14f), Sg.c.muted, modifier = Modifier.weight(1f), maxLines = 1)
                    if (c.unread > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.heightIn(min = 21.dp).widthIn(min = 21.dp).background(if (c.muted) Sg.c.faint else Sg.c.primary, RoundedCornerShape(11.dp)).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                            T(c.unread.toString(), body(11.5f, FontWeight.Bold), if (c.muted) Sg.c.surface else Sg.c.onPrimary)
                        }
                    } else if (c.pinnedAt != null) { Spacer(Modifier.width(8.dp)); Ic("pin", Sg.c.faint, size = 13.dp) }
                }
            }
        }
    }
}

@Composable
private fun SwipeBtn(icon: String, text: String, bg: Color, onClick: () -> Unit) {
    Column(Modifier.width(80.dp).fillMaxHeight().background(bg).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Ic(icon, Color.White); Spacer(Modifier.height(4.dp)); T(text, body(11.5f, FontWeight.SemiBold), Color.White, maxLines = 1)
    }
}

/** Удаление чата: диалог Material с галочкой «Также удалить для собеседника в Telegram». */
fun askDeleteChat(repo: Repo, ov: Overlays, convId: String, after: () -> Unit = {}) {
    val c = repo.conv(convId) ?: return
    val name = repo.nameOfConv(c)
    ov.confirm(DialogSpec("Удалить чат?", "Чат с «$name» пропадёт из списка вместе с карточкой клиента.",
        check = if (c.network == "telegram") "Также удалить для собеседника в Telegram" else null,
        buttons = listOf(DialogBtn("Удалить", danger = true) { both ->
            val snapshot = repo.state.value
            repo.act(obj("type" to "removeContact", "contactId" to c.contactId),
                ok = if (both && !repo.demo.value) "Чат удалён. У собеседника он останется: сервер пока не удаляет у обоих" else if (both) "Чат удалён у вас и у собеседника" else "Чат удалён",
                undo = if (repo.demo.value && !both) ({ repo.state.value = snapshot }) else null)
            after()
        })))
}

/** Долгое нажатие на чат: предпросмотр последних сообщений и быстрые действия. */
@Composable
fun ChatPreview(convId: String, close: () -> Unit) {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val ctx = LocalContext.current
    val c = repo.conv(convId) ?: return
    val contact = repo.contact(c.contactId)
    val inBook = repo.bookName(contact) != null
    androidx.activity.compose.BackHandler(onBack = close)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .32f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close)) {
        AnimatedVisibility(true, enter = fadeIn(tween(260)) + scaleIn(spring(dampingRatio = .7f, stiffness = 380f), initialScale = .95f)) {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
                Column(Modifier.fillMaxWidth().height(318.dp).shadowCompat(20.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Sg.c.surface)
                    .clickable { close(); nav.chat(convId) }) {
                    Row(Modifier.fillMaxWidth().background(Sg.c.c).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(repo.nameOf(contact), 40.dp, c.network, ring = Sg.c.c)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            T(repo.nameOf(contact), body(15f, FontWeight.SemiBold), maxLines = 1)
                            T(Nets.name(c.network) + (contact?.handle?.let { " · $it" } ?: ""), body(12f), Sg.c.muted, maxLines = 1)
                        }
                    }
                    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)) {
                        val ms = repo.messages(convId).takeLast(6)
                        ms.forEachIndexed { i, m -> Bubble(m, c, ms.getOrNull(i - 1), compact = true) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Column(Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max).widthIn(min = 236.dp).shadowCompat(6.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp)).background(Sg.c.c).padding(vertical = 8.dp)) {
                    PvItem(if (inBook) "В контактах телефона" else "Добавить в контакты", if (inBook) "check" else "userPlus") {
                        close(); if (!inBook) addToPhoneBook(ctx, repo.nameOf(contact), contact?.phone)
                    }
                    PvItem(if (c.pinnedAt != null) "Открепить" else "Закрепить", "pin") { close(); repo.setConvFlag(c.id, "pinned", c.pinnedAt == null, if (c.pinnedAt == null) "Чат закреплён наверху" else "Чат откреплён") }
                    PvItem(if (c.muted) "Включить уведомления" else "Выключить уведомления", if (c.muted) "bell" else "bellOff") {
                        close(); repo.setConvFlag(c.id, "muted", !c.muted, if (c.muted) "Уведомления из чата включены" else "Уведомления из чата выключены")
                    }
                    PvItem("Пометить как новое", "chat") { close(); repo.markUnread(c.id) }
                    PvItem("Удалить", "trash", danger = true) { close(); askDeleteChat(repo, ov, c.id) }
                }
            }
        }
    }
}

@Composable
fun PvItem(text: String, icon: String, danger: Boolean = false, onClick: () -> Unit) {
    val col = if (danger) Sg.c.danger else Sg.c.fg
    Row(Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Ic(icon, col, size = 16.dp); Spacer(Modifier.width(14.dp)); T(text, body(15f), col, maxLines = 1)
    }
}

/** «Добавить в контакты»: системная вставка контакта с именем и номером. */
fun addToPhoneBook(ctx: android.content.Context, name: String, phone: String?, note: String? = null) {
    val i = Intent(ContactsContract.Intents.Insert.ACTION).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        putExtra(ContactsContract.Intents.Insert.NAME, name)
        if (!phone.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.PHONE, phone)
        if (!note.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.NOTES, note)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try { ctx.startActivity(i) } catch (_: Exception) {}
}

@Composable
fun ArchiveScreen() {
    val repo = LocalRepo.current; val nav = LocalNav.current
    val st by repo.state.collectAsState()
    val a = sortConvs(st.conversations.filter { it.archived && !it.trashed })
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        SubTopBar("Архив", { nav.back() })
        if (a.isEmpty()) EmptyState("archive", "Архив пуст", "Смахните чат влево и нажмите «В архив»")
        else LazyColumn {
            item { Hint("Сюда попадают чаты, которые вы убрали из списка. Новые сообщения в них приходят без звука.", Modifier.padding(top = 0.dp)) }
            items(a, key = { it.id }) { ChatRow(it, archived = true, modifier = Modifier.animateItem()) }
        }
    }
}
