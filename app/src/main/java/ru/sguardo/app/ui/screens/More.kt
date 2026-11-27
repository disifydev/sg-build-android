package ru.sguardo.app.ui.screens

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.sguardo.app.data.Clock
import ru.sguardo.app.data.Fmt
import ru.sguardo.app.data.Notif
import ru.sguardo.app.data.Subject
import ru.sguardo.app.data.TeamMember
import ru.sguardo.app.data.obj
import ru.sguardo.app.data.parseIso
import ru.sguardo.app.ui.Avatar
import ru.sguardo.app.ui.Btn
import ru.sguardo.app.ui.BtnKind
import ru.sguardo.app.ui.CLASS_P
import ru.sguardo.app.ui.Chev
import ru.sguardo.app.ui.DialogBtn
import ru.sguardo.app.ui.DialogSpec
import ru.sguardo.app.ui.EmptyState
import ru.sguardo.app.ui.EyeSmall
import ru.sguardo.app.ui.Gi
import ru.sguardo.app.ui.Grp
import ru.sguardo.app.ui.Hint
import ru.sguardo.app.ui.IconBtn
import ru.sguardo.app.ui.Ic
import ru.sguardo.app.ui.Lbl
import ru.sguardo.app.ui.LocalNav
import ru.sguardo.app.ui.LocalOv
import ru.sguardo.app.ui.LocalPerms
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.LocalUi
import ru.sguardo.app.ui.PALF
import ru.sguardo.app.ui.ProdRow
import ru.sguardo.app.ui.Radio
import ru.sguardo.app.ui.SField
import ru.sguardo.app.ui.Sect
import ru.sguardo.app.ui.SgSwitch
import ru.sguardo.app.ui.SheetTitle
import ru.sguardo.app.ui.SubTopBar
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.TopBar
import ru.sguardo.app.ui.VGap
import ru.sguardo.app.ui.allClasses
import ru.sguardo.app.ui.classKey
import ru.sguardo.app.ui.classes
import ru.sguardo.app.ui.colFor
import ru.sguardo.app.ui.folderDot
import ru.sguardo.app.ui.folderHex
import ru.sguardo.app.ui.hexColor
import ru.sguardo.app.ui.initials
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import ru.sguardo.app.ui.theme.disp
import kotlin.math.roundToInt

