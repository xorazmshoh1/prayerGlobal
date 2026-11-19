package uz.xumoyun.feature.hifz.plan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HifzDao {

    @Query("SELECT * FROM hifz_plan WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    fun observeActivePlan(): Flow<HifzPlanEntity?>

    @Insert
    suspend fun insertPlan(plan: HifzPlanEntity): Long

    @Query("UPDATE hifz_plan SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllPlans()

    @Query("SELECT * FROM ayah_progress")
    fun observeProgress(): Flow<List<AyahProgressEntity>>

    @Query("SELECT * FROM ayah_progress")
    suspend fun progressAll(): List<AyahProgressEntity>

    @Upsert
    suspend fun upsertProgress(rows: List<AyahProgressEntity>)

    @Insert
    suspend fun insertSession(session: ReviewSessionEntity): Long

    /** [from]..[to] — epoch-day, ikkalasi ham kiradi. */
    @Query("SELECT * FROM review_session WHERE date BETWEEN :from AND :to ORDER BY date, id")
    suspend fun sessionsBetween(from: Long, to: Long): List<ReviewSessionEntity>
}
