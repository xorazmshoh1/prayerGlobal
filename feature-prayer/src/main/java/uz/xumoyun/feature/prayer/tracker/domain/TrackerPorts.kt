package uz.xumoyun.feature.prayer.tracker.domain

import kotlinx.coroutines.flow.Flow
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.Prayer
import java.time.LocalDate

/**
 * Kuzatuvchi va qazo saqlagichi (port). Room'dagi [uz.xumoyun.feature.prayer.tracker.data.RoomTrackerStore]
 * uni amalga oshiradi; testlarda oddiy xotiradagi versiya ishlatiladi.
 */
interface TrackerStore {
    fun observeLogs(from: LocalDate, to: LocalDate): Flow<List<PrayerLogRecord>>
    suspend fun getLog(date: LocalDate, prayer: Prayer): PrayerLogRecord?
    suspend fun putLog(record: PrayerLogRecord)
    suspend fun deleteLog(date: LocalDate, prayer: Prayer)

    fun observeCounters(): Flow<List<QadaCounter>>
    suspend fun counters(): List<QadaCounter>
    suspend fun putCounter(counter: QadaCounter)

    /** Qo'shilgan tarix yozuvining id'sini qaytaradi. */
    suspend fun insertEvent(event: QadaEvent): Long
    fun observeEvents(limit: Int): Flow<List<QadaEvent>>
    suspend fun latestEvent(): QadaEvent?
    suspend fun deleteEvent(id: Long)
}

/** Bir nechta yozuvni bitta tranzaksiyada bajarish (Room: withTransaction). */
interface TransactionRunner {
    suspend fun <T> atomic(block: suspend () -> T): T
}

/** Mazhab manbai: sozlamadagi qo'lda yoki avtomatik tanlangan mazhab (Vitr va taxminiy hisob uchun). */
interface MadhabSource {
    fun madhab(): Flow<AsrMadhab>
}

/** Qazo sozlamalari: MISSED belgilanganda avtomatik +1 (standart yoqilgan). */
interface TrackerPrefs {
    val autoQadaOnMissed: Flow<Boolean>
    suspend fun setAutoQadaOnMissed(enabled: Boolean)
}
