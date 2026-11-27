package ru.sguardo.app.ui.screens

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sguardo.app.ui.Chev
import ru.sguardo.app.ui.Grp
import ru.sguardo.app.ui.Gi
import ru.sguardo.app.ui.Hint
import ru.sguardo.app.ui.LocalNav
import ru.sguardo.app.ui.LocalOv
import ru.sguardo.app.ui.LocalRepo
import ru.sguardo.app.ui.Radio
import ru.sguardo.app.ui.Sect
import ru.sguardo.app.ui.SgSwitch
import ru.sguardo.app.ui.SheetTitle
import ru.sguardo.app.ui.SubTopBar
import ru.sguardo.app.ui.T
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body

/** Оформление чата: цвет своих сообщений, углы, размер текста, анимации. Действует на все пузыри приложения. */
data class ChatStyle(val out: Color?, val outFg: Color?, val radius: Dp, val text: Float, val anim: Boolean)
val LocalChatStyle = compositionLocalOf { ChatStyle(null, null, 18.dp, 1f, true) }

/** Цветовые темы, как лента тем в Telegram: обои + цвет своих сообщений (светлая и тёмная). */
data class ChatTheme(val key: String, val name: String, val icon: String, val wall: String, val light: Pair<Long, Long>?, val dark: Pair<Long, Long>?)
val CHAT_THEMES = listOf(
    ChatTheme("classic", "Классика", "book", "school", null, null),
    ChatTheme("lilac", "Сирень", "heart", "lilac", 0xFFC2B2F7 to 0xFF1D1340, 0xFF6E58C9 to 0xFFFFFFFF),
    ChatTheme("mint", "Мята", "leaf", "mint", 0xFFAEE3CB to 0xFF0F2A20, 0xFF2F7A5E to 0xFFFFFFFF),
    ChatTheme("peach", "Персик", "sun", "peach", 0xFFF8C7AD to 0xFF3A1D10, 0xFFB8643E to 0xFFFFFFFF),
    ChatTheme("sky", "Небо", "cloud", "sky", 0xFFB0D4F6 to 0xFF0F2238, 0xFF3D6FA8 to 0xFFFFFFFF),
    ChatTheme("night", "Ночь", "moon", "night", 0xFF8F7CF0 to 0xFFFFFFFF, 0xFF6A58D6 to 0xFFFFFFFF),
)

@Composable
fun rememberChatStyle(): ChatStyle {
    val repo = LocalRepo.current
    val th by repo.prefs.chatTheme.collectAsState()
    val r by repo.prefs.bubbleRadius.collectAsState()
    val ts by repo.prefs.textScale.collectAsState()
    val an by repo.prefs.animations.collectAsState()
    val t = CHAT_THEMES.firstOrNull { it.key == th }
    val pair = if (Sg.c.dark) t?.dark else t?.light
    return ChatStyle(pair?.let { Color(it.first) }, pair?.let { Color(it.second) }, r.dp, ts, an)
}

@Composable fun chatOut(): Color = LocalChatStyle.current.out ?: Sg.c.pc
@Composable fun chatOutFg(): Color = LocalChatStyle.current.outFg ?: Sg.c.onPc

