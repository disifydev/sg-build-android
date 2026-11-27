package ru.sguardo.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.sguardo.app.BuildConfig
import ru.sguardo.app.ui.Avatar
import ru.sguardo.app.ui.Btn
import ru.sguardo.app.ui.BtnKind
import ru.sguardo.app.ui.EaseM3
import ru.sguardo.app.ui.EyeLogo
import ru.sguardo.app.ui.EyeSmall
import ru.sguardo.app.ui.Ic
import ru.sguardo.app.ui.LocalPerms
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.SField
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.VGap
import ru.sguardo.app.ui.shadowCompat
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import ru.sguardo.app.ui.theme.disp

/** Заставка: глаз в круге проявляется из центра. */
@Composable
fun SplashScreen() {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(700, easing = EaseM3)) }
    Box(Modifier.fillMaxSize().background(Sg.c.surface), contentAlignment = Alignment.Center) {
        EyeLogo(176.dp, Modifier.graphicsLayer { scaleX = .6f + .4f * a.value; scaleY = .6f + .4f * a.value; alpha = a.value }
            .shadowCompat(18.dp, CircleShape))
    }
}

/** Первый запуск: объяснение перед запросом уведомлений, затем доступ к контактам. */
@Composable
fun OnboardingScreen(initial: String = "notify") {
    val repo = LocalRepo.current
    val perms = LocalPerms.current
    var step by remember { mutableStateOf(initial) }
    AnimatedContent(step, transitionSpec = { (slideInHorizontally(tween(450, easing = EaseM3)) { it / 9 } + fadeIn(tween(450))) togetherWith fadeOut(tween(150)) }, label = "ob") { s ->
        if (s == "notify") ObPage(
            art = { NotifyArt() }, dot = 0, title = "Не пропускайте клиентов",
            text = "Пришлём уведомление, когда напишет клиент или подойдёт время напомнить об оплате. Нажатие сразу открывает нужный чат.",
            primary = "Включить уведомления", secondary = "Позже",
            onPrimary = { perms.notifications { ok -> repo.prefs.setNotifyMessages(ok); repo.toast(if (ok) "Уведомления включены" else "Уведомления выключены. Включить можно в разделе «Ещё»"); step = "contacts" } },
            onSecondary = { step = "contacts" },
        ) else ObPage(
            art = { ContactsArt() }, dot = 1, title = "Имена из контактов",
            text = "WhatsApp и MAX присылают только номер. Если он записан у вас в телефоне, в чатах будет имя из контактов. Выключается в разделе «Ещё».",
            primary = "Разрешить доступ к контактам", secondary = "Не сейчас",
            onPrimary = { perms.contacts { ok -> repo.prefs.setNamesFromBook(ok); repo.book.refresh(); repo.prefs.setOnboarded(true) } },
            onSecondary = { repo.prefs.setNamesFromBook(false); repo.prefs.setOnboarded(true) },
        )
    }
}

@Composable
private fun ObPage(art: @Composable () -> Unit, dot: Int, title: String, text: String, primary: String, secondary: String, onPrimary: () -> Unit, onSecondary: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Sg.c.surface).windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 24.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { art() }
        Row(Modifier.padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) { i ->
                val w by animateDpAsState(if (i == dot) 22.dp else 8.dp, tween(400), label = "dot")
                Box(Modifier.size(w, 8.dp).background(if (i == dot) Sg.c.primary else Sg.c.hair, RoundedCornerShape(4.dp)))
            }
        }
        T(title, disp(25, FontWeight.Bold).copy(lineHeight = 29.sp), modifier = Modifier.padding(bottom = 10.dp))
        T(text, body(15f).copy(lineHeight = 22.sp), Sg.c.muted, modifier = Modifier.padding(bottom = 18.dp))
        Btn(primary, wide = true, onClick = onPrimary)
        VGap(6.dp)
        Btn(secondary, wide = true, kind = BtnKind.Text, onClick = onSecondary)
    }
}

