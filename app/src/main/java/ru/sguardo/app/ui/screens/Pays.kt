package ru.sguardo.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.sguardo.app.data.Clock
import ru.sguardo.app.data.Deal
import ru.sguardo.app.data.Fmt
import ru.sguardo.app.data.Load
import ru.sguardo.app.data.obj
import ru.sguardo.app.data.parseIso
import ru.sguardo.app.ui.Avatar
import ru.sguardo.app.ui.Btn
import ru.sguardo.app.ui.EmptyState
import ru.sguardo.app.ui.ErrorState
import ru.sguardo.app.ui.Fab
import ru.sguardo.app.ui.Ic
import ru.sguardo.app.ui.Lbl
import ru.sguardo.app.ui.LocalOv
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.LocalUi
import ru.sguardo.app.ui.OfflineBanner
import ru.sguardo.app.ui.ProdRow
import ru.sguardo.app.ui.SField
import ru.sguardo.app.ui.Seg
import ru.sguardo.app.ui.SheetTitle
import ru.sguardo.app.ui.Skel
import ru.sguardo.app.ui.SkelRows
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.TopBar
import ru.sguardo.app.ui.VGap
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import ru.sguardo.app.ui.theme.disp

/** Статус оплаты для показа: paid / prom / late. */
fun dealState(d: Deal): String = when {
    d.status == "paid" -> "paid"
    d.status == "refund" -> "refund"
    d.dueAt != null && parseIso(d.dueAt) < Clock.now() -> "late"
    else -> "prom"
}

@Composable
fun PaysScreen() {
    val repo = LocalRepo.current; val ui = LocalUi.current; val ov = LocalOv.current
    val st by repo.state.collectAsState()
    val load by repo.load.collectAsState()
    val online by repo.online.collectAsState()
    val scroll = rememberScrollState()
    var fabOpen by remember { mutableStateOf(true) }
    LaunchedEffect(scroll) { var last = 0; snapshotFlow { scroll.value }.collect { v -> if (v > last + 6) fabOpen = false else if (v < last - 6) fabOpen = true; last = v } }
    LaunchedEffect(ui.scrollTopSignal) { if (ui.scrollTopSignal > 0) scroll.animateScrollTo(0) }
    var justPaid by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar("Оплаты", scroll.value > 28) { HeadActions() }
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                T("товары, обещания и оплаченные сделки", body(14f), Sg.c.muted, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp))
                if (!online) OfflineBanner()
                when {
                    load == Load.Loading -> {
                        Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Skel(Modifier.fillMaxWidth().height(104.dp), RoundedCornerShape(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Skel(Modifier.weight(1f).height(96.dp), RoundedCornerShape(24.dp)); Skel(Modifier.weight(1f).height(96.dp), RoundedCornerShape(24.dp)) }
                        }
                        SkelRows(5, 40.dp)
                    }
                    load == Load.Error && st.deals.isEmpty() -> ErrorState({ repo.retry() })
                    st.deals.isEmpty() -> EmptyState("wallet", "Оплат пока нет", "Отмечайте оплаты и обещания, а мы напомним о просроченных", button = "Новая оплата") { ov.openSheet { PaySheet(null, "paid") } }
                    else -> {
                        val paid = st.deals.filter { it.status == "paid" }.sumOf { it.amount }
                        val prom = st.deals.filter { dealState(it) == "prom" }.sumOf { it.amount }
                        val late = st.deals.count { dealState(it) == "late" }
                        Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SumCard(Sg.c.tGreen, "Оплачено", paid, true, Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SumCard(Sg.c.tYellow, "Обещали оплатить", prom, true, Modifier.weight(1f))
                                SumCard(Sg.c.tPink, "Просрочено", late.toLong(), false, Modifier.weight(1f))
                            }
                        }
                        VGap(14.dp)
                        Seg(listOf(Triple("all", "Все", 0), Triple("prom", "Обещали", 0), Triple("paid", "Оплачено", 0), Triple("late", "Просрочено", 0)), ui.pf, { ui.pf = it })
                        val list = st.deals.filter { ui.pf == "all" || dealState(it) == ui.pf }.sortedByDescending { parseIso(it.date) }
                        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            list.forEach { d -> PayRow(d, justPaid == d.id) { justPaid = d.id } }
                        }
                    }
                }
                VGap(120.dp)
            }
        }
        Fab("plus", "Новая оплата", fabOpen, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)) { ov.openSheet { PaySheet(null, "paid") } }
    }
}

