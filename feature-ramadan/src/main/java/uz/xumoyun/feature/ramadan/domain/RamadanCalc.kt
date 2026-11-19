package uz.xumoyun.feature.ramadan.domain

import uz.xumoyun.core.common.hijri.HijriCalendar
import uz.xumoyun.core.common.hijri.HijriMonth
import uz.xumoyun.core.common.hijri.HijriYmd
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class FastStatus { FASTED, MISSED, EXCUSED }

enum class RamadanPhase { BEFORE, DURING, AFTER }

/** Bitta kunning Bomdod va Shom vaqti (feature-prayer'dan, port orqali). */
data class RamadanDayTimes(val date: LocalDate, val fajr: Instant, val maghrib: Instant)

/** Ramazon sozlamalari (TZ 7-bo'lim, RamadanSettings). Standart qiymatlar TZ bo'yicha. */
data class RamadanSettings(
    val imsakOffsetMin: Int = 10,
    val suhoorOn: Boolean = true,
    val suhoorMinBefore: Int = 30,
    val iftarOn: Boolean = true,
    val iftarMinBefore: Int = 0,
    val qadrOn: Boolean = true,
) {
    companion object {
        const val IMSAK_OFFSET_MAX = 20
        const val SUHOOR_MIN_MIN = 15
        const val SUHOOR_MIN_MAX = 60
        const val IFTAR_MIN_MAX = 15
    }
}

data class RamadanDayRow(
    val number: Int,
    val date: LocalDate,
    val hijri: HijriYmd,
    val imsak: Instant?,
    val iftar: Instant?,
    val fast: FastStatus?,
    val isToday: Boolean,
    val isQadrCandidate: Boolean,
)

/**
 * Ramazon hisoblari (TZ 7-bo'lim): imsok = Bomdod − imsakOffsetMin; iftor = Shom; Ramazon kuni;
 * Umm al-Qura'da Ramazonda Xufton = Shom + 120; qadr kechasi nomzodlari (oxirgi 10 kunning toq kunlari).
 */
object RamadanCalc {
    const val DAYS = 30

    fun imsak(fajr: Instant, offsetMin: Int): Instant = fajr.minus(Duration.ofMinutes(offsetMin.toLong()))

    /** Ramazon 1 ning milodiy sanasi (ko'rsatiladigan hijriy tuzatish bilan). */
    fun start(hijri: HijriCalendar, hijriYear: Int): LocalDate? = hijri.gregorianOf(HijriYmd(hijriYear, HijriMonth.RAMADAN, 1))

    fun end(start: LocalDate): LocalDate = start.plusDays(DAYS - 1L)

    /** Bugun Ramazonning nechanchi kuni (1..30); Ramazondan tashqarida null. */
    fun dayNumber(date: LocalDate, start: LocalDate): Int? {
        val n = ChronoUnit.DAYS.between(start, date).toInt() + 1
        return n.takeIf { it in 1..DAYS }
    }

    fun dateOf(start: LocalDate, number: Int): LocalDate = start.plusDays(number - 1L)

    fun phase(today: LocalDate, start: LocalDate): RamadanPhase = when {
        today.isBefore(start) -> RamadanPhase.BEFORE
        today.isAfter(end(start)) -> RamadanPhase.AFTER
        else -> RamadanPhase.DURING
    }

    /** Joriy yoki keyingi Ramazon boshi (o'tgan Ramazon o'tkazib yuboriladi). */
    fun upcomingStart(today: LocalDate, hijri: HijriCalendar): LocalDate? {
        val y = hijri.ymd(today).year
        return listOf(y, y + 1).mapNotNull { start(hijri, it) }.firstOrNull { !today.isAfter(end(it)) }
    }

    /** Namoz tabidagi katta karta: Ramazondan 15 kun oldin va Ramazon davomida (TZ 11-bo'lim). */
    fun cardVisible(today: LocalDate, start: LocalDate): Boolean =
        !today.isBefore(start.minusDays(15)) && !today.isAfter(end(start))

    /** Umm al-Qura: Xufton Shomdan keyin 90 daqiqa, Ramazonda 120 daqiqa (TZ 2-bo'lim). */
    fun ishaMinutesAfterMaghrib(isRamadan: Boolean): Int = if (isRamadan) 120 else 90

    /** Qadr kechasi nomzodi: oxirgi 10 kunning toq kunlari (21, 23, 25, 27, 29). */
    fun isQadrCandidate(number: Int): Boolean = number in 21..29 && number % 2 == 1

