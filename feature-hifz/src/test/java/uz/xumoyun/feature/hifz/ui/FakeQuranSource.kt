package uz.xumoyun.feature.hifz.ui

import uz.xumoyun.feature.hifz.domain.AyahText
import uz.xumoyun.feature.hifz.domain.QuranSource
import uz.xumoyun.feature.hifz.domain.Revelation
import uz.xumoyun.feature.hifz.domain.SurahInfo

/** Sintetik ma'lumot: matnlar "test-S-O" (Qur'on emas). */
class FakeQuranSource(
    private val available: Boolean = true,
    private val fail: Boolean = false,
) : QuranSource {
    val surahList = listOf(
        SurahInfo(1, "sura-a", "Alpha", 2, Revelation.MECCAN, 1),
        SurahInfo(2, "sura-b", "Beta", 1, Revelation.MEDINAN, 3),
    )
    val ayahList = listOf(
        AyahText(1, 1, 1, 1, 1, "test-1-1"),
        AyahText(2, 1, 2, 1, 1, "test-1-2"),
        AyahText(3, 2, 1, 1, 2, "test-2-1"),
    )

    override suspend fun isAvailable(): Boolean = available
    override suspend fun surahs(): List<SurahInfo> = check(!fail).let { surahList }
    override suspend fun ayahsOfSurah(surahId: Int): List<AyahText> = check(!fail).let { ayahList.filter { it.surahId == surahId } }
    override suspend fun ayahsOfJuz(juz: Int): List<AyahText> = check(!fail).let { ayahList.filter { it.juz == juz } }
    override suspend fun juzStarts(): List<Int> = listOf(1)
    override suspend fun ayahsByIds(ids: List<Int>): List<AyahText> = ayahList.filter { it.globalId in ids }
}
