package uz.xumoyun.feature.hifz.plan.domain

import java.time.LocalDate

/**
 * Reja progressi (TZ 7-bo'lim): [total] — rejadagi oyatlar, [memorized] — MEMORIZED,
 * [remaining] = total − memorized, [progressPercent] = memorized / total × 100,
 * [finishDate] — tugash sanasi prognozi.
 */
data class PlanStats(
    val total: Int,
    val memorized: Int,
    val progressPercent: Double,
    val remaining: Int,
    val finishDate: LocalDate,
)

/** Tugash sanasi = bugun + ceil(qolgan oyatlar / dailyNewAyahs) kun. Qolgan 0 bo'lsa bugun. */
fun finishDate(today: LocalDate, remaining: Int, dailyNewAyahs: Int): LocalDate {
    require(dailyNewAyahs >= 1) { "kunlik yangi oyatlar: $dailyNewAyahs" }
    if (remaining <= 0) return today
    val days = (remaining + dailyNewAyahs - 1) / dailyNewAyahs
    return today.plusDays(days.toLong())
}

/** Reja bo'yicha progress va tugash sanasi. Rejada oyat bo'lmasa progress 0. */
fun planStats(
    today: LocalDate,
    planAyahIds: List<Int>,
    progress: Map<Int, AyahProgress>,
    dailyNewAyahs: Int,
): PlanStats {
    val total = planAyahIds.size
    val memorized = planAyahIds.count { progress[it]?.state == MemoState.MEMORIZED }
    val remaining = total - memorized
    val percent = if (total == 0) 0.0 else memorized * 100.0 / total
    return PlanStats(
        total = total,
        memorized = memorized,
        progressPercent = percent,
        remaining = remaining,
        finishDate = finishDate(today, remaining, dailyNewAyahs),
    )
}