    /** 30 kunlik jadval (R2). [times] kun bo'yicha vaqt; [fasts] — belgilangan holatlar. */
    fun dayRows(
        start: LocalDate,
        today: LocalDate,
        hijri: HijriCalendar,
        imsakOffsetMin: Int,
        times: (LocalDate) -> RamadanDayTimes?,
        fasts: Map<LocalDate, FastStatus>,
    ): List<RamadanDayRow> = (1..DAYS).map { n ->
        val date = dateOf(start, n)
        val t = times(date)
        RamadanDayRow(
            number = n,
            date = date,
            hijri = hijri.ymd(date),
            imsak = t?.let { imsak(it.fajr, imsakOffsetMin) },
            iftar = t?.maghrib,
            fast = fasts[date],
            isToday = date == today,
            isQadrCandidate = isQadrCandidate(n),
        )
    }
}

/** Ro'za qazosi: holat o'zgarganda QadaCounter(FAST) ga qo'shiladigan o'zgarish (TZ 7: MISSED → +1). */
object FastCalc {
    fun qadaDelta(old: FastStatus?, new: FastStatus?): Int = when {
        new == FastStatus.MISSED && old != FastStatus.MISSED -> 1
        old == FastStatus.MISSED && new != FastStatus.MISSED -> -1
        else -> 0
    }
}

/** Xatm rejasi (R5): kunlik juz = (30 − juzDone) / qolgan kunlar (bugun ham kiradi). */
object KhatmCalc {
    const val JUZ_TOTAL = 30

    fun remainingDays(today: LocalDate, start: LocalDate): Int {
        val end = RamadanCalc.end(start)
        return (ChronoUnit.DAYS.between(today, end).toInt() + 1).coerceIn(1, RamadanCalc.DAYS)
    }

    fun dailyJuz(juzDone: Int, today: LocalDate, start: LocalDate): Double {
        val left = (JUZ_TOTAL - juzDone.coerceIn(0, JUZ_TOTAL)).toDouble()
        return left / remainingDays(today, start)
    }

    /** Bugun o'qish kerak bo'lgan juzlar soni (yaxlitlangan, kamida 1 agar xatm tugamagan bo'lsa). */
    fun todayTarget(juzDone: Int, today: LocalDate, start: LocalDate): Int {
        if (juzDone >= JUZ_TOTAL) return 0
        return Math.ceil(dailyJuz(juzDone, today, start)).toInt().coerceAtLeast(1)
    }
}

enum class RamadanReminderKind { SUHOOR, IFTAR, QADR }

data class RamadanReminder(val kind: RamadanReminderKind, val date: LocalDate, val fireAt: Instant)

/**
 * Ramazon eslatmalari: saharlik (imsokdan N daqiqa oldin), iftor (shomdan M daqiqa oldin), qadr (oxirgi 10 kunning
 * toq kechalarida shom vaqtida). Keyingi bitta eslatma; ilova uni AlarmManager'ga qo'yadi.
 */
object RamadanReminderPlanner {
    fun next(
        now: Instant,
        zone: ZoneId,
        hijri: HijriCalendar,
        times: (LocalDate) -> RamadanDayTimes?,
        s: RamadanSettings,
    ): RamadanReminder? {
        val today = now.atZone(zone).toLocalDate()
        val y = hijri.ymd(today).year
        var best: RamadanReminder? = null
        for (year in listOf(y, y + 1)) {
            val start = RamadanCalc.start(hijri, year) ?: continue
            for (n in 1..RamadanCalc.DAYS) {
                val date = RamadanCalc.dateOf(start, n)
                val t = times(date) ?: continue
                val candidates = buildList {
                    if (s.suhoorOn) add(RamadanReminder(RamadanReminderKind.SUHOOR, date, RamadanCalc.imsak(t.fajr, s.imsakOffsetMin).minus(Duration.ofMinutes(s.suhoorMinBefore.toLong()))))
                    if (s.iftarOn) add(RamadanReminder(RamadanReminderKind.IFTAR, date, t.maghrib.minus(Duration.ofMinutes(s.iftarMinBefore.toLong()))))
                    if (s.qadrOn && RamadanCalc.isQadrCandidate(n)) add(RamadanReminder(RamadanReminderKind.QADR, date, t.maghrib))
                }
                for (c in candidates) {
                    if (c.fireAt.isAfter(now) && (best == null || c.fireAt.isBefore(best.fireAt))) best = c
                }
            }
            // Bu yilning Ramazonida eslatma topilsa, keyingi yil izlanmaydi (u doim undan keyin).
            if (best != null) break
        }
        return best
    }
}
