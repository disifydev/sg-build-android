package ru.sguardo.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.sguardo.app.data.Clock
import ru.sguardo.app.data.Fmt
import ru.sguardo.app.data.LinkSuggestion
import ru.sguardo.app.data.Message
import ru.sguardo.app.data.isoOf
import ru.sguardo.app.data.obj
import ru.sguardo.app.ui.Avatar
import ru.sguardo.app.ui.Btn
import ru.sguardo.app.ui.BtnKind
import ru.sguardo.app.ui.CLASS_P
import ru.sguardo.app.ui.Dot
import ru.sguardo.app.ui.EmptyState
import ru.sguardo.app.ui.Gi
import ru.sguardo.app.ui.Grp
import ru.sguardo.app.ui.Hint
import ru.sguardo.app.ui.IconBtn
import ru.sguardo.app.ui.Ic
import ru.sguardo.app.ui.LocalNav
import ru.sguardo.app.ui.LocalOv
import ru.sguardo.app.ui.LocalPerms
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.Nets
import ru.sguardo.app.ui.SgSwitch
import ru.sguardo.app.ui.SubTopBar
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.VGap
import ru.sguardo.app.ui.allClasses
import ru.sguardo.app.ui.classKey
import ru.sguardo.app.ui.classes
import ru.sguardo.app.ui.folderDot
import ru.sguardo.app.ui.lastTime
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import ru.sguardo.app.ui.theme.disp

fun copyText(ctx: Context, s: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("text", s))
}

