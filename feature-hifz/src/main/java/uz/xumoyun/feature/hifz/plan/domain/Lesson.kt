package uz.xumoyun.feature.hifz.plan.domain

import java.time.LocalDate

/** Darsdagi bitta oyat va u qaysi turga tegishli (Sabaq / Sabqi / Manzil). */
data class LessonItem(val ayahId: Int, val type: ReviewType)

/**
 * Dars holati (TZ 6-bo'lim, 5-ekran). Sof qiymat: har amal yangi holat qaytaradi.
 * [ratings] — oyat id → baho (5, 3 yoki 1); baholangan oyatlar qayta navbatga qo'yilmaydi.
 * [hidden] — matn yashirilgan; [revealedWords] — yashirin rejimda ochilgan so'zlar soni (joriy oyat uchun).
 */
data class LessonState(
    val items: List<LessonItem>,
    val index: Int = 0,
    val ratings: Map<Int, Int> = emptyMap(),
    val hidden: Boolean = false,
    val revealedWords: Int = 0,
) {
    val current: LessonItem? get() = items.getOrNull(index)
    val finished: Boolean get() = items.isNotEmpty() && ratings.size == items.size
    val ratedCount: Int get() = ratings.size
}

/** Bugungi darsni navbatga aylantiradi: oyat bir marta chiqadi, birinchi uchragan tur bo'yicha (Sabaq → Sabqi → Manzil). */
fun lessonItemsOf(lesson: DailyLesson): List<LessonItem> {
    val seen = HashSet<Int>()
    val out = ArrayList<LessonItem>()
    fun add(ids: List<Int>, type: ReviewType) {
        for (id in ids) if (seen.add(id)) out += LessonItem(id, type)
    }
    add(lesson.sabaq, ReviewType.SABAQ)
    add(lesson.sabqi, ReviewType.SABQI)
    add(lesson.manzil, ReviewType.MANZIL)
    return out
}

/** Oyatni baholaydi va keyingi baholanmagan oyatga o'tadi. Faqat 5, 3, 1 qabul qilinadi. */
fun LessonState.rate(quality: Int): LessonState {
    require(quality == Quality.EASY || quality == Quality.HARD || quality == Quality.FORGOT) { "baho: $quality" }
    val cur = current ?: return this
    val newRatings = ratings + (cur.ayahId to quality)
    val next = items.indices.firstOrNull { it > index && items[it].ayahId !in newRatings }
        ?: items.indices.firstOrNull { items[it].ayahId !in newRatings }
        ?: index
    return copy(ratings = newRatings, index = next, revealedWords = 0)
}

/** Berilgan indeksga o'tadi (oraliqqa moslab). Baholash talab qilinmaydi. */
fun LessonState.moveTo(target: Int): LessonState {
    if (items.isEmpty()) return this
    return copy(index = target.coerceIn(0, items.lastIndex), revealedWords = 0)
}

fun LessonState.next(): LessonState = moveTo(index + 1)

fun LessonState.previous(): LessonState = moveTo(index - 1)

/** Matnni yashirish rejimini almashtiradi; ochilgan so'zlar nolga qaytadi. */
fun LessonState.toggleHidden(): LessonState = copy(hidden = !hidden, revealedWords = 0)

/** Yashirin rejimda keyingi so'zni ochadi ([totalWords] — joriy oyatdagi so'zlar soni). */
fun LessonState.revealNextWord(totalWords: Int): LessonState =
    copy(revealedWords = (revealedWords + 1).coerceAtMost(totalWords.coerceAtLeast(0)))

/** Arabcha matnni so'zlarga ajratadi (bo'shliqlar bo'yicha). */
fun arabicWords(text: String): List<String> = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

/**
 * Tugatilgan darsdan seanslar. Har tur (Sabaq/Sabqi/Manzil) ichida ketma-ket oyatlar bitta seans bo'ladi
 * (ReviewSession diapazon bo'lgani uchun). [durationSec] — butun dars davomiyligi; u baholangan oyatlar
 * ulushiga qarab taqsimlanadi. Baholanmagan oyatlar seansga kirmaydi.
 */
fun lessonSessions(
    date: LocalDate,
    items: List<LessonItem>,
    ratings: Map<Int, Int>,
    durationSec: Int,
): List<ReviewSession> {
    require(durationSec >= 0) { "davomiylik: $durationSec" }
    val rated = items.filter { it.ayahId in ratings }
    if (rated.isEmpty()) return emptyList()
    val out = ArrayList<ReviewSession>()
    for (type in ReviewType.entries) {
        val ids = rated.filter { it.type == type }.map { it.ayahId }.sorted()
        if (ids.isEmpty()) continue
        for (run in contiguousRuns(ids)) {
            val qualities = run.map { ratings.getValue(it) }
            val share = durationSec.toLong() * run.size / rated.size
            out += ReviewSession(
                date = date,
                type = type,
                ayahFrom = run.first(),
                ayahTo = run.last(),
                durationSec = share.toInt(),
                avgRating = qualities.average(),
            )
        }
    }
    return out
}

/** Tartiblangan id ro'yxatini ketma-ket (+1) bo'laklarga ajratadi. Bo'sh bo'lsa bo'sh ro'yxat. */
internal fun contiguousRuns(sortedIds: List<Int>): List<List<Int>> {
    if (sortedIds.isEmpty()) return emptyList()
    val runs = ArrayList<List<Int>>()
    var current = arrayListOf(sortedIds.first())
    for (i in 1 until sortedIds.size) {
        if (sortedIds[i] == sortedIds[i - 1] + 1) {
            current += sortedIds[i]
        } else {
            runs += current
            current = arrayListOf(sortedIds[i])
        }
    }
    runs += current
    return runs
}
