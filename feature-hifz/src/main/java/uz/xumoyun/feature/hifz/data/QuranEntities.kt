package uz.xumoyun.feature.hifz.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * quran.db sxemasi (TZ 7-bo'lim). Jadval va ustun nomlari, tiplari va indekslari
 * tools/build-quran-db dagi QuranDbBuilder bilan AYNAN bir xil bo'lishi shart: `createFromAsset`
 * ochilganda Room sxemani tekshiradi. O'zgartirsangiz — ikkala joyni ham yangilang.
 */
@Entity(tableName = "surah")
data class SurahEntity(
    @PrimaryKey val id: Int,
    val nameArabic: String,
    val nameTranslit: String,
    val ayahCount: Int,
    /** "MECCAN" yoki "MEDINAN" */
    val revelation: String,
    val startAyahGlobal: Int,
)

@Entity(
    tableName = "ayah",
    indices = [
        Index(value = ["surahId"], name = "index_ayah_surahId"),
        Index(value = ["juz"], name = "index_ayah_juz"),
        Index(value = ["page"], name = "index_ayah_page"),
    ],
)
data class AyahEntity(
    @PrimaryKey val globalId: Int,
    val surahId: Int,
    val numberInSurah: Int,
    val juz: Int,
    val page: Int,
    val textUthmani: String,
)
