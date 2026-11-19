package uz.xumoyun.feature.calendar.domain

import uz.xumoyun.core.common.hijri.HijriYmd
import uz.xumoyun.core.common.hijri.UmmAlQuraSource
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Sinov uchun soddalashtirilgan Umm al-Qura: 1 Ramazon 1448 = 2027-02-08, Ramazon 29 kun, Shavvol 1 = 2027-03-09.
 * Haqiqiy jadval (ICU) core-common testlarida tekshiriladi; bu yerda faqat domain mantig'i.
 */
object FakeUmmAlQura : UmmAlQuraSource {
    private val ramadanStart: LocalDate = LocalDate.of(2027, 2, 8)

    override fun ymd(date: LocalDate): HijriYmd {
        val k = ChronoUnit.DAYS.between(ramadanStart, date).toInt()
        return when {
            k < 0 -> HijriYmd(1448, 8, (k + 31).coerceIn(1, 30))
            k < 29 -> HijriYmd(1448, 9, k + 1)
            else -> HijriYmd(1448, 10, ((k - 29) % 29) + 1)
        }
    }
}
