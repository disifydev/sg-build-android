package ru.sguardo.app

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ru.sguardo.app.data.Clock
import ru.sguardo.app.data.Fixture
import ru.sguardo.app.data.Load
import ru.sguardo.app.data.Repo
import ru.sguardo.app.ui.BottomNav
import ru.sguardo.app.ui.LocalNav
import ru.sguardo.app.ui.LocalOv
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.LocalUi
import ru.sguardo.app.ui.Nav
import ru.sguardo.app.ui.OverlayHost
import ru.sguardo.app.ui.Overlays
import ru.sguardo.app.ui.ToastHost
import ru.sguardo.app.ui.UiState
import ru.sguardo.app.ui.screens.AccountScreen
import ru.sguardo.app.ui.screens.ArchiveScreen
import ru.sguardo.app.ui.screens.ChatPreview
import ru.sguardo.app.ui.screens.ChatScreen
import ru.sguardo.app.ui.screens.ClientsScreen
import ru.sguardo.app.ui.screens.EditContactScreen
import ru.sguardo.app.ui.screens.FoldersScreen
import ru.sguardo.app.ui.screens.HomeScreen
import ru.sguardo.app.ui.screens.LessonEditSheet
import ru.sguardo.app.ui.screens.ListsScreen
import ru.sguardo.app.ui.screens.LoginScreen
import ru.sguardo.app.ui.screens.MessagesScreen
import ru.sguardo.app.ui.screens.MoreScreen
import ru.sguardo.app.ui.screens.NewClientSheet
import ru.sguardo.app.ui.screens.NotifsScreen
import ru.sguardo.app.ui.screens.OnboardingScreen
import ru.sguardo.app.ui.screens.PaySheet
import ru.sguardo.app.ui.screens.PaysScreen
import ru.sguardo.app.ui.screens.ProfileScreen
import ru.sguardo.app.ui.screens.ScheduleScreen
import ru.sguardo.app.ui.screens.SecurityScreen
import ru.sguardo.app.ui.screens.SplashScreen
import ru.sguardo.app.ui.screens.StaffScreen
import ru.sguardo.app.ui.screens.askDeleteChat
import ru.sguardo.app.ui.screens.openAttach
import ru.sguardo.app.ui.screens.openLater
import ru.sguardo.app.ui.screens.RecState
import ru.sguardo.app.ui.NoPermissions
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.SgTheme

class TestApp : Application()

