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
import uz.xumoyun.feature.prayer.tracker.data.PrayerLogEntity
import uz.xumoyun.feature.prayer.tracker.data.QadaCounterEntity
import uz.xumoyun.feature.prayer.tracker.data.QadaEventEntity
import uz.xumoyun.feature.prayer.tracker.data.TrackerDao
import uz.xumoyun.feature.tasbih.data.TasbihDao
import uz.xumoyun.feature.tasbih.data.TasbihEntry

/** Bazaning joriy sxema versiyasi. Zaxira faylida ham yoziladi (BackupFile.schemaVersion). */
const val DB_SCHEMA_VERSION = 5

/**
 * Ilova bazasi: joylar (feature-prayer), tasbeh seanslari (feature-tasbih), audio, hifz, namoz kuzatuvchisi va qazo.
 * v2: tasbih_entry ([MIGRATION_1_2]). v3: audio_download ([MIGRATION_2_3]). v4: hifz jadvallari ([MIGRATION_3_4]).
 * v5: prayer_log, qada_counter, qada_event (9-bosqich, [MIGRATION_4_5]).
 */
@Database(
    entities = [
        SavedLocation::class, TasbihEntry::class, AudioDownloadEntity::class,
        HifzPlanEntity::class, AyahProgressEntity::class, ReviewSessionEntity::class,
        PrayerLogEntity::class, QadaCounterEntity::class, QadaEventEntity::class,
    ],
    version = DB_SCHEMA_VERSION,
    exportSchema = true,
)
abstract class NamozDatabase : RoomDatabase() {
    abstract fun locations(): LocationDao
    abstract fun tasbeh(): TasbihDao
    abstract fun audio(): AudioDownloadDao
    abstract fun hifz(): HifzDao
    abstract fun tracker(): TrackerDao
}
