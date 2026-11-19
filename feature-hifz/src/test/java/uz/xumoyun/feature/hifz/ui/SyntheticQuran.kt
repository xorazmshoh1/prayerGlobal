package uz.xumoyun.feature.hifz.ui

import uz.xumoyun.feature.hifz.domain.AyahText
import uz.xumoyun.feature.hifz.domain.QuranSource
import uz.xumoyun.feature.hifz.domain.Revelation
import uz.xumoyun.feature.hifz.domain.SurahInfo
import uz.xumoyun.feature.hifz.plan.domain.QURAN_AYAH_COUNT
import uz.xumoyun.feature.hifz.plan.domain.QuranLayout

/**
 * Sintetik Qur'on tuzilmasi (testlar uchun): 114 sura, jami 6236 oyat, 30 juz tekis taqsimlangan.
 * Oyat matni "s<sura>-<n>" — bu Qur'on matni EMAS; faqat tuzilmani tekshirish uchun.
 * Haqiqiy juz/sura chegaralari emas.
 */
object SyntheticQuran {
    private const val SURAHS = 114
    private val counts: List<Int> = (1..SURAHS).map { if (it <= QURAN_AYAH_COUNT % SURAHS) QURAN_AYAH_COUNT / SURAHS + 1 else QURAN_AYAH_COUNT / SURAHS }

    val surahs: List<SurahInfo> = run {
        var start = 1
        counts.mapIndexed { i, c ->
            SurahInfo(i + 1, "arab-${i + 1}", "S${i + 1}", c, Revelation.MECCAN, start).also { start += c }
        }
    }

    val juzStarts: List<Int> = (0 until 30).map { (it * QURAN_AYAH_COUNT) / 30 + 1 }

    val layout: QuranLayout get() = QuranLayout(surahs, juzStarts)

    /** Barcha oyatlar (global id tartibida). */
    val ayahs: List<AyahText> by lazy {
        val juzOf = layout
        surahs.flatMap { s ->
            (1..s.ayahCount).map { n ->
                val g = s.startAyahGlobal + n - 1
                AyahText(g, s.id, n, juzOf.juzOf(g), 1, "s${s.id}-$n")
            }
        }
    }
}

/** QuranSource: [SyntheticQuran] ustida. [available] false — quran.db yo'q; [empty] — baza bor, lekin bo'sh. */
class SyntheticQuranSource(
    private val available: Boolean = true,
    private val empty: Boolean = false,
) : QuranSource {
    override suspend fun isAvailable(): Boolean = available
    override suspend fun surahs(): List<SurahInfo> = if (empty) emptyList() else SyntheticQuran.surahs
    override suspend fun ayahsOfSurah(surahId: Int): List<AyahText> = SyntheticQuran.ayahs.filter { it.surahId == surahId }
    override suspend fun ayahsOfJuz(juz: Int): List<AyahText> = SyntheticQuran.ayahs.filter { it.juz == juz }
    override suspend fun juzStarts(): List<Int> = if (empty) emptyList() else SyntheticQuran.juzStarts
    override suspend fun ayahsByIds(ids: List<Int>): List<AyahText> {
        val set = ids.toHashSet()
        return SyntheticQuran.ayahs.filter { it.globalId in set }
    }
}
