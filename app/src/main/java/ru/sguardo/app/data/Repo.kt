package ru.sguardo.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Request
import java.io.File
import java.time.LocalDate

enum class Phase { Launching, Login, Ready }
enum class Load { Loading, Ok, Error }

data class ToastMsg(val text: String, val undo: (() -> Unit)? = null, val id: Long = System.nanoTime())

data class Notif(val id: String, val kind: String, val title: String, val text: String, val at: Long, val target: String)

/**
 * Состояние приложения. Данные — /api/state, действия — /api/actions и отдельные адреса,
 * живые обновления — SSE /api/events (delta, typing, presence). В демо-режиме всё то же самое, но локально.
 */
class Repo private constructor(val ctx: Context) {
    companion object {
        @Volatile private var inst: Repo? = null
        fun get(ctx: Context): Repo = inst ?: synchronized(this) { inst ?: Repo(ctx.applicationContext).also { inst = it } }
        /** Для снимков экранов: отдельный экземпляр в демо-режиме. */
        fun demo(ctx: Context): Repo = Repo(ctx.applicationContext).also { it.enterDemo(seed = true) }
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val api = Api(ctx)
    var prefs = Prefs(ctx); private set
    val book = PhoneBook(ctx)

    val demo = MutableStateFlow(false)
    val phase = MutableStateFlow(Phase.Launching)
    val load = MutableStateFlow(Load.Loading)
    val loadError = MutableStateFlow<String?>(null)
    val state = MutableStateFlow(AppState())
    val user = MutableStateFlow<User?>(null)
    val account = MutableStateFlow<AccountInfo?>(null)
    /** разделы из админки; null — ещё не загружены */
    val modules = MutableStateFlow<ModuleFlags?>(null)
    /** сервер ответил 403 на расписание (модуль «Расписание» выключен в админке) */
    val scheduleBlocked = MutableStateFlow(false)
    val online = MutableStateFlow(true)
    val history = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val hasMore = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val pending = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val typing = MutableStateFlow<Map<String, Pair<String, Long>>>(emptyMap())
    val presence = MutableStateFlow<Map<String, PresenceItem>>(emptyMap())
    val drafts = MutableStateFlow<Map<String, String>>(emptyMap())
    val scheduled = MutableStateFlow<Map<String, List<ScheduledMessage>>>(emptyMap())
    val lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessonsLoading = MutableStateFlow(false)
    val suggestions = MutableStateFlow<List<LinkSuggestion>>(emptyList())
    val localStaffRoles = MutableStateFlow<Map<String, String>>(emptyMap())
    val toasts = MutableSharedFlow<ToastMsg>(extraBufferCapacity = 8)
    /** открыть чат по нажатию на уведомление: /messages?c=<id> */
    val openChat = MutableStateFlow<String?>(null)

    private var etag: String? = null
    private var liveJob: Job? = null
    private var pollJob: Job? = null
    private var lastDelta = 0L
    private var demoSeq = 1000

    fun toast(text: String, undo: (() -> Unit)? = null) { toasts.tryEmit(ToastMsg(text, undo)) }

    // ---------- выборки ----------
    // индексы по id пересчитываются только когда меняется само состояние (строки списков не ищут перебором)
    private class Index(val s: AppState) {
        val contacts = s.contacts.associateBy { it.id }
        val convs = s.conversations.associateBy { it.id }
        val convOf = s.conversations.filter { !it.trashed }.groupBy { it.contactId }.mapValues { (_, l) -> l.maxByOrNull { it.last?.time ?: 0 } }
        val folders = s.folders.associateBy { it.id }
    }
    @Volatile private var index: Index? = null
    private fun idx(): Index { val s = state.value; return index?.takeIf { it.s === s } ?: Index(s).also { index = it } }
    fun contact(id: String?) = id?.let { idx().contacts[it] }
    fun conv(id: String?) = id?.let { idx().convs[it] }
    fun convOf(contactId: String) = idx().convOf[contactId]
    fun folder(id: String?) = id?.let { idx().folders[it] }

    /** Имя из телефонной книги, если включено и номер там есть. */
    fun bookName(c: Contact?): String? {
        if (c == null || !prefs.namesFromBook.value) return null
        val phone = c.phone ?: c.name.takeIf { it.startsWith("+") || it.firstOrNull()?.isDigit() == true }
        return book.nameFor(phone)
    }
    fun nameOf(c: Contact?): String = bookName(c) ?: c?.name?.ifBlank { null } ?: c?.phone ?: c?.handle ?: "Без имени"
    fun nameOfConv(cv: Conversation?): String = nameOf(contact(cv?.contactId))

    fun messages(convId: String): List<Message> {
        val recent = conv(convId)?.messages ?: emptyList()
        val full = history.value[convId]
        val merged = if (full == null) recent else {
            val ids = full.map { it.id }.toHashSet()
            (full + recent.filter { it.id !in ids }).sortedBy { it.time }
        }
        val p = pending.value[convId].orEmpty().filter { pm -> merged.none { it.id == pm.id } }
        return merged + p
    }

    // ---------- запуск ----------
    fun bootstrap() {
        if (demo.value) return
        scope.launch {
            if (!api.cookies.hasSession()) { phase.value = Phase.Login; return@launch }
            try {
                user.value = api.get<MeResponse>("/api/auth/me/").user
                phase.value = Phase.Ready
                registerPushIfPossible()
                reload(force = true)
                startLive()
                launch { loadAccount() }
                launch { loadDrafts() }
                launch { loadPresence() }
            } catch (e: ApiError) {
                if (e.status == 401) phase.value = Phase.Login else { phase.value = Phase.Ready; load.value = Load.Error; loadError.value = e.message }
            } catch (e: Exception) {
                // нет сети: кука есть — пускаем в кабинет, данные подтянем позже
                phase.value = Phase.Ready; load.value = Load.Error; loadError.value = friendly(e); startLive(); registerPushIfPossible()
            }
        }
    }

    suspend fun login(email: String, password: String): String? = try {
        api.call("POST", "/api/auth/login/", obj("email" to email.trim(), "password" to password)); afterLogin(); null
    } catch (e: Exception) { friendly(e) }

    suspend fun requestCode(email: String): String? = try {
        api.call("POST", "/api/auth/code/", obj("email" to email.trim())); null
    } catch (e: Exception) { friendly(e) }

    suspend fun verifyCode(email: String, code: String): String? = try {
        api.call("POST", "/api/auth/verify/", obj("email" to email.trim(), "code" to code.trim(), "mode" to "login")); afterLogin(); null
    } catch (e: Exception) { friendly(e) }

    private suspend fun afterLogin() {
        etag = null
        user.value = runCatching { api.get<MeResponse>("/api/auth/me/").user }.getOrNull()
        phase.value = Phase.Ready
        reload(force = true)
        startLive()
        scope.launch { loadAccount() }
        scope.launch { loadDrafts() }
        registerPushIfPossible()
    }

    fun logout() {
        scope.launch {
            if (!demo.value) {
                prefs.fcmToken?.let { t -> runCatching { api.call("DELETE", "/api/push/device/", obj("token" to t)) } }
                runCatching { api.call("POST", "/api/auth/logout/") }
            }
            stopLive()
            api.cookies.clear()
            if (demo.value) exitDemo()
            prefs.clearAccountData()
            state.value = AppState(); user.value = null; account.value = null
            history.value = emptyMap(); pending.value = emptyMap(); drafts.value = emptyMap(); scheduled.value = emptyMap()
            lessons.value = emptyList(); etag = null
            phase.value = Phase.Login
        }
    }

    // ---------- демо ----------
    fun enterDemo(seed: Boolean = true) {
        if (!ru.sguardo.app.BuildConfig.DEMO_ALLOWED) return // в релизе демо-режима нет
        demo.value = true
        prefs = Prefs(ctx, memory = true)
        prefs.setOnboarded(true)
        Clock.fixed = Fixture.NOW; Clock.zone = Fixture.ZONE
        book.demo = Fixture.phoneBook
        modules.value = ModuleFlags(schedule = true, partners = true)
        if (seed) Fixture.apply(this)
        phase.value = Phase.Ready
        load.value = Load.Ok
    }

    private fun exitDemo() {
        demo.value = false; prefs = Prefs(ctx); Clock.fixed = null; Clock.zone = java.time.ZoneId.systemDefault(); book.demo = null
    }

    fun nextId(p: String) = "$p${++demoSeq}"

    // ---------- данные ----------
    suspend fun reload(force: Boolean = false) {
        if (demo.value) return
        if (state.value.contacts.isEmpty() && state.value.conversations.isEmpty()) load.value = Load.Loading
        try {
            val headers = if (!force && etag != null) mapOf("If-None-Match" to etag!!) else emptyMap()
            val text = withContext(Dispatchers.IO) {
                api.raw("GET", "/api/state/", headers = headers).use { r ->
                    if (r.code == 304) return@withContext null
                    if (r.code == 401) throw ApiError(401, "Нужно войти заново")
                    val t = r.body?.string().orEmpty()
                    if (r.code >= 400) throw ApiError(r.code, api.errorText(t, r.code))
                    etag = r.header("ETag") ?: r.header("Etag")
                    t
                }
            }
            if (text != null) {
                val sr = api.decode<StateResponse>(text)
                sr.state?.let { state.value = it }
                sr.user?.let { user.value = it }
            }
            load.value = Load.Ok; loadError.value = null
        } catch (e: ApiError) {
            if (e.status == 401) { logout(); return }
            loadError.value = e.message; if (state.value.conversations.isEmpty()) load.value = Load.Error
        } catch (e: Exception) {
            loadError.value = friendly(e); if (state.value.conversations.isEmpty()) load.value = Load.Error
        }
    }

    fun retry() { scope.launch { load.value = Load.Loading; if (demo.value) { delay(600); load.value = Load.Ok } else reload(force = true) } }

    private suspend fun reloadConvs(ids: List<String>) {
        try {
            val p = api.get<PartialState>("/api/state/", mapOf("conv" to ids.joinToString(",")))
            val byId = p.conversations.associateBy { it.id }
            val s = state.value
            val known = s.conversations.map { it.id }.toSet()
            val merged = s.conversations.filter { it.id !in p.gone }.map { byId[it.id] ?: it } + p.conversations.filter { it.id !in known }
            state.value = s.copy(conversations = merged)
            pending.value = pending.value.mapValues { (k, v) -> v.filter { pm -> byId[k]?.messages?.none { it.text == pm.text && it.isOut && it.time >= pm.time - 60_000 } ?: true } }
            if (p.conversations.any { it.contactId !in s.contacts.map { c -> c.id } }) reload()
        } catch (_: Exception) { reload() }
    }

    fun loadAccount() {
        if (demo.value) { modules.value = ModuleFlags(schedule = true, partners = true); return }
        scope.launch { account.value = runCatching { api.get<AccountInfo>("/api/account/") }.getOrNull() }
        scope.launch { runCatching { api.get<ModulesResponse>("/api/me/modules/") }.getOrNull()?.let { modules.value = it.modules } }
    }

    /** «Партнёры» клуба за N дней; null — ошибка или нет доступа. */
    suspend fun partners(days: Int): List<ClubPartner>? {
        if (demo.value) { kotlinx.coroutines.delay(300); return listOf(
            ClubPartner("p1", "Школа «Логос»", "RUB", 42, 11, 186000.0, .26), ClubPartner("p2", "Анна Морозова, психолог", "RUB", 27, 9, 94500.0, .33),
            ClubPartner("p3", "Студия английского Hello", "RUB", 18, 4, 52000.0, .22)) }
        return runCatching { api.get<ClubReport>("/api/partners/", mapOf("days" to days.toString())).partners }.getOrNull()
    }

    suspend fun loadHistory(convId: String, older: Boolean = false) {
        if (demo.value) return
        val cv = conv(convId) ?: return
        if (!older && (cv.older ?: 0) == 0) return
        try {
            val before = if (older) messages(convId).firstOrNull()?.id else null
            val q = mutableMapOf("c" to convId, "limit" to "40"); if (before != null) q["before"] = before
            val r = api.get<HistoryResponse>("/api/history/", q)
            val cur = history.value[convId].orEmpty()
            val ids = cur.map { it.id }.toHashSet()
            history.value = history.value + (convId to (r.messages.filter { it.id !in ids } + cur).sortedBy { it.time })
            hasMore.value = hasMore.value + (convId to r.hasMore)
        } catch (_: Exception) {}
    }

    // ---------- живые обновления ----------
    fun startLive() {
        if (demo.value) return
        stopLive()
        liveJob = scope.launch(Dispatchers.IO) { eventsLoop() }
        pollJob = scope.launch { while (isActive) { delay(60_000); reload() } }
    }

    fun stopLive() { liveJob?.cancel(); liveJob = null; pollJob?.cancel(); pollJob = null }

    private suspend fun eventsLoop() {
        while (kotlin.coroutines.coroutineContext.isActive) {
            try {
                val req = Request.Builder().url(api.url("/api/events/")).header("Accept", "text/event-stream").build()
                api.streamClient.newCall(req).await().use { r ->
                    if (r.code == 401) { withContext(Dispatchers.Main) { logout() }; return }
                    val src = r.body?.source() ?: return@use
                    var event = "message"
                    while (kotlin.coroutines.coroutineContext.isActive) {
                        val line = src.readUtf8Line() ?: break
                        when {
                            line.startsWith("event:") -> event = line.removePrefix("event:").trim()
                            line.startsWith("data:") -> {
                                val data = line.removePrefix("data:").trim()
                                val ev = event
                                withContext(Dispatchers.Main) { onEvent(ev, data) }
                            }
                            line.isEmpty() -> event = "message"
                        }
                    }
                }
            } catch (_: Exception) {}
            delay(3000)
        }
    }

    private fun onEvent(event: String, data: String) {
        when (event) {
            "delta" -> {
                lastDelta = System.currentTimeMillis()
                val ids = runCatching { JsonCfg.decodeFromString<Map<String, List<String>>>(data)["c"] }.getOrNull().orEmpty()
                scope.launch { if (ids.isNotEmpty()) reloadConvs(ids) else reload() }
            }
            "typing" -> runCatching {
                val o = JsonCfg.decodeFromString<Map<String, String>>(data)
                val c = o["c"] ?: return
                typing.value = typing.value + (c to ((o["k"] ?: "typing") to System.currentTimeMillis() + 5000))
                scope.launch { delay(5200); typing.value = typing.value.filterValues { it.second > System.currentTimeMillis() } }
            }
            "presence" -> runCatching {
                val o = JsonCfg.decodeFromString<JsonObject>(data)
                val c = o["c"]?.jsonPrimitive?.contentOrNull ?: return
                presence.value = presence.value + (c to PresenceItem(o["online"]?.jsonPrimitive?.booleanOrNull ?: false, o["lastSeen"]?.jsonPrimitive?.contentOrNull))
            }
            "ping" -> {}
            else -> if (data.contains("changed") && System.currentTimeMillis() - lastDelta > 800) scope.launch { reload() }
        }
    }

    fun watchNetwork() {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return
        online.value = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    online.value = true
                    scope.launch { if (phase.value == Phase.Ready && !demo.value) { reload(); if (liveJob == null) startLive() } }
                }
                override fun onLost(network: Network) { online.value = false }
            })
        }
    }

    fun loadPresence() { if (!demo.value) scope.launch { runCatching { presence.value = api.get<PresenceResponse>("/api/presence/").presence } } }

    // ---------- действия кабинета ----------
    /** /api/actions: сервер возвращает новое состояние целиком. В демо — локальная правка. */
    fun act(body: JsonObject, ok: String? = null, undo: (() -> Unit)? = null, after: (() -> Unit)? = null) {
        scope.launch {
            if (demo.value) {
                state.value = DemoReducer.reduce(this@Repo, state.value, body)
                ok?.let { toast(it, undo) }; after?.invoke(); return@launch
            }
            try {
                val text = api.call("POST", "/api/actions/", body)
                val r = api.decode<StateResponse>(text)
                r.state?.let { state.value = it }
                etag = null
                val warn = runCatching { api.decode<ErrorBody>(text).warning }.getOrNull()
                if (!warn.isNullOrBlank()) toast(warn) else ok?.let { toast(it, undo) }
                after?.invoke()
            } catch (e: Exception) { toast(friendly(e)) }
        }
    }

    /** То же действие, но дожидаемся ответа (для последовательных шагов, например порядка папок). */
    suspend fun actNow(body: JsonObject): Boolean {
        if (demo.value) { state.value = DemoReducer.reduce(this, state.value, body); return true }
        return try { applyStateFrom(api.call("POST", "/api/actions/", body)); true } catch (e: Exception) { toast(friendly(e)); false }
    }

    /** Переставить папку: сервер умеет только «на одну вверх/вниз», поэтому шагаем по очереди. */
    fun reorderFolder(id: String, from: Int, to: Int) {
        if (from == to) return
        scope.launch {
            val dir = if (to > from) 1 else -1
            repeat(kotlin.math.abs(to - from)) { if (!actNow(obj("type" to "moveFolder", "id" to id, "dir" to dir))) return@launch }
            toast("Порядок папок сохранён")
        }
    }

    private suspend fun applyStateFrom(text: String) {
        runCatching { api.decode<StateResponse>(text).state }.getOrNull()?.let { state.value = it }
        etag = null
    }

    fun setConvFlag(convId: String, flag: String, on: Boolean, ok: String? = null, undo: (() -> Unit)? = null) =
        act(obj("type" to "setConvFlag", "conversationId" to convId, "flag" to flag, "on" to on), ok, undo)

    fun markRead(convId: String) {
        val c = conv(convId) ?: return
        if (c.unread == 0) return
        state.value = state.value.copy(conversations = state.value.conversations.map { if (it.id == convId) it.copy(unread = 0) else it })
        act(obj("type" to "read", "conversationId" to convId))
    }

    fun markUnread(convId: String) {
        state.value = state.value.copy(conversations = state.value.conversations.map { if (it.id == convId) it.copy(unread = maxOf(1, it.unread)) else it })
        if (!demo.value) toast("Пометка «новое» только на этом телефоне")
    }

    // ---------- сообщения ----------
    fun send(convId: String, text: String, replyTo: String? = null, attachments: List<Attachment> = emptyList(), round: Boolean = false) {
        val tmp = Message(id = "tmp-${System.nanoTime()}", dir = "out", text = text, at = isoOf(Clock.now()), attachments = attachments.ifEmpty { null },
            replyTo = replyTo?.let { id -> messages(convId).firstOrNull { it.id == id }?.let { ReplyRef(it.id, it.snippet, it.dir) } }, pending = true)
        if (demo.value) {
            val m = tmp.copy(id = nextId("m"), pending = !online.value)
            state.value = state.value.copy(conversations = state.value.conversations.map { if (it.id == convId) it.copy(messages = it.messages + m) else it })
            if (!online.value) { toast("Отправим, когда появится интернет"); return }
            scope.launch {
                delay(700); typing.value = typing.value + (convId to ("typing" to Clock.now() + 60_000))
                delay(1700); typing.value = typing.value - convId
                val reply = Message(id = nextId("m"), dir = "in", text = Fixture.replies[(demoSeq) % Fixture.replies.size], at = isoOf(Clock.now()))
                state.value = state.value.copy(conversations = state.value.conversations.map { if (it.id == convId) it.copy(messages = it.messages + reply) else it })
            }
            return
        }
        pending.value = pending.value + (convId to (pending.value[convId].orEmpty() + tmp))
        scope.launch {
            try {
                val body = obj("conversationId" to convId, "text" to text, "replyTo" to replyTo,
                    "attachments" to attachments.mapNotNull { a -> a.mediaId?.let { mapOf("mediaId" to it, "round" to (a.round == true || round)) } })
                applyStateFrom(api.call("POST", "/api/messages/send/", body))
                saveDraft(convId, "")
            } catch (e: Exception) {
                toast("Не отправлено: ${friendly(e)}")
            } finally {
                pending.value = pending.value + (convId to pending.value[convId].orEmpty().filter { it.id != tmp.id })
            }
        }
    }

    /** Загрузить файл (фото, голосовое, кружок, документ) и отправить. */
    fun sendFile(convId: String, file: File, name: String, mime: String, kind: String, round: Boolean = false, duration: Int? = null, caption: String = "") {
        if (demo.value) {
            // в демо запись настоящая и остаётся на телефоне: её можно сразу прослушать или посмотреть
            val local = if (file.exists() && file.length() > 0) file.toURI().toString() else ""
            val a = Attachment(kind = kind, name = name, url = local, round = round, duration = duration?.toDouble())
            send(convId, caption, attachments = listOf(a)); return
        }
        if (!file.exists() || file.length() == 0L) { toast("Файл пустой или не сохранился, попробуйте ещё раз"); return }
        val tmp = Message(id = "tmp-${System.nanoTime()}", dir = "out", text = caption, at = isoOf(Clock.now()), pending = true,
            attachments = listOf(Attachment(kind = kind, name = name, url = file.toURI().toString(), round = round, duration = duration?.toDouble())))
        pending.value = pending.value + (convId to (pending.value[convId].orEmpty() + tmp))
        scope.launch {
            try {
                val a = api.upload(file, name, mime)
                val body = obj("conversationId" to convId, "text" to caption, "attachments" to listOf(mapOf("mediaId" to (a.mediaId ?: ""), "round" to round)))
                applyStateFrom(api.call("POST", "/api/messages/send/", body))
            } catch (e: Exception) { toast("Не отправлено: ${friendly(e)}") }
            finally { pending.value = pending.value + (convId to pending.value[convId].orEmpty().filter { it.id != tmp.id }) }
        }
    }

    private fun localMsg(convId: String, f: (List<Message>) -> List<Message>) {
        state.value = state.value.copy(conversations = state.value.conversations.map { if (it.id == convId) it.copy(messages = f(it.messages)) else it })
        history.value[convId]?.let { history.value = history.value + (convId to f(it)) }
    }

    fun editMessage(convId: String, id: String, text: String) {
        if (demo.value) { localMsg(convId) { l -> l.map { if (it.id == id) it.copy(text = text, edited = true) else it } }; toast("Сообщение изменено"); return }
        scope.launch {
            try { applyStateFrom(api.call("POST", "/api/messages/edit/", obj("messageId" to id, "text" to text))); localMsg(convId) { l -> l.map { if (it.id == id) it.copy(text = text, edited = true) else it } }; toast("Сообщение изменено") }
            catch (e: Exception) { toast(friendly(e)) }
        }
    }

    fun deleteMessages(convId: String, ids: List<String>, everyone: Boolean) {
        if (demo.value) { localMsg(convId) { l -> l.filter { it.id !in ids } }; toast(if (ids.size > 1) "Удалено сообщений: ${ids.size}" else "Сообщение удалено"); return }
        scope.launch {
            var ok = 0
            for (id in ids) try {
                applyStateFrom(api.call("POST", "/api/messages/delete/", obj("messageId" to id, "mode" to if (everyone) "everyone" else "local"))); ok++
            } catch (e: Exception) { toast(friendly(e)) }
            localMsg(convId) { l -> l.filter { it.id !in ids } }
            if (ok > 0) toast(if (ok > 1) "Удалено сообщений: $ok" else "Сообщение удалено")
        }
    }

    fun react(convId: String, id: String, emoji: String?) {
        val apply = { localMsg(convId) { l -> l.map { m -> if (m.id != id) m else m.copy(reactions = (m.reactions.orEmpty().filter { !it.mine } + listOfNotNull(emoji?.let { Reaction(it, true) })).ifEmpty { null }) } } }
        if (demo.value) { apply(); return }
        apply()
        scope.launch { try { applyStateFrom(api.call("POST", "/api/messages/react/", obj("messageId" to id, "emoji" to (emoji ?: kotlinx.serialization.json.JsonNull)))) } catch (e: Exception) { toast(friendly(e)) } }
    }

    fun pinMessage(convId: String, id: String, on: Boolean, both: Boolean = true) {
        val setLocal = {
            state.value = state.value.copy(conversations = state.value.conversations.map { c ->
                if (c.id != convId) c else {
                    val pins = c.pins.filter { it != id }.let { if (on) listOf(id) + it else it }
                    c.copy(meta = (c.meta.orEmpty() + ("pins" to pins.joinToString(","))))
                }
            })
        }
        if (demo.value) { setLocal(); toast(if (on) "Сообщение закреплено" else "Сообщение откреплено"); return }
        scope.launch {
            try { applyStateFrom(api.call("POST", "/api/messages/pin/", obj("messageId" to id, "on" to on, "both" to both))); toast(if (on) "Сообщение закреплено" else "Сообщение откреплено") }
            catch (e: Exception) { toast(friendly(e)) }
        }
    }

    fun forward(fromConv: String, ids: List<String>, toConv: String) {
        val msgs = messages(fromConv).filter { it.id in ids }
        val src = nameOfConv(conv(fromConv))
        if (demo.value) {
            val add = msgs.map { it.copy(id = nextId("m"), dir = "out", at = isoOf(Clock.now()), reactions = null, replyTo = null, edited = false, forwardedFrom = if (it.isOut) "вас" else src) }
            state.value = state.value.copy(conversations = state.value.conversations.map { if (it.id == toConv) it.copy(messages = it.messages + add) else it })
            toast("Переслано: ${nameOfConv(conv(toConv))}"); return
        }
        scope.launch {
            for (m in msgs) try {
                val body = obj("conversationId" to toConv, "text" to m.text, "attachments" to m.attachments.orEmpty().mapNotNull { a -> a.mediaId?.let { mapOf("mediaId" to it, "round" to (a.round == true)) } })
                applyStateFrom(api.call("POST", "/api/messages/send/", body))
            } catch (e: Exception) { toast(friendly(e)); return@launch }
            toast("Переслано: ${nameOfConv(conv(toConv))}")
        }
    }

    // ---------- отложенные ----------
    fun loadScheduled(convId: String) {
        if (demo.value) return
        scope.launch { runCatching { scheduled.value = scheduled.value + (convId to api.get<ScheduledList>("/api/scheduled-messages/", mapOf("conversationId" to convId)).items) } }
    }

    fun schedule(convId: String, text: String, at: Long, label: String, replyTo: String? = null) {
        if (demo.value) {
            scheduled.value = scheduled.value + (convId to (scheduled.value[convId].orEmpty() + ScheduledMessage(nextId("s"), convId, text, Clock.isoLocal(at))))
            toast("Отправим $label"); return
        }
        scope.launch {
            try {
                val r = api.post<ScheduledList>("/api/scheduled-messages/", obj("conversationId" to convId, "text" to text, "sendAt" to Clock.isoLocal(at), "replyTo" to replyTo))
                scheduled.value = scheduled.value + (convId to r.items)
                toast("Отправим $label")
            } catch (e: Exception) { toast(friendly(e)) }
        }
    }

    fun cancelScheduled(convId: String, id: String) {
        scheduled.value = scheduled.value + (convId to scheduled.value[convId].orEmpty().filter { it.id != id })
        if (demo.value) { toast("Отправка отменена"); return }
        scope.launch { try { api.call("DELETE", "/api/scheduled-messages/$id/"); toast("Отправка отменена") } catch (e: Exception) { toast(friendly(e)); loadScheduled(convId) } }
    }

    // ---------- черновики ----------
    private fun loadDrafts() { scope.launch { runCatching { drafts.value = api.get<DraftsResponse>("/api/drafts/").drafts.mapValues { it.value.text } } } }
    private val draftJobs = HashMap<String, Job>()
    fun saveDraft(convId: String, text: String) {
        if (drafts.value[convId].orEmpty() == text) return
        drafts.value = if (text.isBlank()) drafts.value - convId else drafts.value + (convId to text)
        if (demo.value) return
        draftJobs[convId]?.cancel()
        draftJobs[convId] = scope.launch { delay(1200); runCatching { api.call("PUT", "/api/drafts/", obj("conversationId" to convId, "text" to text)) } }
    }

    // ---------- расписание ----------
    fun loadLessons(weekStart: LocalDate) {
        if (demo.value) return
        scope.launch {
            lessonsLoading.value = true
            try {
                lessons.value = api.get<LessonsResponse>("/api/schedule/", mapOf("from" to weekStart.toString(), "to" to weekStart.plusDays(7).toString())).lessons
                scheduleBlocked.value = false
            } catch (e: Exception) {
                // 403: расписание не включено кабинету в админке — не сыпем тостами, экран расписания объяснит сам
                if (friendly(e) == "Нет доступа") scheduleBlocked.value = true else toast(friendly(e))
            }
            lessonsLoading.value = false
        }
    }

    fun addLesson(convId: String, day: LocalDate, h: Int, m: Int, minutes: Int, extra: LessonExtra, weekStart: LocalDate, repeatWeeks: Int = 0) {
        if (demo.value) {
            val id = nextId("l"); val st = Clock.at(day, h, m)
            lessons.value = lessons.value + Lesson(id, convId, conv(convId)?.contactId ?: "", Clock.isoLocal(st), Clock.isoLocal(st + minutes * 60_000L))
            prefs.setLessonExtra(id, extra); toast("Занятие в ${"%02d:%02d".format(h, m)} добавлено"); return
        }
        scope.launch {
            try {
                val body = obj("action" to "add", "conversationId" to convId, "start" to Clock.localStamp(day, h, m), "duration" to minutes,
                    "repeat" to if (repeatWeeks > 1) mapOf("weeks" to repeatWeeks) else null)
                api.call("POST", "/api/schedule/", body)
                val r = api.get<LessonsResponse>("/api/schedule/", mapOf("from" to weekStart.toString(), "to" to weekStart.plusDays(7).toString()))
                val start = Clock.at(day, h, m)
                r.lessons.filter { it.conversationId == convId && kotlin.math.abs(parseIso(it.startsAt) - start) < 60_000 }.forEach { prefs.setLessonExtra(it.id, extra) }
                lessons.value = r.lessons
                toast("Занятие в ${"%02d:%02d".format(h, m)} добавлено")
            } catch (e: Exception) { toast(friendly(e)) }
        }
    }

    fun moveLesson(l: Lesson, day: LocalDate, h: Int, m: Int, minutes: Int, extra: LessonExtra, weekStart: LocalDate, ok: String = "Занятие изменено") {
        prefs.setLessonExtra(l.id, extra)
        if (demo.value) {
            val st = Clock.at(day, h, m)
            lessons.value = lessons.value.map { if (it.id == l.id) it.copy(startsAt = Clock.isoLocal(st), endsAt = Clock.isoLocal(st + minutes * 60_000L)) else it }
            toast(ok); return
        }
        scope.launch {
            try { api.call("POST", "/api/schedule/", obj("action" to "move", "id" to l.id, "start" to Clock.localStamp(day, h, m), "duration" to minutes)); loadLessons(weekStart); toast(ok) }
            catch (e: Exception) { toast(friendly(e)) }
        }
    }

    fun deleteLesson(l: Lesson, following: Boolean = false) {
        lessons.value = lessons.value.filter { it.id != l.id }
        if (demo.value) { toast("Занятие удалено"); return }
        scope.launch {
            try { api.call("POST", "/api/schedule/", obj("action" to "delete", "id" to l.id, "following" to following)); toast("Занятие удалено") }
            catch (e: Exception) { toast(friendly(e)) }
        }
    }

    // ---------- команда, кабинет ----------
    suspend fun invite(name: String, email: String, password: String): String? {
        if (demo.value) {
            state.value = state.value.copy(team = state.value.team + TeamMember(nextId("u"), name, "agent", email)); return null
        }
        return try { api.call("POST", "/api/team/", obj("name" to name, "email" to email, "password" to password)); null } catch (e: Exception) { friendly(e) }
    }

    suspend fun confirmInvite(email: String, code: String): String? = try {
        applyStateFrom(api.call("POST", "/api/team/", obj("email" to email, "code" to code))); null
    } catch (e: Exception) { friendly(e) }

    fun setRole(id: String, role: String) {
        localStaffRoles.value = localStaffRoles.value + (id to role)
        if (demo.value) { toast("Роль сохранена"); return }
        val p = when (role) {
            "Администратор" -> mapOf("channels" to "all", "onlyAssigned" to false, "canPay" to true, "canExport" to true, "seeMoney" to true)
            "Только просмотр" -> mapOf("channels" to "all", "onlyAssigned" to false, "canPay" to false, "canExport" to false, "seeMoney" to false)
            else -> mapOf("channels" to "all", "onlyAssigned" to false, "canPay" to true, "canExport" to false, "seeMoney" to true)
        }
        scope.launch {
            try { applyStateFrom(api.call("PUT", "/api/team/$id/permissions/", obj(*p.map { it.key to it.value }.toTypedArray()))); toast("Роль сохранена") }
            catch (e: Exception) { toast(friendly(e)) }
        }
    }

    fun removeMember(id: String) {
        if (demo.value) { state.value = state.value.copy(team = state.value.team.filter { it.id != id }); toast("Доступ закрыт"); return }
        scope.launch { try { applyStateFrom(api.call("DELETE", "/api/team/$id/")); toast("Доступ закрыт") } catch (e: Exception) { toast(friendly(e)) } }
    }

    fun logoutOthers() {
        if (demo.value) { toast("Сеанс на компьютере завершён"); return }
        scope.launch { try { api.call("DELETE", "/api/account/sessions/"); toast("Вышли на других устройствах"); loadAccount() } catch (e: Exception) { toast(friendly(e)) } }
    }

    fun passwordCode() {
        if (demo.value) { toast("Код для смены пароля отправлен на почту"); return }
        scope.launch { try { api.call("POST", "/api/account/password/code/"); toast("Код для смены пароля отправлен на почту") } catch (e: Exception) { toast(friendly(e)) } }
    }

    suspend fun setPassword(code: String, password: String): String? {
        if (demo.value) return null
        return try { api.call("POST", "/api/account/password/", obj("code" to code, "password" to password)); null } catch (e: Exception) { friendly(e) }
    }

    fun renameMe(name: String) {
        if (demo.value) { user.value = user.value?.copy(name = name); toast("Имя сохранено"); return }
        scope.launch {
            try {
                val t = api.call("PATCH", "/api/profile/", obj("name" to name))
                runCatching { api.decode<StateResponse>(t).user }.getOrNull()?.let { user.value = it }
                loadAccount(); toast("Имя сохранено")
            } catch (e: Exception) { toast(friendly(e)) }
        }
    }

    /** Поиск по тексту сообщений (/api/search); в демо — по локальным сообщениям. */
    suspend fun search(q: String): List<SearchItem> {
        if (demo.value) return state.value.conversations.flatMap { c -> c.messages.filter { it.text.contains(q, true) }.map {
            SearchItem(it.id, c.id, c.contactId, nameOfConv(c), c.network, it.dir, it.at, it.text) } }.take(30)
        return try { api.get<SearchResponse>("/api/search/", mapOf("q" to q, "limit" to "30")).items } catch (_: Exception) { emptyList() }
    }

    // ---------- связка карточек ----------
    fun loadSuggestions() {
        if (demo.value) return
        scope.launch { runCatching { suggestions.value = api.get<LinkSuggestions>("/api/link-suggestions/").items } }
    }

    fun dismissSuggestion(s: LinkSuggestion) {
        suggestions.value = suggestions.value - s
        if (demo.value) { toast("Поняли, это разные люди"); return }
        scope.launch { runCatching { api.call("POST", "/api/link-suggestions/", obj("aId" to s.aId, "bId" to s.bId)) }; toast("Поняли, это разные люди") }
    }

    fun merge(s: LinkSuggestion, keep: String) {
        suggestions.value = suggestions.value - s
        act(obj("type" to "merge", "keepId" to keep, "dropId" to if (keep == s.aId) s.bId else s.aId), "Объединили: одна карточка, две соцсети")
    }

    // ---------- push ----------
    /** Состояние push для экрана «Ещё»: «Подключены» или «Недоступны: причина». */
    val pushStatus = MutableStateFlow("Ещё не проверялось")
    private var pushToastShown = false

    private fun pushFail(reason: String) {
        android.util.Log.w("SGPUSH", reason)
        pushStatus.value = "Недоступны: $reason"
        if (phase.value == Phase.Ready && !pushToastShown) { pushToastShown = true; toast("Уведомления недоступны: $reason") }
    }

    /**
     * Получить токен FCM и зарегистрировать устройство на сервере (POST /api/push/device).
     * Зовётся при каждом запуске, после входа и при возвращении в приложение. Токен получаем и до входа: после входа он уйдёт сразу.
     * На Android 8–12 разрешение на уведомления не нужно (POST_NOTIFICATIONS спрашивается только с Android 13).
     */
    fun registerPushIfPossible() {
        if (demo.value) return
        if (!prefs.notifyMessages.value) { pushStatus.value = "Выключены в приложении"; return }
        try {
            if (com.google.firebase.FirebaseApp.getApps(ctx).isEmpty()) { pushFail("Firebase не настроен в этой сборке"); return }
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { t ->
                val tok = t.result
                if (t.isSuccessful && !tok.isNullOrBlank()) {
                    android.util.Log.i("SGPUSH", "token ok, длина ${tok.length}")
                    registerToken(tok)
                } else pushFail(t.exception?.message?.take(120) ?: "токен не получен: нет сервисов Google Play или нет сети")
            }
        } catch (e: Throwable) { pushFail(e.message?.take(120) ?: e.javaClass.simpleName) }
    }

    fun registerToken(token: String) {
        if (demo.value) return
        prefs.fcmToken = token
        if (!api.cookies.hasSession()) { pushStatus.value = "Токен получен, ждём вход"; return }
        scope.launch {
            try {
                api.call("POST", "/api/push/device/", obj("platform" to "android", "token" to token))
                android.util.Log.i("SGPUSH", "устройство зарегистрировано на сервере")
                pushStatus.value = "Подключены"
            } catch (e: Exception) { pushFail("сервер не принял устройство: ${friendly(e)}") }
        }
    }

    fun unregisterPush() {
        val t = prefs.fcmToken ?: return
        if (demo.value) return
        scope.launch { runCatching { api.call("DELETE", "/api/push/device/", obj("token" to t)) } }
    }

    /** Ссылка из push: /messages?c=<id диалога>. */
    fun handleUrl(url: String?) {
        val c = url?.let { Regex("[?&]c=([^&]+)").find(it)?.groupValues?.get(1) } ?: return
        openChat.value = java.net.URLDecoder.decode(c, "UTF-8")
    }

    // ---------- уведомления внутри приложения ----------
    fun notifications(): List<Notif> {
        if (demo.value) return Fixture.notifs(this)
        val s = state.value; val now = Clock.now(); val out = mutableListOf<Notif>()
        for (c in s.conversations) if (c.unread > 0 && !c.muted && !c.archived && !c.trashed) c.last?.let {
            out += Notif("m-${it.id}", "msg", nameOfConv(c), it.snippet, it.time, "chat:${c.id}")
        }
        for (d in s.deals) if (d.status == "promised" && d.dueAt != null && parseIso(d.dueAt) < now) {
            out += Notif("p-${d.id}", "pay", "Просрочена оплата", "${nameOf(contact(d.contactId))} · ${Fmt.rub(d.amount)}, ждали ${Fmt.dayMonth(parseIso(d.dueAt))}", parseIso(d.dueAt), "pays")
        }
        for (r in s.reminders) if (!r.done && parseIso(r.at) <= now) {
            val cv = convOf(r.contactId)
            out += Notif("r-${r.id}", "rem", "Напоминание", "${nameOf(contact(r.contactId))}: ${r.text.ifBlank { "написать" }}", parseIso(r.at), cv?.let { "chat:${it.id}" } ?: "clients")
        }
        val today = Clock.today()
        for (l in lessons.value) {
            val st = parseIso(l.startsAt)
            if (Clock.local(st).toLocalDate() == today && l.status == "planned") {
                val ex = prefs.lessonExtra.value[l.id]
                out += Notif("l-${l.id}", "les", "Занятие в ${Fmt.hm(st)}", "${ex?.who?.ifBlank { null } ?: nameOf(contact(l.contactId))}${ex?.subject?.let { " · $it" } ?: ""}", st - 8 * 3_600_000L, "sched")
            }
        }
        return out.sortedByDescending { it.at }.take(40)
    }
}