/** «Ещё → Оформление», как в Telegram: предпросмотр, лента цветовых тем, обои, тема, текст, углы, анимации. */
@Composable
fun AppearanceScreen() {
    val repo = LocalRepo.current; val nav = LocalNav.current; val ov = LocalOv.current
    val cur by repo.prefs.chatTheme.collectAsState()
    val theme by repo.prefs.theme.collectAsState()
    val ts by repo.prefs.textScale.collectAsState()
    val rad by repo.prefs.bubbleRadius.collectAsState()
    val anim by repo.prefs.animations.collectAsState()
    Column(Modifier.fillMaxSize().background(Sg.c.surface)) {
        SubTopBar("Оформление", { nav.back() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Sect("Цветовая тема", top = 6.dp)
            Grp {
                Box(Modifier.fillMaxWidth().height(190.dp)) {
                    ChatWallpaper(Modifier.fillMaxSize())
                    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                        PreviewBubble("Добрый день! Можно перенести занятие на четверг?", false)
                        PreviewBubble("Да, давайте в 17:00", true)
                    }
                }
                LazyRow(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                    items(CHAT_THEMES, key = { it.key }) { t ->
                        val on = t.key == cur
                        val pair = if (Sg.c.dark) t.dark else t.light
                        Box(Modifier.size(92.dp, 124.dp).clip(RoundedCornerShape(16.dp))
                            .border(if (on) 2.5.dp else 0.dp, if (on) Sg.c.primary else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { repo.prefs.setChatTheme(t.key); repo.prefs.setChatWall(t.wall) }) {
                            ChatWallpaper(Modifier.fillMaxSize(), key = t.wall)
                            Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.align(Alignment.End).size(52.dp, 18.dp).clip(RoundedCornerShape(9.dp))
                                    .background(Brush.horizontalGradient(listOf(pair?.let { Color(it.first) } ?: Sg.c.pc, (pair?.let { Color(it.first) } ?: Sg.c.pc).copy(alpha = .75f)))))
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.align(Alignment.Start).size(46.dp, 18.dp).clip(RoundedCornerShape(9.dp)).background(Sg.c.cHigh))
                                Spacer(Modifier.weight(1f))
                                // значок темы вместо эмодзи: белый кружок и иконка цвета темы
                                val accent = pair?.let { Color(it.first) } ?: Sg.c.primary
                                Box(Modifier.size(34.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.White.copy(alpha = if (Sg.c.dark) .14f else .85f)), contentAlignment = Alignment.Center) {
                                    ru.sguardo.app.ui.Ic(t.icon, if (Sg.c.dark) Color.White else androidx.compose.ui.graphics.lerp(accent, Color.Black, .35f), size = 18.dp)
                                }
                                Spacer(Modifier.height(2.dp))
                                T(t.name, body(11.5f, FontWeight.Medium), if (t.key == "night") Color.White.copy(alpha = .9f) else Sg.c.fg.copy(alpha = .8f))
                            }
                        }
                    }
                }
                Gi("Обои для чатов", onClick = { nav.go("chatstyle") }) { Chev() }
                Gi("Тема", value = when (theme) { "light" -> "Светлая"; "dark" -> "Тёмная"; else -> "Как в системе" }, onClick = {
                    ov.openSheet {
                        SheetTitle("Тема")
                        Grp(Modifier.padding(bottom = 8.dp)) {
                            listOf("system" to "Как в системе", "light" to "Светлая", "dark" to "Тёмная").forEach { (k, t) ->
                                Gi(t, leading = { Radio(repo.prefs.theme.collectAsState().value == k) }, onClick = { repo.prefs.setTheme(k); ov.closeSheet() })
                            }
                        }
                    }
                }) { Chev() }
            }
            Spacer(Modifier.height(18.dp))
            Grp {
                Gi("Размер текста", value = when { ts < .95f -> "Мелкий"; ts > 1.05f -> "Крупный"; else -> "Обычный" }, onClick = {
                    ov.openSheet {
                        SheetTitle("Размер текста в чатах")
                        Grp(Modifier.padding(bottom = 8.dp)) {
                            listOf(.9f to "Мелкий", 1f to "Обычный", 1.12f to "Крупный").forEach { (k, t) ->
                                Gi(t, leading = { Radio(kotlin.math.abs(repo.prefs.textScale.collectAsState().value - k) < .01f) }, onClick = { repo.prefs.setTextScale(k); ov.closeSheet() })
                            }
                        }
                    }
                }) { Chev() }
                Gi("Углы сообщений", value = when { rad >= 18 -> "Круглые"; rad >= 12 -> "Средние"; else -> "Острые" }, onClick = {
                    ov.openSheet {
                        SheetTitle("Углы сообщений")
                        Grp(Modifier.padding(bottom = 8.dp)) {
                            listOf(18 to "Круглые", 12 to "Средние", 7 to "Острые").forEach { (k, t) ->
                                Gi(t, leading = { Radio(repo.prefs.bubbleRadius.collectAsState().value == k) }, onClick = { repo.prefs.setBubbleRadius(k); ov.closeSheet() })
                            }
                        }
                    }
                }) { Chev() }
                Gi("Анимации", onClick = { repo.prefs.setAnimations(!anim) }) { SgSwitch(anim) { repo.prefs.setAnimations(it) } }
            }
            Hint("Анимации: удаление «пылью» и плавное появление новых сообщений.")
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PreviewBubble(text: String, out: Boolean) {
    val st = LocalChatStyle.current
    val shape = bubbleShape(out, top = false, bottom = false, big = st.radius)
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = if (out) Arrangement.End else Arrangement.Start) {
        Box(Modifier.widthIn(max = 240.dp).bubbleBg(if (out) chatOut() else Sg.c.cHigh, out, shape, true).clip(shape).padding(horizontal = 12.dp, vertical = 8.dp)) {
            T(text, body(14.5f * st.text), if (out) chatOutFg() else Sg.c.fg)
        }
    }
}
