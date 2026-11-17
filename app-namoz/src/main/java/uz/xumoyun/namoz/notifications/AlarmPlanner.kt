package uz.xumoyun.namoz.notifications

import uz.xumoyun.namoz.data.PrayerAlert
import uz.xumoyun.namoz.domain.DayTimes
import uz.xumoyun.namoz.domain.Prayer
import java.time.Instant

/** Bitta rejalashtiriladigan signal. */
data class PlannedAlarm(
    val requestCode: Int,
    val prayer: Prayer,
    val triggerAt: Instant,
    val prayerTime: Instant,
    val isReminder: Boolean,
)

/** Qaysi signallar qo'yilishini hisoblaydi (Android'siz, unit test qilinadi). */
object AlarmPlanner {

    fun plan(now: Instant, days: List<DayTimes>, alerts: Map<Prayer, PrayerAlert>): List<PlannedAlarm> = buildList {
        days.forEach { day ->
            Prayer.entries.forEach { prayer ->
                val alert = alerts[prayer] ?: return@forEach
                if (!alert.enabled) return@forEach
                val time = day[prayer].toInstant()
                if (time.isAfter(now)) add(PlannedAlarm(code(day, prayer, false), prayer, time, time, false))
                if (alert.minutesBefore > 0) {
                    val pre = time.minusSeconds(alert.minutesBefore * 60L)
                    if (pre.isAfter(now)) add(PlannedAlarm(code(day, prayer, true), prayer, pre, time, true))
                }
            }
        }
    }

    /** Kun + namoz + eslatma turi bo'yicha barqaror kod: qayta rejalashtirish eskisini almashtiradi. */
    fun code(day: DayTimes, prayer: Prayer, reminder: Boolean): Int =
        ((day.date.toEpochDay() % 1000).toInt() * 20) + prayer.ordinal * 2 + if (reminder) 1 else 0

    fun allCodes(days: List<DayTimes>): List<Int> =
        days.flatMap { d -> Prayer.entries.flatMap { listOf(code(d, it, false), code(d, it, true)) } }
}