@Composable
fun MoreScreen() {
    val repo = LocalRepo.current; val nav = LocalNav.current; val perms = LocalPerms.current
    val st by repo.state.collectAsState()
    val user by repo.user.collectAsState()
    val acc by repo.account.collectAsState()
    val lessons by repo.lessons.collectAsState()
    val theme by repo.prefs.theme.collectAsState()
    val n1 by repo.prefs.notifyMessages.collectAsState()
    val n2 by repo.prefs.notifyReminders.collectAsState()
    val book by repo.prefs.namesFromBook.collectAsState()
    val subjects by repo.prefs.subjects.collectAsState()
    val extra by repo.prefs.extraClasses.collectAsState()
    LaunchedEffect(Unit) { repo.loadLessons(Fmt.weekStart(Clock.today())) }
    val notifOk = perms.hasNotifications()
    val bookOk = perms.hasContacts()
    TabPage("Ещё", actions = { HeadActions(me = false) }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(20.dp)).background(Sg.c.cLow).clickable { nav.go("account") }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Avatar((user?.name ?: "В").take(1), 56.dp, color = Color(0xFFC7E2D3))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                T(user?.name ?: "", body(17f, FontWeight.SemiBold))
                T("${acc?.workspaceName?.ifBlank { null } ?: "Кабинет"} · ${if (user?.isOwner != false) "владелец" else "сотрудник"}", body(13f), Sg.c.muted)
            }
            Ic("chevR", Sg.c.fg, size = 16.dp)
        }
        val today = lessons.count { Clock.local(parseIso(it.startsAt)).toLocalDate() == Clock.today() && it.status == "planned" }
        val kl = remember(st, extra) { allClasses(repo, st).size }
        Sect("Работа")
        Grp {
            Gi("Расписание", "Сегодня $today ${Fmt.plural(today, "занятие", "занятия", "занятий")}", "cal", onClick = { nav.go("schedule") }) { Chev() }
            Gi("Папки", "Названия, цвета и порядок", "sliders", onClick = { nav.go("folders") }) { Chev() }
            Gi("Классы и предметы", "$kl ${Fmt.plural(kl, "класс", "класса", "классов")}, ${subjects.size} ${Fmt.plural(subjects.size, "предмет", "предмета", "предметов")}", "book", onClick = { nav.go("lists") }) { Chev() }
            val s = acc?.sessions ?: 1
            Gi("Вход и устройства", "$s ${Fmt.plural(s, "активный сеанс", "активных сеанса", "активных сеансов")}", "shield", onClick = { nav.go("security") }) { Chev() }
        }
        Sect("Уведомления")
        Grp {
            val toggle1 = { on: Boolean ->
                if (on && !notifOk) perms.notifications { ok -> repo.prefs.setNotifyMessages(ok); if (ok) repo.registerPushIfPossible(); repo.toast(if (ok) "Уведомления включены" else "Доступ не выдан") }
                else { repo.prefs.setNotifyMessages(on); if (on) repo.registerPushIfPossible() else repo.unregisterPush() }
            }
            val ps by repo.pushStatus.collectAsState()
            Gi("Новые сообщения", if (!notifOk && !repo.demo.value) "Выключены в настройках телефона" else if (!repo.demo.value && ps.startsWith("Недоступны")) "Уведомления $ps".replaceFirst("Уведомления Недоступны", "Уведомления недоступны") else "Имя клиента и текст сообщения", "chat", onClick = { toggle1(!(n1 && notifOk)) }) { SgSwitch(n1 && (notifOk || repo.demo.value), toggle1) }
            Gi("Напоминания", "Об оплатах и занятиях", "bell", onClick = { repo.prefs.setNotifyReminders(!n2) }) { SgSwitch(n2) { repo.prefs.setNotifyReminders(it) } }
        }
        Sect("Телефонная книга")
        Grp {
            val toggleB = { on: Boolean ->
                if (on && !bookOk) perms.contacts { ok -> repo.prefs.setNamesFromBook(ok); repo.book.refresh(); repo.toast(if (ok) "Имена берём из контактов" else "Доступ не выдан") }
                else { repo.prefs.setNamesFromBook(on); repo.book.refresh() }
            }
            Gi("Имена из контактов", if (!bookOk && !repo.demo.value) "Нет доступа к контактам. Нажмите, чтобы разрешить" else "Если номер клиента есть в контактах телефона, покажем имя оттуда", "book",
                onClick = { toggleB(!(book && bookOk)) }) { SgSwitch(book && (bookOk || repo.demo.value), toggleB) }
        }
        Sect("Оформление")
        Grp {
            listOf(Triple("system", "Как в системе", "Светлая днём, тёмная ночью"), Triple("light", "Светлая", null), Triple("dark", "Тёмная", null)).forEach { (k, t, s) ->
                Gi(t, s, leading = { Radio(theme == k) }, onClick = { repo.prefs.setTheme(k) })
            }
        }
        T("Sguardo для Android · версия ${ru.sguardo.app.BuildConfig.VERSION_NAME}", body(12f), Sg.c.faint, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 22.dp))
    }
}

/** Кабинет: имя, почта, сотрудники, вход, выход. */
@Composable
fun AccountScreen() {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val user by repo.user.collectAsState()
    val acc by repo.account.collectAsState()
    val st by repo.state.collectAsState()
    LaunchedEffect(Unit) { repo.loadAccount() }
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        SubTopBar("Кабинет", { nav.back() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar((user?.name ?: "В").take(1), 96.dp, color = Color(0xFFC7E2D3))
                VGap(10.dp)
                T(user?.name ?: "", disp(23))
                VGap(4.dp)
                T("${if (user?.isOwner != false) "владелец" else "сотрудник"} · ${acc?.workspaceName?.ifBlank { null } ?: "кабинет"}", body(13f), Sg.c.muted)
            }
            Grp { Gi("Сотрудники", st.team.joinToString(", ") { it.name.split(" ").first() }.ifBlank { "Только вы" }, "users", onClick = { nav.go("staff") }) { Chev() } }
            VGap(12.dp)
            Grp {
                Gi("Имя и фото", "Как вас видят сотрудники", "user", onClick = { ov.openSheet { RenameSheet(user?.name ?: "") } }) { Chev() }
                Gi("Почта для входа", user?.email ?: acc?.email ?: "", "mail", onClick = { repo.toast("Почта меняется в кабинете на сайте") }) { Chev() }
                val s = acc?.sessions ?: 1
                Gi("Вход и устройства", "$s ${Fmt.plural(s, "активный сеанс", "активных сеанса", "активных сеансов")}", "shield", onClick = { nav.go("security") }) { Chev() }
            }
            VGap(16.dp)
            Grp {
                Gi("Выйти из кабинета", titleColor = Sg.c.danger, center = true, onClick = {
                    ov.confirm(DialogSpec("Выйти из кабинета?", "На этом телефоне перестанут приходить уведомления.", buttons = listOf(DialogBtn("Выйти", danger = true) { repo.logout() })))
                })
            }
            VGap(40.dp)
        }
    }
}

