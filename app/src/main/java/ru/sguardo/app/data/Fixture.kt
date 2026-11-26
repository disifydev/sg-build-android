package ru.sguardo.app.data

import java.time.LocalDate
import java.time.ZoneId

/** Демо-кабинет: данные-примеры из макета (CHATS, CLIENTS, PAYS, TODO, LESSONS…). Без сети, только для показа и снимков. */
object Fixture {
    val ZONE: ZoneId = ZoneId.of("Europe/Moscow")
    val TODAY: LocalDate = LocalDate.of(2026, 11, 27)
    val NOW: Long = TODAY.atTime(16, 20).atZone(ZONE).toInstant().toEpochMilli()

    val replies = listOf("Спасибо! Сейчас посмотрю", "Отлично, договорились", "А можно в четверг?", "Поняла, жду ссылку")
    val templates = listOf("Пришлю ссылку на оплату", "Свободные окна: чт 18:30, сб 12:00", "Спасибо, записала вас", "Рассрочка есть: три платежа без переплаты")
    val sizes = mapOf("Домашка_геометрия.pdf" to "1,2 МБ", "Счёт_27_ноября.pdf" to "214 КБ")
    val phoneBook = mapOf("79162041833" to "Ирина Белова", "79037715209" to "Ольга Никитина")

    private fun day(label: String): LocalDate = when (label) {
        "Сегодня" -> TODAY; "Вчера" -> TODAY.minusDays(1); "Пн" -> LocalDate.of(2026, 11, 23); "Вс" -> LocalDate.of(2026, 11, 22)
        else -> { val (d, _) = label.split(" "); LocalDate.of(2026, 11, d.toInt()) }
    }
    private fun at(d: LocalDate, hm: String): String { val (h, m) = hm.split(":").map { it.toInt() }; return Clock.isoLocal(d.atTime(h, m).atZone(ZONE).toInstant().toEpochMilli()) }

    private data class M(val o: Int, val t: String = "", val at: String, val v: Int = 0, val img: Boolean = false, val cv: Int = 0, val file: String? = null, val rx: String? = null)
    private data class Ch(val id: String, val n: String, val h: String?, val ph: String?, val net: String, val day: String, val unread: Int, val folder: Int, val pin: Boolean = false, val mute: Boolean = false, val arch: Boolean = false, val msgs: List<M>)

