package uz.xumoyun.feature.prayer.domain

import java.time.ZonedDateTime

/** Namoz ekranidagi bitta qator: nom, vaqt, joriy vaqtmi, ovoz belgisi (ogohlantirish rejimi). */
data class PrayerRow(val prayer: Prayer, val time: ZonedDateTime, val isCurrent: Boolean, val alert: AlertMode) {
    val alertOn: Boolean get() = alert != AlertMode.OFF
}

/** Namoz ekranining hisoblangan qismi: 6 qator, keyingi vaqt va teskari sanoq, kun bosqichi. */
data class PrayerBoard(
    val rows: List<PrayerRow>,
    val next: NextEvent,
    val current: Prayer?,
    val phase: DayPhase,
) {
    companion object {
        /**
         * Android'siz yig'iladi (unit test qilinadi).
         * Bomdoddan oldin hech bir qator joriy deb belgilanmaydi (kechagi Xufton davom etmoqda).
         */
        fun build(now: ZonedDateTime, today: DayTimes, tomorrow: DayTimes, alertMode: (Prayer) -> AlertMode): PrayerBoard {
            val current = Schedule.current(now, today).takeIf { !now.isBefore(today[Prayer.FAJR]) }
            return PrayerBoard(
                rows = today.ordered.map { (p, t) -> PrayerRow(p, t, isCurrent = p == current, alert = alertMode(p)) },
                next = Schedule.next(now, today, tomorrow),
                current = current,
                phase = Schedule.phase(now, today),
            )
        }
    }
}