/** Локальная правка состояния в демо-режиме: те же действия, что у /api/actions. */
object DemoReducer {
    private fun JsonObject.s(k: String) = this[k]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.b(k: String) = this[k]?.jsonPrimitive?.boolean ?: false
    private fun JsonObject.l(k: String) = this[k]?.jsonPrimitive?.longOrNull

    fun reduce(r: Repo, s: AppState, a: JsonObject): AppState {
        val now = isoOf(Clock.now())
        return when (a.s("type")) {
            "setConvFlag" -> {
                val flag = a.s("flag")!!; val on = a.b("on")
                s.copy(conversations = s.conversations.map { c ->
                    if (c.id != a.s("conversationId")) c else {
                        var m = c.meta.orEmpty().toMutableMap()
                        if (on) m[flag] = now else m.remove(flag)
                        if (on && flag == "archived") m.remove("pinned")
                        if (on && flag == "pinned") m.remove("archived")
                        c.copy(meta = m)
                    }
                })
            }
            "read" -> s.copy(conversations = s.conversations.map { if (it.id == a.s("conversationId")) it.copy(unread = 0) else it })
            "move" -> s.copy(contacts = s.contacts.map { if (it.id == a.s("contactId")) it.copy(folderId = a.s("folderId")) else it })
            "setNote" -> s.copy(contacts = s.contacts.map { if (it.id == a.s("contactId")) it.copy(note = a.s("note")) else it })
            "setAssignee" -> s.copy(contacts = s.contacts.map { if (it.id == a.s("contactId")) it.copy(assignee = a.s("name")) else it })
            "updateContact" -> s.copy(contacts = s.contacts.map { c ->
                if (c.id != a.s("contactId")) c else c.copy(name = a.s("name") ?: c.name, phone = a.s("phone") ?: c.phone,
                    tags = a["tags"]?.let { t -> (t as kotlinx.serialization.json.JsonArray).map { it.jsonPrimitive.content } } ?: c.tags)
            })
            "addContact" -> {
                val id = r.nextId("c")
                val tags = a["tags"]?.let { t -> (t as kotlinx.serialization.json.JsonArray).map { it.jsonPrimitive.content } }
                s.copy(contacts = listOf(Contact(id, a.s("name") ?: "", phone = a.s("phone"), source = a.s("network"), networks = listOfNotNull(a.s("network")),
                    folderId = a.s("folderId") ?: s.folders.firstOrNull()?.id, tags = tags, firstContactAt = now)) + s.contacts)
            }
            "removeContact" -> s.copy(contacts = s.contacts.filter { it.id != a.s("contactId") }, conversations = s.conversations.filter { it.contactId != a.s("contactId") })
            "pay" -> {
                val dealId = a.s("dealId")
                if (dealId != null && s.deals.any { it.id == dealId }) s.copy(deals = s.deals.map { if (it.id == dealId) it.copy(status = "paid", date = now, dueAt = null) else it },
                    reminders = s.reminders.map { if (it.contactId == a.s("contactId")) it.copy(done = true) else it })
                else s.copy(deals = listOf(Deal(r.nextId("d"), a.s("contactId")!!, a.s("productId"), a.l("amount") ?: 0, "paid", now)) + s.deals)
            }
            "promise" -> s.copy(deals = listOf(Deal(r.nextId("d"), a.s("contactId")!!, a.s("productId"), a.l("amount") ?: 0, "promised", now, a.s("remindAt"))) + s.deals,
                reminders = s.reminders + Reminder(r.nextId("r"), a.s("contactId")!!, a.s("remindAt") ?: now, "Обещал оплатить"))
            "updateDeal" -> s.copy(deals = s.deals.map { if (it.id == a.s("id")) it.copy(status = a.s("status") ?: it.status, dueAt = a.s("dueAt") ?: it.dueAt, date = if (a.s("status") == "promised") it.date else it.date) else it })
            "removeDeal" -> s.copy(deals = s.deals.filter { it.id != a.s("id") })
            "remind" -> s.copy(reminders = s.reminders + Reminder(r.nextId("r"), a.s("contactId")!!, a.s("at")!!, a.s("text") ?: ""))
            "doneReminder" -> s.copy(reminders = s.reminders.map { if (it.id == a.s("id")) it.copy(done = true) else it })
            "updateFolder" -> s.copy(folders = s.folders.map { if (it.id == a.s("id")) it.copy(name = a.s("name") ?: it.name, color = a.s("color") ?: it.color) else it })
            "addFolder" -> s.copy(folders = s.folders + Folder(r.nextId("f"), "custom", a.s("name")!!, a.s("color")))
            "removeFolder" -> {
                val id = a.s("id"); val to = a.s("moveTo") ?: s.folders.first { it.id != id }.id
                s.copy(folders = s.folders.filter { it.id != id }, contacts = s.contacts.map { if (it.folderId == id) it.copy(folderId = to) else it })
            }
            "moveFolder" -> {
                val i = s.folders.indexOfFirst { it.id == a.s("id") }; val j = i + (a.l("dir") ?: 0).toInt()
                if (i < 0 || j !in s.folders.indices) s else s.copy(folders = s.folders.toMutableList().also { java.util.Collections.swap(it, i, j) })
            }
            "setCollection" -> {
                val tag = (if (a.s("kind") == "group") "группа:" else "класс:") + a.s("name"); val on = a.b("on")
                s.copy(contacts = s.contacts.map { c -> if (c.id != a.s("contactId")) c else c.copy(tags = c.tags.orEmpty().filter { it != tag }.let { if (on) it + tag else it }) })
            }
            "renameCollection" -> {
                val p = if (a.s("kind") == "group") "группа:" else "класс:"
                s.copy(contacts = s.contacts.map { c -> c.copy(tags = c.tags?.map { if (it == p + a.s("from")) p + a.s("to") else it }) })
            }
            "deleteCollection" -> {
                val p = if (a.s("kind") == "group") "группа:" else "класс:"
                s.copy(contacts = s.contacts.map { c -> c.copy(tags = c.tags?.filter { it != p + a.s("name") }) })
            }
            "merge" -> s
            else -> s
        }
    }
}