    private val CHATS = listOf(
        Ch("anna", "Анна Соколова", "@anna.sokolova", null, "telegram", "Сегодня", 1, 1, pin = true, msgs = listOf(
            M(0, "Здравствуйте! Увидела у вас в профиле разбор, хочу так же для своего аккаунта", "15:52"),
            M(1, "Добрый день! Да, конечно. Разбор профиля стоит 5 000 ₽, делаю за два дня", "15:58", rx = "❤️"),
            M(0, at = "16:01", v = 14), M(0, "Вот тетрадь с последней темой", "16:03", img = true),
            M(1, "Возьмите её на занятие. Посмотрим, на каком шаге стало непонятно", "16:07"),
            M(1, at = "16:09", cv = 11), M(0, "Отлично, а когда можно начать?", "16:13"))),
        Ch("maria", "Мария Козлова", "@mkozlova", null, "telegram", "Сегодня", 3, 1, msgs = listOf(
            M(1, "Мария, добрый день! Курс «Старт» стартует 1 декабря, осталось три места", "15:20"),
            M(0, "Здравствуйте! Звучит интересно", "15:38"), M(0, "Сколько стоит?", "15:40"), M(0, "А рассрочка есть?", "15:41"))),
        Ch("maxim", "Максим Орлов", "max.orlov", null, "instagram", "Сегодня", 2, 0, msgs = listOf(
            M(0, "Добрый день, подскажите, у вас есть групповые занятия?", "13:50"), M(0, "Для 9 класса", "13:51"))),
        Ch("ivan", "Иван Громов", "@grom", null, "telegram", "Сегодня", 0, 1, msgs = listOf(
            M(0, at = "12:09", file = "Домашка_геометрия.pdf"), M(0, "Скинул домашку, посмотрите?", "12:10"), M(1, "Получила, посмотрю сегодня вечером", "12:27"))),
        Ch("irina", "+7 916 204-18-33", null, "+7 916 204-18-33", "whatsapp", "Сегодня", 3, 0, msgs = listOf(
            M(0, "Привет!", "12:19"), M(0, at = "12:20", v = 9), M(0, "Хочу заказать разбор профиля", "12:21"))),
        Ch("dmitry", "Дмитрий Кузнецов", "dkuz", null, "tiktok", "Сегодня", 1, 0, msgs = listOf(M(0, "Здравствуйте, это актуально? Хотел бы попробовать", "10:51"))),
        Ch("artem", "Артём Волков", "artem.v", null, "threads", "Сегодня", 3, 0, msgs = listOf(M(0, "Классный пост!", "07:50"), M(0, "А можно записаться на пробное?", "07:51"))),
        Ch("natalia", "Наталья Фёдорова", "id48211", null, "vk", "Вчера", 2, 1, msgs = listOf(M(1, "Когда вам удобно на следующей неделе?", "18:02"), M(0, "Лучше в четверг вечером", "19:40"))),
        Ch("olga", "+7 903 771-52-09", null, "+7 903 771-52-09", "max", "Вчера", 0, 1, msgs = listOf(M(0, "Тетрадь ещё продаётся?", "14:12"), M(1, "Да, ещё продаётся", "14:30"))),
        Ch("egor", "Егор Жуков", "Егор", null, "avito", "Пн", 0, 4, mute = true, msgs = listOf(M(1, "Ссылка на оплату: разбор профиля, 5 000 ₽", "11:02"), M(0, "Спасибо, оплатил", "11:40"))),
        Ch("nikolay", "Николай Попов", "@npopov", null, "telegram", "Вс", 0, 3, msgs = listOf(M(1, "Николай, напоминаю про оплату консультации", "10:00"), M(0, "Да, помню, на неделе переведу", "10:41"))),
        Ch("ksenia", "Ксения Лаврова", "@ksu_l", null, "telegram", "12 нояб", 0, 4, arch = true, msgs = listOf(M(0, "Спасибо за занятия, курс закончила!", "18:20"), M(1, "Рада была помочь, приходите ещё", "18:31"))),
        Ch("shop", "Типография «Лист»", "avito", null, "avito", "3 нояб", 0, 0, mute = true, arch = true, msgs = listOf(M(0, "Тетради готовы, можно забирать", "11:00"))),
    )