/** Профиль клиента как в Telegram: действия, поля, заметка, контакты телефона, медиа/файлы/ссылки/голосовые. */
@Composable
fun ProfileScreen(id: String) {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val ctx = LocalContext.current
    val st by repo.state.collectAsState()
    val sugg by repo.suggestions.collectAsState()
    val conv = st.conversations.firstOrNull { it.id == id }
    if (conv == null) { Column(Modifier.fillMaxSize().background(Sg.c.surface)) { SubTopBar("", { nav.back() }); EmptyState("user", "Клиент не найден", "Возможно, чат удалили") }; return }
    val c = repo.contact(conv.contactId)
    val name = repo.nameOf(c)
    val inBook = repo.bookName(c) != null
    LaunchedEffect(id) { repo.loadSuggestions(); repo.loadHistory(id) }
    var ptab by remember { mutableStateOf("media") }
    var demoMerged by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        SubTopBar("", { nav.back() }, scrolled = scroll.value > 28) {
            IconBtn("edit", { nav.go("editc/$id") })
            ChatMenu(conv, inChat = false)
        }
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val s = remember { Animatable(.4f) }
                LaunchedEffect(Unit) { s.animateTo(1f, spring(dampingRatio = .55f, stiffness = 300f)) }
                Avatar(if (!inBook && c?.phone != null && c.name == c.phone) c.phone else name, 104.dp, conv.network, modifier = Modifier.graphicsLayer { scaleX = s.value; scaleY = s.value })
                VGap(10.dp)
                T(name, disp(23), align = TextAlign.Center)
                VGap(4.dp)
                T("${Nets.name(conv.network)} · последнее сообщение ${Fmt.listTime(lastTime(conv)).lowercase()}", body(13f), Sg.c.muted, align = TextAlign.Center)
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PAct("chat", "Чат", Modifier.weight(1f)) { nav.chat(id) }
                PAct(if (conv.muted) "bell" else "bellOff", if (conv.muted) "Со звуком" else "Без звука", Modifier.weight(1f)) {
                    repo.setConvFlag(id, "muted", !conv.muted, if (conv.muted) "Уведомления из чата включены" else "Уведомления из чата выключены")
                }
                PAct("card", "Оплата", Modifier.weight(1f)) { ov.openSheet { PaySheet(conv.contactId, "paid") } }
                PAct("bell", "Напомнить", Modifier.weight(1f)) {
                    val at = Clock.at(Clock.today().plusDays(1), 10, 0)
                    repo.act(obj("type" to "remind", "contactId" to conv.contactId, "at" to Clock.isoLocal(at), "text" to "Написать клиенту"), "Напомним завтра в 10:00")
                }
            }
            Grp {
                if (c?.phone != null) IRow("мобильный", c.phone, link = true) { copyText(ctx, c.phone); repo.toast("Номер скопирован") }
                if (!c?.handle.isNullOrBlank()) IRow("имя пользователя", c!!.handle!!, link = true) { copyText(ctx, c.handle!!); repo.toast("Ссылка на профиль скопирована") }
                // папка
                var fOpen by remember { mutableStateOf(false) }
                val f = repo.folder(c?.folderId)
                Box {
                    IRowCustom("папка", onClick = { fOpen = true }) { Row(verticalAlignment = Alignment.CenterVertically) { Dot(folderDot(f)); Spacer(Modifier.width(7.dp)); T(f?.name ?: "Без папки", body(15.5f)) } }
                    DropdownMenu(fOpen, { fOpen = false }, containerColor = Sg.c.c, shape = RoundedCornerShape(6.dp)) {
                        st.folders.forEach { ff -> MenuItem(ff.name, null, dot = folderDot(ff), checked = ff.id == c?.folderId) {
                            fOpen = false; repo.act(obj("type" to "move", "contactId" to conv.contactId, "folderId" to ff.id), "Перенесли в «${ff.name}»")
                        } }
                    }
                }
                // класс
                var kOpen by remember { mutableStateOf(false) }
                val cls = c?.classes()?.firstOrNull()
                Box {
                    IRowCustom("класс", onClick = { kOpen = true }) { T(cls ?: "Не указан", body(15.5f), if (cls == null) Sg.c.faint else Sg.c.fg) }
                    DropdownMenu(kOpen, { kOpen = false }, containerColor = Sg.c.c, shape = RoundedCornerShape(6.dp)) {
                        allClasses(repo, st).forEach { k -> MenuItem(k, if (k == cls) "check" else null) {
                            kOpen = false
                            cls?.let { old -> repo.act(obj("type" to "setCollection", "contactId" to conv.contactId, "kind" to "class", "name" to classKey(old), "on" to false)) }
                            if (k != cls) repo.act(obj("type" to "setCollection", "contactId" to conv.contactId, "kind" to "class", "name" to classKey(k), "on" to true), "Класс: $k")
                        } }
                    }
                }
                // ответственный
                var aOpen by remember { mutableStateOf(false) }
                Box {
                    IRow("ответственный", c?.assignee?.ifBlank { null } ?: "Не назначен") { aOpen = true }
                    DropdownMenu(aOpen, { aOpen = false }, containerColor = Sg.c.c, shape = RoundedCornerShape(6.dp)) {
                        st.team.forEach { t -> MenuItem(t.name, if (t.name == c?.assignee) "check" else null) {
                            aOpen = false; repo.act(obj("type" to "setAssignee", "contactId" to conv.contactId, "name" to t.name, "userId" to t.id), "Ответственный: ${t.name}")
                        } }
                    }
                }
                // заметка
                var note by remember(c?.id) { mutableStateOf(c?.note ?: "") }
                LaunchedEffect(note) { delay(900); if (note != (c?.note ?: "")) repo.act(obj("type" to "setNote", "contactId" to conv.contactId, "note" to note)) }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    T("заметка", body(12.5f), Sg.c.muted)
                    Box(Modifier.padding(top = 2.dp)) {
                        if (note.isEmpty()) T("Что важно помнить об этом клиенте", body(15f), Sg.c.faint)
                        BasicTextField(note, { note = it }, textStyle = body(15f).copy(color = Sg.c.fg), cursorBrush = SolidColor(Sg.c.primary), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            VGap(12.dp)
            Grp {
                if (inBook) Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Ic("book", Sg.c.fg, size = 16.dp); Spacer(Modifier.width(8.dp)); T("В контактах телефона", body(15.5f)) }
                    T("Имя совпадает с телефонной книгой", body(12.5f), Sg.c.muted)
                } else Column(Modifier.fillMaxWidth().clickable { addToPhoneBook(ctx, name, c?.phone, c?.handle?.let { "${Nets.name(conv.network)}: $it" }) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Ic("userPlus", Sg.c.primary, size = 16.dp); Spacer(Modifier.width(8.dp)); T("Добавить в контакты", body(15.5f), Sg.c.primary) }
                    T("Имя и ${if (c?.phone != null) "номер" else "ссылка на соцсеть"} появятся в телефонной книге", body(12.5f), Sg.c.muted)
                }
            }
            // «Похоже, это тот же человек»
            val s = sugg.firstOrNull { it.aId == conv.contactId || it.bId == conv.contactId }
            val showDemo = repo.demo.value && !demoMerged && conv.network != "whatsapp"
            AnimatedVisibility(s != null || showDemo, exit = fadeOut() + shrinkVertically()) {
                val other = s?.let { if (it.aId == conv.contactId) it.bName else it.aName } ?: name
                MergeCard("Похоже, это тот же человек: $name в ${Nets.name(conv.network)} и ${if (s != null) other else "WhatsApp"}", s?.reason?.ifBlank { null } ?: "Совпадают имя и фамилия",
                    onMerge = { if (s != null) repo.merge(s, conv.contactId) else { demoMerged = true; repo.toast("Объединили: одна карточка, две соцсети") } },
                    onSkip = { if (s != null) repo.dismissSuggestion(s) else { demoMerged = true; repo.toast("Поняли, это разные люди") } })
            }
            VGap(18.dp)
            MTabs(listOf("media" to "Медиа", "files" to "Файлы", "links" to "Ссылки", "voice" to "Голосовые"), ptab) { ptab = it }
            MediaTab(ptab, repo.messages(id), repo.demo.value)
            VGap(16.dp)
            Grp { Gi("Удалить чат", titleColor = Sg.c.danger, center = true, onClick = { askDeleteChat(repo, ov, id) { nav.back(); nav.back() } }) }
            VGap(40.dp)
        }
    }
}

