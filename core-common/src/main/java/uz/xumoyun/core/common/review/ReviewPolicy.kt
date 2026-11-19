package uz.xumoyun.core.common.review

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * In-App Review qachon so'raladi (TZ 12-bo'lim): faqat qiymat olingandan keyin — 7 kunlik hifz rejasi bajarilganda
 * yoki Ramazonning 10-kunida; 30 kunda ko'pi bilan bir marta. Tugma orqali chaqirilmaydi.
 */
object ReviewPolicy {
    const val HIFZ_STREAK_TRIGGER = 7
    const val RAMADAN_TRIGGER_DAY = 10
    const val MIN_DAYS_BETWEEN_REQUESTS = 30L

    fun shouldRequest(today: LocalDate, lastRequested: LocalDate?, hifzStreakDays: Int, ramadanDay: Int?): Boolean {
        val valuable = hifzStreakDays >= HIFZ_STREAK_TRIGGER || ramadanDay == RAMADAN_TRIGGER_DAY
        if (!valuable) return false
        if (lastRequested == null) return true
        return ChronoUnit.DAYS.between(lastRequested, today) >= MIN_DAYS_BETWEEN_REQUESTS
    }
}
