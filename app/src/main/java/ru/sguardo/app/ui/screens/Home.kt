package ru.sguardo.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sguardo.app.data.Clock
import ru.sguardo.app.data.Fmt
import ru.sguardo.app.data.Load
import ru.sguardo.app.data.parseIso
import ru.sguardo.app.ui.Avatar
import ru.sguardo.app.ui.EaseM3
import ru.sguardo.app.ui.EmptyState
import ru.sguardo.app.ui.ErrorState
import ru.sguardo.app.ui.IconBtn
import ru.sguardo.app.ui.Ic
import ru.sguardo.app.ui.LocalNav
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.LocalUi
import ru.sguardo.app.ui.MeButton
import ru.sguardo.app.ui.NetDot
import ru.sguardo.app.ui.OfflineBanner
import ru.sguardo.app.ui.Sect
import ru.sguardo.app.ui.Seg
import ru.sguardo.app.ui.Skel
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.TopBar
import ru.sguardo.app.ui.VGap
import ru.sguardo.app.ui.inboxByDay
import ru.sguardo.app.ui.stats
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import ru.sguardo.app.ui.theme.disp

/** Колокольчик и «В» в шапке вкладки. */
@Composable
fun RowScope.HeadActions(bell: Boolean = true, me: Boolean = true) {
    val repo = LocalRepo.current; val nav = LocalNav.current
    val user by repo.user.collectAsState()
    val read by repo.prefs.readNotifs.collectAsState()
    val st by repo.state.collectAsState()
    val unreadN = remember(st, read) { repo.notifications().count { it.id !in read } }
    if (bell) IconBtn("bell", { nav.go("notifs") }, badge = unreadN)
    if (me) MeButton(user?.name?.take(1) ?: "В") { nav.go("account") }
}

/** Вкладка: шапка + прокрутка; шапка темнеет при прокрутке (.layer.scrolled .atop). */
@Composable
fun TabPage(title: String, sub: String? = null, scroll: ScrollState = rememberScrollState(), actions: @Composable RowScope.() -> Unit = { HeadActions() }, content: @Composable ColumnScope.() -> Unit) {
    val ui = LocalUi.current
    LaunchedEffect(ui.scrollTopSignal) { if (ui.scrollTopSignal > 0) scroll.animateScrollTo(0) }
    Column(Modifier.fillMaxSize()) {
        TopBar(title, scroll.value > 28, actions)
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            if (sub != null) T(sub, body(14f), Sg.c.muted, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 14.dp))
            content()
            VGap(110.dp)
        }
    }
}

