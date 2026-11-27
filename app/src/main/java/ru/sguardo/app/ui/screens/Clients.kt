package ru.sguardo.app.ui.screens

import androidx.compose.foundation.layout.size
import ru.sguardo.app.ui.SgSwitch
import ru.sguardo.app.ui.Gi
import ru.sguardo.app.ui.Sect
import ru.sguardo.app.ui.GROUP_P
import ru.sguardo.app.ui.klsTitle
import ru.sguardo.app.ui.inKls
import ru.sguardo.app.ui.allGroups
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.sguardo.app.data.Clock
import ru.sguardo.app.data.Contact
import ru.sguardo.app.data.Fmt
import ru.sguardo.app.data.Load
import ru.sguardo.app.data.obj
import ru.sguardo.app.data.parseIso
import ru.sguardo.app.ui.Avatar
import ru.sguardo.app.ui.Btn
import ru.sguardo.app.ui.BtnKind
import ru.sguardo.app.ui.CLASS_P
import ru.sguardo.app.ui.Chip
import ru.sguardo.app.ui.Dot
import ru.sguardo.app.ui.EmptyState
import ru.sguardo.app.ui.ErrorState
import ru.sguardo.app.ui.FChip
import ru.sguardo.app.ui.Fab
import ru.sguardo.app.ui.HScroll
import ru.sguardo.app.ui.IconBtn
import ru.sguardo.app.ui.Ic
import ru.sguardo.app.ui.Lbl
import ru.sguardo.app.ui.LocalNav
import ru.sguardo.app.ui.LocalOv
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.LocalUi
import ru.sguardo.app.ui.MeButton
import ru.sguardo.app.ui.NetDot
import ru.sguardo.app.ui.Nets
import ru.sguardo.app.ui.OfflineBanner
import ru.sguardo.app.ui.SField
import ru.sguardo.app.ui.SheetTitle
import ru.sguardo.app.ui.SkelRows
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.Tag
import ru.sguardo.app.ui.Tile
import ru.sguardo.app.ui.TopBar
import ru.sguardo.app.ui.VGap
import ru.sguardo.app.ui.allClasses
import ru.sguardo.app.ui.classKey
import ru.sguardo.app.ui.classes
import ru.sguardo.app.ui.contactTime
import ru.sguardo.app.ui.folderDot
import ru.sguardo.app.ui.plainTags
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import ru.sguardo.app.ui.theme.disp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClientsScreen() {
    val repo = LocalRepo.current; val ui = LocalUi.current; val nav = LocalNav.current; val ov = LocalOv.current
    val st by repo.state.collectAsState()
    val load by repo.load.collectAsState()
    val online by repo.online.collectAsState()
    val user by repo.user.collectAsState()
    val extra by repo.prefs.extraClasses.collectAsState()
    val scroll = rememberScrollState()
    var fabOpen by remember { mutableStateOf(true) }
    LaunchedEffect(scroll) { var last = 0; snapshotFlow { scroll.value }.collect { v -> if (v > last + 6) fabOpen = false else if (v < last - 6) fabOpen = true; last = v } }
    LaunchedEffect(ui.scrollTopSignal) { if (ui.scrollTopSignal > 0) scroll.animateScrollTo(0) }
    val all = ui.cs == "all"
    val classes = remember(st, extra) { allClasses(repo, st) }
    val groups = remember(st, extra) { allGroups(repo, st) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar("Клиенты", scroll.value > 28) {
                IconBtn("sliders", { nav.go("folders") })
                MeButton(user?.name?.take(1) ?: "В") { nav.go("account") }
            }
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                T("папки по статусу", body(14f), Sg.c.muted, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp))
                if (!online) OfflineBanner()
                val counts = st.folders.associate { f -> f.id to st.contacts.count { it.folderId == f.id } } + ("all" to st.contacts.size)
                MTabs(listOf("all" to "Все") + st.folders.map { it.id to it.name }, ui.cs, counts = counts, extra = { k ->
                    if (k != "all") { Dot(folderDot(repo.folder(k))); Spacer(Modifier.width(6.dp)) }
                }) { ui.cs = it }
                if (ru.sguardo.app.ui.tutorOn()) HScroll(Modifier.padding(top = 10.dp, bottom = 2.dp)) {
                    Chip("Все классы", on = ui.kls == "all", check = true) { ui.kls = "all" }
                    classes.forEach { k -> Chip(k, on = ui.kls == k, check = true) { ui.kls = k } }
                    groups.forEach { g -> Chip(g, on = ui.kls == GROUP_P + g, check = true) { ui.kls = GROUP_P + g } }
                    Chip("Настроить", leading = { Ic("sliders", Sg.c.fg, size = 16.dp) }) { nav.go("lists") }
                }
                VGap(10.dp)
                when {
                    load == Load.Loading -> SkelRows(6, 40.dp)
                    load == Load.Error && st.contacts.isEmpty() -> ErrorState({ repo.retry() })
                    st.contacts.isEmpty() -> EmptyState("users", "Клиентов пока нет", "Они появятся, когда вам напишут, или добавьте клиента сами", button = "Добавить клиента") { ov.openSheet { NewClientSheet() } }
                    else -> {
                        val list = st.contacts.filter { (all || it.folderId == ui.cs) && it.inKls(ui.kls) }
                            .sortedByDescending { contactTime(st, it) }
                        if (list.isEmpty()) EmptyState("users", "Здесь пока никого", if (ui.kls == "all") "В этой папке пока нет клиентов" else "Нет учеников в «${klsTitle(ui.kls)}»")
                        else Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            list.forEach { c -> ClientCard(c, all) }
                        }
                    }
                }
                VGap(120.dp)
            }
        }
        Fab("userPlus", "Клиент", fabOpen, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)) { ov.openSheet { NewClientSheet() } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientCard(c: Contact, all: Boolean) {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val st by repo.state.collectAsState()
    val f = repo.folder(c.folderId)
    val conv = repo.convOf(c.id)
    val rem = st.reminders.filter { it.contactId == c.id && !it.done }.minByOrNull { parseIso(it.at) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Sg.c.cLow).clickable {
        if (conv != null) nav.go("profile/${conv.id}") else ov.openSheet { ClientSheet(c.id) }
    }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Avatar(repo.nameOf(c), 42.dp, ring = Sg.c.cLow)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(repo.nameOf(c), body(15.5f, FontWeight.SemiBold), maxLines = 1, modifier = Modifier.weight(1f, false))
                Spacer(Modifier.width(5.dp)); NetDot(conv?.network ?: c.source ?: "telegram", 15.dp)
            }
            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (all) { Dot(folderDot(f)); Spacer(Modifier.width(5.dp)); T("${f?.name ?: ""} · ", body(12.5f), Sg.c.muted) }
                T(Fmt.listTime(contactTime(st, c)), body(12.5f), Sg.c.muted)
            }
            val tags = c.classes() + c.plainTags()
            if (tags.isNotEmpty() || rem != null) FlowRow(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                tags.forEach { Tag(it) }
                if (rem != null) Tag("напомнить ${Fmt.dayMonth(parseIso(rem.at))}", Sg.c.wine, if (Sg.c.dark) Color(0xFF2A0C0D) else Color.White, icon = "bell")
            }
        }
    }
}

