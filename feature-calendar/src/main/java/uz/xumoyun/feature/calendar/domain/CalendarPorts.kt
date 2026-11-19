package uz.xumoyun.feature.calendar.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Namoz vaqtlari manbai. feature-calendar feature-prayer'ga bog'lanmaydi; ilova (app-namoz-hifz/di) ni ulaydi.
 * Vaqt matni tayyor (TimeFormat bilan), nom — feature-prayer string resursi.
 */
interface PrayerTimetablePort {
    /** Joy va [start] dan boshlab 30 kunlik jadval; joy tanlanmagan bo'lsa null. */
    fun timetable(start: Flow<LocalDate>): Flow<PrayerTimetable?>
}

data class PrayerTimeText(val nameRes: Int, val text: String)

data class PrayerDayRow(val date: LocalDate, val times: List<PrayerTimeText>)

data class PrayerTimetable(val city: String, val rows: List<PrayerDayRow>)

/** Taqvim sozlamalari: asosiy tizim (true — hijriy, false — milodiy). */
interface CalendarPrefsStore {
    val hijriPrimary: Flow<Boolean>
    suspend fun setHijriPrimary(value: Boolean)
}

/** Islomiy sana sozlamalari (EventPrefs). Yozilmagan kalitlar uchun [EventSetting.default]. */
interface EventPrefsStore {
    val settings: Flow<Map<IslamicEvent, EventSetting>>
    suspend fun set(event: IslamicEvent, setting: EventSetting)
}
