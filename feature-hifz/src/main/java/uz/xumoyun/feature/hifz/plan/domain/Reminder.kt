package uz.xumoyun.feature.hifz.plan.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Hifz eslatmasi sozlamalari (TZ 5-bo'lim 16-band: namozdan keyin, 8-bo'lim: Hifz eslatmasi kanali). */
object HifzReminder {
    /** Eslatma bog'lanishi mumkin bo'lgan namozlar (Quyosh chiqishi kiritilmagan). Qiymatlar feature-prayer Prayer.name bilan bir xil. */
    val PRAYER_KEYS: List<String> = listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA")

    /** Namozdan keyingi daqiqalar (tanlov). */
    val OFFSETS_MIN: List<Int> = listOf(0, 5, 10, 15, 20, 30, 45, 60)

    /** Hifz eslatmasi uchun AlarmManager so'rov kodi; feature-prayer kodlari (0..15999, 20000) bilan to'qnashmaydi. */
    const val REQUEST_CODE = 30_001
}

/**
 * Keyingi Hifz eslatmasining vaqti: [prayerKey] namozi vaqti + [offsetMin] daqiqa. Bugun (yoki ertaga/indinga)
 * shu vaqt [now] dan keyin bo'lsa qaytaradi, aks holda null. [prayerTimeOf] — (sana, namoz kaliti) bo'yicha
 * namoz vaqti; manba feature-prayer (app moduli ulaydi), shu sababli bu funksiya feature-prayer'ga bog'lanmaydi.
 */
fun nextHifzReminder(
    now: Instant,
    zone: ZoneId,
    prayerKey: String,
    offsetMin: Int,
    prayerTimeOf: (LocalDate, String) -> Instant?,
): Instant? {
    require(offsetMin >= 0) { "eslatma oralig'i: $offsetMin" }
    if (prayerKey !in HifzReminder.PRAYER_KEYS) return null
    val today = now.atZone(zone).toLocalDate()
    for (d in 0L..2L) {
        val prayer = prayerTimeOf(today.plusDays(d), prayerKey) ?: continue
        val trigger = prayer.plusSeconds(offsetMin * 60L)
        if (trigger.isAfter(now)) return trigger
    }
    return null
}
