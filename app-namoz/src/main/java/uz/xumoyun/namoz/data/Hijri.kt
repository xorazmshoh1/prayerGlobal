package uz.xumoyun.namoz.data

import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class HijriDate(val day: Int, val month: Int, val year: Int, val formatted: String) {
    /** Ramazon — 9-oy. */
    val isRamadan: Boolean get() = month == 9
}

interface HijriProvider {
    fun of(date: LocalDate, offsetDays: Int, locale: Locale = Locale.getDefault()): HijriDate
}

/** Umm al-Qura taqvimi (android.icu, API 24+), sozlamada ±kun tuzatish. */
@Singleton
class IcuHijriProvider @Inject constructor() : HijriProvider {
    override fun of(date: LocalDate, offsetDays: Int, locale: Locale): HijriDate {
        val shifted = date.plusDays(offsetDays.toLong())
        val millis = shifted.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val uLocale = ULocale.forLocale(locale).setKeywordValue("calendar", "islamic-umalqura")
        val cal = IslamicCalendar(TimeZone.getTimeZone("UTC"), uLocale).apply {
            calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
            timeInMillis = millis
        }
        val format = android.icu.text.DateFormat.getDateInstance(cal, android.icu.text.DateFormat.LONG, uLocale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return HijriDate(
            day = cal.get(IslamicCalendar.DAY_OF_MONTH),
            month = cal.get(IslamicCalendar.MONTH) + 1,
            year = cal.get(IslamicCalendar.YEAR),
            formatted = format.format(cal.time),
        )
    }
}
