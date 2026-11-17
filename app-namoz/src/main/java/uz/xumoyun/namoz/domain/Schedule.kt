package uz.xumoyun.namoz.domain

import java.time.Duration
import java.time.ZonedDateTime

data class NextEvent(val prayer: Prayer, val time: ZonedDateTime, val remaining: Duration)

/** Kun bosqichi: bosh sahifa fon gradienti uchun. */
enum class DayPhase { DAWN, DAY, DUSK, NIGHT }

object Schedule {

    /** Hozirdan keyingi birinchi vaqt (bugun yoki ertangi Bomdod). */
    fun next(now: ZonedDateTime, today: DayTimes, tomorrow: DayTimes): NextEvent {
        val upcoming = today.ordered.firstOrNull { it.second.isAfter(now) } ?: (Prayer.FAJR to tomorrow[Prayer.FAJR])
        return NextEvent(upcoming.first, upcoming.second, Duration.between(now, upcoming.second))
    }

    /** Hozir kirgan vaqt. Bomdoddan oldin — kechagi Xufton. */
    fun current(now: ZonedDateTime, today: DayTimes): Prayer =
        today.ordered.lastOrNull { !it.second.isAfter(now) }?.first ?: Prayer.ISHA

    fun phase(now: ZonedDateTime, today: DayTimes): DayPhase = when {
        now.isBefore(today[Prayer.FAJR]) -> DayPhase.NIGHT
        now.isBefore(today[Prayer.SUNRISE].plusMinutes(45)) -> DayPhase.DAWN
        now.isBefore(today[Prayer.MAGHRIB].minusMinutes(60)) -> DayPhase.DAY
        now.isBefore(today[Prayer.ISHA]) -> DayPhase.DUSK
        else -> DayPhase.NIGHT
    }
}
