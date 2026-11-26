package ru.sguardo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.sguardo.app.data.Clock
import ru.sguardo.app.data.Fmt
import ru.sguardo.app.data.Lesson
import ru.sguardo.app.data.LessonExtra
import ru.sguardo.app.data.parseIso
import ru.sguardo.app.ui.Btn
import ru.sguardo.app.ui.BtnKind
import ru.sguardo.app.ui.DialogBtn
import ru.sguardo.app.ui.DialogSpec
import ru.sguardo.app.ui.EmptyState
import ru.sguardo.app.ui.FChip
import ru.sguardo.app.ui.Fab
import ru.sguardo.app.ui.Lbl
import ru.sguardo.app.ui.LocalNav
import ru.sguardo.app.ui.LocalOv
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.SField
import ru.sguardo.app.ui.Sect
import ru.sguardo.app.ui.SheetTitle
import ru.sguardo.app.ui.SkelRows
import ru.sguardo.app.ui.SubTopBar
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.VGap
import ru.sguardo.app.ui.hexColor
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import java.time.LocalDate

/** Расписание недели: дни сверху, занятия дня, добавление и правка. */
@Composable
fun ScheduleScreen() {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    var day by remember { mutableStateOf(Clock.today()) }
    val week = Fmt.weekStart(day)
    val lessons by repo.lessons.collectAsState()
    val loading by repo.lessonsLoading.collectAsState()
    val extras by repo.prefs.lessonExtra.collectAsState()
    val subjects by repo.prefs.subjects.collectAsState()
    LaunchedEffect(week) { repo.loadLessons(week) }
    val scroll = rememberScrollState()
    fun dayOf(l: Lesson) = Clock.local(parseIso(l.startsAt)).toLocalDate()
    Box(Modifier.fillMaxSize().background(Sg.c.surface)) {
        Column(Modifier.fillMaxSize()) {
            SubTopBar("Расписание", { nav.back() }, scrolled = scroll.value > 28)
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                VGap(8.dp)
                var drag by remember { mutableStateOf(0f) }
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 14.dp).clip(RoundedCornerShape(20.dp)).background(Sg.c.cLow).padding(6.dp)
                    .pointerInput(Unit) { detectHorizontalDragGestures(onDragEnd = { if (drag < -80) day = day.plusWeeks(1) else if (drag > 80) day = day.minusWeeks(1); drag = 0f }) { _, dx -> drag += dx } },
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0 until 7).forEach { i ->
                        val d = week.plusDays(i.toLong()); val on = d == day
                        val has = lessons.any { dayOf(it) == d && it.status != "cancelled" }
                        Column(Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Sg.c.primary else Color.Transparent).clickable { day = d },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            T(Fmt.wd(d), body(11f, FontWeight.Medium), if (on) Sg.c.onPrimary else Sg.c.muted)
                            T(d.dayOfMonth.toString(), body(17f, FontWeight.SemiBold), if (on) Sg.c.onPrimary else Sg.c.fg)
                            Box(Modifier.size(4.dp).graphicsLayer { alpha = if (has) .6f else 0f }.background(if (on) Sg.c.onPrimary else Sg.c.muted, CircleShape))
                        }
                    }
                }
                val today = lessons.filter { dayOf(it) == day && it.status != "cancelled" }.sortedBy { it.startsAt }
                Sect(Fmt.dayLabel(day), "${today.size} ${Fmt.plural(today.size, "занятие", "занятия", "занятий")}", top = 6.dp)
                if (loading && today.isEmpty()) SkelRows(3, 40.dp)
                else if (today.isEmpty()) EmptyState("cal", "Свободный день", "Занятий нет. Нажмите «+», чтобы добавить")
                else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    today.forEach { l ->
                        val ex = extras[l.id]
                        val st = parseIso(l.startsAt); val mins = ((parseIso(l.endsAt) - st) / 60_000).toInt()
                        val subj = ex?.subject?.ifBlank { null } ?: "Занятие"
                        val col = hexColor(subjects.firstOrNull { it.name == subj }?.color ?: subjectColor(subj))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(16.dp)).background(Sg.c.cLow)
                            .clickable { ov.openSheet { LessonSheet(l) } }.padding(12.dp).height(IntrinsicSize.Min)) {
                            Column(Modifier.width(52.dp)) { T(Fmt.hm(st), body(15f, FontWeight.Bold)); T("$mins мин", body(12f, FontWeight.Medium), Sg.c.muted) }
                            Spacer(Modifier.width(12.dp))
                            Box(Modifier.width(3.dp).fillMaxHeight().background(col))
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                T(subj, body(15f, FontWeight.SemiBold), maxLines = 1)
                                T(ex?.who?.ifBlank { null } ?: repo.nameOf(repo.contact(l.contactId)), body(13f), Sg.c.muted, maxLines = 1)
                            }
                        }
                    }
                }
                VGap(120.dp)
            }
        }
        Fab("plus", "Занятие", true, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 36.dp)) { ov.openSheet { LessonEditSheet(day, null, null) } }
    }
}

private fun subjectColor(s: String) = when { s.contains("физ", true) -> "#9CC3D3"; s.contains("проб", true) -> "#E2C06A"; s.contains("разбор", true) || s.contains("консульт", true) -> "#E9A37E"; else -> "#86C7A6" }