/** Снимки экранов в демо-режиме: светлая и тёмная тема, все основные экраны (Roborazzi на JVM, без эмулятора). */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xxhdpi", application = TestApp::class)
class ShotsTest(private val theme: String) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes() = listOf(arrayOf<Any>("light"), arrayOf<Any>("dark"))
    }

    @get:Rule val compose = createComposeRule()
    private lateinit var repo: Repo
    private val ui = UiState()
    private val ov = Overlays()

    @Before fun setUp() {
        if (theme == "dark") RuntimeEnvironment.setQualifiers("+night")
        repo = Repo.demo(ApplicationProvider.getApplicationContext())
        Clock.fixed = Fixture.NOW; Clock.zone = Fixture.ZONE
    }

    private fun shot(name: String, advance: Long = 1500, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SgTheme(theme) {
                CompositionLocalProvider(LocalRepo provides repo, LocalUi provides ui, LocalOv provides ov, LocalNav provides Nav(null, ui)) {
                    Box(Modifier.fillMaxSize().background(Sg.c.surface)) {
                        content()
                        OverlayHost(ov)
                        ToastHost(repo)
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(advance)
        compose.onRoot().captureRoboImage("screenshots/$theme/$name.png")
    }

    @Composable private fun Tab(content: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize()) { Box(Modifier.weight(1f)) { content() }; BottomNav() }
    }

    @Test fun s01_splash() = shot("01_splash") { SplashScreen() }
    @Test fun s02_onboarding_notify() = shot("02_onboarding_notify") { OnboardingScreen("notify") }
    @Test fun s03_onboarding_contacts() = shot("03_onboarding_contacts") { OnboardingScreen("contacts") }
    @Test fun s04_login() = shot("04_login") { LoginScreen() }
    @Test fun s05_home_week() { ui.tab = "home"; shot("05_home_week") { Tab { HomeScreen() } } }
    @Test fun s06_home_today() { ui.tab = "home"; ui.period = "today"; shot("06_home_today") { Tab { HomeScreen() } } }
    @Test fun s07_messages() { ui.tab = "msgs"; shot("07_messages") { Tab { MessagesScreen() } } }
    @Test fun s08_messages_archive() { ui.tab = "msgs"; ui.archShown = true; shot("08_messages_archive_row") { Tab { MessagesScreen() } } }
    @Test fun s09_messages_unread() { ui.tab = "msgs"; ui.mf = "unread"; ui.net = "telegram"; shot("09_messages_filter") { Tab { MessagesScreen() } } }
    @Test fun s10_chat() = shot("10_chat_anna") { ChatScreen("anna") }
    @Test fun s11_chat_preview() { ui.tab = "msgs"; ov.layer = { ChatPreview("maria") { } }; shot("11_chat_preview") { Tab { MessagesScreen() } } }
    @Test fun s12_profile() = shot("12_profile") { ProfileScreen("anna") }
    @Test fun s13_profile_book() = shot("13_profile_whatsapp_book") { ProfileScreen("irina") }
    @Test fun s14_edit_contact() = shot("14_edit_contact") { EditContactScreen("anna") }
    @Test fun s15_clients() { ui.tab = "clients"; shot("15_clients_all") { Tab { ClientsScreen() } } }
    @Test fun s16_clients_folder() { ui.tab = "clients"; ui.cs = "f4"; shot("16_clients_folder") { Tab { ClientsScreen() } } }
    @Test fun s17_pays() { ui.tab = "pays"; shot("17_pays") { Tab { PaysScreen() } } }
    @Test fun s18_more() { ui.tab = "more"; shot("18_more") { Tab { MoreScreen() } } }
    @Test fun s19_account() = shot("19_account") { AccountScreen() }
    @Test fun s20_staff() = shot("20_staff") { StaffScreen() }
    @Test fun s21_lists() = shot("21_classes_subjects") { ListsScreen() }
    @Test fun s22_folders() = shot("22_folders") { FoldersScreen() }
    @Test fun s23_notifs() = shot("23_notifications") { NotifsScreen() }
    @Test fun s24_schedule() = shot("24_schedule") { ScheduleScreen() }
    @Test fun s25_security() = shot("25_security") { SecurityScreen() }
    @Test fun s26_archive() = shot("26_archive") { ArchiveScreen() }
    @Test fun s27_sheet_newclient() { ui.tab = "clients"; ov.openSheet { NewClientSheet() }; shot("27_sheet_new_client") { Tab { ClientsScreen() } } }
    @Test fun s28_sheet_pay() { ui.tab = "pays"; ov.openSheet { PaySheet("anna", "paid") }; shot("28_sheet_pay") { Tab { PaysScreen() } } }
    @Test fun s29_sheet_lesson() { ov.openSheet { LessonEditSheet(Fixture.TODAY, null, "anna") }; shot("29_sheet_lesson") { ScheduleScreen() } }
    @Test fun s30_dialog_delete() { askDeleteChat(repo, ov, "anna"); shot("30_dialog_delete_chat") { ChatScreen("anna") } }
    @Test fun s31_loading() { repo.load.value = Load.Loading; ui.tab = "msgs"; shot("31_state_loading") { Tab { MessagesScreen() } } }
    @Test fun s32_error() { repo.load.value = Load.Error; repo.state.value = repo.state.value.copy(conversations = emptyList()); ui.tab = "msgs"; shot("32_state_error") { Tab { MessagesScreen() } } }
    @Test fun s33_empty() { repo.state.value = repo.state.value.copy(deals = emptyList()); ui.tab = "pays"; shot("33_state_empty") { Tab { PaysScreen() } } }
    @Test fun s34_offline() { repo.online.value = false; ui.tab = "home"; shot("34_state_offline") { Tab { HomeScreen() } } }
    @Test fun s35_chat_maria() = shot("35_chat_maria") { ChatScreen("maria") }
    @Test fun s36_chat_voice() = shot("36_chat_whatsapp_voice") { ChatScreen("irina", initialReply = "m19") }
    @Test fun s37_msg_context() {
        shot("37_message_menu_pre", advance = 800) { ChatScreen("maria") }
        compose.mainClock.autoAdvance = true
        compose.onNodeWithText("А рассрочка есть?").performTouchInput { longClick() }
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(1500)
        compose.onRoot().captureRoboImage("screenshots/$theme/37_message_menu.png")
    }
    @Test fun s38_attach() { openAttach(ov, repo, NoPermissions, ApplicationProvider.getApplicationContext(), "anna") {}; shot("38_sheet_attach") { ChatScreen("anna") } }
    @Test fun s39_later() { openLater(ov, repo, ApplicationProvider.getApplicationContext(), "anna", "Пришлю ссылку", null) {}; shot("39_sheet_send_later") { ChatScreen("anna") } }
    @Test fun s40_rec_voice() = shot("40_recording_voice") { ChatScreen("maria", initialRec = RecState(false, Clock.now() - 3200).also { it.dx = -30f }) }
    @Test fun s41_rec_circle() = shot("41_recording_circle") { ChatScreen("maria", initialRec = RecState(true, Clock.now()).also { it.locked = true }) }
}
