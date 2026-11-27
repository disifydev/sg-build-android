package ru.sguardo.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import ru.sguardo.app.data.AppState
import ru.sguardo.app.data.Clock
import ru.sguardo.app.data.Contact
import ru.sguardo.app.data.Conversation
import ru.sguardo.app.data.Folder
import ru.sguardo.app.data.Repo
import ru.sguardo.app.data.parseIso
import ru.sguardo.app.ui.theme.SgColors

/** Палитра выбора цвета папок и предметов (PALF макета). */
val PALF = listOf("#9CC3D3", "#B9A9DE", "#E2C06A", "#E9A37E", "#86C7A6", "#E59AA0", "#7FB3E8", "#C7B299", "#A3C97A", "#D68FD6", "#8FA3B8", "#F0A35E")

private val NAMED = mapOf(
    "blue" to "#9CC3D3", "violet" to "#B9A9DE", "ochre" to "#E2C06A", "orange" to "#E9A37E", "green" to "#86C7A6",
    "teal" to "#A4D2CD", "red" to "#EBB0AC", "gray" to "#DDD3C3", "pink" to "#EBBCCB", "lime" to "#CFE0A6", "indigo" to "#BCC3E6",
    "coral" to "#F0AE98", "plum" to "#D6B3D1", "sea" to "#9FCFC0", "butter" to "#F1E1A0", "wine" to "#D49A9E", "teal2" to "#8FBFC2",
    "stone" to "#CFCAC2", "sky2" to "#A9C9EE", "sage" to "#BFCDB4",
)

fun hexColor(s: String?): Color {
    val h = (s ?: "#9CC3D3").removePrefix("#")
    return try { Color(("FF" + h.padStart(6, '0')).toLong(16)) } catch (_: Exception) { Color(0xFF9CC3D3) }
}

/** Насыщенная точка папки. */
fun folderDot(f: Folder?): Color = hexColor(NAMED[f?.color] ?: f?.color)
fun folderHex(f: Folder?): String = NAMED[f?.color] ?: f?.color ?: "#9CC3D3"

/** Светлая подложка папки (вкладка, плашка) с учётом темы. */
fun folderTint(f: Folder?, c: SgColors): Color = when (f?.color) {
    "blue" -> c.tBlue; "violet" -> c.tLilac; "ochre" -> c.tYellow; "orange" -> c.tPeach; "green" -> c.tGreen
    else -> lerp(c.surface, folderDot(f), if (c.dark) 0.35f else 0.45f)
}

const val CLASS_P = "класс:"
fun classTitle(n: String) = if (n.matches(Regex("^\\d{1,2}$"))) "$n класс" else n
fun classKey(title: String) = Regex("^(\\d{1,2}) класс$").find(title)?.groupValues?.get(1) ?: title
fun Contact.classes() = tags.orEmpty().filter { it.startsWith(CLASS_P) }.map { classTitle(it.removePrefix(CLASS_P)) }
const val GROUP_P = "группа:"
fun Contact.groups() = tags.orEmpty().filter { it.startsWith(GROUP_P) }.map { it.removePrefix(GROUP_P) }
/** Фильтр «класс или группа»: значение фильтра — название класса или «группа:Имя». */
fun Contact.inKls(k: String) = k == "all" || (if (k.startsWith(GROUP_P)) k.removePrefix(GROUP_P) in groups() else k in classes())
fun klsTitle(k: String) = if (k.startsWith(GROUP_P)) k.removePrefix(GROUP_P) else k
fun allGroups(r: Repo, s: AppState): List<String> = (s.contacts.flatMap { it.groups() } + r.prefs.extraGroups.value).distinct().sorted()
fun Contact.plainTags() = tags.orEmpty().filter { !it.startsWith(CLASS_P) && !it.startsWith("группа:") }

/** Классы кабинета: из меток клиентов плюс созданные в приложении без учеников. */
fun allClasses(r: Repo, s: AppState): List<String> {
    val fromTags = s.contacts.flatMap { it.classes() }
    val all = (fromTags + r.prefs.extraClasses.value).distinct()
    return all.sortedWith(compareBy({ Regex("^\\d+").find(it)?.value?.toInt() ?: 999 }, { it }))
}

fun visibleConvs(s: AppState) = s.conversations.filter { !it.trashed }

fun lastTime(c: Conversation) = c.last?.time ?: 0L

/** Последняя активность клиента: последнее сообщение или первое обращение. */
fun contactTime(s: AppState, c: Contact): Long =
    s.conversations.filter { it.contactId == c.id }.maxOfOrNull { lastTime(it) }?.takeIf { it > 0 } ?: parseIso(c.firstContactAt)

fun dayStart(daysBack: Int): Long = Clock.startOfDay(Clock.today().minusDays(daysBack.toLong()))

data class Stats(val leads: Int, val paidCount: Int, val revenue: Long, val waiting: Int, val remind: Int)

fun stats(s: AppState, period: String): Stats {
    val from = when (period) { "today" -> dayStart(0); "week" -> dayStart(6); else -> dayStart(29) }
    val leads = s.events.count { it.type == "lead_created" && parseIso(it.at) >= from }
    val paid = s.deals.filter { it.status == "paid" && parseIso(it.date) >= from }
    val waiting = visibleConvs(s).count { !it.archived && it.last?.isOut == false }
    val endToday = dayStart(-1)
    val remind = s.reminders.count { !it.done && parseIso(it.at) < endToday }
    return Stats(leads, paid.size, paid.sumOf { it.amount }, waiting, remind)
}

/** Входящие по дням за 14 дней: события message_in, а если их нет — входящие сообщения. */
fun inboxByDay(s: AppState): List<Pair<Int, Int>> {
    val today = Clock.today()
    val stamps = s.events.filter { it.type == "message_in" }.map { parseIso(it.at) }.ifEmpty {
        s.conversations.flatMap { c -> c.messages.filter { !it.isOut }.map { it.time } }
    }
    return (13 downTo 0).map { back ->
        val d = today.minusDays(back.toLong())
        val a = Clock.startOfDay(d); val b = Clock.startOfDay(d.plusDays(1))
        d.dayOfMonth to stamps.count { it in a until b }
    }
}
