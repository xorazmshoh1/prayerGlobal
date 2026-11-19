package uz.xumoyun.core.common.hijri

import java.time.LocalDate

/** Hijriy sana: yil, oy (1–12), kun (1–30). Android'siz. */
data class HijriYmd(val year: Int, val month: Int, val day: Int) {
    init {
        require(month in 1..12 && day in 1..30) { "Noto'g'ri hijriy sana: $year/$month/$day" }
    }
}

object HijriMonth {
    const val MUHARRAM = 1
    const val RAJAB = 7
    const val SHABAN = 8
    const val RAMADAN = 9
    const val SHAWWAL = 10
    const val DHUL_QADAH = 11
    const val DHUL_HIJJAH = 12
}

/**
 * Umm al-Qura jadvali, tuzatishsiz. Android'da android.icu.util.IslamicCalendar (ISLAMIC_UMALQURA),
 * unit testlarda ICU4J (core-common/src/test) — ikkalasi bir xil ICU ma'lumotidan.
 */
fun interface UmmAlQuraSource {
    fun ymd(date: LocalDate): HijriYmd
}

/**
 * Ko'rsatiladigan hijriy sana: sozlamadagi ±kun tuzatish (TZ 2-bo'lim, hijriAdjustDays).
 *
 * Tuzatish +1 bo'lsa, har bir sana Umm al-Qura'dan bir kun "orqada" ko'rinadi:
 * shown(d) = umm(d − offset). Shu sababli TZ: hijriAdjustDays = +1 bo'lsa 1 Ramazon 2027-02-09.
 */
class HijriCalendar(private val source: UmmAlQuraSource, val offsetDays: Int = 0) {
    init {
        require(offsetDays in -MAX_OFFSET_DAYS..MAX_OFFSET_DAYS) { "Tuzatish ±$MAX_OFFSET_DAYS kundan oshmasin" }
    }

    fun ymd(date: LocalDate): HijriYmd = source.ymd(date.minusDays(offsetDays.toLong()))

    /**
     * Ko'rsatiladigan [target] hijriy sanaga mos milodiy sana. Umm al-Qura bo'yicha teskari qidiruv:
     * tabular taxmin atrofida ±[SEARCH_DAYS] kun. Topilmasa null.
     */
    fun gregorianOf(target: HijriYmd): LocalDate? {
        val guess = tabularGuess(target).plusDays(offsetDays.toLong())
        for (k in 0..SEARCH_DAYS) {
            for (candidate in listOf(guess.plusDays(k.toLong()), guess.minusDays(k.toLong()))) {
                if (ymd(candidate) == target) return candidate
            }
        }
        return null
    }

    companion object {
        /** TZ 2-bo'lim: tuzatish −1/0/+1. */
        const val MAX_OFFSET_DAYS = 1
        const val SEARCH_DAYS = 40

        /** Islomiy fuqarolik (tabular) taqvimi: taxmin uchun, ±2 kun farq bo'lishi mumkin. */
        fun tabularGuess(h: HijriYmd): LocalDate {
            val jdn = h.day + Math.ceil(29.5 * (h.month - 1)).toLong() + (h.year - 1).toLong() * 354 +
                Math.floorDiv(3L + 11L * h.year, 30L) + 1948439L
            return LocalDate.ofEpochDay(jdn - 2440588L)
        }
    }
}
