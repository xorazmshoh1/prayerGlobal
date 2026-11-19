package uz.xumoyun.feature.prayer.domain

import java.time.LocalDate

/** Ramazon tekshiruvi (hijriy oy = 9). Ilova HijriProvider orqali amalga oshiradi; testda [NONE]. */
interface RamadanCheck {
    fun isRamadan(date: LocalDate): Boolean

    companion object {
        val NONE: RamadanCheck = object : RamadanCheck {
            override fun isRamadan(date: LocalDate): Boolean = false
        }
    }
}

/**
 * Umm al-Qura'da Ramazonda Xufton = Shom + 120 daqiqa (TZ 2-bo'lim). Adhan 1.2.1 Umm al-Qura uchun doim 90 daqiqa
 * beradi, shuning uchun qoida shu yerda qo'llanadi. Boshqa usullarga tegmaydi.
 */
object RamadanIsha {
    const val RAMADAN_INTERVAL_MIN = 120L

    fun apply(method: CalcMethod, base: DayTimes, ramadan: Boolean): DayTimes {
        if (method != CalcMethod.UMM_AL_QURA || !ramadan) return base
        val maghrib = base[Prayer.MAGHRIB]
        return DayTimes(base.date, base.times + (Prayer.ISHA to maghrib.plusMinutes(RAMADAN_INTERVAL_MIN)))
    }
}