@Composable
private fun ColumnScope.RenameSheet(cur: String) {
    val repo = LocalRepo.current; val ov = LocalOv.current
    var n by remember { mutableStateOf(cur) }
    SheetTitle("Имя и фото")
    Lbl("Имя"); SField(n, { n = it }, "Как вас зовут", "user")
    T("Фото пока ставится в кабинете на сайте", body(12.5f), Sg.c.muted, modifier = Modifier.padding(top = 8.dp))
    VGap(18.dp)
    Btn("Сохранить", icon = "check", wide = true) { if (n.isBlank()) repo.toast("Введите имя") else { ov.closeSheet(); repo.renameMe(n.trim()) } }
}

fun roleOf(t: TeamMember, local: Map<String, String>): String = when {
    t.role == "owner" -> "Владелец"
    local[t.id] != null -> local[t.id]!!
    t.permissions == null -> "Менеджер"
    t.permissions.canExport && t.permissions.canPay && t.permissions.seeMoney -> "Администратор"
    !t.permissions.canPay && !t.permissions.seeMoney -> "Только просмотр"
    else -> "Менеджер"
}

private val ROLES = listOf("Администратор" to "всё, кроме удаления кабинета", "Менеджер" to "чаты, клиенты и оплаты", "Только просмотр" to "только читать чаты")

/** Сотрудники с ролями. */
@Composable
fun StaffScreen() {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val st by repo.state.collectAsState()
    val user by repo.user.collectAsState()
    val roles by repo.localStaffRoles.collectAsState()
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        SubTopBar("Сотрудники", { nav.back() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Hint("Сотрудники видят чаты кабинета. Роль определяет, что им можно менять.", Modifier.padding(top = 8.dp))
            Grp {
                st.team.forEach { t ->
                    val me = t.id == user?.id || (repo.demo.value && t.role == "owner")
                    Gi(t.name + if (me) " (вы)" else "", if (me || t.role == "owner") "Полный доступ" else if (roleOf(t, roles) == "Только просмотр") "Только читает чаты" else "Видит все чаты",
                        leading = { Avatar(t.name, 36.dp, color = colFor(t.name)) },
                        onClick = { if (me || t.role == "owner") repo.toast("Это владелец кабинета") else if (user?.isOwner == true) ov.openSheet { StaffSheet(t) } else repo.toast("Роли меняет владелец") }) {
                        T(roleOf(t, roles), body(12f, FontWeight.SemiBold), Sg.c.onSc, modifier = Modifier.background(Sg.c.sc, RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 3.dp))
                    }
                }
            }
            if (user?.isOwner != false) Box(Modifier.fillMaxWidth().padding(top = 14.dp), contentAlignment = Alignment.Center) {
                Btn("Пригласить сотрудника", icon = "userPlus", kind = BtnKind.Tonal) { ov.openSheet { InviteSheet() } }
            }
        }
    }
}

