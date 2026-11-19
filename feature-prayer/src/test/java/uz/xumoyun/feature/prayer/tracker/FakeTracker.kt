package uz.xumoyun.feature.prayer.tracker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.tracker.domain.PrayerLogRecord
import uz.xumoyun.feature.prayer.tracker.domain.QadaCounter
import uz.xumoyun.feature.prayer.tracker.domain.QadaEvent
import uz.xumoyun.feature.prayer.tracker.domain.QadaPrayer
import uz.xumoyun.feature.prayer.tracker.domain.TrackerPrefs
import uz.xumoyun.feature.prayer.tracker.domain.TrackerStore
import uz.xumoyun.feature.prayer.tracker.domain.TransactionRunner
import java.time.LocalDate

/** Xotiradagi saqlagich: Room'siz repository va ViewModel mantig'ini sinash uchun. */
class FakeTrackerStore : TrackerStore {
    private val logs = MutableStateFlow<Map<Pair<LocalDate, Prayer>, PrayerLogRecord>>(emptyMap())
    private val counterMap = MutableStateFlow<Map<QadaPrayer, QadaCounter>>(emptyMap())
    private val events = MutableStateFlow<List<QadaEvent>>(emptyList())
    private var nextId = 1L

    override fun observeLogs(from: LocalDate, to: LocalDate): Flow<List<PrayerLogRecord>> =
        logs.map { m -> m.values.filter { it.date >= from && it.date <= to } }

    override suspend fun getLog(date: LocalDate, prayer: Prayer): PrayerLogRecord? = logs.value[date to prayer]

    override suspend fun putLog(record: PrayerLogRecord) {
        logs.value = logs.value + ((record.date to record.prayer) to record)
    }

    override suspend fun deleteLog(date: LocalDate, prayer: Prayer) {
        logs.value = logs.value - (date to prayer)
    }

    override fun observeCounters(): Flow<List<QadaCounter>> = counterMap.map { it.values.toList() }

    override suspend fun counters(): List<QadaCounter> = counterMap.value.values.toList()

    override suspend fun putCounter(counter: QadaCounter) {
        counterMap.value = counterMap.value + (counter.prayer to counter)
    }

    override suspend fun insertEvent(event: QadaEvent): Long {
        val id = nextId++
        events.value = events.value + event.copy(id = id)
        return id
    }

    override fun observeEvents(limit: Int): Flow<List<QadaEvent>> = events.map { it.sortedByDescending { e -> e.id }.take(limit) }

    override suspend fun latestEvent(): QadaEvent? = events.value.maxByOrNull { it.id }

    override suspend fun deleteEvent(id: Long) {
        events.value = events.value.filterNot { it.id == id }
    }

    /** Testlarda: tarix yozuvlari ro'yxati. */
    fun allEvents(): List<QadaEvent> = events.value
}

object DirectTransactions : TransactionRunner {
    override suspend fun <T> atomic(block: suspend () -> T): T = block()
}

class FakeTrackerPrefs(autoQada: Boolean = true) : TrackerPrefs {
    private val auto = MutableStateFlow(autoQada)
    override val autoQadaOnMissed: Flow<Boolean> get() = auto
    override suspend fun setAutoQadaOnMissed(enabled: Boolean) {
        auto.value = enabled
    }
}