    private data class Cl(val n: String, val net: String, val t: String, val tags: List<String> = emptyList(), val rem: String? = null)
    private val CLIENTS = listOf(
        listOf(Cl("Максим Орлов", "instagram", "13:51"), Cl("Ирина Белова", "whatsapp", "12:21"), Cl("Дмитрий Кузнецов", "tiktok", "10:51"), Cl("Артём Волков", "threads", "07:51"),
            Cl("Елена Морозова", "instagram", "Вчера"), Cl("Софья Кузнецова", "telegram", "18 нояб", listOf("Математика", "Демо")), Cl("Никита Попов", "whatsapp", "18 нояб", listOf("Физика", "Демо"))),
        listOf(Cl("Анна Соколова", "telegram", "16:13"), Cl("Мария Козлова", "telegram", "15:41", rem = "25 нояб"), Cl("Иван Громов", "telegram", "12:27"), Cl("Наталья Фёдорова", "vk", "Вчера"),
            Cl("Ольга Никитина", "max", "Вчера"), Cl("Сергей Лебедев", "instagram", "24 нояб"), Cl("Павел Новиков", "tiktok", "23 нояб")),
        listOf(Cl("Алина Захарова", "instagram", "23 нояб", rem = "26 нояб"), Cl("Юлия Мельникова", "threads", "23 нояб"), Cl("Кирилл Тихонов", "telegram", "22 нояб", rem = "30 нояб"),
            Cl("Дарья Крылова", "telegram", "21 нояб"), Cl("Роман Беляев", "instagram", "17 нояб")),
        listOf(Cl("Николай Попов", "telegram", "21 нояб", rem = "25 нояб"), Cl("Андрей Семёнов", "telegram", "20 нояб", rem = "26 нояб"), Cl("Полина Ковалёва", "tiktok", "19 нояб", rem = "26 нояб"),
            Cl("Мария Павлова", "telegram", "18 нояб", listOf("Математика", "Демо"))),
        listOf(Cl("Иван Иванов", "telegram", "19 нояб", listOf("Математика", "Демо", "1 800 ₽")), Cl("Полина Фёдорова", "telegram", "18 нояб", listOf("Математика", "Демо")),
            Cl("Степан Петров", "whatsapp", "18 нояб", listOf("Физика", "Демо")), Cl("Ева Богданова", "telegram", "17 нояб", listOf("Математика")), Cl("Ярослав Беляев", "whatsapp", "16 нояб", listOf("Физика"))),
    )
    private val KLS = listOf("5", "7", "9", "11")
    private val FOLDERS = listOf("Новые" to "blue", "В диалоге" to "violet", "Думает" to "ochre", "Оплатит позже" to "orange", "Оплатил" to "green")
    private val PRODUCTS = listOf("Консультация 60 мин" to 3500L, "Разбор профиля" to 5000L, "Курс «Старт»" to 12900L, "Менторство 30 дней" to 24000L, "Математика · 60 мин" to 1800L, "Физика · 60 мин" to 2000L)
    private data class P(val n: String, val p: String, val d: String, val a: Long, val s: String, val w: String? = null)
    private val PAYS = listOf(
        P("Егор Жуков", "Разбор профиля", "27 нояб", 5000, "paid"), P("Екатерина Руденко", "Шаблоны Notion", "26 нояб", 1500, "paid"),
        P("Светлана Дроздова", "Консультация 60 мин", "25 нояб", 3500, "paid"), P("Андрей Семёнов", "Консультация 60 мин", "24 нояб", 3500, "late", "26 нояб"),
        P("Виктория Ершова", "Курс «Старт»", "24 нояб", 12900, "prom", "28 нояб"), P("Николай Попов", "Консультация 60 мин", "24 нояб", 3500, "late", "25 нояб"),
        P("Полина Ковалёва", "Разбор профиля", "24 нояб", 5000, "late", "26 нояб"), P("Алина Захарова", "Менторство 30 дней", "23 нояб", 24000, "prom", "1 дек"),
        P("Мария Павлова", "Менторство 30 дней", "21 нояб", 24000, "paid"), P("Анна Соколова", "Курс «Старт»", "20 нояб", 12900, "paid"),
        P("Иван Иванов", "Математика · 60 мин", "19 нояб", 1800, "paid"), P("Степан Петров", "Физика · 60 мин", "18 нояб", 2000, "late", "23 нояб"),
    )
    private data class T(val chat: String, val text: String, val due: String, val late: Boolean)
    private val TODO = listOf(T("anna", "Уточнить оплату до занятия", "20 нояб", true), T("maria", "Прислать рассрочку", "25 нояб", true), T("nikolay", "Напомнить об оплате", "25 нояб", true),
        T("artem", "Записать на пробное", "16:00", false), T("natalia", "Предложить четверг 18:30", "17:21", false))

    private fun dateOf(label: String): LocalDate = when {
        label.contains(":") -> TODAY
        label == "Вчера" -> TODAY.minusDays(1)
        label == "1 дек" -> LocalDate.of(2026, 12, 1)
        else -> LocalDate.of(2026, 11, label.split(" ")[0].toInt())
    }
    private fun iso(label: String, hm: String = "12:00"): String = at(dateOf(label), if (label.contains(":")) label else hm)

