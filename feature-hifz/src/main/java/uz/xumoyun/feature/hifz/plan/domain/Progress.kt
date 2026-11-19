package uz.xumoyun.feature.hifz.plan.domain

import java.time.LocalDate
import uz.xumoyun.feature.hifz.domain.SurahInfo

/** Juz holati: yodlangan oyatlar bo'yicha. */
enum class JuzStatus { NONE, PARTIAL, DONE }

/** Juz katagi (30 katakli xarita). */
data class JuzCell(val juz: Int, val total: Int, val memorized: Int) {
    val status: JuzStatus
        get() = when {
            memorized <= 0 -> JuzStatus.NONE
            memorized >= total -> JuzStatus.DONE
            else -> JuzStatus.PARTIAL
        }
}

enum class SurahStatus { NOT_STARTED, IN_PROGRESS, MEMORIZED }

/** Sura ro'yxatidagi qator. */
data class SurahProgress(val surah: SurahInfo, val total: Int, val memorized: Int, val started: Int) {
    val status: SurahStatus
        get() = when {
            memorized >= total && total > 0 -> SurahStatus.MEMORIZED
            started > 0 -> SurahStatus.IN_PROGRESS
            else -> SurahStatus.NOT_STARTED
        }
}

/** Haftalik grafik nuqtasi: shu kuni takrorlangan oyatlar soni. */
data class DayReview(val date: LocalDate, val ayahs: Int)

/** 30 juzning holati. Yodlangan (MEMORIZED) oyatlar sanaladi. */
fun juzCells(layout: QuranLayout, progress: Map<Int, AyahProgress>): List<JuzCell> {
    val counts = IntArray(layout.juzStarts.size + 1)
    for (p in progress.values) {
        if (p.state == MemoState.MEMORIZED) counts[layout.juzOf(p.ayahGlobalId)]++
    }
    return (1..layout.juzStarts.size).map { juz ->
        JuzCell(juz = juz, total = layout.juzRange(juz).count, memorized = counts[juz])
    }
}

/** Har bir sura: jami oyat, yodlangan va boshlangan (yozuvi bor) oyatlar. */
fun surahStatuses(layout: QuranLayout, progress: Map<Int, AyahProgress>): List<SurahProgress> =
    layout.surahs.map { s ->
        val range = layout.surahRange(s.id)
        var memorized = 0
        var started = 0
        for (id in range.from..range.to) {
            val p = progress[id] ?: continue
            started++
            if (p.state == MemoState.MEMORIZED) memorized++
        }
        SurahProgress(surah = s, total = range.count, memorized = memorized, started = started)
    }

/** Oxirgi [days] kun (bugun ham kiradi, eskisidan yangisiga) takrorlangan oyatlar soni. */
fun weeklyReviews(today: LocalDate, sessions: List<ReviewSession>, days: Int = 7): List<DayReview> {
    require(days >= 1) { "kunlar: $days" }
    val from = today.minusDays((days - 1).toLong())
    val byDay = HashMap<LocalDate, Int>()
    for (s in sessions) {
        if (s.date.isBefore(from) || s.date.isAfter(today)) continue
        byDay[s.date] = (byDay[s.date] ?: 0) + (s.ayahTo - s.ayahFrom + 1)
    }
    return (0 until days).map { i ->
        val d = from.plusDays(i.toLong())
        DayReview(d, byDay[d] ?: 0)
    }
}
