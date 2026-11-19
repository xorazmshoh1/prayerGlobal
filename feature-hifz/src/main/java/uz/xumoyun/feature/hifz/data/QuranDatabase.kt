package uz.xumoyun.feature.hifz.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Qur'on matni bazasi (faqat o'qish). Fayl `assets/quran.db` dan nusxalanadi (`createFromAsset`).
 * Fayl repoda YO'Q — uni tools/build-quran-db bilan Tanzil fayllaridan yarating (tools/README.md).
 * Sxema o'zgarsa `version` ni oshiring va schemas/ papkasini yangilang.
 */
@Database(entities = [SurahEntity::class, AyahEntity::class], version = 1, exportSchema = true)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quran(): QuranDao

    companion object {
        const val ASSET_NAME = "quran.db"
    }
}
