package ru.sguardo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ru.sguardo.app.data.Clock
import ru.sguardo.app.data.Fixture
import ru.sguardo.app.data.Repo
import ru.sguardo.app.ui.BottomNav
import ru.sguardo.app.ui.LocalNav
import ru.sguardo.app.ui.LocalOv
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.LocalUi
import ru.sguardo.app.ui.Nav
import ru.sguardo.app.ui.OverlayHost
import ru.sguardo.app.ui.Overlays
import ru.sguardo.app.ui.UiState
import ru.sguardo.app.ui.screens.ChatScreen
import ru.sguardo.app.ui.screens.HomeScreen
import ru.sguardo.app.ui.screens.MessagesScreen
import ru.sguardo.app.ui.screens.PaysScreen
import ru.sguardo.app.ui.screens.ProfileScreen
import ru.sguardo.app.ui.screens.RecState
import ru.sguardo.app.ui.screens.ScheduleScreen
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.SgTheme
import ru.sguardo.app.ui.theme.Unbounded

/** Промо-кадры для Google Play: 1080×1920, кремовый фон, заголовок Unbounded, экран приложения со скруглёнными углами. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h696dp-xxhdpi"  // минус 56dp системных панелей = 640dp = 1920 px, application = TestApp::class)
class StoreShotsTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var repo: Repo
    private val ui = UiState()
    private val ov = Overlays()

    @Before fun setUp() {
        repo = Repo.demo(ApplicationProvider.getApplicationContext())
        Clock.fixed = Fixture.NOW; Clock.zone = Fixture.ZONE
    }

    private val scale = .78f

    private fun promo(title: String, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFFECE6DC))) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, fontFamily = Unbounded, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 31.sp, color = Color(0xFF14211F),
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 30.dp))
                    Spacer(Modifier.height(22.dp))
                    val shape = RoundedCornerShape(24.dp)
                    Box(Modifier.shadow(20.dp, shape, ambientColor = Color(0x5514211F), spotColor = Color(0x5514211F)).clip(shape)
                        .border(1.dp, Color(0x2214211F), shape)
                        .layout { m, _ ->
                            val p = m.measure(Constraints.fixed(360.dp.roundToPx(), 660.dp.roundToPx()))
                            layout((p.width * scale).toInt(), (p.height * scale).toInt()) {
                                p.placeWithLayer(0, 0) { scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(0f, 0f) }
                            }
                        }) {
                        SgTheme("light") {
                            CompositionLocalProvider(LocalRepo provides repo, LocalUi provides ui, LocalOv provides ov, LocalNav provides Nav(null, ui)) {
                                Box(Modifier.fillMaxSize().background(Sg.c.surface)) { content(); OverlayHost(ov) }
                            }
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1500)
    }

    private fun save(name: String) = compose.onRoot().captureRoboImage("screenshots/play/$name.png")

    @Composable private fun Tab(content: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize()) { Box(Modifier.weight(1f)) { content() }; BottomNav() }
    }

    @Test fun p1_messages() { ui.tab = "msgs"; promo("Все переписки\nв одном окне") { Tab { MessagesScreen() } }; save("01_messages") }

    @Test fun p2_chat() {
        promo("Отвечайте\nкак в Telegram") { ChatScreen("maria") }
        compose.mainClock.autoAdvance = true
        compose.onNodeWithText("А рассрочка есть?").performTouchInput { longClick() }
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(1500)
        save("02_chat")
    }

    @Test fun p3_profile() { promo("Карточка клиента\nвсегда под рукой") { ProfileScreen("anna") }; save("03_profile") }
    @Test fun p4_pays() { ui.tab = "pays"; promo("Кто оплатил,\nа кто обещал") { Tab { PaysScreen() } }; save("04_pays") }
    @Test fun p5_schedule() { promo("Расписание\nзанятий") { ScheduleScreen() }; save("05_schedule") }
    @Test fun p6_home() { ui.tab = "home"; ui.period = "today"; promo("Сводка за день\nи неделю") { Tab { HomeScreen() } }; save("06_home") }
    @Test fun p7_voice() {
        promo("Голосовые\nи кружки") { ChatScreen("anna", initialRec = RecState(false, System.currentTimeMillis() - 4300).also { it.dx = -24f }) }
        save("07_voice")
    }
}
