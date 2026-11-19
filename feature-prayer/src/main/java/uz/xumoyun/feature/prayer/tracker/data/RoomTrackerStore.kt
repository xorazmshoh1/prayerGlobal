package uz.xumoyun.feature.prayer.tracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.tracker.domain.PrayerLogRecord
import uz.xumoyun.feature.prayer.tracker.domain.PrayerStatus
import uz.xumoyun.feature.prayer.tracker.domain.QadaCounter
import uz.xumoyun.feature.prayer.tracker.domain.QadaEvent
import uz.xumoyun.feature.prayer.tracker.domain.QadaPrayer
import uz.xumoyun.feature.prayer.tracker.domain.QadaSource
import uz.xumoyun.feature.prayer.tracker.domain.TrackerStore
import java.time.LocalDate
import javax.inject.Inject

/** [TrackerStore] ning Room amalga oshirilishi. Noma'lum enum/sana qatorlari o'tkazib yuboriladi. */
class RoomTrackerStore @Inject constructor(private val dao: TrackerDao) : TrackerStore {

    override fun observeLogs(from: LocalDate, to: LocalDate): Flow<List<PrayerLogRecord>> =
        dao.observeLogs(from.toString(), to.toString()).map { list -> list.mapNotNull { it.toDomain() } }

    override suspend fun getLog(date: LocalDate, prayer: Prayer): PrayerLogRecord? =
        dao.getLog(date.toString(), prayer.name)?.toDomain()

    override suspend fun putLog(record: PrayerLogRecord) =
        dao.upsertLog(PrayerLogEntity(record.date.toString(), record.prayer.name, record.status.name, record.updatedAt))

    override suspend fun deleteLog(date: LocalDate, prayer: Prayer) = dao.deleteLog(date.toString(), prayer.name)

    override fun observeCounters(): Flow<List<QadaCounter>> =
        dao.observeCounters().map { list -> list.mapNotNull { it.toDomain() } }

    override suspend fun counters(): List<QadaCounter> = dao.counters().mapNotNull { it.toDomain() }

    override suspend fun putCounter(counter: QadaCounter) = dao.upsertCounter(
        QadaCounterEntity(counter.prayer.name, counter.remaining, counter.initialEstimate, counter.dailyGoal, counter.updatedAt),
    )

    override suspend fun insertEvent(event: QadaEvent): Long = dao.insertEvent(
        QadaEventEntity(prayer = event.prayer.name, delta = event.delta, source = event.source.name, createdAt = event.createdAt),
    )

    override fun observeEvents(limit: Int): Flow<List<QadaEvent>> =
        dao.observeEvents(limit).map { list -> list.mapNotNull { it.toDomain() } }

    override suspend fun latestEvent(): QadaEvent? = dao.latestEvent()?.toDomain()

    override suspend fun deleteEvent(id: Long) = dao.deleteEvent(id)
}

private fun PrayerLogEntity.toDomain(): PrayerLogRecord? = runCatching {
    PrayerLogRecord(LocalDate.parse(date), Prayer.valueOf(prayer), PrayerStatus.valueOf(status), updatedAt)
}.getOrNull()

private fun QadaCounterEntity.toDomain(): QadaCounter? = runCatching {
    QadaCounter(QadaPrayer.valueOf(prayer), remaining, initialEstimate, dailyGoal, updatedAt)
}.getOrNull()

private fun QadaEventEntity.toDomain(): QadaEvent? = runCatching {
    QadaEvent(id, QadaPrayer.valueOf(prayer), delta, QadaSource.valueOf(source), createdAt)
}.getOrNull()
