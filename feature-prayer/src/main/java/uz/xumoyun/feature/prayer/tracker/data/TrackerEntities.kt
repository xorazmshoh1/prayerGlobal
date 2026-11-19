package uz.xumoyun.feature.prayer.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** prayer_log (TZ 7-bo'lim): sana ISO matnda (yyyy-MM-dd), namoz va holat — enum nomi. */
@Entity(tableName = "prayer_log", primaryKeys = ["date", "prayer"])
data class PrayerLogEntity(
    val date: String,
    val prayer: String,
    val status: String,
    val updatedAt: Long,
)

/** qada_counter: har QadaPrayer uchun bitta qator. */
@Entity(tableName = "qada_counter")
data class QadaCounterEntity(
    @PrimaryKey val prayer: String,
    val remaining: Int,
    val initialEstimate: Int,
    val dailyGoal: Int,
    val updatedAt: Long,
)

/** qada_event: qazo o'zgarishlari tarixi (bekor qilish uchun). */
@Entity(tableName = "qada_event")
data class QadaEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prayer: String,
    val delta: Int,
    val source: String,
    val createdAt: Long,
)
