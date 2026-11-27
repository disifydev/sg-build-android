package ru.sguardo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.togetherWith
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.sguardo.app.data.Phase
import ru.sguardo.app.data.Repo
import ru.sguardo.app.data.ToastMsg
import ru.sguardo.app.ui.screens.AccountScreen
import ru.sguardo.app.ui.screens.ArchiveScreen
import ru.sguardo.app.ui.screens.ChatScreen
import ru.sguardo.app.ui.screens.ClientsScreen
import ru.sguardo.app.ui.screens.EditContactScreen
import ru.sguardo.app.ui.screens.FoldersScreen
import ru.sguardo.app.ui.screens.HomeScreen
import ru.sguardo.app.ui.screens.ListsScreen
import ru.sguardo.app.ui.screens.LoginScreen
import ru.sguardo.app.ui.screens.MessagesScreen
import ru.sguardo.app.ui.screens.MoreScreen
import ru.sguardo.app.ui.screens.NotifsScreen
import ru.sguardo.app.ui.screens.OnboardingScreen
import ru.sguardo.app.ui.screens.PaysScreen
import ru.sguardo.app.ui.screens.ProfileScreen
import ru.sguardo.app.ui.screens.ScheduleScreen
import ru.sguardo.app.ui.screens.SecurityScreen
import ru.sguardo.app.ui.screens.SplashScreen
import ru.sguardo.app.ui.screens.StaffScreen
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.SgTheme
import ru.sguardo.app.ui.theme.body
import ru.sguardo.app.ui.theme.disp

val EaseM3 = CubicBezierEasing(.05f, .7f, .1f, 1f)
val EaseOut = CubicBezierEasing(.3f, 0f, 1f, 1f)

/** Состояние экранов, которое переживает переходы (фильтры, выбранная вкладка). */
class UiState {
    var tab by mutableStateOf("home")
    var period by mutableStateOf("week")
    var mf by mutableStateOf("all")
    var net by mutableStateOf("all")
    var fold by mutableStateOf("all")
    var q by mutableStateOf("")
    var cs by mutableStateOf("all")
    var kls by mutableStateOf("all")
    var pf by mutableStateOf("all")
    var archShown by mutableStateOf(false)
    var prevTab by mutableIntStateOf(0)
    var scrollTopSignal by mutableIntStateOf(0)
}

data class DialogBtn(val text: String, val danger: Boolean = false, val action: (Boolean) -> Unit)
data class DialogSpec(val title: String, val text: String = "", val check: String? = null, val buttons: List<DialogBtn>)

/** Листы, диалоги и всплывающие слои рисуются в том же окне, поверх экранов. */
class Overlays {
    var sheet by mutableStateOf<(@Composable ColumnScope.() -> Unit)?>(null)
    var sheetKey by mutableIntStateOf(0)
    var dialog by mutableStateOf<DialogSpec?>(null)
    var layer by mutableStateOf<(@Composable () -> Unit)?>(null)
    fun openSheet(content: @Composable ColumnScope.() -> Unit) { sheet = content; sheetKey++ }
    fun closeSheet() { sheet = null }
    fun confirm(d: DialogSpec) { dialog = d }
}

class Nav(val nc: NavHostController?, val ui: UiState) {
    fun go(route: String) { nc?.navigate(route) { launchSingleTop = true } }
    fun back() { nc?.popBackStack() }
    fun chat(id: String) {
        val nc = nc ?: return
        val cur = nc.currentBackStackEntry
        if (cur?.destination?.route == "chat/{id}" && cur.arguments?.getString("id") == id) return
        nc.navigate("chat/$id") { launchSingleTop = true }
    }
    fun tab(t: String) {
        nc?.popBackStack("tabs", inclusive = false)
        if (ui.tab == t) ui.scrollTopSignal++ else ui.tab = t
    }
}

val LocalRepo = staticCompositionLocalOf<Repo> { error("repo") }
val LocalUi = staticCompositionLocalOf { UiState() }
val LocalOv = staticCompositionLocalOf { Overlays() }
val LocalNav = staticCompositionLocalOf { Nav(null, UiState()) }

