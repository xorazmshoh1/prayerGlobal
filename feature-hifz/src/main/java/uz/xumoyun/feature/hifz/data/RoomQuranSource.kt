package uz.xumoyun.feature.hifz.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import uz.xumoyun.feature.hifz.domain.AyahText
import uz.xumoyun.feature.hifz.domain.QuranSource
import uz.xumoyun.feature.hifz.domain.Revelation
import uz.xumoyun.feature.hifz.domain.SurahInfo

/**
 * [QuranSource] ning Room asosidagi amalga oshirishi. quran.db assets'da bo'lmasa [isAvailable] false
 * qaytaradi va Room'ga umuman murojaat qilinmaydi (aks holda Room ochishda noaniq xato beradi).
 */
@Singleton
class RoomQuranSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: Provider<QuranDatabase>,
) : QuranSource {

    override suspend fun isAvailable(): Boolean =
        context.assets.list("")?.contains(QuranDatabase.ASSET_NAME) == true

    override suspend fun surahs(): List<SurahInfo> = db.get().quran().surahs().map { it.toDomain() }

    override suspend fun ayahsOfSurah(surahId: Int): List<AyahText> =
        db.get().quran().ayahsOfSurah(surahId).map { it.toDomain() }

    override suspend fun ayahsOfJuz(juz: Int): List<AyahText> =
        db.get().quran().ayahsOfJuz(juz).map { it.toDomain() }

    override suspend fun juzStarts(): List<Int> = db.get().quran().juzStarts()

    /** SQLite o'zgaruvchilar chegarasi (eski qurilmalarda 999) uchun bo'laklab so'raladi. */
    override suspend fun ayahsByIds(ids: List<Int>): List<AyahText> =
        ids.distinct().chunked(ID_CHUNK).flatMap { chunk ->
            db.get().quran().ayahsByIds(chunk).map { it.toDomain() }
        }

    private fun SurahEntity.toDomain() = SurahInfo(
        id = id,
        nameArabic = nameArabic,
        nameTranslit = nameTranslit,
        ayahCount = ayahCount,
        revelation = Revelation.valueOf(revelation),
        startAyahGlobal = startAyahGlobal,
    )

    private fun AyahEntity.toDomain() = AyahText(
        globalId = globalId,
        surahId = surahId,
        numberInSurah = numberInSurah,
        juz = juz,
        page = page,
        text = textUthmani,
    )

    private companion object {
        const val ID_CHUNK = 500
    }
}