/** Новый клиент: имя, телефон, соцсеть, папка, класс → действие addContact. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.NewClientSheet() {
    val repo = LocalRepo.current; val ui = LocalUi.current; val ov = LocalOv.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val st by repo.state.collectAsState()
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var net by remember { mutableStateOf("telegram") }
    var book by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf(if (ui.cs == "all") st.folders.firstOrNull()?.id else ui.cs) }
    val classes = allClasses(repo, st)
    var kl by remember { mutableStateOf<String?>(null) }
    val add = add@{
        val n = "$first $last".trim()
        if (n.isEmpty()) { repo.toast("Укажите имя клиента"); return@add }
        ov.closeSheet()
        repo.act(obj("type" to "addContact", "name" to n, "phone" to phone.trim().ifBlank { null }, "network" to net, "folderId" to folder, "note" to note.trim().ifBlank { null },
            "tags" to listOfNotNull(kl?.let { CLASS_P + classKey(it) })), "$n: папка «${repo.folder(folder)?.name ?: ""}»")
        if (book) addToPhoneBook(ctx, n, phone.trim().ifBlank { null })
        ui.cs = folder ?: "all"; ui.kls = "all"
    }
    // как «Новый контакт» в Telegram: крестик, название, галочка; аватар рядом с именем и фамилией; карточки с пояснениями
    ru.sguardo.app.ui.FormTop("Новый клиент", { ov.closeSheet() }, add)
    VGap(10.dp)
    ru.sguardo.app.ui.FormCard(bg = Sg.c.cHigh) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                val nm = "$first $last".trim()
                if (nm.isEmpty()) Box(Modifier.size(64.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Sg.c.primary.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Ic("user", Sg.c.primary, size = 28.dp) }
                else Avatar(nm, 64.dp, ring = Sg.c.cHigh)
            }
            Column(Modifier.weight(1f)) {
                ru.sguardo.app.ui.FormField(first, { first = it }, "Имя", cap = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
                ru.sguardo.app.ui.FormField(last, { last = it }, "Фамилия", divider = true, cap = androidx.compose.ui.text.input.KeyboardCapitalization.Words)
            }
        }
    }
    VGap(16.dp)
    ru.sguardo.app.ui.FormCard(bg = Sg.c.cHigh) { ru.sguardo.app.ui.FormField(phone, { phone = it }, "Телефон", keyboard = KeyboardType.Phone) }
    ru.sguardo.app.ui.FormHint("Номер нужен для WhatsApp и MAX. Для Telegram и ВК можно без него.")
    Sect("Где общаетесь", top = 0.dp)
    FlowRow(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("telegram", "whatsapp", "instagram", "vk", "max", "avito").forEach { n -> FChip(Nets.name(n), net == n, leading = { NetDot(n, 16.dp) }) { net = n } }
    }
    Sect("Папка")
    FlowRow(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        st.folders.forEach { f -> FChip(f.name, folder == f.id, dot = folderDot(f)) { folder = f.id } }
    }
    if (classes.isNotEmpty() && ru.sguardo.app.ui.tutorOn()) {
        Sect("Класс")
        FlowRow(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            classes.forEach { k -> FChip(k, kl == k) { kl = if (kl == k) null else k } }
        }
    }
    VGap(18.dp)
    ru.sguardo.app.ui.FormCard(bg = Sg.c.cHigh) {
        Gi("Сохранить в контактах телефона", onClick = { book = !book }) { SgSwitch(book) { book = it } }
    }
    VGap(16.dp)
    ru.sguardo.app.ui.FormCard(bg = Sg.c.cHigh) { ru.sguardo.app.ui.FormField(note, { note = it }, "Заметка (видна только вам)", singleLine = false) }
    VGap(8.dp)
}

/** Карточка клиента без чата (лист): оплата, обещание, напоминание, занятие, папка, ответственный, заметка. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.ClientSheet(contactId: String) {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val st by repo.state.collectAsState()
    val c = st.contacts.firstOrNull { it.id == contactId } ?: return
    val conv = repo.convOf(c.id)
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(repo.nameOf(c), 78.dp, conv?.network ?: c.source, ring = Sg.c.cLow)
        VGap(6.dp)
        T(repo.nameOf(c), disp(20), align = TextAlign.Center)
        T(Nets.name(conv?.network ?: c.source ?: "") + (c.handle?.let { " · $it" } ?: c.phone?.let { " · $it" } ?: ""), body(13f), Sg.c.muted)
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        if (conv != null) Btn("Написать", icon = "chat", small = true) { ov.closeSheet(); nav.chat(conv.id) }
        Btn("Изменить", icon = "edit", small = true, kind = BtnKind.Outline) { if (conv != null) { ov.closeSheet(); nav.go("editc/${conv.id}") } else repo.toast("Имя меняется после первого сообщения") }
    }
    Row(Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile("plus", "Оплата", primary = true, modifier = Modifier.weight(1f)) { ov.openSheet { PaySheet(c.id, "paid") } }
        Tile("hour", "Обещал", modifier = Modifier.weight(1f)) { ov.openSheet { PaySheet(c.id, "promised") } }
        Tile("bell", "Напомнить", modifier = Modifier.weight(1f)) {
            repo.act(obj("type" to "remind", "contactId" to c.id, "at" to Clock.isoLocal(Clock.at(Clock.today().plusDays(1), 10, 0)), "text" to "Написать клиенту"), "Напомним завтра в 10:00")
        }
        Tile("cal", "Занятие", modifier = Modifier.weight(1f)) {
            if (conv == null) repo.toast("Занятие можно назначить, когда есть переписка") else ov.openSheet { LessonEditSheet(Clock.today(), null, conv.id) }
        }
    }
    Lbl("Папка")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        st.folders.forEach { f -> FChip(f.name, c.folderId == f.id, dot = folderDot(f)) {
            if (c.folderId != f.id) repo.act(obj("type" to "move", "contactId" to c.id, "folderId" to f.id), "Перенесли в «${f.name}»")
        } }
    }
    Lbl("Ответственный")
    Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp)).background(Sg.c.cHigh).clickable {
        val next = st.team.getOrNull((st.team.indexOfFirst { it.name == c.assignee } + 1) % maxOf(1, st.team.size))
        if (next != null) repo.act(obj("type" to "setAssignee", "contactId" to c.id, "name" to next.name, "userId" to next.id), "Ответственный: ${next.name}")
    }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        T(c.assignee?.ifBlank { null } ?: "Не назначен", body(15f), modifier = Modifier.weight(1f)); Ic("chevD", Sg.c.fg, size = 16.dp)
    }
    Lbl("Заметка")
    var note by remember { mutableStateOf(c.note ?: "") }
    LaunchedEffect(note) { delay(900); if (note != (c.note ?: "")) repo.act(obj("type" to "setNote", "contactId" to c.id, "note" to note)) }
    Box(Modifier.fillMaxWidth().heightIn(min = 76.dp).clip(RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp)).background(Sg.c.cHigh).padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (note.isEmpty()) T("Что важно помнить об этом клиенте", body(14.5f), Sg.c.faint)
        BasicTextField(note, { note = it }, textStyle = body(14.5f).copy(color = Sg.c.fg), cursorBrush = SolidColor(Sg.c.primary), modifier = Modifier.fillMaxWidth())
    }
}