@Composable
private fun ColumnScope.InviteSheet() {
    val repo = LocalRepo.current; val ov = LocalOv.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Менеджер") }
    var code by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    SheetTitle("Пригласить сотрудника")
    if (code == null) {
        Lbl("Имя"); SField(name, { name = it }, "Имя и фамилия", "user")
        Lbl("Почта"); SField(email, { email = it }, "Куда отправить приглашение", "mail", keyboard = KeyboardType.Email)
        Lbl("Пароль для входа"); SField(pass, { pass = it }, "Не короче 8 символов", "lock", password = true)
        Lbl("Роль")
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { ROLES.forEach { (r, s) -> ProdRow(r, s, role == r, rightSmall = true) { role = r } } }
        VGap(18.dp)
        Btn("Отправить приглашение", icon = "check", wide = true, enabled = !busy) {
            if (name.isBlank() || !email.contains("@")) { repo.toast("Укажите имя и почту"); return@Btn }
            if (pass.length < 8 && !repo.demo.value) { repo.toast("Пароль не короче 8 символов"); return@Btn }
            busy = true
            scope.launch {
                val err = repo.invite(name.trim(), email.trim(), pass)
                busy = false
                if (err != null) repo.toast(err)
                else if (repo.demo.value) { ov.closeSheet(); repo.toast("Приглашение для ${name.trim()} отправлено") }
                else code = ""
            }
        }
    } else {
        T("Мы отправили код на ${email.trim()}. Попросите сотрудника продиктовать его.", body(14f), Sg.c.muted)
        Lbl("Код из письма"); SField(code!!, { code = it.filter { c -> c.isDigit() }.take(8) }, "Например, 123456", "shield", keyboard = KeyboardType.Number)
        VGap(18.dp)
        Btn("Добавить сотрудника", icon = "check", wide = true, enabled = !busy) {
            busy = true
            scope.launch {
                val err = repo.confirmInvite(email.trim(), code!!)
                busy = false
                if (err != null) repo.toast(err) else {
                    ov.closeSheet()
                    repo.state.value.team.firstOrNull { it.email.equals(email.trim(), true) }?.let { if (role != "Менеджер") repo.setRole(it.id, role) }
                    repo.toast("${name.trim()} теперь в кабинете")
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.StaffSheet(t: TeamMember) {
    val repo = LocalRepo.current; val ov = LocalOv.current
    val roles by repo.localStaffRoles.collectAsState()
    var role by remember { mutableStateOf(roleOf(t, roles)) }
    SheetTitle(t.name)
    Lbl("Роль")
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { ROLES.forEach { (r, s) -> ProdRow(r, s, role == r, rightSmall = true) { role = r } } }
    VGap(18.dp)
    Btn("Сохранить", icon = "check", wide = true) { ov.closeSheet(); repo.setRole(t.id, role) }
    VGap(6.dp)
    Btn("Убрать из кабинета", wide = true, kind = BtnKind.Text, danger = true) {
        ov.closeSheet()
        ov.confirm(DialogSpec("Убрать ${t.name} из кабинета?", "Сотрудник потеряет доступ к чатам.", buttons = listOf(DialogBtn("Убрать", danger = true) { repo.removeMember(t.id) })))
    }
}

/** Классы (для фильтра учеников) и предметы (для расписания). */
@Composable
fun ListsScreen() {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val st by repo.state.collectAsState()
    val extra by repo.prefs.extraClasses.collectAsState()
    val subjects by repo.prefs.subjects.collectAsState()
    val classes = remember(st, extra) { allClasses(repo, st) }
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        SubTopBar("Классы и предметы", { nav.back() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Hint("Классы нужны для фильтра учеников, предметы для расписания. Нажмите, чтобы переименовать или удалить.", Modifier.padding(top = 8.dp))
            Sect("Классы", top = 4.dp)
            Grp {
                if (classes.isEmpty()) Gi("Классов пока нет", "Добавьте первый, например «9 класс»")
                classes.forEach { k ->
                    val n = st.contacts.count { k in it.classes() }
                    Gi(k, "$n ${Fmt.plural(n, "ученик", "ученика", "учеников")}", "users", onClick = { ov.openSheet { ListItemSheet(true, k) } }) { Ic("edit", Sg.c.faint, size = 16.dp) }
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) { Btn("Добавить класс", icon = "plus", kind = BtnKind.Tonal) { ov.openSheet { ListItemSheet(true, null) } } }
            Sect("Предметы")
            Grp {
                subjects.forEach { s ->
                    Gi(s.name, leading = { Box(Modifier.padding(horizontal = 5.dp).size(14.dp).background(hexColor(s.color), CircleShape)) }, onClick = { ov.openSheet { ListItemSheet(false, s.name) } }) { Ic("edit", Sg.c.faint, size = 16.dp) }
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 40.dp), contentAlignment = Alignment.Center) { Btn("Добавить предмет", icon = "plus", kind = BtnKind.Tonal) { ov.openSheet { ListItemSheet(false, null) } } }
        }
    }
}

@Composable
private fun ColumnScope.ListItemSheet(isClass: Boolean, cur: String?) {
    val repo = LocalRepo.current; val ov = LocalOv.current
    val subjects = repo.prefs.subjects.value
    var n by remember { mutableStateOf(cur ?: "") }
    var col by remember { mutableStateOf(subjects.firstOrNull { it.name == cur }?.color ?: PALF[0]) }
    SheetTitle(if (cur != null) (if (isClass) "Класс" else "Предмет") else (if (isClass) "Новый класс" else "Новый предмет"))
    Lbl("Название"); SField(n, { n = it }, if (isClass) "Например, 8 класс" else "Например, Английский", "edit")
    if (!isClass) { Lbl("Цвет в расписании"); Swatches(col) { col = it } }
    VGap(18.dp)
    Btn(if (cur != null) "Сохранить" else "Добавить", icon = "check", wide = true) {
        val name = n.trim()
        if (name.isEmpty()) { repo.toast("Введите название"); return@Btn }
        ov.closeSheet()
        if (isClass) {
            val extra = repo.prefs.extraClasses.value
            if (cur == null) { repo.prefs.setExtraClasses((extra + name).distinct()); repo.toast("Добавлено") }
            else {
                repo.prefs.setExtraClasses(extra.map { if (it == cur) name else it })
                if (repo.state.value.contacts.any { cur in it.classes() }) repo.act(obj("type" to "renameCollection", "kind" to "class", "from" to classKey(cur), "to" to classKey(name)), "Сохранено")
                else repo.toast("Сохранено")
            }
        } else {
            val list = repo.prefs.subjects.value
            repo.prefs.setSubjects(if (cur == null) list + Subject(name, col) else list.map { if (it.name == cur) Subject(name, col) else it })
            if (cur != null && cur != name) repo.prefs.lessonExtra.value.forEach { (id, e) -> if (e.subject == cur) repo.prefs.setLessonExtra(id, e.copy(subject = name)) }
            repo.toast(if (cur == null) "Добавлено" else "Сохранено")
        }
    }
    if (cur != null) {
        VGap(6.dp)
        Btn("Удалить", wide = true, kind = BtnKind.Text, danger = true) {
            ov.closeSheet()
            ov.confirm(DialogSpec("Удалить «$cur»?", if (isClass) "У учеников этого класса поле станет пустым." else "Занятия по этому предмету останутся в расписании.",
                buttons = listOf(DialogBtn("Удалить", danger = true) {
                    if (isClass) {
                        repo.prefs.setExtraClasses(repo.prefs.extraClasses.value - cur)
                        if (repo.state.value.contacts.any { cur in it.classes() }) repo.act(obj("type" to "deleteCollection", "kind" to "class", "name" to classKey(cur)), "Удалено") else repo.toast("Удалено")
                    } else { repo.prefs.setSubjects(repo.prefs.subjects.value.filter { it.name != cur }); repo.toast("Удалено") }
                })))
        }
    }
}

@Composable
fun Swatches(cur: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PALF.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { c ->
                    val on = c.equals(cur, true)
                    val sc by animateFloatAsState(if (on) 1.12f else 1f, label = "sw")
                    Box(Modifier.weight(1f).aspectRatio(1f).graphicsLayer { scaleX = sc; scaleY = sc }.clip(CircleShape)
                        .border(3.dp, if (on) Sg.c.fg else Color.Transparent, CircleShape).padding(3.dp).clip(CircleShape).background(hexColor(c)).clickable { onPick(c) })
                }
            }
        }
    }
}

/** Папки: имя, цвет, порядок (перетаскивание за точки слева). */
@Composable
fun FoldersScreen() {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val st by repo.state.collectAsState()
    val density = LocalDensity.current
    val rowH = with(density) { 64.dp.toPx() }
    var dragIdx by remember { mutableIntStateOf(-1) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        SubTopBar("Папки", { nav.back() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Hint("Папка показывает, на каком этапе клиент. Потяните за точки слева, чтобы поменять порядок. Нажмите на папку, чтобы переименовать или сменить цвет.", Modifier.padding(top = 8.dp))
            val folders = st.folders
            val to = if (dragIdx >= 0) (dragIdx + (dragDy / rowH).roundToInt()).coerceIn(0, folders.lastIndex) else -1
            Grp {
                folders.forEachIndexed { i, f ->
                    val n = st.contacts.count { it.folderId == f.id }
                    val shift = when {
                        dragIdx < 0 || i == dragIdx -> 0f
                        dragIdx < to && i in (dragIdx + 1)..to -> -rowH
                        dragIdx > to && i in to until dragIdx -> rowH
                        else -> 0f
                    }
                    val dragging = i == dragIdx
                    Row(Modifier.fillMaxWidth().height(64.dp).offset { IntOffset(0, (if (dragging) dragDy else shift).roundToInt()) }
                        .graphicsLayer { if (dragging) { scaleX = 1.02f; scaleY = 1.02f; shadowElevation = 20f } }
                        .background(if (dragging) Sg.c.cHigh else Color.Transparent, RoundedCornerShape(16.dp)).padding(start = 6.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(26.dp, 48.dp).pointerInput(f.id) {
                            detectDragGestures(onDragStart = { dragIdx = i; dragDy = 0f }, onDragEnd = {
                                val target = (i + (dragDy / rowH).roundToInt()).coerceIn(0, folders.lastIndex)
                                dragIdx = -1; dragDy = 0f
                                if (target != i) {
                                    if (repo.demo.value) {
                                        val l = folders.toMutableList(); val m = l.removeAt(i); l.add(target, m)
                                        repo.state.value = repo.state.value.copy(folders = l); repo.toast("Порядок папок сохранён")
                                    } else repo.reorderFolder(f.id, i, target)
                                }
                            }, onDragCancel = { dragIdx = -1; dragDy = 0f }) { ch, d -> ch.consume(); dragDy += d.y }
                        }, contentAlignment = Alignment.Center) { Ic("grip", Sg.c.faint, size = 16.dp) }
                        Box(Modifier.padding(horizontal = 8.dp).size(14.dp).background(folderDot(f), CircleShape))
                        Column(Modifier.weight(1f).clickable { ov.openSheet { FolderSheet(f.id) } }) {
                            T(f.name, body(15f)); T("$n ${Fmt.plural(n, "клиент", "клиента", "клиентов")}", body(12.5f), Sg.c.muted)
                        }
                        IconBtn("edit", { ov.openSheet { FolderSheet(f.id) } }, size = 36.dp, iconSize = 16.dp, tint = Sg.c.muted)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 40.dp), contentAlignment = Alignment.Center) { Btn("Создать папку", icon = "plus", kind = BtnKind.Tonal) { ov.openSheet { FolderSheet(null) } } }
        }
    }
}

@Composable
private fun ColumnScope.FolderSheet(id: String?) {
    val repo = LocalRepo.current; val ov = LocalOv.current; val ui = LocalUi.current
    val st = repo.state.value
    val f = st.folders.firstOrNull { it.id == id }
    var n by remember { mutableStateOf(f?.name ?: "") }
    var col by remember { mutableStateOf(f?.let { folderHex(it) } ?: PALF[5]) }
    SheetTitle(if (f != null) "Папка" else "Новая папка")
    Lbl("Название"); SField(n, { n = it }, "Например, Пробное занятие", "edit")
    Lbl("Цвет"); Swatches(col) { col = it }
    VGap(18.dp)
    Btn(if (f != null) "Сохранить" else "Создать папку", icon = "check", wide = true) {
        val name = n.trim(); if (name.isEmpty()) { repo.toast("Введите название папки"); return@Btn }
        ov.closeSheet()
        if (f == null) repo.act(obj("type" to "addFolder", "name" to name, "color" to col), "Папка «$name» создана")
        else repo.act(obj("type" to "updateFolder", "id" to f.id, "name" to name, "color" to if (col.equals(folderHex(f), true)) null else col), "Папка сохранена")
    }
    if (f != null && st.folders.size > 1) {
        VGap(6.dp)
        Btn("Удалить папку", wide = true, kind = BtnKind.Text, danger = true) {
            ov.closeSheet()
            val to = st.folders.first { it.id != f.id }
            ov.confirm(DialogSpec("Удалить папку «${f.name}»?", "Клиенты из неё перейдут в «${to.name}».", buttons = listOf(DialogBtn("Удалить папку", danger = true) {
                repo.act(obj("type" to "removeFolder", "id" to f.id, "moveTo" to to.id), "Папка удалена"); ui.cs = "all"; ui.fold = "all"
            })))
        }
    }
}

/** Уведомления за последние сутки: сообщения, просроченные оплаты, напоминания, занятия. */
@Composable
fun NotifsScreen() {
    val repo = LocalRepo.current; val nav = LocalNav.current
    val st by repo.state.collectAsState()
    val read by repo.prefs.readNotifs.collectAsState()
    val list = remember(st) { repo.notifications() }
    val fresh = remember { list.filter { it.id !in read && !(repo.demo.value && it.id in setOf("n4", "n5")) }.map { it.id }.toSet() }
    DisposableEffect(Unit) { repo.prefs.markNotifsRead(list.map { it.id }); onDispose { } }
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        SubTopBar("Уведомления", { nav.back() })
        if (list.isEmpty()) { EmptyState("bell", "Новых уведомлений нет", "Здесь появятся сообщения клиентов и напоминания"); return@Column }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            VGap(8.dp)
            Grp {
                list.forEach { n -> NRow(n, n.id in fresh) {
                    when {
                        n.target.startsWith("chat:") -> nav.chat(n.target.removePrefix("chat:"))
                        n.target == "sched" -> nav.go("schedule")
                        n.target == "pays" -> nav.tab("pays")
                        else -> nav.tab("clients")
                    }
                } }
            }
            Hint("Здесь собраны новые сообщения, напоминания об оплатах и занятиях за последние сутки.")
        }
    }
}

@Composable
private fun NRow(n: Notif, fresh: Boolean, onClick: () -> Unit) {
    val (icon, tint) = when (n.kind) { "msg" -> "chat" to Sg.c.tBlue; "pay" -> "wallet" to Sg.c.tPink; "rem" -> "bell" to Sg.c.tYellow; else -> "cal" to Sg.c.tGreen }
    Box(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(40.dp).background(tint, CircleShape), contentAlignment = Alignment.Center) { Ic(icon, Sg.c.statInk, size = 16.dp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { T(n.title, body(15f, FontWeight.SemiBold), maxLines = 1); T(n.text, body(14f), Sg.c.muted, maxLines = 2, modifier = Modifier.padding(top = 2.dp)) }
            T(Fmt.listTime(n.at), body(12f), Sg.c.faint, modifier = Modifier.padding(start = 8.dp))
        }
        if (fresh) Box(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp).size(9.dp).background(Sg.c.primary, CircleShape))
    }
}

/** Вход и устройства: это устройство, остальные сеансы, смена пароля кодом из письма. */
@Composable
fun SecurityScreen() {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val acc by repo.account.collectAsState()
    LaunchedEffect(Unit) { repo.loadAccount() }
    val others = ((acc?.sessions ?: 1) - 1).coerceAtLeast(0)
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        SubTopBar("Вход и устройства", { nav.back() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            VGap(8.dp)
            Grp {
                val model = if (repo.demo.value) "Pixel 9" else "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
                Gi("$model · это устройство", "Сейчас", "phone")
                if (others > 0 || repo.demo.value) Gi(if (repo.demo.value) "Chrome · Windows" else "Другие устройства: $others", if (repo.demo.value) "Сегодня, 09:12 · Москва" else "Компьютеры и телефоны, где открыт кабинет", "laptop") {
                    Btn("Выйти", small = true, kind = BtnKind.Outline) { repo.logoutOthers() }
                }
            }
            Sect("Пароль")
            Grp { Gi("Сменить пароль", "Пришлём код на почту", "mail", onClick = { repo.passwordCode(); ov.openSheet { PasswordSheet() } }) { Chev() } }
        }
    }
}

@Composable
private fun ColumnScope.PasswordSheet() {
    val repo = LocalRepo.current; val ov = LocalOv.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    SheetTitle("Новый пароль")
    T("Код пришёл на почту для входа.", body(14f), Sg.c.muted)
    Lbl("Код из письма"); SField(code, { code = it.filter { c -> c.isDigit() }.take(8) }, "Например, 123456", "shield", keyboard = KeyboardType.Number)
    Lbl("Новый пароль"); SField(pass, { pass = it }, "Не короче 8 символов", "lock", password = true)
    VGap(18.dp)
    Btn("Сменить пароль", icon = "check", wide = true) {
        if (pass.length < 8) { repo.toast("Пароль не короче 8 символов"); return@Btn }
        scope.launch { val e = repo.setPassword(code, pass); if (e != null) repo.toast(e) else { ov.closeSheet(); repo.toast("Пароль изменён") } }
    }
}
