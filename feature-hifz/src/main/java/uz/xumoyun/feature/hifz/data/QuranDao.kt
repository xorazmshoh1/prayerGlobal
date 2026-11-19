package uz.xumoyun.feature.hifz.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface QuranDao {
    @Query("SELECT * FROM surah ORDER BY id")
    suspend fun surahs(): List<SurahEntity>

    @Query("SELECT * FROM ayah WHERE surahId = :surahId ORDER BY globalId")
    suspend fun ayahsOfSurah(surahId: Int): List<AyahEntity>

    @Query("SELECT * FROM ayah WHERE juz = :juz ORDER BY globalId")
    suspend fun ayahsOfJuz(juz: Int): List<AyahEntity>

    @Query("SELECT MIN(globalId) FROM ayah GROUP BY juz ORDER BY juz")
    suspend fun juzStarts(): List<Int>

    @Query("SELECT * FROM ayah WHERE globalId IN (:ids)")
    suspend fun ayahsByIds(ids: List<Int>): List<AyahEntity>
}