/** Карточка итога с подсчётом цифр (числа «докручиваются» за 0,7 с). */
@Composable
private fun SumCard(tint: Color, label: String, value: Long, money: Boolean, modifier: Modifier) {
    val a = remember { Animatable(value.toFloat()) }
    LaunchedEffect(value) { a.animateTo(value.toFloat(), tween(700, easing = CubicBezierEasing(.33f, 1f, .68f, 1f))) }
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Sg.c.cLow).padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.height(34.dp).clip(RoundedCornerShape(18.dp)).background(tint).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            T(label, body(13f, FontWeight.SemiBold), Sg.c.statInk, maxLines = 1)
        }
        val v = a.value.toLong()
        ru.sguardo.app.ui.FitText(if (money) Fmt.rub(v) else v.toString(), disp(25), modifier = Modifier.padding(horizontal = 12.dp))
    }
}

@Composable
private fun PayRow(d: Deal, just: Boolean, onPaid: () -> Unit) {
    val repo = LocalRepo.current; val ov = ru.sguardo.app.ui.LocalOv.current
    val st by repo.state.collectAsState()
    val c = repo.contact(d.contactId)
    val prod = st.products.firstOrNull { it.id == d.productId }?.name ?: "Оплата"
    val s = dealState(d)
    val flash = remember { Animatable(if (just) 1f else 0f) }
    LaunchedEffect(just) { if (just) { flash.snapTo(1f); flash.animateTo(0f, tween(1200)) } }
    val tealC = Sg.c.teal
    // нажатие на оплату: удалить (ошибочную или тестовую)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Sg.c.cLow).background(tealC.copy(alpha = .22f * flash.value))
        .clickable {
            ov.confirm(ru.sguardo.app.ui.DialogSpec("Удалить оплату?", "${repo.nameOf(c)} · ${Fmt.rub(d.amount)} · ${Fmt.dayMonth(parseIso(d.date))}. Вернуть будет нельзя.",
                buttons = listOf(ru.sguardo.app.ui.DialogBtn("Удалить", danger = true) { repo.act(obj("type" to "removeDeal", "id" to d.id), "Оплата удалена") })))
        }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Avatar(repo.nameOf(c), 40.dp, ring = Sg.c.cLow)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            T(repo.nameOf(c), body(15.5f, FontWeight.SemiBold), maxLines = 1)
            T("$prod · ${Fmt.dayMonth(parseIso(d.date))}", body(13f), Sg.c.muted, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 2.dp)) {
            T(Fmt.rub(d.amount), body(15f, FontWeight.Bold))
            val due = d.dueAt?.let { Fmt.dayMonth(parseIso(it)) } ?: ""
            T(when (s) { "paid" -> "оплачено"; "prom" -> "обещал${if (due.isNotEmpty()) " до $due" else ""}"; "refund" -> "возврат"; else -> "просрочено${if (due.isNotEmpty()) " с $due" else ""}" },
                body(11f, FontWeight.SemiBold), when (s) { "paid" -> Sg.c.ok; "prom" -> Sg.c.warn; else -> Sg.c.wine })
        }
        if (s == "prom" || s == "late") {
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(38.dp).clip(CircleShape).border(1.dp, Sg.c.outline, CircleShape).clickable {
                onPaid()
                val prev = d.status
                repo.act(obj("type" to "pay", "contactId" to d.contactId, "productId" to (d.productId ?: kotlinx.serialization.json.JsonNull), "amount" to d.amount.coerceAtLeast(1), "dealId" to d.id),
                    "${repo.nameOf(c)}: ${Fmt.rub(d.amount)} оплачено",
                    undo = { repo.act(obj("type" to "updateDeal", "id" to d.id, "status" to prev, "dueAt" to d.dueAt)) })
            }, contentAlignment = Alignment.Center) { Ic("check", Sg.c.fg, size = 16.dp) }
        } else if (just) {
            Spacer(Modifier.width(12.dp))
            val pop = remember { Animatable(.6f) }
            LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = .4f, stiffness = 500f)) }
            Box(Modifier.size(38.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.background(Sg.c.ok, CircleShape), contentAlignment = Alignment.Center) { Ic("check", Color.White, size = 16.dp) }
        }
    }
}

