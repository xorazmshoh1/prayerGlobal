package uz.xumoyun.core.data

import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.core.common.hijri.HijriCalendar
import uz.xumoyun.core.common.hijri.HijriYmd
import uz.xumoyun.core.common.hijri.UmmAlQuraSource
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class HijriDate(val day: Int, val month: Int, val year: Int, val formatted: String) {
    /** Ramazon — 9-oy. */
    val isRamadan: Boolean get() = month == 9
}

/** Hijriy sana manbai; namoz, taqvim va Ramazon modullari umumiy foydalanadi. Testlarda soxta versiya. */
interface HijriProvider {
    /** Ko'rsatiladigan sana (tuzatish bilan) va locale bo'yicha to'liq matn. */
    fun of(date: LocalDate, offsetDays: Int, locale: Locale = Locale.getDefault()): HijriDate

    /** Tuzatishli taqvim: sana → hijriy sana va teskari qidiruv (taqvim, Ramazon modullari shu bilan ishlaydi). */
    fun calendar(offsetDays: Int): HijriCalendar
}

/** Android'dagi Umm al-Qura manbai (android.icu, API 24+). Sof hisob [HijriCalendar] da. */
object AndroidUmmAlQura : UmmAlQuraSource {
    override fun ymd(date: LocalDate): HijriYmd {
        val millis = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val cal = IslamicCalendar(TimeZone.getTimeZone("UTC"), ULocale.forLanguageTag("en-u-ca-islamic-umalqura")).apply {
            calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
            timeInMillis = millis
        }
        return HijriYmd(cal.get(IslamicCalendar.YEAR), cal.get(IslamicCalendar.MONTH) + 1, cal.get(IslamicCalendar.DAY_OF_MONTH))
    }
}

@Singleton
class IcuHijriProvider @Inject constructor() : HijriProvider {
    override fun calendar(offsetDays: Int): HijriCalendar = HijriCalendar(AndroidUmmAlQura, offsetDays)

    override fun of(date: LocalDate, offsetDays: Int, locale: Locale): HijriDate {
        val shown = calendar(offsetDays).ymd(date)
        // Matn uchun umm al-Qura sanasi (tuzatishdan keyingi ko'rinish bilan bir xil kun).
        val raw = date.minusDays(offsetDays.toLong())
        val millis = raw.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val uLocale = ULocale.forLocale(locale).setKeywordValue("calendar", "islamic-umalqura")
        val cal = IslamicCalendar(TimeZone.getTimeZone("UTC"), uLocale).apply {
            calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
            timeInMillis = millis
        }
        val format = android.icu.text.DateFormat.getDateInstance(cal, android.icu.text.DateFormat.LONG, uLocale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return HijriDate(shown.day, shown.month, shown.year, format.format(cal.time))
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class HijriModule {
    @Binds abstract fun hijri(impl: IcuHijriProvider): HijriProvider
}
