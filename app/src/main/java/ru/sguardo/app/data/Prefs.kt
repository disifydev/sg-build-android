package ru.sguardo.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

@Serializable
data class Subject(val name: String, val color: String)

@Serializable
data class LessonExtra(val subject: String = "", val who: String = "")

/** Локальные настройки телефона: тема, первый запуск, переключатели и то, чего нет в API (предметы, класс без учеников). */
class Prefs(context: Context, private val memory: Boolean = false) {
    private val sp = context.getSharedPreferences(if (memory) "sg_demo" else "sg", Context.MODE_PRIVATE)

    val theme = MutableStateFlow(sp.getString("theme", "system") ?: "system")
    val onboarded = MutableStateFlow(sp.getBoolean("onboarded", false))
    val namesFromBook = MutableStateFlow(sp.getBoolean("book", true))
    val notifyMessages = MutableStateFlow(sp.getBoolean("n1", true))
    val notifyReminders = MutableStateFlow(sp.getBoolean("n2", true))
    val extraClasses = MutableStateFlow(sp.getStringSet("classes", emptySet())!!.toList().sorted())
    val subjects = MutableStateFlow(
        sp.getString("subjects", null)?.let { runCatching { JsonCfg.decodeFromString(ListSerializer(Subject.serializer()), it) }.getOrNull() }
            ?: listOf(Subject("Математика", "#86C7A6"), Subject("Физика", "#9CC3D3"), Subject("Пробное занятие", "#E2C06A"), Subject("Консультация", "#E9A37E"))
    )
    val lessonExtra = MutableStateFlow(
        sp.getString("lessonExtra", null)?.let { runCatching { JsonCfg.decodeFromString(MapSerializer(String.serializer(), LessonExtra.serializer()), it) }.getOrNull() } ?: emptyMap()
    )
    val readNotifs = MutableStateFlow(sp.getStringSet("readNotifs", emptySet())!!.toSet())
    var fcmToken: String?
        get() = sp.getString("fcm", null)
        set(v) { sp.edit().putString("fcm", v).apply() }

    fun setTheme(v: String) { theme.value = v; sp.edit().putString("theme", v).apply() }
    fun setOnboarded(v: Boolean) { onboarded.value = v; sp.edit().putBoolean("onboarded", v).apply() }
    fun setNamesFromBook(v: Boolean) { namesFromBook.value = v; sp.edit().putBoolean("book", v).apply() }
    fun setNotifyMessages(v: Boolean) { notifyMessages.value = v; sp.edit().putBoolean("n1", v).apply() }
    fun setNotifyReminders(v: Boolean) { notifyReminders.value = v; sp.edit().putBoolean("n2", v).apply() }
    fun setExtraClasses(v: List<String>) { extraClasses.value = v; sp.edit().putStringSet("classes", v.toSet()).apply() }
    fun setSubjects(v: List<Subject>) { subjects.value = v; sp.edit().putString("subjects", JsonCfg.encodeToString(ListSerializer(Subject.serializer()), v)).apply() }
    fun setLessonExtra(id: String, e: LessonExtra?) {
        val m = lessonExtra.value.toMutableMap(); if (e == null) m.remove(id) else m[id] = e
        lessonExtra.value = m
        sp.edit().putString("lessonExtra", JsonCfg.encodeToString(MapSerializer(String.serializer(), LessonExtra.serializer()), m)).apply()
    }
    fun markNotifsRead(ids: Collection<String>) {
        val s = (readNotifs.value + ids).toList().takeLast(400).toSet()
        readNotifs.value = s; sp.edit().putStringSet("readNotifs", s).apply()
    }
    fun clearAccountData() {
        sp.edit().remove("lessonExtra").remove("readNotifs").remove("classes").remove("fcm").apply()
        lessonExtra.value = emptyMap(); readNotifs.value = emptySet(); extraClasses.value = emptyList()
    }
}

/** Телефонная книга: имя по номеру (WhatsApp и MAX присылают только номер). */
class PhoneBook(private val context: Context) {
    private val cache = HashMap<String, String?>()
    val version = MutableStateFlow(0)
    var demo: Map<String, String>? = null

    fun granted(): Boolean = demo != null ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun digits(phone: String) = phone.filter { it.isDigit() }.let { if (it.length == 11 && it.startsWith("8")) "7" + it.drop(1) else it }

    fun nameFor(phone: String?): String? {
        if (phone.isNullOrBlank() || !granted()) return null
        val key = digits(phone)
        if (key.length < 6) return null
        demo?.let { return it[key] }
        return cache.getOrPut(key) {
            try {
                val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
                context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            } catch (_: Exception) { null }
        }
    }

    fun refresh() { cache.clear(); version.value++ }
}