@Composable
fun SgApp(repo: Repo, permissions: Permissions) {
    val demo by repo.demo.collectAsState()
    val prefs = remember(demo) { repo.prefs }
    val theme by prefs.theme.collectAsState()
    SgTheme(theme) {
        val phase by repo.phase.collectAsState()
        val onboarded by prefs.onboarded.collectAsState()
        var splash by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) { delay(650); splash = false }
        val ui = remember { UiState() }
        val ov = remember { Overlays() }
        CompositionLocalProvider(LocalRepo provides repo, LocalUi provides ui, LocalOv provides ov, LocalPerms provides permissions) {
            Box(Modifier.fillMaxSize().background(Sg.c.surface)) {
                androidx.compose.runtime.key(demo) { when {
                    splash || phase == Phase.Launching -> SplashScreen()
                    !onboarded -> OnboardingScreen()
                    phase == Phase.Login -> LoginScreen()
                    else -> MainNav(ui)
                } }
                OverlayHost(ov)
                ToastHost(repo)
            }
        }
    }
}

/** Разрешения системы: запрос делает активити, экран узнаёт ответ через колбэк. */
interface Permissions {
    fun notifications(done: (Boolean) -> Unit)
    fun contacts(done: (Boolean) -> Unit)
    fun audio(done: (Boolean) -> Unit)
    fun camera(done: (Boolean) -> Unit)
    fun hasContacts(): Boolean
    fun hasNotifications(): Boolean
    fun hasAudio(): Boolean
    fun hasCamera(): Boolean
    fun pickImage(done: (android.net.Uri?) -> Unit)
    fun pickFile(done: (android.net.Uri?) -> Unit)
}

object NoPermissions : Permissions {
    override fun notifications(done: (Boolean) -> Unit) = done(true)
    override fun contacts(done: (Boolean) -> Unit) = done(true)
    override fun audio(done: (Boolean) -> Unit) = done(false)
    override fun camera(done: (Boolean) -> Unit) = done(false)
    override fun hasContacts() = true
    override fun hasNotifications() = true
    override fun hasAudio() = false
    override fun hasCamera() = false
    override fun pickImage(done: (android.net.Uri?) -> Unit) = done(null)
    override fun pickFile(done: (android.net.Uri?) -> Unit) = done(null)
}

val LocalPerms = staticCompositionLocalOf<Permissions> { NoPermissions }

@Composable
fun MainNav(ui: UiState) {
    val nc = rememberNavController()
    val nav = remember(nc) { Nav(nc, ui) }
    val repo = LocalRepo.current
    val open by repo.openChat.collectAsState()
    LaunchedEffect(open) { open?.let { nc.popBackStack("tabs", false); nav.chat(it); repo.openChat.value = null } }
    CompositionLocalProvider(LocalNav provides nav) {
        NavHost(
            nc, startDestination = "tabs",
            enterTransition = { slideInHorizontally(tween(220, easing = EaseM3)) { (it * 0.08f).toInt() } + fadeIn(tween(160)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(160)) },
            popExitTransition = { slideOutHorizontally(tween(180, easing = EaseOut)) { (it * 0.08f).toInt() } + fadeOut(tween(160)) },
        ) {
            composable("tabs") { TabsScreen() }
            composable("chat/{id}") { ChatScreen(it.arguments?.getString("id") ?: "") }
            composable("profile/{id}") { ProfileScreen(it.arguments?.getString("id") ?: "") }
            composable("editc/{id}") { EditContactScreen(it.arguments?.getString("id") ?: "") }
            composable("archive") { ArchiveScreen() }
            composable("notifs") { NotifsScreen() }
            composable("schedule") { ScheduleScreen() }
            composable("folders") { FoldersScreen() }
            composable("lists") { ListsScreen() }
            composable("staff") { StaffScreen() }
            composable("account") { AccountScreen() }
            composable("security") { SecurityScreen() }
        }
    }
}

val TABS = listOf(Triple("home", "Сводка", "grid"), Triple("msgs", "Сообщения", "chat"), Triple("clients", "Клиенты", "users"), Triple("pays", "Оплаты", "card"), Triple("more", "Ещё", "ell"))