@Composable
fun HomeScreen() {
    val repo = LocalRepo.current; val ui = LocalUi.current; val nav = LocalNav.current
    val st by repo.state.collectAsState()
    val load by repo.load.collectAsState()
    val online by repo.online.collectAsState()
    TabPage("Сводка", "лиды, оплаты и кому написать сегодня") {
        Seg(listOf(Triple("today", "Сегодня", 0), Triple("week", "Неделя", 0), Triple("month", "Месяц", 0)), ui.period, { ui.period = it })
        when {
            load == Load.Loading -> {
                Column(Modifier.padding(12.dp).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(2) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Skel(Modifier.weight(1f).height(112.dp), RoundedCornerShape(24.dp)); Skel(Modifier.weight(1f).height(112.dp), RoundedCornerShape(24.dp)) } }
                    Skel(Modifier.fillMaxWidth().height(180.dp), RoundedCornerShape(26.dp))
                }
                return@TabPage
            }
            load == Load.Error && st.conversations.isEmpty() -> { ErrorState({ repo.retry() }); return@TabPage }
            st.conversations.isEmpty() && st.contacts.isEmpty() -> { EmptyState("trend", "Пока тихо", "Сводка заполнится, когда придут первые сообщения"); return@TabPage }
        }
        if (!online) { VGap(12.dp); OfflineBanner() }
        val s = remember(st, ui.period) { stats(st, ui.period) }
        val per = when (ui.period) { "today" -> "за сегодня"; "week" -> "за неделю"; else -> "за месяц" }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Sg.c.tBlue, "userPlus", "Новых лидов", Fmt.group(s.leads.toLong()), per, Modifier.weight(1f)) { nav.tab("clients") }
                StatCard(Sg.c.tGreen, "wallet", "Оплатили", Fmt.group(s.paidCount.toLong()), per, Modifier.weight(1f)) { nav.tab("pays") }
            }
            if (ui.period == "today") Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Sg.c.tYellow, "trend", "Выручка", Fmt.rub(s.revenue), per, Modifier.weight(1f)) { nav.tab("pays") }
                StatCard(Sg.c.tLilac, "msgSq", "Ждут ответа", s.waiting.toString(), "клиент написал, мы молчим", Modifier.weight(1f)) { ui.mf = "wait"; nav.tab("msgs") }
            } else StatCard(Sg.c.tYellow, "trend", "Выручка", Fmt.rub(s.revenue), per, Modifier.fillMaxWidth()) { nav.tab("pays") }
            StatFull(Sg.c.tPink, "bell", "Напомнить сегодня", s.remind.toString(), "включая просроченные") {
                repo.toast("${s.remind} ${Fmt.plural(s.remind, "напоминание", "напоминания", "напоминаний")} на сегодня")
            }
        }
        Chart(remember(st) { inboxByDay(st) })
        val todo = remember(st) {
            val end = Clock.startOfDay(Clock.today().plusDays(1))
            st.reminders.filter { !it.done && parseIso(it.at) < end }.sortedBy { parseIso(it.at) }.take(8)
        }
        Sect("Написать сегодня", todo.size.toString())
        Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (todo.isEmpty()) T("На сегодня всё сделано", body(14f), Sg.c.muted, modifier = Modifier.padding(8.dp))
            todo.forEach { r ->
                val c = repo.contact(r.contactId); val cv = repo.convOf(r.contactId)
                val at = parseIso(r.at); val late = at < Clock.startOfDay(Clock.today())
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Sg.c.cLow).clickable {
                    if (cv != null) nav.chat(cv.id) else nav.tab("clients")
                }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(repo.nameOf(c), 40.dp, ring = Sg.c.cLow)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            T(repo.nameOf(c), body(15.5f, FontWeight.SemiBold), maxLines = 1, modifier = Modifier.weight(1f, false))
                            Spacer(Modifier.width(5.dp))
                            NetDot(cv?.network ?: c?.source ?: "telegram", 15.dp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                            T(r.text.ifBlank { "Написать клиенту" }, body(14f), Sg.c.muted, maxLines = 1, modifier = Modifier.weight(1f))
                            val col = if (late) Sg.c.wine else Sg.c.muted
                            Ic("clock", col, size = 13.dp)
                            Spacer(Modifier.width(3.dp))
                            T(if (Clock.local(at).toLocalDate() == Clock.today()) Fmt.hm(at) else Fmt.dayMonth(at), body(11.5f, FontWeight.SemiBold), col)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatHead(tint: Color, icon: String, label: String) {
    Row(Modifier.height(34.dp).clip(RoundedCornerShape(18.dp)).background(tint).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Ic(icon, Sg.c.statInk, size = 18.dp); Spacer(Modifier.width(7.dp))
        T(label, body(13f, FontWeight.SemiBold), Sg.c.statInk, maxLines = 1)
    }
}

/** Карточка сводки (.stat): цифры и подписи слева. */
@Composable
fun StatCard(tint: Color, icon: String, label: String, num: String, cap: String, modifier: Modifier = Modifier, minH: Dp = 112.dp, onClick: () -> Unit) {
    Column(modifier.heightIn(min = minH).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Sg.c.cLow).clickable(onClick = onClick).padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 14.dp)) {
        StatHead(tint, icon, label)
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.Center) {
            ru.sguardo.app.ui.FitText(num, disp(25))
            T(cap, body(12f), Sg.c.muted, maxLines = 2)
        }
    }
}

@Composable
fun StatFull(tint: Color, icon: String, label: String, num: String, cap: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Sg.c.cLow).clickable(onClick = onClick).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        StatHead(tint, icon, label)
        T(num, disp(22), modifier = Modifier.padding(start = 8.dp, end = 8.dp))
        T(cap, body(12f), Sg.c.muted, modifier = Modifier.weight(1f), maxLines = 2)
    }
}

/** «Входящие по дням»: 14 столбиков, сегодняшний — основным цветом, вырастают снизу. */
@Composable
private fun Chart(data: List<Pair<Int, Int>>) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(700, easing = EaseM3)) }
    val mx = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp).clip(RoundedCornerShape(20.dp)).background(Sg.c.cLow).padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 12.dp)) {
        Row(Modifier.padding(bottom = 14.dp), verticalAlignment = Alignment.Bottom) {
            T("Входящие по дням", disp(15), modifier = Modifier.weight(1f))
            T("последние 14 дней", body(12f, FontWeight.Medium), Sg.c.muted)
        }
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
            data.forEachIndexed { i, (day, v) ->
                val today = i == data.lastIndex
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    val col = if (today) Sg.c.fg else Sg.c.muted
                    T(v.toString(), body(9.5f, if (today) FontWeight.Bold else FontWeight.Normal), col, align = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    val frac = maxOf(.06f, v.toFloat() / mx) * grow.value
                    Box(Modifier.fillMaxWidth().height(76.dp * frac).clip(RoundedCornerShape(7.dp)).background(if (today) Sg.c.teal else Sg.c.fg.copy(alpha = .11f)))
                    Spacer(Modifier.height(4.dp))
                    T(day.toString(), body(9.5f, if (today) FontWeight.Bold else FontWeight.Normal), col, align = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun Sz(w: Dp, h: Dp) = Spacer(Modifier.size(w, h))
