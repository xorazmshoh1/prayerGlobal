package uz.xumoyun.feature.prayer.tracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.tracker.domain.PrayerLogRecord
import uz.xumoyun.feature.prayer.tracker.domain.PrayerStatus
import uz.xumoyun.feature.prayer.tracker.domain.Qada
import uz.xumoyun.feature.prayer.tracker.domain.QadaCounter
import uz.xumoyun.feature.prayer.tracker.domain.QadaEvent
import uz.xumoyun.feature.prayer.tracker.domain.QadaPrayer
import uz.xumoyun.feature.prayer.tracker.domain.QadaSource
import uz.xumoyun.feature.prayer.tracker.domain.PrayerLog
import uz.xumoyun.feature.prayer.tracker.domain.TrackerPrefs
import uz.xumoyun.feature.prayer.tracker.domain.TrackerStore
import uz.xumoyun.feature.prayer.tracker.domain.TransactionRunner
import java.time.LocalDate
import javax.inject.Inject

/**
 * Namoz kuzatuvchisi va qazo uchun yagona kirish nuqtasi. Har bir o'zgarish (holat + qazo + tarix)
 * [TransactionRunner.atomic] ichida bajariladi, shuning uchun yarim yozilgan holat bo'lmaydi.
 */
class TrackerRepository @Inject constructor(
    private val store: TrackerStore,
    private val tx: TransactionRunner,
    private val prefs: TrackerPrefs,
) {
    /** Har bir QadaPrayer uchun yozuv bo'ladi (saqlanmagan bo'lsa — nol qiymatli). */
    val counters: Flow<List<QadaCounter>> = store.observeCounters().map { list ->
        QadaPrayer.entries.map { p -> list.firstOrNull { it.prayer == p } ?: QadaCounter(p) }
    }

    val autoQadaOnMissed: Flow<Boolean> = prefs.autoQadaOnMissed

    fun observeLogs(from: LocalDate, to: LocalDate): Flow<List<PrayerLogRecord>> = store.observeLogs(from, to)

    fun observeEvents(limit: Int): Flow<List<QadaEvent>> = store.observeEvents(limit)

    suspend fun setAutoQadaOnMissed(enabled: Boolean) = prefs.setAutoQadaOnMissed(enabled)

    /**
     * Namoz holatini o'rnatadi. [status] null bo'lsa yozuv o'chadi. MISSED ga yangi o'tganda va
     * avtomatik qazo yoqilgan bo'lsa, tegishli QadaCounter +1 (source AUTO_MISSED).
     * MISSED dan chiqilganda qazo kamaytirilmaydi (qo'lda −1 bilan tuzatiladi).
     */
    suspend fun setStatus(date: LocalDate, prayer: Prayer, status: PrayerStatus?, now: Long) = tx.atomic {
        val previous = store.getLog(date, prayer)?.status
        if (status == null) store.deleteLog(date, prayer)
        else store.putLog(PrayerLogRecord(date, prayer, status, now))
        if (PrayerLog.isMissedTransition(previous, status) && prefs.autoQadaOnMissed.first()) {
            QadaPrayer.of(prayer)?.let { adjustInternal(it, +1, QadaSource.AUTO_MISSED, now) }
        }
    }

    /** Qo'lda ±o'zgartirish. Qo'llangan haqiqiy o'zgarish qaytadi (0 bo'lsa tarixga yozilmaydi). */
    suspend fun adjust(prayer: QadaPrayer, delta: Int, now: Long): Int = tx.atomic {
        adjustInternal(prayer, delta, QadaSource.MANUAL, now)
    }

    /** Boshlang'ich sonni qo'lda kiritish: qolgan son [value] ga tenglashtiriladi (farq tarixga yoziladi). */
    suspend fun setRemaining(prayer: QadaPrayer, value: Int, now: Long): Int = tx.atomic {
        val current = store.counters().firstOrNull { it.prayer == prayer }?.remaining ?: 0
        val target = value.coerceIn(0, Qada.MAX_REMAINING)
        adjustInternal(prayer, target - current, QadaSource.MANUAL, now)
    }

    /** Oxirgi qazo o'zgarishini bekor qiladi (LIFO). Bekor qilish bo'lsa true. */
    suspend fun undoLast(now: Long): Boolean = tx.atomic {
        val last = store.latestEvent() ?: return@atomic false
        store.deleteEvent(last.id)
        val current = store.counters().firstOrNull { it.prayer == last.prayer } ?: QadaCounter(last.prayer)
        store.putCounter(
            current.copy(remaining = Qada.applyDelta(current.remaining, -last.delta), updatedAt = now),
        )
        true
    }

    /** Taxminiy hisob ustasi: mazhabdagi har namozga [perPrayer] qo'shiladi (WIZARD). */
    suspend fun applyEstimate(perPrayer: Int, madhab: AsrMadhab, now: Long): Int = tx.atomic {
        require(perPrayer in 1..Qada.MAX_REMAINING) { "perPrayer: $perPrayer" }
        Qada.prayersFor(madhab).forEach { p ->
            val current = store.counters().firstOrNull { it.prayer == p } ?: QadaCounter(p)
            val applied = Qada.applyDelta(current.remaining, perPrayer) - current.remaining
            store.putCounter(
                current.copy(remaining = current.remaining + applied, initialEstimate = perPrayer, updatedAt = now),
            )
            if (applied != 0) {
                store.insertEvent(QadaEvent(prayer = p, delta = applied, source = QadaSource.WIZARD, createdAt = now))
            }
        }
        perPrayer
    }

    /** Kunlik maqsad (har namoz uchun) — qazo hisoblagichlarining barchasiga (FAST dan tashqari). */
    suspend fun setDailyGoal(goal: Int, now: Long) = tx.atomic {
        val value = goal.coerceIn(0, Qada.MAX_DAILY_GOAL)
        QadaPrayer.entries.filter { it != QadaPrayer.FAST }.forEach { p ->
            val current = store.counters().firstOrNull { it.prayer == p } ?: QadaCounter(p)
            store.putCounter(current.copy(dailyGoal = value, updatedAt = now))
        }
    }

    private suspend fun adjustInternal(prayer: QadaPrayer, delta: Int, source: QadaSource, now: Long): Int {
        val current = store.counters().firstOrNull { it.prayer == prayer } ?: QadaCounter(prayer)
        val next = Qada.applyDelta(current.remaining, delta)
        val applied = next - current.remaining
        if (applied == 0) return 0
        store.putCounter(current.copy(remaining = next, updatedAt = now))
        store.insertEvent(QadaEvent(prayer = prayer, delta = applied, source = source, createdAt = now))
        return applied
    }
}
