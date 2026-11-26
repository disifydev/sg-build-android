package ru.sguardo.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Часы приложения: в демо-режиме время застыло на пятнице 27 ноября 2026, 16:20 по Москве (как в макете). */
object Clock {
    @Volatile var fixed: Long? = null
    @Volatile var zone: ZoneId = ZoneId.systemDefault()
    fun now(): Long = fixed ?: System.currentTimeMillis()
    fun today(): LocalDate = Instant.ofEpochMilli(now()).atZone(zone).toLocalDate()
    fun local(ms: Long): ZonedDateTime = Instant.ofEpochMilli(ms).atZone(zone)
    fun startOfDay(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()
    fun at(d: LocalDate, h: Int, m: Int): Long = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
    private val ISO = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    fun isoLocal(ms: Long): String = local(ms).format(ISO)
    /** Формат для /api/schedule: «2026-11-27T18:30» в поясе пользователя. */
    fun localStamp(d: LocalDate, h: Int, m: Int): String = LocalDateTime.of(d.year, d.month, d.dayOfMonth, h, m).toString().take(16)
}

object Fmt {
    private val MON = listOf("янв", "февр", "марта", "апр", "мая", "июня", "июля", "авг", "сент", "окт", "нояб", "дек")
    private val WD = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    private val WD_FULL = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")

    fun hm(ms: Long): String { val t = Clock.local(ms); return "%02d:%02d".format(t.hour, t.minute) }
    fun dayMonth(ms: Long): String { val t = Clock.local(ms); return "${t.dayOfMonth} ${MON[t.monthValue - 1]}" }
    fun dayMonth(d: LocalDate): String = "${d.dayOfMonth} ${MON[d.monthValue - 1]}"
    fun wd(d: LocalDate): String = WD[d.dayOfWeek.value - 1]
    fun wdFull(d: LocalDate): String = WD_FULL[d.dayOfWeek.value - 1]

    /** Время в списке чатов: сегодня «16:13», вчера «Вчера», на этой неделе «Пн», иначе «12 нояб». */
    fun listTime(ms: Long): String {
        if (ms <= 0) return ""
        val d = Clock.local(ms).toLocalDate(); val today = Clock.today()
        val days = ChronoUnit.DAYS.between(d, today)
        return when {
            days <= 0L -> hm(ms)
            days == 1L -> "Вчера"
            days < 7L -> WD[d.dayOfWeek.value - 1]
            else -> dayMonth(ms)
        }
    }

    /** Подпись даты в ленте чата. */
    fun dayChip(ms: Long): String {
        val d = Clock.local(ms).toLocalDate(); val today = Clock.today()
        val days = ChronoUnit.DAYS.between(d, today)
        return when (days) {
            0L -> "Сегодня"; 1L -> "Вчера"
            else -> dayMonth(ms) + if (d.year != today.year) " ${d.year}" else ""
        }
    }

    fun dayLabel(d: LocalDate): String =
        if (d == Clock.today()) "Сегодня, ${WD_FULL[d.dayOfWeek.value - 1].lowercase()}" else "${WD_FULL[d.dayOfWeek.value - 1]}, ${dayMonth(d)}"

    fun weekStart(d: LocalDate): LocalDate = d.with(DayOfWeek.MONDAY)

    fun rub(n: Long): String = group(n) + " ₽"
    fun group(n: Long): String {
        val s = kotlin.math.abs(n).toString().reversed().chunked(3).joinToString(" ").reversed()
        return if (n < 0) "-$s" else s
    }

    fun plural(n: Int, one: String, few: String, many: String): String {
        val m = n % 10; val h = n % 100
        return if (m == 1 && h != 11) one else if (m in 2..4 && (h < 10 || h >= 20)) few else many
    }

    fun dur(sec: Int): String = "${sec / 60}:${"%02d".format(sec % 60)}"

    /** «был(а) в сети в 14:05 / вчера в 14:05 / 12 нояб». */
    fun lastSeen(ms: Long): String {
        val d = Clock.local(ms).toLocalDate(); val days = ChronoUnit.DAYS.between(d, Clock.today())
        return when (days) {
            0L -> "был(а) в сети в ${hm(ms)}"
            1L -> "был(а) в сети вчера в ${hm(ms)}"
            else -> "был(а) в сети ${dayMonth(ms)}"
        }
    }
}