@Composable
fun TabsScreen() {
    val ui = LocalUi.current
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        Box(Modifier.weight(1f)) {
            // вкладки переключаются мгновенно: короткое проявление без масштаба, старая вкладка сразу уходит
            androidx.compose.animation.AnimatedContent(ui.tab, transitionSpec = {
                fadeIn(tween(140)) togetherWith fadeOut(tween(0))
            }, label = "tab") { t ->
                when (t) {
                    "home" -> HomeScreen(); "msgs" -> MessagesScreen(); "clients" -> ClientsScreen(); "pays" -> PaysScreen(); else -> MoreScreen()
                }
            }
        }
        BottomNav()
    }
}

/** Нижняя панель: кружок перекатывается под выбранную вкладку (решение владелицы вместо подсветки). */
@Composable
fun BottomNav() {
    val ui = LocalUi.current
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val st by repo.state.collectAsState()
    val unread = st.conversations.filter { !it.archived && !it.muted && !it.trashed }.sumOf { it.unread }
    val idx = TABS.indexOfFirst { it.first == ui.tab }.coerceAtLeast(0)
    val pos = remember { Animatable(idx.toFloat()) }
    val squish = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }
    var from by remember { mutableIntStateOf(idx) }
    LaunchedEffect(idx) {
        if (idx == from) { pos.snapTo(idx.toFloat()); return@LaunchedEffect }
        val f = from; from = idx
        launch { pos.animateTo(idx.toFloat(), tween(560, easing = CubicBezierEasing(.3f, 1.2f, .45f, 1f))) }
        launch { squish.snapTo(0f); squish.animateTo(1f, tween(560)) }
        launch { spin.snapTo(0f); spin.animateTo((idx - f) * 200f, tween(560, easing = CubicBezierEasing(.3f, 1.1f, .45f, 1f))) }
    }
    var w by remember { mutableIntStateOf(0) }
    val d = LocalDensity.current
    val circle = Sg.c.sc
    val dotC = Sg.c.onSc.copy(alpha = .3f)
    Box(Modifier.fillMaxWidth().background(Sg.c.c).windowInsetsPadding(WindowInsets.navigationBars).onSizeChanged { w = it.width }) {
        // кружок
        if (w > 0) {
            val cell = w / 5f
            val k = squish.value
            val sx = if (k < .45f) 1f + .55f * (k / .45f) else 1f + .55f * (1 - (k - .45f) / .55f)
            val sy = if (k < .45f) 1f - .18f * (k / .45f) else 1f - .18f * (1 - (k - .45f) / .55f)
            val r = with(d) { 20.dp.toPx() }
            Box(Modifier.offset { IntOffset((pos.value * cell + cell / 2 - r).toInt(), with(d) { 8.dp.roundToPx() }) }
                .size(40.dp).graphicsLayer { scaleX = sx; scaleY = sy }
                .drawBehind {
                    drawCircle(circle)
                })
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp)) {
            TABS.forEachIndexed { i, (key, label, icon) ->
                val on = i == idx
                Column(Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { nav.tab(key) },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(width = 40.dp, height = 32.dp), contentAlignment = Alignment.Center) {
                        Ic(icon, if (on) Sg.c.onSc else Sg.c.muted)
                        if (key == "msgs" && unread > 0) Badge(unread, Modifier.align(Alignment.TopStart).offset(26.dp, (-4).dp), border = Sg.c.c)
                    }
                    Spacer(Modifier.height(4.dp))
                    T(label, body(12f, if (on) FontWeight.Bold else FontWeight.Medium), if (on) Sg.c.fg else Sg.c.muted, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun OverlayHost(ov: Overlays) {
    // всплывающий слой (меню сообщения, предпросмотр чата)
    ov.layer?.let { it() }
    // лист снизу
    val sheet = ov.sheet
    AnimatedVisibility(sheet != null, enter = fadeIn(tween(300)), exit = fadeOut(tween(260))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .36f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { ov.closeSheet() })
    }
    var last by remember { mutableStateOf<(@Composable ColumnScope.() -> Unit)?>(null) }
    if (sheet != null) last = sheet
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(sheet != null, enter = slideInVertically(tween(420, easing = EaseM3)) { it }, exit = slideOutVertically(tween(280, easing = CubicBezierEasing(.3f, 0f, .8f, .15f))) { it }) {
            val drag = remember { Animatable(0f) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(ov.sheetKey) { drag.snapTo(0f) }
            Column(Modifier.fillMaxWidth().heightIn(max = 700.dp).offset { IntOffset(0, drag.value.toInt()) }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Sg.c.cLow)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .imePadding()) {
                Box(Modifier.fillMaxWidth().height(36.dp).draggable(rememberDraggableState { dy -> scope.launch { drag.snapTo((drag.value + dy).coerceAtLeast(0f)) } }, Orientation.Vertical,
                    onDragStopped = { if (drag.value > 220f) ov.closeSheet() else drag.animateTo(0f) }), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(32.dp, 4.dp).background(Sg.c.outline, RoundedCornerShape(2.dp)))
                }
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 30.dp).windowInsetsPadding(WindowInsets.navigationBars)) {
                    last?.invoke(this)
                }
            }
        }
    }
    if (sheet != null) BackHandler { ov.closeSheet() }
    // диалог Material
    val dlg = ov.dialog
    AnimatedVisibility(dlg != null, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .36f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { ov.dialog = null })
    }
    var lastD by remember { mutableStateOf<DialogSpec?>(null) }
    if (dlg != null) lastD = dlg
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(dlg != null, enter = fadeIn(tween(300, easing = EaseM3)) + scaleIn(tween(300, easing = EaseM3), initialScale = .85f), exit = fadeOut(tween(180))) {
            lastD?.let { DialogCard(it) { ov.dialog = null } }
        }
    }
    if (dlg != null) BackHandler { ov.dialog = null }
}

