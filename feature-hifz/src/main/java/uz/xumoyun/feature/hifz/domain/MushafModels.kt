package uz.xumoyun.feature.hifz.domain

/** Revelation: Makka yoki Madina (quran-data.xml `type`). */
enum class Revelation { MECCAN, MEDINAN }

/** Sura ma'lumoti (quran.db `surah` jadvali). */
data class SurahInfo(
    val id: Int,
    val nameArabic: String,
    val nameTranslit: String,
    val ayahCount: Int,
    val revelation: Revelation,
    val startAyahGlobal: Int,
)

/** Bitta oyat (quran.db `ayah` jadvali). [text] — Tanzil matni, o'zgartirilmagan. */
data class AyahText(
    val globalId: Int,
    val surahId: Int,
    val numberInSurah: Int,
    val juz: Int,
    val page: Int,
    val text: String,
)

/**
 * Qur'on ma'lumotlariga kirish (o'qish). Implementatsiya — data qatlamida (Room, quran.db asset).
 * [isAvailable] false bo'lsa quran.db bu buildda yo'q.
 */
interface QuranSource {
    suspend fun isAvailable(): Boolean
    suspend fun surahs(): List<SurahInfo>
    suspend fun ayahsOfSurah(surahId: Int): List<AyahText>
    suspend fun ayahsOfJuz(juz: Int): List<AyahText>
}
