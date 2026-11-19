package uz.xumoyun.namoz.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import uz.xumoyun.feature.hifz.audio.data.AudioDownloadDao
import uz.xumoyun.feature.hifz.audio.data.AudioDownloadEntity
import uz.xumoyun.feature.hifz.plan.data.AyahProgressEntity
import uz.xumoyun.feature.hifz.plan.data.HifzDao
import uz.xumoyun.feature.hifz.plan.data.HifzPlanEntity
import uz.xumoyun.feature.hifz.plan.data.ReviewSessionEntity
import uz.xumoyun.feature.prayer.data.LocationDao
import uz.xumoyun.feature.prayer.data.SavedLocation
import uz.xumoyun.feature.tasbih.data.TasbihDao
import uz.xumoyun.feature.tasbih.data.TasbihEntry

/**
 * Ilova bazasi: joylar (feature-prayer) va tasbeh seanslari (feature-tasbih).
 * v2: tasbeh_day o'rniga tasbih_entry (seanslar); eski ma'lumot [MIGRATION_1_2] orqali ko'chiriladi.
 * v3: audio_download (6-bosqich, [MIGRATION_2_3]).
 * v4: hifz_plan, ayah_progress, review_session (7-bosqich, [MIGRATION_3_4]).
 */
@Database(
    entities = [
        SavedLocation::class, TasbihEntry::class, AudioDownloadEntity::class,
        HifzPlanEntity::class, AyahProgressEntity::class, ReviewSessionEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class NamozDatabase : RoomDatabase() {
    abstract fun locations(): LocationDao
    abstract fun tasbeh(): TasbihDao
    abstract fun audio(): AudioDownloadDao
    abstract fun hifz(): HifzDao
}
