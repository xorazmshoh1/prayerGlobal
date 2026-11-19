package uz.xumoyun.feature.calendar.domain

import uz.xumoyun.core.common.hijri.HijriCalendar
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Keyingi eslatma: [event] sanasidan 1 kun oldin [fireAt] da. */
data class IslamicReminder(val event: IslamicEvent, val eventDate: LocalDate, val fireAt: Instant)

/**
 * Islomiy sana eslatmasi (TZ 7-bo'lim: EventPrefs, sanadan 1 kun oldin). Vaqt 09:00 (mahalliy).
 * Android'siz; AlarmManager rejalashtiruvi app modulida ([IslamicReminder] ni oladi).
 */
object IslamicReminderPlanner {
    val FIRE_TIME: LocalTime = LocalTime.of(9, 0)
    const val HORIZON_DAYS = 400L

    /**
     * Hozirdan keyingi birinchi eslatma. Kun bo'yicha yuradi: sana [event] bo'lsa va sozlamada yoqilgan
     * bo'lsa, eslatma (sana − 1 kun, [FIRE_TIME]) hozirgidan keyin bo'lsa tanlanadi.
     */
    fun next(
        now: Instant,
        zone: ZoneId,
        hijri: HijriCalendar,
        setting: (IslamicEvent) -> EventSetting,
    ): IslamicReminder? {
        val today = now.atZone(zone).toLocalDate()
        for (k in -1L..HORIZON_DAYS) {
            val eventDate = today.plusDays(k)
            val h = hijri.ymd(eventDate)
            val fireAt = eventDate.minusDays(1).atTime(FIRE_TIME).atZone(zone).toInstant()
            if (!fireAt.isAfter(now)) continue
            val event = IslamicEvent.on(h).firstOrNull { setting(it).enabled && setting(it).remindDayBefore }
            if (event != null) return IslamicReminder(event, eventDate, fireAt)
        }
        return null
    }
}
