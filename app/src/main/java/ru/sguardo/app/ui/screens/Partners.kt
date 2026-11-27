package ru.sguardo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.sguardo.app.data.ClubPartner
import ru.sguardo.app.ui.Btn
import ru.sguardo.app.ui.BtnKind
import ru.sguardo.app.ui.FormHint
import ru.sguardo.app.ui.LocalNav
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.SubTopBar
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import ru.sguardo.app.ui.theme.disp

/**
 * «Партнёры»: только для кабинетов-клубов, которым модуль включён в админке (там же привязываются партнёры из Sguardo).
 * В приложении только просмотр цифр по партнёрам: лиды, оплаты, конверсия, выручка. Переписок партнёров не видно.
 */
@Composable
fun PartnersScreen() {
    val repo = LocalRepo.current; val nav = LocalNav.current
    var days by remember { mutableIntStateOf(30) }
    var list by remember { mutableStateOf<List<ClubPartner>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(days, reload) { list = null; failed = false; val r = repo.partners(days); if (r == null) failed = true else list = r }
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        SubTopBar("Партнёры", { nav.back() })
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Sg.c.cHigh).padding(4.dp)) {
            listOf(7 to "Неделя", 30 to "Месяц", 90 to "Квартал").forEach { (d, t) ->
                val on = d == days
                Box(Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(18.dp)).background(if (on) Sg.c.c else Sg.c.cHigh).clickable { days = d }, contentAlignment = Alignment.Center) {
                    T(t, body(14f, FontWeight.SemiBold), if (on) Sg.c.fg else Sg.c.muted)
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val l = list
            when {
                failed -> Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    T("Не получилось загрузить", body(15f), Sg.c.muted); Spacer(Modifier.height(10.dp))
                    Btn("Повторить", kind = BtnKind.Tonal) { reload++ }
                }
                l == null -> Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = Sg.c.primary)
                }
                l.isEmpty() -> Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    T("Партнёров пока нет", body(16f, FontWeight.SemiBold)); Spacer(Modifier.height(6.dp))
                    T("Их подключают в админке Sguardo", body(14f), Sg.c.muted)
                }
                else -> l.sortedByDescending { it.leads }.forEach { p ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Sg.c.cLow).padding(16.dp)) {
                        T(p.name, body(16.5f, FontWeight.SemiBold))
                        Spacer(Modifier.height(4.dp))
                        T("${p.leads} ${ru.sguardo.app.data.Fmt.plural(p.leads, "лид", "лида", "лидов")} · ${p.payments} ${ru.sguardo.app.data.Fmt.plural(p.payments, "оплата", "оплаты", "оплат")} · ${(p.conversion * 100).toInt()}%",
                            body(13.5f), Sg.c.muted)
                        Spacer(Modifier.height(6.dp))
                        T(money(p.revenue, p.currency), disp(19))
                    }
                }
            }
            if (!l.isNullOrEmpty()) FormHint("Здесь видны только цифры, без переписок партнёров.")
        }
    }
}

private fun money(v: Double, cur: String): String {
    val n = String.format(java.util.Locale("ru"), "%,.0f", v).replace(',', ' ')
    return n + " " + when (cur.uppercase()) { "RUB" -> "₽"; "USD" -> "$"; "EUR" -> "€"; "KZT" -> "₸"; else -> cur }
}
