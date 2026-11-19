package uz.xumoyun.feature.prayer.tracker.domain

import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.Prayer
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Qazo hisoblagich kalitlari (TZ 7-bo'lim, QadaCounter.prayer).
 * FAST (ro'za qazosi) 11-bosqichda Ramazon menyusi bilan bog'lanadi; bu yerdagi namoz jamlariga kirmaydi.
 */
enum class QadaPrayer {
    FAJR, DHUHR, ASR, MAGHRIB, ISHA, WITR, FAST;

    companion object {
        /** Besh vaqtli namoz — [Prayer] dan. SUNRISE qazo emas, null qaytadi. */
        fun of(prayer: Prayer): QadaPrayer? = when (prayer) {
            Prayer.FAJR -> FAJR
            Prayer.DHUHR -> DHUHR
            Prayer.ASR -> ASR
            Prayer.MAGHRIB -> MAGHRIB
            Prayer.ISHA -> ISHA
            Prayer.SUNRISE -> null
        }
    }
}

/** QadaEvent.source: qo'lda, o'tkazib yuborilgan namoz avtomatik, taxminiy hisoblash ustasi. */
enum class QadaSource { MANUAL, AUTO_MISSED, WIZARD }

/** Qolgan qazo soni (remaining) va kunlik maqsad. updatedAt — millisekund. */
data class QadaCounter(
    val prayer: QadaPrayer,
    val remaining: Int = 0,
    val initialEstimate: Int = 0,
    val dailyGoal: Int = 0,
    val updatedAt: Long = 0,
)

/** Qazo o'zgarishi tarixi: delta — haqiqatda qo'llangan o'zgarish (cheklovdan keyin). */
data class QadaEvent(
    val id: Long = 0,
    val prayer: QadaPrayer,
    val delta: Int,
    val source: QadaSource,
    val createdAt: Long,
)

/**
 * Qazo hisobi (sof Kotlin). Hanafiy mazhabida Vitr ham qazo hisoblanadi; Shofeiyda Vitr yo'q.
 */
object Qada {
    /** Bitta hisoblagich uchun yuqori chegara (xato kiritishdan himoya). */
    const val MAX_REMAINING = 1_000_000
    const val MAX_DAILY_GOAL = 100

    private val SHAFI_PRAYERS = listOf(QadaPrayer.FAJR, QadaPrayer.DHUHR, QadaPrayer.ASR, QadaPrayer.MAGHRIB, QadaPrayer.ISHA)
    private val HANAFI_PRAYERS = SHAFI_PRAYERS + QadaPrayer.WITR

    /** Mazhabga mos namoz hisoblagichlari (tartib bo'yicha). */
    fun prayersFor(madhab: AsrMadhab): List<QadaPrayer> =
        if (madhab == AsrMadhab.HANAFI) HANAFI_PRAYERS else SHAFI_PRAYERS

    /** Qo'llangan o'zgarish: natija 0 dan kam bo'lmaydi va [MAX_REMAINING] dan oshmaydi. */
    fun applyDelta(current: Int, delta: Int): Int =
        (current.toLong() + delta).coerceIn(0L, MAX_REMAINING.toLong()).toInt()

    /**
     * Ikki chegara ham kiradigan kunlar soni (inklyuziv). 2020-01-01..2020-12-31 = 366.
     * Oraliq teskari bo'lsa 0.
     */
    fun inclusiveDays(start: LocalDate, end: LocalDate): Int =
        if (end.isBefore(start)) 0 else (ChronoUnit.DAYS.between(start, end) + 1).coerceAtMost(MAX_REMAINING.toLong()).toInt()

    /** Taxminiy hisob: har namoz uchun bir xil son (kunlar soni × 1), mazhabga qarab Vitr bilan. */
    fun estimateTotal(perPrayer: Int, madhab: AsrMadhab): Int =
        (perPrayer.toLong() * prayersFor(madhab).size).coerceAtMost(MAX_REMAINING.toLong()).toInt()

    /** Mazhabdagi namozlar bo'yicha umumiy qolgan son. */
    fun totalRemaining(counters: Collection<QadaCounter>, madhab: AsrMadhab): Int =
        prayersFor(madhab).sumOf { p -> counters.firstOrNull { it.prayer == p }?.remaining ?: 0 }

    /**
     * Tugash sanasi: bugun + ceil(umumiy qolgan / Σ kunlik maqsad). Uniform maqsadda bu
     * ceil(qolgan / (maqsad × namozlar soni)) ga teng. Maqsad 0 bo'lsa null (tugash sanasi yo'q).
     */
    fun finishDate(today: LocalDate, counters: Collection<QadaCounter>, madhab: AsrMadhab): LocalDate? {
        val list = prayersFor(madhab).map { p -> counters.firstOrNull { it.prayer == p } ?: QadaCounter(p) }
        val total = list.sumOf { it.remaining.toLong() }
        if (total == 0L) return today
        val perDay = list.sumOf { it.dailyGoal.toLong() }
        if (perDay <= 0L) return null
        return today.plusDays((total + perDay - 1) / perDay)
    }
}