@Composable
private fun NotifyArt() {
    val inf = rememberInfiniteTransition(label = "flo")
    val f by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    val ring by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400)), label = "ring")
    val rot = when { ring < .6f -> 0f; ring < .65f -> 14f * ((ring - .6f) / .05f); ring < .72f -> 14f - 26f * ((ring - .65f) / .07f); ring < .79f -> -12f + 20f * ((ring - .72f) / .07f); ring < .86f -> 8f - 12f * ((ring - .79f) / .07f); else -> -4f * (1 - (ring - .86f) / .14f) }
    Box(Modifier.size(270.dp, 250.dp)) {
        val cards = listOf(Triple("Наталья Фёдорова", "Лучше в четверг вечером", 20), Triple("Напоминание", "Николай Попов обещал оплатить сегодня", 70), Triple("Мария Козлова · Telegram", "А рассрочка есть?", 124))
        cards.forEachIndexed { i, (t, s, top) ->
            val sc = listOf(.88f, .94f, 1f)[i]; val al = listOf(.55f, .8f, 1f)[i]
            val dy = (if (i % 2 == 0) f else 1 - f) * -8f
            Row(Modifier.fillMaxWidth().offset(y = (top + dy).dp).scale(sc).graphicsLayer { alpha = al }.shadowCompat(6.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp)).background(Sg.c.cHigh).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                EyeSmall(32.dp); Box(Modifier.width(10.dp))
                Column { T(t, body(13f, FontWeight.SemiBold), maxLines = 1); T(s, body(12.5f), Sg.c.muted, maxLines = 1) }
            }
        }
        Box(Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = (-6).dp).size(54.dp).rotate(rot).background(Sg.c.primary, CircleShape), contentAlignment = Alignment.Center) {
            Ic("bell", Sg.c.onPrimary)
        }
    }
}

@Composable
private fun ContactsArt() {
    val inf = rememberInfiniteTransition(label = "swap")
    val k by inf.animateFloat(0f, 1f, infiniteRepeatable(keyframes { durationMillis = 4000; 0f at 0; 0f at 1600; 1f at 2000; 1f at 3600; 0f at 4000 }), label = "k")
    Column(Modifier.width(270.dp).clip(RoundedCornerShape(22.dp)).background(Sg.c.cLow).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(Triple("Ирина Белова", "whatsapp", "+7 916 204-18-33"), Triple("Ольга Никитина", "max", "+7 903 771-52-09")).forEachIndexed { i, (n, net, ph) ->
            Row(Modifier.graphicsLayer { alpha = if (i == 0) 1f else .75f }, verticalAlignment = Alignment.CenterVertically) {
                Avatar(n, 48.dp, net, ring = Sg.c.cLow)
                Box(Modifier.width(12.dp))
                Box(Modifier.height(22.dp).fillMaxWidth().clip(RoundedCornerShape(2.dp))) {
                    T(ph, body(15.5f, FontWeight.SemiBold), modifier = Modifier.offset(y = (-22 * k).dp).graphicsLayer { alpha = 1 - k })
                    T(n, body(15.5f, FontWeight.SemiBold), modifier = Modifier.offset(y = (22 * (1 - k)).dp).graphicsLayer { alpha = k })
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ic("book", Sg.c.muted, size = 16.dp); Box(Modifier.width(8.dp))
            T("имена из вашей телефонной книги", body(12.5f), Sg.c.muted)
        }
    }
}

/** Вход: почта и пароль или код из письма. */
@Composable
fun LoginScreen() {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("password") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Sg.c.surface).windowInsetsPadding(WindowInsets.safeDrawing).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        VGap(56.dp)
        EyeLogo(96.dp, Modifier.shadowCompat(14.dp, CircleShape))
        VGap(22.dp)
        T("Вход в Sguardo", disp(25, FontWeight.Bold))
        VGap(8.dp)
        T(if (mode == "code") "Пришлём код на почту, пароль не нужен" else "Тот же кабинет, что на сайте sguardo.ru", body(15f), Sg.c.muted)
        VGap(28.dp)
        SField(email, { email = it; err = null }, "Почта", "mail", height = 56.dp, keyboard = KeyboardType.Email)
        VGap(12.dp)
        when (mode) {
            "password" -> SField(pass, { pass = it; err = null }, "Пароль", "lock", height = 56.dp, keyboard = KeyboardType.Password, password = true)
            "codeSent" -> SField(code, { code = it.filter { c -> c.isDigit() }.take(8); err = null }, "Код из письма", "shield", height = 56.dp, keyboard = KeyboardType.Number)
        }
        if (err != null) T(err!!, body(13.5f, FontWeight.Medium), Sg.c.danger, modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 6.dp))
        VGap(20.dp)
        Btn(when (mode) { "password" -> "Войти"; "code" -> "Прислать код"; else -> "Войти" }, wide = true, enabled = !busy && email.contains("@")) {
            busy = true
            scope.launch {
                err = when (mode) {
                    "password" -> repo.login(email, pass)
                    "code" -> repo.requestCode(email).also { if (it == null) mode = "codeSent" }
                    else -> repo.verifyCode(email, code)
                }
                busy = false
            }
        }
        VGap(6.dp)
        Btn(if (mode == "password") "Войти по коду из письма" else "Войти с паролем", wide = true, kind = BtnKind.Text) { mode = if (mode == "password") "code" else "password"; err = null }
        VGap(24.dp)
    }
}
