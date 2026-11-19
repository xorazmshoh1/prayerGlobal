package uz.xumoyun.core.common.hijri

import com.ibm.icu.util.IslamicCalendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import uz.xumoyun.core.common.hijri.HijriYmd
import uz.xumoyun.core.common.hijri.UmmAlQuraSource
import java.time.LocalDate
import java.time.ZoneOffset

/** Testdagi referens: ICU4J IslamicCalendar (ISLAMIC_UMALQURA). Ilovada android.icu bilan bir xil algoritm. */
object Icu4jUmmAlQura : UmmAlQuraSource {
    override fun ymd(date: LocalDate): HijriYmd {
        val cal = IslamicCalendar(TimeZone.getTimeZone("UTC"), ULocale("en@calendar=islamic-umalqura")).apply {
            calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
            timeInMillis = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        }
        return HijriYmd(cal.get(IslamicCalendar.YEAR), cal.get(IslamicCalendar.MONTH) + 1, cal.get(IslamicCalendar.DAY_OF_MONTH))
    }
}