@Composable
fun MergeCard(text: String, small: String, onMerge: () -> Unit, onSkip: () -> Unit) {
    val dash = Sg.c.fg.copy(alpha = .25f)
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp).drawBehind {
        drawRoundRect(dash, cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
    }.padding(horizontal = 14.dp, vertical = 12.dp)) {
        T(text, body(13.5f).copy(lineHeight = 19.sp_()))
        T(small, body(13.5f), Sg.c.muted, modifier = Modifier.padding(top = 2.dp))
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Объединить", small = true, onClick = onMerge)
            Btn("Это разные люди", small = true, kind = BtnKind.Outline, onClick = onSkip)
        }
    }
}

private fun Int.sp_() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

@Composable
private fun PAct(icon: String, text: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(62.dp).clip(RoundedCornerShape(16.dp)).background(Sg.c.cLow).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Ic(icon, Sg.c.primary); Spacer(Modifier.height(4.dp)); T(text, body(11.5f, FontWeight.SemiBold), Sg.c.primary, maxLines = 1)
    }
}

@Composable
fun IRow(label: String, value: String, link: Boolean = false, onClick: () -> Unit) =
    IRowCustom(label, onClick) { T(value, body(15.5f), if (link) Sg.c.primary else Sg.c.fg) }

@Composable
fun IRowCustom(label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)) {
        T(label, body(12.5f), Sg.c.muted); Spacer(Modifier.height(2.dp)); content()
    }
}