@Composable
fun DialogCard(d: DialogSpec, close: () -> Unit) {
    var checked by remember(d) { mutableStateOf(false) }
    Column(Modifier.widthIn(max = 312.dp).fillMaxWidth(.85f).clip(RoundedCornerShape(28.dp)).background(Sg.c.cHigh)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 18.dp)) {
        T(d.title, body(22f), modifier = Modifier.padding(bottom = 12.dp))
        if (d.text.isNotBlank()) T(d.text, body(13.5f).copy(lineHeight = 19.sp), Sg.c.muted, modifier = Modifier.padding(bottom = 22.dp))
        if (d.check != null) Row(Modifier.padding(bottom = 18.dp).clickable { checked = !checked }, verticalAlignment = Alignment.CenterVertically) {
            CompositionLocalProvider(androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 0.dp) {
                Checkbox(checked, { checked = it }, colors = CheckboxDefaults.colors(checkedColor = Sg.c.primary, uncheckedColor = Sg.c.outline))
            }
            T(d.check, body(14.5f), modifier = Modifier.padding(start = 10.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            T("Отмена", body(14.5f, FontWeight.SemiBold), Sg.c.primary, modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = close).padding(horizontal = 14.dp, vertical = 10.dp))
            d.buttons.forEach { b ->
                T(b.text, body(14.5f, FontWeight.SemiBold), if (b.danger) Sg.c.danger else Sg.c.primary,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { close(); b.action(checked) }.padding(horizontal = 14.dp, vertical = 10.dp))
            }
        }
    }
}


/** Тост Material (.toast на Android): тёмная полоса снизу, «Отменить» справа. */
@Composable
fun ToastHost(repo: Repo) {
    var cur by remember { mutableStateOf<ToastMsg?>(null) }
    LaunchedEffect(repo) {
        repo.toasts.collect { t -> cur = t; launch { delay(3200); if (cur?.id == t.id) cur = null } }
    }
    Box(Modifier.fillMaxSize().imePadding().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 92.dp), contentAlignment = Alignment.BottomCenter) {
        var shown by remember { mutableStateOf<ToastMsg?>(null) }
        if (cur != null) shown = cur
        AnimatedVisibility(cur != null, enter = fadeIn(tween(400, easing = EaseM3)) + slideInVertically(tween(400, easing = EaseM3)) { it / 3 }, exit = fadeOut(tween(220))) {
            shown?.let { t ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).shadowCompat(4.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp)).background(Sg.c.inv)
                    .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    T(t.text, body(14f, FontWeight.Medium), Sg.c.onInv, modifier = Modifier.weight(1f))
                    if (t.undo != null) T("Отменить", body(14f, FontWeight.Bold), if (Sg.c.dark) Color(0xFF1F4E50) else Sg.c.pc,
                        modifier = Modifier.clickable { cur = null; t.undo.invoke() }.padding(horizontal = 8.dp))
                }
            }
        }
    }
}
