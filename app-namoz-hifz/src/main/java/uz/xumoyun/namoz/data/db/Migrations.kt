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

/**
 * 3 → 4: hifz jadvallari (7-bosqich) — hifz_plan, ayah_progress, review_session. Yangi jadvallar, eski ma'lumotga tegmaydi.
 * Ustun turlari Room sxemasi bilan mos: Long/Int → INTEGER NOT NULL, Double → REAL, nullable → NULL ruxsat.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `hifz_plan` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`scope` TEXT NOT NULL, " +
                "`fromAyah` INTEGER NOT NULL, " +
                "`toAyah` INTEGER NOT NULL, " +
                "`dailyNewAyahs` INTEGER NOT NULL, " +
                "`reminderAfterPrayer` TEXT, " +
                "`reminderOffsetMin` INTEGER NOT NULL, " +
                "`startDate` INTEGER NOT NULL, " +
                "`isActive` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `ayah_progress` (" +
                "`ayahGlobalId` INTEGER NOT NULL, " +
                "`state` TEXT NOT NULL, " +
                "`easeFactor` REAL NOT NULL, " +
                "`intervalDays` INTEGER NOT NULL, " +
                "`repetitions` INTEGER NOT NULL, " +
                "`dueDate` INTEGER NOT NULL, " +
                "`lastRating` INTEGER, " +
                "`lastReviewedAt` INTEGER, " +
                "PRIMARY KEY(`ayahGlobalId`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ayah_progress_dueDate` ON `ayah_progress` (`dueDate`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_session` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`ayahFrom` INTEGER NOT NULL, " +
                "`ayahTo` INTEGER NOT NULL, " +
                "`durationSec` INTEGER NOT NULL, " +
                "`avgRating` REAL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_session_date` ON `review_session` (`date`)")
    }
}

/**
 * 4 → 5: namoz kuzatuvchisi va qazo (9-bosqich) — prayer_log, qada_counter, qada_event. Yangi jadvallar.
 * Ustun turlari Room sxemasi bilan mos: String → TEXT NOT NULL, Int/Long → INTEGER NOT NULL.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `prayer_log` (" +
                "`date` TEXT NOT NULL, " +
                "`prayer` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`date`, `prayer`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `qada_counter` (" +
                "`prayer` TEXT NOT NULL, " +
                "`remaining` INTEGER NOT NULL, " +
                "`initialEstimate` INTEGER NOT NULL, " +
                "`dailyGoal` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`prayer`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `qada_event` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`prayer` TEXT NOT NULL, " +
                "`delta` INTEGER NOT NULL, " +
                "`source` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)",
        )
    }
}

/**
 * 5 → 6: ro'za kuzatuvchisi (fast_log) va Ramazon xatmi (khatm_plan), 11-bosqich. Yangi jadvallar;
 * eski ma'lumotga tegmaydi.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fast_log` (" +
                "`date` TEXT NOT NULL, " +
                "`hijriYear` INTEGER NOT NULL, " +
                "`hijriDay` INTEGER NOT NULL, " +
                "`status` TEXT NOT NULL, " +
                "PRIMARY KEY(`date`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `khatm_plan` (" +
                "`hijriYear` INTEGER NOT NULL, " +
                "`startDate` TEXT NOT NULL, " +
                "`juzDone` INTEGER NOT NULL, " +
                "`lastReadAyah` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`hijriYear`))",
        )
    }
}
