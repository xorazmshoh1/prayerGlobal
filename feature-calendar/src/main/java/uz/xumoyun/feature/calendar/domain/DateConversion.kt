package uz.xumoyun.feature.calendar.domain

import uz.xumoyun.core.common.hijri.HijriCalendar
import uz.xumoyun.core.common.hijri.HijriYmd
import java.time.LocalDate

data class DateConversion(val gregorian: LocalDate, val hijri: HijriYmd)

/** Sana konvertori (TZ 5-bo'lim, 3-band): ikki yo'nalishda. Hijriy kiritish noto'g'ri bo'lsa null. */
object DateConverter {
    fun fromGregorian(date: LocalDate, hijri: HijriCalendar): DateConversion =
        DateConversion(date, hijri.ymd(date))

    fun fromHijri(year: Int, month: Int, day: Int, hijri: HijriCalendar): DateConversion? {
        val h = runCatching { HijriYmd(year, month, day) }.getOrNull() ?: return null
        val g = hijri.gregorianOf(h) ?: return null
        return DateConversion(g, h)
    }
}
