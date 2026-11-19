package uz.xumoyun.feature.hifz.plan.domain

import uz.xumoyun.feature.hifz.domain.SurahInfo

/** Oyat diapazoni (global id, ikkalasi ham kiradi). */
data class AyahRange(val from: Int, val to: Int) {
    init {
        require(from in 1..QURAN_AYAH_COUNT && to in from..QURAN_AYAH_COUNT) { "oyat oralig'i: $from..$to" }
    }

    val count: Int get() = to - from + 1

    val ids: List<Int> get() = (from..to).toList()
}

/** Oyatning joyi: sura raqami va suradagi tartib raqami. */
data class AyahLocation(val surahId: Int, val numberInSurah: Int)

/** Hifz rejasi nishoni (TZ 6-bo'lim, 6-ekran): sura, juz yoki butun Qur'on. */
sealed interface PlanTarget {
    val scope: PlanScope

    data class Surah(val surahId: Int) : PlanTarget {
        override val scope: PlanScope get() = PlanScope.SURAH
    }

    data class Juz(val juz: Int) : PlanTarget {
        override val scope: PlanScope get() = PlanScope.JUZ
    }

    data object All : PlanTarget {
        override val scope: PlanScope get() = PlanScope.ALL
    }
}

/**
 * Qur'on tuzilmasi: suralar (quran.db `surah`) va har juzning boshlang'ich global oyat raqami.
 * Juzlar Qur'onda ketma-ket joylashgan, shu sababli juz va oyat orasidagi moslik ikkilik qidiruv bilan topiladi.
 */
class QuranLayout(val surahs: List<SurahInfo>, val juzStarts: List<Int>) {
    init {
        require(surahs.isNotEmpty()) { "suralar bo'sh" }
        require(juzStarts.isNotEmpty() && juzStarts.first() == 1) { "juz boshlanishi 1 dan bo'lishi kerak" }
        require(juzStarts.zipWithNext().all { (a, b) -> a < b }) { "juz boshlanishlari o'sib borishi kerak" }
    }

    /** Juz raqami (1..30) — [ayahId] qaysi juzga tegishli. */
    fun juzOf(ayahId: Int): Int {
        require(ayahId in 1..QURAN_AYAH_COUNT) { "oyat raqami: $ayahId" }
        val idx = juzStarts.binarySearch(ayahId)
        val insertion = if (idx >= 0) idx else -idx - 1
        return if (idx >= 0) insertion + 1 else insertion
    }

    fun juzRange(juz: Int): AyahRange {
        require(juz in 1..juzStarts.size) { "juz: $juz" }
        val from = juzStarts[juz - 1]
        val to = if (juz == juzStarts.size) QURAN_AYAH_COUNT else juzStarts[juz] - 1
        return AyahRange(from, to)
    }

    fun surah(surahId: Int): SurahInfo? = surahs.firstOrNull { it.id == surahId }

    fun surahRange(surahId: Int): AyahRange {
        val s = surah(surahId) ?: throw IllegalArgumentException("sura topilmadi: $surahId")
        return AyahRange(s.startAyahGlobal, s.startAyahGlobal + s.ayahCount - 1)
    }

    /** Global oyat raqamini sura va suradagi raqamga aylantiradi. */
    fun locate(ayahId: Int): AyahLocation {
        val s = surahs.last { it.startAyahGlobal <= ayahId }
        return AyahLocation(s.id, ayahId - s.startAyahGlobal + 1)
    }

    fun rangeOf(target: PlanTarget): AyahRange = when (target) {
        is PlanTarget.Surah -> surahRange(target.surahId)
        is PlanTarget.Juz -> juzRange(target.juz)
        PlanTarget.All -> AyahRange(1, QURAN_AYAH_COUNT)
    }

    /** Rejani yaratgan nishon (sura/juz tanlovini qayta tiklash uchun). Mos kelmasa null. */
    fun targetOf(plan: HifzPlan): PlanTarget? = when (plan.scope) {
        PlanScope.ALL -> PlanTarget.All
        PlanScope.JUZ -> juzStarts.indexOf(plan.fromAyah).takeIf { it >= 0 }?.let { PlanTarget.Juz(it + 1) }
        PlanScope.SURAH -> surahs.firstOrNull { it.startAyahGlobal == plan.fromAyah }?.let { PlanTarget.Surah(it.id) }
    }
}

/**
 * Rejani tuzadi: nishon oralig'i va sozlamalar. Eslatma kaliti (namoz nomi) va oralig' [HifzReminder] bilan tekshiriladi.
 */
fun newHifzPlan(
    target: PlanTarget,
    layout: QuranLayout,
    dailyNewAyahs: Int,
    reminderPrayer: String?,
    reminderOffsetMin: Int,
    today: java.time.LocalDate,
): HifzPlan {
    val range = layout.rangeOf(target)
    return HifzPlan(
        scope = target.scope,
        fromAyah = range.from,
        toAyah = range.to,
        dailyNewAyahs = dailyNewAyahs,
        reminderAfterPrayer = reminderPrayer,
        reminderOffsetMin = reminderOffsetMin,
        startDate = today,
        isActive = true,
    )
}