/** Лист занятия: когда, длительность, ученик; написать, изменить, перенести на завтра, удалить. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.LessonSheet(l: Lesson) {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val ex = repo.prefs.lessonExtra.collectAsState().value[l.id]
    val st = parseIso(l.startsAt); val mins = ((parseIso(l.endsAt) - st) / 60_000).toInt()
    val d = Clock.local(st).toLocalDate()
    val who = ex?.who?.ifBlank { null } ?: repo.nameOf(repo.contact(l.contactId))
    SheetTitle(ex?.subject?.ifBlank { null } ?: "Занятие")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Sg.c.cHigh)) {
        IRow("когда", "${Fmt.dayLabel(d)}, ${Fmt.hm(st)}") {}
        IRow("длительность", "$mins мин") {}
        IRow("ученик", who) {}
    }
    VGap(16.dp)
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (l.conversationId.isNotBlank() && repo.conv(l.conversationId) != null) Btn("Написать", icon = "chat", small = true) { ov.closeSheet(); nav.chat(l.conversationId) }
        Btn("Изменить", icon = "edit", small = true, kind = BtnKind.Outline) { ov.openSheet { LessonEditSheet(d, l, l.conversationId) } }
        Btn("На завтра", icon = "clock", small = true, kind = BtnKind.Outline) {
            ov.closeSheet(); val t = Clock.local(st)
            repo.moveLesson(l, d.plusDays(1), t.hour, t.minute, mins, ex ?: LessonExtra(), Fmt.weekStart(d), "Перенесли на ${Fmt.dayLabel(d.plusDays(1)).lowercase()}")
        }
        Btn("Удалить", icon = "trash", small = true, kind = BtnKind.Outline, danger = true) {
            ov.closeSheet()
            ov.confirm(DialogSpec("Удалить занятие?", "Ученик не получит уведомление об отмене.", check = if (l.seriesId != null) "И все следующие занятия этой серии" else null,
                buttons = listOf(DialogBtn("Удалить занятие", danger = true) { following -> repo.deleteLesson(l, following) })))
        }
    }
}

/** Новое занятие или правка: ученик, время, длительность, предмет, повтор. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.LessonEditSheet(day: LocalDate, l: Lesson?, convId: String?) {
    val repo = LocalRepo.current; val ov = LocalOv.current
    val subjects by repo.prefs.subjects.collectAsState()
    val ex = l?.let { repo.prefs.lessonExtra.value[it.id] }
    val st0 = l?.let { Clock.local(parseIso(it.startsAt)) }
    var conv by remember { mutableStateOf(convId?.ifBlank { null }) }
    var who by remember { mutableStateOf(ex?.who?.ifBlank { null } ?: conv?.let { repo.nameOfConv(repo.conv(it)) } ?: "") }
    var time by remember { mutableStateOf(st0?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "18:30") }
    var dur by remember { mutableStateOf(l?.let { "${(parseIso(it.endsAt) - parseIso(it.startsAt)) / 60_000} мин" } ?: "60 мин") }
    var subj by remember { mutableStateOf(ex?.subject?.ifBlank { null } ?: subjects.firstOrNull()?.name ?: "") }
    var rep by remember { mutableStateOf("Нет") }
    SheetTitle(if (l != null) "Изменить занятие" else "Новое занятие")
    Lbl(Fmt.dayLabel(day))
    Lbl("Ученик")
    SField(who, { who = it; conv = null }, "Имя ученика или группа", "user")
    if (conv == null && who.isNotBlank()) {
        val st = repo.state.value
        val found = st.conversations.filter { !it.trashed && repo.nameOfConv(it).contains(who.trim(), true) }.take(4)
        Column(Modifier.padding(top = 4.dp)) {
            found.forEach { c ->
                Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).clickable { conv = c.id; who = repo.nameOfConv(c) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ru.sguardo.app.ui.Avatar(repo.nameOfConv(c), 28.dp, ring = Sg.c.cLow); Spacer(Modifier.width(10.dp)); T(repo.nameOfConv(c), body(14.5f), maxLines = 1)
                }
            }
        }
    }
    Lbl("Время")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        (listOf("09:00", "10:00", "12:00", "14:00", "16:00", "17:00", "18:30", "19:30") + listOfNotNull(time.takeIf { it !in listOf("09:00", "10:00", "12:00", "14:00", "16:00", "17:00", "18:30", "19:30") }))
            .forEach { t -> FChip(t, time == t) { time = t } }
    }
    Lbl("Длительность")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("30 мин", "45 мин", "60 мин", "90 мин").forEach { t -> FChip(t, dur == t) { dur = t } }
    }
    Lbl("Предмет")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        subjects.forEach { s -> FChip(s.name, subj == s.name, dot = hexColor(s.color)) { subj = s.name } }
    }
    if (l == null) {
        Lbl("Повторять")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Нет", "4 недели", "8 недель", "16 недель").forEach { t -> FChip(t, rep == t) { rep = t } }
        }
    }
    VGap(18.dp)
    Btn("Сохранить", icon = "check", wide = true) {
        if (who.isBlank()) { repo.toast("Укажите ученика"); return@Btn }
        val (h, m) = time.split(":").map { it.toInt() }
        val minutes = dur.filter { it.isDigit() }.toInt()
        val extra = LessonExtra(subj, if (conv == null) who.trim() else "")
        val c = conv ?: if (repo.demo.value) "" else null
        if (c == null) { repo.toast("Выберите ученика из списка: занятие привязывается к переписке"); return@Btn }
        ov.closeSheet()
        if (l != null) repo.moveLesson(l, day, h, m, minutes, extra, Fmt.weekStart(day))
        else repo.addLesson(c, day, h, m, minutes, extra, Fmt.weekStart(day), rep.filter { it.isDigit() }.toIntOrNull() ?: 0)
    }
}
