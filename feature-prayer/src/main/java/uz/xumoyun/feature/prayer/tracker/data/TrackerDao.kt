package uz.xumoyun.feature.prayer.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Query("SELECT * FROM prayer_log WHERE date BETWEEN :from AND :to")
    fun observeLogs(from: String, to: String): Flow<List<PrayerLogEntity>>

    @Query("SELECT * FROM prayer_log WHERE date = :date AND prayer = :prayer LIMIT 1")
    suspend fun getLog(date: String, prayer: String): PrayerLogEntity?

    @Upsert
    suspend fun upsertLog(entity: PrayerLogEntity)

    @Query("DELETE FROM prayer_log WHERE date = :date AND prayer = :prayer")
    suspend fun deleteLog(date: String, prayer: String)

    @Query("SELECT * FROM qada_counter")
    fun observeCounters(): Flow<List<QadaCounterEntity>>

    @Query("SELECT * FROM qada_counter")
    suspend fun counters(): List<QadaCounterEntity>

    @Upsert
    suspend fun upsertCounter(entity: QadaCounterEntity)

    @Insert
    suspend fun insertEvent(entity: QadaEventEntity): Long

    @Query("SELECT * FROM qada_event ORDER BY id DESC LIMIT :limit")
    fun observeEvents(limit: Int): Flow<List<QadaEventEntity>>

    @Query("SELECT * FROM qada_event ORDER BY id DESC LIMIT 1")
    suspend fun latestEvent(): QadaEventEntity?

    @Query("DELETE FROM qada_event WHERE id = :id")
    suspend fun deleteEvent(id: Long)
}
