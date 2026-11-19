package uz.xumoyun.feature.calendar.domain

import uz.xumoyun.core.common.hijri.HijriMonth
import uz.xumoyun.core.common.hijri.HijriYmd

/**
 * Islomiy sanalar (TZ 4-bo'lim, 9 ta). Kod ichida, bazada emas. 27 Rajab va 15 Sha'bon standartda o'chiq.
 * Kalit ([key]) — sozlamalar va bildirishnoma ID'lari uchun barqaror qiymat.
 */
enum class IslamicEvent(val key: String, val hijriMonth: Int, val hijriDay: Int, val enabledByDefault: Boolean) {
    MUHARRAM_1("muharram_1", HijriMonth.MUHARRAM, 1, true),
    ASHURA("ashura", HijriMonth.MUHARRAM, 10, true),
    RAJAB_27("rajab_27", HijriMonth.RAJAB, 27, false),
    SHABAN_15("shaban_15", HijriMonth.SHABAN, 15, false),
    RAMADAN_1("ramadan_1", HijriMonth.RAMADAN, 1, true),
    LAYLAT_AL_QADR("laylat_al_qadr", HijriMonth.RAMADAN, 27, true),
    EID_AL_FITR("eid_al_fitr", HijriMonth.SHAWWAL, 1, true),
    ARAFA("arafa", HijriMonth.DHUL_HIJJAH, 9, true),
    EID_AL_ADHA("eid_al_adha", HijriMonth.DHUL_HIJJAH, 10, true);

    fun matches(h: HijriYmd): Boolean = h.month == hijriMonth && h.day == hijriDay

    companion object {
        fun on(h: HijriYmd): List<IslamicEvent> = entries.filter { it.matches(h) }
        fun byKey(key: String): IslamicEvent? = entries.firstOrNull { it.key == key }
    }
}

/** Foydalanuvchi sozlamasi: yoqilgan-yo'qligi va sanadan 1 kun oldin eslatma. Yozilmagan bo'lsa standart. */
data class EventSetting(val enabled: Boolean, val remindDayBefore: Boolean) {
    companion object {
        fun default(event: IslamicEvent) = EventSetting(enabled = event.enabledByDefault, remindDayBefore = false)
    }
}