/** Вкладки с подчёркиванием (.mtabs). */
@Composable
fun MTabs(tabs: List<Pair<String, String>>, cur: String, counts: Map<String, Int> = emptyMap(), extra: (@Composable (String) -> Unit)? = null, onSelect: (String) -> Unit) {
    val line = Sg.c.ov
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).drawBehind { drawRect(line, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx())) }) {
        tabs.forEach { (k, t) ->
            val on = k == cur
            val pc = Sg.c.primary
            Row(Modifier.height(48.dp).clickable { onSelect(k) }.drawBehind {
                if (on) drawRoundRect(pc, Offset(12.dp.toPx(), size.height - 3.dp.toPx()), Size(size.width - 24.dp.toPx(), 3.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                extra?.invoke(k)
                T(t, body(14f, FontWeight.SemiBold), if (on) Sg.c.primary else Sg.c.muted, maxLines = 1)
                counts[k]?.let { n -> Spacer(Modifier.width(6.dp)); T(n.toString(), body(12f), (if (on) Sg.c.primary else Sg.c.muted).copy(alpha = .7f)) }
            }
        }
    }
}

@Composable
private fun MediaTab(tab: String, msgs: List<Message>, demo: Boolean) {
    val repo = LocalRepo.current
    when (tab) {
        "media" -> {
            val own = msgs.filter { it.kind in listOf("photo", "circle", "video") }
            val cells = own.map { it as Any } + if (demo) (0 until 6).map { it as Any } else emptyList()
            if (cells.isEmpty()) { T("Фото и видео из переписки появятся здесь", body(14f), Sg.c.muted, modifier = Modifier.padding(20.dp)); return }
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp).clip(RoundedCornerShape(18.dp)), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                cells.take(12).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        row.forEach { cell ->
                            Box(Modifier.weight(1f).aspectRatio(1f)) {
                                when (cell) {
                                    is Int -> Box(Modifier.fillMaxSize().background(GAL[cell]))
                                    is Message -> if (cell.kind == "circle") Box(Modifier.fillMaxSize().background(Sg.c.fg.copy(alpha = .08f))) {
                                        DemoCam(false, Modifier.fillMaxSize().padding(12.dp).clip(CircleShape))
                                        T(Fmt.dur((cell.att?.duration ?: 0.0).toInt()), body(11f), Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).background(Color.Black.copy(alpha = .45f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
                                    } else {
                                        val url = repo.api.media(cell.att?.url)
                                        if (url == null || cell.att?.name?.startsWith("demo:") == true) DemoPhoto(cell.att?.name, Modifier.fillMaxSize())
                                        else coil.compose.AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                    }
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        "files" -> {
            val files = msgs.filter { it.kind == "file" }.map { (it.att?.name ?: "Файл") to "${ru.sguardo.app.data.Fixture.sizes[it.att?.name] ?: "файл"} · ${Fmt.dayMonth(it.time)}" } +
                if (demo) listOf("Договор_оферта.pdf" to "340 КБ · 20 нояб", "Расписание_декабрь.xlsx" to "88 КБ · 14 нояб") else emptyList()
            FList(files.map { Triple(null, it.first, it.second) }, "file", "Файлов пока нет")
        }
        "links" -> {
            val re = Regex("https?://[^\\s]+")
            val links = msgs.flatMap { m -> re.findAll(m.text).map { it.value }.toList() }.distinct().map { u -> Triple(u.removePrefix("https://").removePrefix("http://").take(1).uppercase(), u.substringAfter("://").substringBefore('/'), u.substringAfter("://")) } +
                if (demo) listOf(Triple("S", "Ссылка на оплату", "sguardo.ru/pay/7f3a"), Triple("N", "Шаблоны Notion", "notion.so/templates"), Triple("Y", "Разбор задачи 14", "youtube.com/watch")) else emptyList()
            FList(links, null, "Ссылок пока нет", link = true)
        }
        else -> {
            val vs = msgs.filter { it.kind == "voice" }.map { "Голосовое · ${Fmt.dur((it.att?.duration ?: 0.0).toInt())}" to (Fmt.listTime(it.time).let { t -> if (t.contains(':')) "сегодня, $t" else t }) } +
                if (demo) listOf("Голосовое · 0:22" to "24 нояб", "Голосовое · 0:08" to "20 нояб") else emptyList()
            FList(vs.map { Triple(null, it.first, it.second) }, "play", "Голосовых пока нет", round = true)
        }
    }
}

@Composable
private fun FList(items: List<Triple<String?, String, String>>, icon: String?, empty: String, link: Boolean = false, round: Boolean = false) {
    if (items.isEmpty()) { T(empty, body(14f), Sg.c.muted, modifier = Modifier.padding(20.dp)); return }
    Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp)) {
        items.forEach { (letter, t, s) ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(if (letter != null) Sg.c.tBlue else Sg.c.teal, if (round) CircleShape else RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    if (letter != null) T(letter, body(15f, FontWeight.Bold), Color(0xFF14211F)) else Ic(icon ?: "file", Sg.c.onTeal, size = if (round) 16.dp else 22.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column { T(t, body(14.5f, FontWeight.SemiBold), maxLines = 1); T(s, body(12.5f), if (link) Sg.c.primary else Sg.c.muted, maxLines = 1) }
            }
        }
    }
}

/** Правка контакта: имя и фамилия видны только вам; можно сохранить в телефонную книгу. */
@Composable
fun EditContactScreen(id: String) {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current; val perms = LocalPerms.current
    val ctx = LocalContext.current
    val conv = repo.conv(id)
    val c = repo.contact(conv?.contactId)
    val parts = (c?.name ?: "").split(" ")
    var first by remember { mutableStateOf(parts.firstOrNull() ?: "") }
    var last by remember { mutableStateOf(parts.drop(1).joinToString(" ")) }
    val inBook = repo.bookName(c) != null
    var book by remember { mutableStateOf(inBook) }
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        SubTopBar("Изменить контакт", { nav.back() }, backIcon = "x") {
            IconBtn("check", {
                val nn = "$first $last".trim()
                if (c != null && nn.isNotEmpty() && nn != c.name) repo.act(obj("type" to "updateContact", "contactId" to c.id, "name" to nn))
                if (book && !inBook) addToPhoneBook(ctx, nn.ifBlank { c?.name ?: "" }, c?.phone)
                repo.toast(if (book && !inBook) "Сохранили и открыли телефонную книгу" else "Контакт сохранён")
                nav.back()
            })
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(c?.name ?: "", 96.dp)
                Btn("Выбрать фото", kind = BtnKind.Text, small = true) { repo.toast("Фото клиента приходит из мессенджера") }
            }
            MField("Имя", first) { first = it }
            MField("Фамилия", last) { last = it }
            Hint("Это имя видите только вы. Клиент его не узнает.", Modifier.padding(top = 0.dp))
            Grp {
                Gi("Сохранить в контактах телефона", c?.phone ?: "Имя и ссылка на ${Nets.name(conv?.network ?: "")}", icon = "book") {
                    SgSwitch(book) { book = it }
                }
            }
            Btn("Удалить контакт", kind = BtnKind.Text, danger = true, modifier = Modifier.padding(16.dp)) { askDeleteChat(repo, ov, id) { nav.back(); nav.back(); nav.back() } }
        }
    }
}

@Composable
fun MField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { T(label, body(12f)) }, singleLine = true, textStyle = body(16f),
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), shape = RoundedCornerShape(4.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Sg.c.primary, unfocusedBorderColor = Sg.c.outline, focusedLabelColor = Sg.c.primary, unfocusedLabelColor = Sg.c.muted,
            focusedTextColor = Sg.c.fg, unfocusedTextColor = Sg.c.fg, cursorColor = Sg.c.primary))
}
