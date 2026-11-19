package uz.xumoyun.feature.hifz.plan.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Hifz jadvallari (TZ 7-bo'lim), ilova bazasi NamozDatabase v4 ([MIGRATION_3_4] yaratadi).
 * Sanalar epoch-day (Long), vaqt epoch-millis (Long) — konvertersiz; domen bilan [HifzMapping.kt] orqali.
 * Jadval/ustun nomlari va indekslari MIGRATION_3_4 bilan AYNAN bir xil bo'lishi shart.
 */
@Entity(tableName = "hifz_plan")
data class HifzPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scope: String,
    val fromAyah: Int,
    val toAyah: Int,
    val dailyNewAyahs: Int,
    val reminderAfterPrayer: String?,
    val reminderOffsetMin: Int,
    val startDate: Long,
    val isActive: Boolean,
)

@Entity(
    tableName = "ayah_progress",
    indices = [Index(value = ["dueDate"], name = "index_ayah_progress_dueDate")],
)
data class AyahProgressEntity(
    @PrimaryKey val ayahGlobalId: Int,
    val state: String,
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueDate: Long,
    val lastRating: Int?,
    val lastReviewedAt: Long?,
)

@Entity(
    tableName = "review_session",
    indices = [Index(value = ["date"], name = "index_review_session_date")],
)
data class ReviewSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val type: String,
    val ayahFrom: Int,
    val ayahTo: Int,
    val durationSec: Int,
    val avgRating: Double?,
)
