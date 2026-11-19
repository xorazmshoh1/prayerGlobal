package uz.xumoyun.namoz.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 1 → 2: kunlik yig'indi (tasbeh_day) o'rniga seanslar (tasbih_entry). Eski kunlik hisoblar
 * bitta qator sifatida ko'chiriladi (maqsad: 33, eski yozuvda maqsad saqlanmagan).
 * Ma'lumot yo'qolmaydi; tasbeh_day jadvali shundan keyin o'chiriladi.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tasbih_entry` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` TEXT NOT NULL, " +
                "`dhikrKey` TEXT NOT NULL, " +
                "`count` INTEGER NOT NULL, " +
                "`target` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO `tasbih_entry` (`date`, `dhikrKey`, `count`, `target`) " +
                "SELECT `date`, `dhikr`, `count`, 33 FROM `tasbeh_day` WHERE `count` > 0",
        )
        db.execSQL("DROP TABLE IF EXISTS `tasbeh_day`")
    }
}

/**
 * 2 → 3: audio_download (6-bosqich). Yozuv — bitta qori va bitta sura; (reciterKey, surahId) kompozit kalit.
 * Yangi jadval, eski ma'lumotga tegmaydi.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `audio_download` (" +
                "`reciterKey` TEXT NOT NULL, " +
                "`surahId` INTEGER NOT NULL, " +
                "`bytes` INTEGER NOT NULL, " +
                "`downloadedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`reciterKey`, `surahId`))",
        )
    }
}
