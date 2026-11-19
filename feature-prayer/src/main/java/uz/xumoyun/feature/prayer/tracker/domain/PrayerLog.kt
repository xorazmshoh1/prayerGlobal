package uz.xumoyun.feature.prayer.tracker.domain

import uz.xumoyun.feature.prayer.domain.Prayer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** PrayerLog.status (TZ 7-bo'lim). Belgilanmagan kun/namoz — yozuv yo'q (null). */
enum class PrayerStatus { PRAYED, MISSED, QADA }

/** Kuzatuvchidagi bitta yozuv: sana + namoz (FAJR..ISHA) + holat. */
data class PrayerLogRecord(
    val date: LocalDate,
    val prayer: Prayer,
    val status: PrayerStatus,
    val updatedAt: Long,
)

/** Namoz kuzatuvchisi uchun sof mantiq: hafta, belgilash tsikli, ketma-ket kunlar. */
object PrayerLog {
    /** Kuzatuvchida qatnashadigan 5 ta vaqtli namoz (Quyosh va Vitr emas). */
    val TRACKED: List<Prayer> = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

    /** Haftaning 7 kuni, [anchor] kuni joylashgan hafta boshidan (firstDay bilan). */
    fun weekDays(anchor: LocalDate, firstDay: DayOfWeek = DayOfWeek.MONDAY): List<LocalDate> {
        val start = anchor.with(TemporalAdjusters.previousOrSame(firstDay))
        return (0L..6L).map { start.plusDays(it) }
    }

    /**
     * Katakni bosganda: belgilanmagan → O'qildi → O'qilmadi → belgilanmagan (yozuv o'chadi).
     * QADA kuzatuvchi tsiklida qatnashmaydi, lekin QADA yozuvi bo'lsa keyingi qadam belgilanmagan.
     */
    fun nextStatus(current: PrayerStatus?): PrayerStatus? = when (current) {
        null -> PrayerStatus.PRAYED
        PrayerStatus.PRAYED -> PrayerStatus.MISSED
        PrayerStatus.MISSED, PrayerStatus.QADA -> null
    }

    /** Qazo avtomatik qo'shiladigan o'tish: MISSED ga yangi kirish (takror bosish ikki marta qo'shmaydi). */
    fun isMissedTransition(previous: PrayerStatus?, next: PrayerStatus?): Boolean =
        next == PrayerStatus.MISSED && previous != PrayerStatus.MISSED

    /** "Mukammal kun": barcha 5 namoz O'QILDI. */
    fun isPerfectDay(statuses: Map<Prayer, PrayerStatus>): Boolean =
        TRACKED.all { statuses[it] == PrayerStatus.PRAYED }

    /**
     * Ketma-ket mukammal kunlar soni. Bugun hali tugallanmagan bo'lsa, seriya kechagidan
     * hisoblanadi (kun tugamasdan seriya uzilmaydi).
     */
    fun streak(today: LocalDate, perfectDays: Set<LocalDate>): Int {
        var day = if (today in perfectDays) today else today.minusDays(1)
        var count = 0
        while (day in perfectDays) {
            count++
            day = day.minusDays(1)
        }
        return count
    }
}
