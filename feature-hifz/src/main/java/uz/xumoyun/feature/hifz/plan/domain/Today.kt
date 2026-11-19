package uz.xumoyun.feature.hifz.plan.domain

import java.time.LocalDate
import kotlin.math.max

/** Taxminiy vaqt (daqiqa, bir oyatga): yangi oyat ko'proq vaqt oladi. Ekrandagi "taxminiy daqiqalar" uchun. */
const val MINUTES_PER_NEW_AYAH = 3
const val MINUTES_PER_REVIEW_AYAH = 1

/** Bugungi darsning bitta kartasi (Sabaq / Sabqi / Manzil). [first]/[last] — birinchi va oxirgi global oyat, bo'sh bo'lsa null. */
data class StepCard(
    val type: ReviewType,
    val first: Int?,
    val last: Int?,
    val count: Int,
    val minutes: Int,
)

/** Hifz tabi uchun to'liq holat. */
data class TodaySummary(
    val hasPlan: Boolean,
    val memorized: Int,
    val total: Int,
    val percent: Double,
    val finishDate: LocalDate?,
    val streakDays: Int,
    val cards: List<StepCard>,
    val items: List<LessonItem>,
    val completedToday: Boolean,
)

fun estimateMinutes(type: ReviewType, count: Int): Int {
    require(count >= 0) { "oyatlar soni: $count" }
    if (count == 0) return 0
    val per = if (type == ReviewType.SABAQ) MINUTES_PER_NEW_AYAH else MINUTES_PER_REVIEW_AYAH
    return max(1, count * per)
}

/**
 * Ketma-ket o'qilgan kunlar. Bugun seans bo'lsa bugundan, aks holda kechadan boshlab orqaga sanaladi.
 * [sessionDates] — seans bo'lgan kunlar (to'plam).
 */
fun streakDays(today: LocalDate, sessionDates: Set<LocalDate>): Int {
    var day = if (today in sessionDates) today else today.minusDays(1)
    var count = 0
    while (day in sessionDates) {
        count++
        day = day.minusDays(1)
    }
    return count
}

/**
 * Hifz tabi holati. Sof funksiya: [today], rejа, progress va seanslar kiradi.
 * [sessions] — kamida bugungi va oxirgi 7 kunning seanslari (Sabqi va bugungi yangi oyatlar uchun);
 * [sessionDates] — streak uchun barcha seans kunlari.
 */
fun buildToday(
    today: LocalDate,
    plan: HifzPlan?,
    progress: Map<Int, AyahProgress>,
    sessions: List<ReviewSession>,
    sessionDates: Set<LocalDate>,
    juzOf: (Int) -> Int,
): TodaySummary {
    val streak = streakDays(today, sessionDates)
    if (plan == null) {
        return TodaySummary(
            hasPlan = false, memorized = 0, total = 0, percent = 0.0, finishDate = null, streakDays = streak,
            cards = emptyList(), items = emptyList(), completedToday = false,
        )
    }
    val ids = plan.ayahIds
    val newToday = sessions
        .filter { it.date == today && it.type == ReviewType.SABAQ }
        .sumOf { it.ayahTo - it.ayahFrom + 1 }
    val stats = planStats(today, ids, progress, plan.dailyNewAyahs)
    val lesson = buildDailyLesson(today, ids, plan.dailyNewAyahs, newToday, progress, sessions, juzOf)
    val cards = listOf(
        stepCard(ReviewType.SABAQ, lesson.sabaq),
        stepCard(ReviewType.SABQI, lesson.sabqi),
        stepCard(ReviewType.MANZIL, lesson.manzil),
    )
    val items = lessonItemsOf(lesson)
    return TodaySummary(
        hasPlan = true,
        memorized = stats.memorized,
        total = stats.total,
        percent = stats.progressPercent,
        finishDate = stats.finishDate,
        streakDays = streak,
        cards = cards,
        items = items,
        completedToday = newToday > 0 && items.isEmpty(),
    )
}

private fun stepCard(type: ReviewType, ids: List<Int>): StepCard = StepCard(
    type = type,
    first = ids.minOrNull(),
    last = ids.maxOrNull(),
    count = ids.size,
    minutes = estimateMinutes(type, ids.size),
)