    fun apply(r: Repo) {
        val folders = FOLDERS.mapIndexed { i, (n, c) -> Folder("f$i", listOf("new", "talking", "thinking", "later", "paid")[i], n, c) }
        val contacts = mutableListOf<Contact>()
        val byName = HashMap<String, String>()
        fun addContact(c: Contact) { contacts += c; byName[c.name] = c.id }
        val chatName = mapOf("Ирина Белова" to "irina", "Ольга Никитина" to "olga")
        // клиенты по папкам
        CLIENTS.forEachIndexed { i, list ->
            list.forEachIndexed { j, cl ->
                val chatId = CHATS.firstOrNull { it.n == cl.n }?.id ?: chatName[cl.n]
                val ch = CHATS.firstOrNull { it.id == chatId }
                val tags = listOf("класс:" + KLS[(i * 3 + j) % 4]) + cl.tags
                val id = chatId ?: "c${i}_$j"
                addContact(Contact(id, ch?.n ?: cl.n, handle = ch?.h, source = cl.net, networks = listOf(cl.net), folderId = "f$i", phone = ch?.ph,
                    tags = tags, assignee = "Виктория", firstContactAt = iso(cl.t)))
                if (ch?.ph != null) byName[cl.n] = id
            }
        }
        // чаты без карточки в списке клиентов
        CHATS.filter { ch -> contacts.none { it.id == ch.id } }.forEach { ch ->
            addContact(Contact(ch.id, ch.n, handle = ch.h, source = ch.net, networks = listOf(ch.net), folderId = "f${ch.folder}", phone = ch.ph, assignee = "Виктория", firstContactAt = iso("1 нояб")))
        }
        PAYS.map { it.n }.distinct().filter { it !in byName }.forEach { n ->
            addContact(Contact("p_" + n.hashCode().toUInt(), n, source = "telegram", networks = listOf("telegram"), folderId = if (n == "Виктория Ершова") "f3" else "f4", assignee = "Виктория", firstContactAt = iso("20 нояб")))
        }
        var mid = 0
        val convs = CHATS.map { ch ->
            val d = day(ch.day)
            val meta = mutableMapOf<String, String>()
            if (ch.pin) meta["pinned"] = iso("26 нояб")
            if (ch.mute) meta["muted"] = iso("20 нояб")
            if (ch.arch) meta["archived"] = iso("20 нояб")
            Conversation(ch.id, ch.id, ch.net, ch.msgs.map { m ->
                val att = when {
                    m.v > 0 -> Attachment("audio", "voice.ogg", "", duration = m.v.toDouble())
                    m.img -> Attachment("image", "demo:note", "")
                    m.cv > 0 -> Attachment("video", "circle.mp4", "", round = true, duration = m.cv.toDouble())
                    m.file != null -> Attachment("file", m.file, "", mime = "application/pdf")
                    else -> null
                }
                Message("m${++mid}", if (m.o == 1) "out" else "in", m.t, at(d, m.at), attachments = att?.let { listOf(it) },
                    reactions = m.rx?.let { listOf(Reaction(it, false)) })
            }, unread = ch.unread, meta = meta)
        }
        val products = PRODUCTS.mapIndexed { i, (n, p) -> Product("pr$i", n, p) } + Product("pr9", "Шаблоны Notion", 1500)
        val deals = PAYS.mapIndexed { i, p ->
            Deal("d$i", byName[p.n]!!, products.firstOrNull { it.name == p.p }?.id, p.a, if (p.s == "paid") "paid" else "promised", iso(p.d), p.w?.let { iso(it, "10:00") })
        }
        val reminders = mutableListOf<Reminder>()
        TODO.forEachIndexed { i, t -> reminders += Reminder("rt$i", t.chat, iso(t.due, "10:00"), t.text) }
        CLIENTS.flatten().filter { it.rem != null && it.n !in listOf("Мария Козлова", "Николай Попов") }.forEachIndexed { i, cl ->
            reminders += Reminder("rc$i", byName[cl.n]!!, iso(cl.rem!!, "10:00"), "Напомнить об оплате")
        }
        val events = mutableListOf<AppEvent>()
        val bars = listOf(0, 2, 2, 2, 2, 2, 2, 4, 2, 6, 2, 5, 9, 4)
        bars.forEachIndexed { i, n -> val dd = TODAY.minusDays((13 - i).toLong()); repeat(n) { k -> events += AppEvent("ei$i$k", "message_in", "x", at(dd, "1${k % 10}:00")) } }
        // лиды: сегодня 3, за неделю 18, за месяц 64
        repeat(64) { k ->
            val back = when { k < 3 -> 0L; k < 18 -> 1L + (k % 6); else -> 7L + (k % 23) }
            events += AppEvent("el$k", "lead_created", "x", at(TODAY.minusDays(back), if (back == 0L) "0${8 + k}:30" else "12:00"))
        }
        val team = listOf(TeamMember("me", "Виктория", "owner", "demo@sguardo.ru"), TeamMember("u2", "Анна Морозова", "agent", "anna@example.com"))
        r.state.value = AppState(WorkspaceInfo("RUB", false), emptyList(), folders, contacts, convs, products, deals, reminders, events,
            templates.mapIndexed { i, t -> Template("t$i", t.take(20), t) }, team)
        r.user.value = User("me", "w1", "demo@sguardo.ru", "Виктория", "owner")
        r.account.value = AccountInfo("Виктория", "demo@sguardo.ru", "owner", "pro", "active", "Демо-кабинет", sessions = 2)
        r.history.value = emptyMap(); r.pending.value = emptyMap(); r.scheduled.value = emptyMap()
        // занятия недели 23–29 ноября
        val L = mapOf(
            27 to listOf(listOf("10:00", "60", "Математика", "Анна Соколова"), listOf("12:30", "90", "Мини-группа · физика", "4 ученика"), listOf("16:00", "30", "Пробное занятие", "Артём Волков"), listOf("18:30", "60", "Математика", "Наталья Фёдорова")),
            23 to listOf(listOf("11:00", "60", "Физика", "Степан Петров")), 24 to listOf(listOf("17:00", "60", "Математика", "Иван Иванов"), listOf("19:00", "60", "Консультация", "Андрей Семёнов")),
            25 to listOf(listOf("15:00", "60", "Разбор профиля", "Полина Ковалёва")), 26 to listOf(listOf("10:00", "60", "Математика", "Мария Павлова"), listOf("18:00", "60", "Физика", "Ярослав Беляев")),
            28 to listOf(listOf("12:00", "90", "Мини-группа · математика", "5 учеников")),
        )
        val lessons = mutableListOf<Lesson>()
        L.forEach { (dnum, ls) -> ls.forEachIndexed { i, l ->
            val d = LocalDate.of(2026, 11, dnum); val (h, m) = l[0].split(":").map { it.toInt() }
            val st = Clock.at(d, h, m); val id = "les$dnum$i"
            val cid = byName[l[3]] ?: ""
            lessons += Lesson(id, cid, cid, Clock.isoLocal(st), Clock.isoLocal(st + l[1].toInt() * 60_000L))
            r.prefs.setLessonExtra(id, LessonExtra(l[2], l[3]))
        } }
        r.lessons.value = lessons
        r.prefs.setSubjects(listOf(Subject("Математика", "#86C7A6"), Subject("Физика", "#9CC3D3"), Subject("Пробное занятие", "#E2C06A"), Subject("Консультация", "#E9A37E")))
    }

    fun notifs(r: Repo): List<Notif> = listOf(
        Notif("n1", "msg", "Мария Козлова", "А рассрочка есть?", Clock.at(TODAY, 15, 41), "chat:maria"),
        Notif("n2", "pay", "Просрочена оплата", "Андрей Семёнов · 3 500 ₽, ждали 26 нояб", Clock.at(TODAY, 10, 0), "pays"),
        Notif("n3", "rem", "Напоминание", "Николай Попов обещал оплатить сегодня", Clock.at(TODAY, 9, 0), "chat:nikolay"),
        Notif("n4", "les", "Занятие в 16:00", "Артём Волков · пробное, 30 мин", Clock.at(TODAY, 8, 0), "sched"),
        Notif("n5", "msg", "Ирина Белова", "Хочу заказать разбор профиля", Clock.at(TODAY.minusDays(1), 20, 0), "chat:irina"),
    )
}