/** Новая оплата: клиент, товар, статус «Оплачено» или «Обещал оплатить». */
@Composable
fun ColumnScope.PaySheet(contactId: String?, status: String, title: String = "Новая оплата", lockClient: Boolean = false) {
    val repo = LocalRepo.current; val ov = LocalOv.current
    val st by repo.state.collectAsState()
    var picked by remember { mutableStateOf(contactId) }
    var q by remember { mutableStateOf(contactId?.let { repo.nameOf(repo.contact(it)) } ?: "") }
    var prod by remember { mutableStateOf<String?>(null) }
    var stt by remember { mutableStateOf(if (status == "promised") "prom" else "paid") }
    SheetTitle(title)
    Lbl("Клиент")
    if (lockClient && picked != null) Row(Modifier.fillMaxWidth().height(48.dp).background(Sg.c.cHigh, RoundedCornerShape(24.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(q, 28.dp, ring = Sg.c.cHigh); Spacer(Modifier.width(10.dp)); T(q, body(15f))
    } else SField(q, { q = it; picked = null }, "Имя клиента", "user", bg = Sg.c.cHigh)
    if (picked == null && q.isNotBlank()) {
        val found = st.contacts.filter { repo.nameOf(it).contains(q.trim(), ignoreCase = true) }.take(4)
        Column(Modifier.padding(top = 4.dp)) {
            found.forEach { c ->
                Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).clickable { picked = c.id; q = repo.nameOf(c) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(repo.nameOf(c), 28.dp, ring = Sg.c.cLow); Spacer(Modifier.width(10.dp)); T(repo.nameOf(c), body(14.5f), maxLines = 1)
                }
            }
            if (found.isEmpty()) T("Такого клиента нет. Сначала добавьте его во вкладке «Клиенты»", body(12.5f), Sg.c.muted, modifier = Modifier.padding(6.dp))
        }
    }
    Lbl("Товар или услуга")
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        st.products.forEach { p -> ProdRow(p.name, Fmt.rub(p.price), prod == p.id) { prod = p.id } }
    }
    // товаров в кабинете нет или нужна другая сумма — вводим вручную
    var custom by remember { mutableStateOf("") }
    Lbl(if (st.products.isEmpty()) "Сумма" else "Или другая сумма")
    SField(custom, { custom = it.filter { ch -> ch.isDigit() }.take(9); if (custom.isNotEmpty()) prod = null }, "Например, 2500", "card", keyboard = androidx.compose.ui.text.input.KeyboardType.Number)
    Lbl("Статус")
    Seg(listOf(Triple("paid", "Оплачено", 0), Triple("prom", "Обещал оплатить", 0)), stt, { stt = it }, hpad = 0.dp)
    VGap(18.dp)
    val p = st.products.firstOrNull { it.id == prod }
    val sum = p?.price ?: custom.toLongOrNull() ?: 0L
    Btn("Добавить" + (if (sum > 0) " · " + Fmt.rub(sum) else ""), icon = "check", wide = true) {
        val cid = picked ?: st.contacts.firstOrNull { repo.nameOf(it).equals(q.trim(), true) }?.id
        if (cid == null) { repo.toast("Укажите клиента"); return@Btn }
        if (sum <= 0) { repo.toast("Выберите товар или введите сумму"); return@Btn }
        ov.closeSheet()
        val name = repo.nameOf(repo.contact(cid))
        val pid: Any = p?.id ?: kotlinx.serialization.json.JsonNull
        if (stt == "paid") repo.act(obj("type" to "pay", "contactId" to cid, "productId" to pid, "amount" to sum), "$name: ${Fmt.rub(sum)}, оплачено")
        else repo.act(obj("type" to "promise", "contactId" to cid, "productId" to pid, "amount" to sum,
            "remindAt" to Clock.isoLocal(Clock.at(Clock.today().plusDays(7), 10, 0))), "$name: ${Fmt.rub(sum)}, обещал оплатить")
    }
}
