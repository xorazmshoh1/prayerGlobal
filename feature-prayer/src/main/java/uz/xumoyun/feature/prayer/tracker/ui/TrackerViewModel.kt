package uz.xumoyun.feature.prayer.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.tracker.data.TrackerRepository
import uz.xumoyun.feature.prayer.tracker.domain.PrayerLog
import uz.xumoyun.feature.prayer.tracker.domain.PrayerStatus
import uz.xumoyun.feature.prayer.tracker.domain.Qada
import uz.xumoyun.feature.prayer.tracker.domain.QadaCounter
import uz.xumoyun.feature.prayer.tracker.domain.QadaPrayer
import uz.xumoyun.feature.prayer.tracker.domain.MadhabSource
import java.time.LocalDate
import javax.inject.Inject

/** Kuzatuv ekrani holati. [counters] — barcha 7 kalit; ekranda mazhabga mos [visibleCounters] ko'rsatiladi. */
data class TrackerUiState(
    val today: LocalDate,
    val weekStart: LocalDate,
    val week: List<LocalDate>,
    /** Sana → (namoz → holat). Faqat belgilangan namozlar. */
    val logs: Map<LocalDate, Map<Prayer, PrayerStatus>>,
    val streak: Int,
    val madhab: AsrMadhab,
    val counters: List<QadaCounter>,
    val autoQada: Boolean,
    val canUndo: Boolean,
) {
    val visibleCounters: List<QadaCounter>
        get() = counters.filter { it.prayer in Qada.prayersFor(madhab) }

    val totalRemaining: Int get() = Qada.totalRemaining(counters, madhab)

    /** Tugash sanasi; maqsad 0 bo'lsa null. */
    val finishDate: LocalDate? get() = Qada.finishDate(today, counters, madhab)

    /** Ko'rsatiladigan kunlik maqsad (mazhabdagi namozlar bo'yicha eng katta). */
    val dailyGoal: Int get() = visibleCounters.maxOfOrNull { it.dailyGoal } ?: 0
}

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val repository: TrackerRepository,
    private val madhabSource: MadhabSource,
    private val time: TimeSource,
) : ViewModel() {

    private val today: LocalDate = time.today()
    private val weekAnchor = MutableStateFlow(today)

    private val logs = repository.observeLogs(today.minusDays(LOOKBACK_DAYS), today.plusDays(7)).map { list ->
        list.groupBy { it.date }.mapValues { (_, recs) -> recs.associate { it.prayer to it.status } }
    }

    private val core = combine(logs, repository.counters, madhabSource.madhab()) { l, c, m -> Core(l, c, m) }
    private val flags = combine(repository.autoQadaOnMissed, repository.observeEvents(1).map { it.isNotEmpty() }) { a, u -> a to u }

    val state: StateFlow<TrackerUiState> = combine(core, flags, weekAnchor) { c, (auto, canUndo), anchor ->
        val perfect = c.logs.filterValues { PrayerLog.isPerfectDay(it) }.keys
        TrackerUiState(
            today = today,
            weekStart = PrayerLog.weekDays(anchor).first(),
            week = PrayerLog.weekDays(anchor),
            logs = c.logs,
            streak = PrayerLog.streak(today, perfect),
            madhab = c.madhab,
            counters = c.counters,
            autoQada = auto,
            canUndo = canUndo,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TrackerUiState(today, today, PrayerLog.weekDays(today), emptyMap(), 0, AsrMadhab.SHAFI, emptyList(), true, false))

    fun shiftWeek(weeks: Int) = weekAnchor.update { it.plusWeeks(weeks.toLong()) }

    /** Katakka bosish: belgilanmagan → O'qildi → O'qilmadi → belgilanmagan. */
    fun cycleStatus(date: LocalDate, prayer: Prayer): Job = launch {
        val current = state.value.logs[date]?.get(prayer)
        repository.setStatus(date, prayer, PrayerLog.nextStatus(current), now())
    }

    fun adjust(prayer: QadaPrayer, delta: Int): Job = launch { repository.adjust(prayer, delta, now()) }

    fun setRemaining(prayer: QadaPrayer, value: Int): Job = launch { repository.setRemaining(prayer, value, now()) }

    fun undo(): Job = launch { repository.undoLast(now()) }

    fun setDailyGoal(goal: Int): Job = launch { repository.setDailyGoal(goal, now()) }

    fun setAutoQada(enabled: Boolean): Job = launch { repository.setAutoQadaOnMissed(enabled) }

    /** Ustaning oldindan ko'rinishi: kunlar soni (ikki chegara kiradi). */
    fun estimatePreview(start: LocalDate, end: LocalDate): Int = Qada.inclusiveDays(start, end)

    /** Ustani qo'llash: har namozga [estimatePreview] qiymati qo'shiladi. 0 bo'lsa hech narsa qilinmaydi. */
    fun applyEstimate(start: LocalDate, end: LocalDate): Job? {
        val perPrayer = estimatePreview(start, end)
        if (perPrayer <= 0) return null
        return launch { repository.applyEstimate(perPrayer, state.value.madhab, now()) }
    }

    private fun now(): Long = time.now().toEpochMilli()

    private fun launch(block: suspend () -> Unit): Job = viewModelScope.launch { block() }

    private data class Core(
        val logs: Map<LocalDate, Map<Prayer, PrayerStatus>>,
        val counters: List<QadaCounter>,
        val madhab: AsrMadhab,
    )

    companion object {
        /** Ketma-ket kunlarni hisoblash uchun tarix oynasi. */
        const val LOOKBACK_DAYS = 400L
    }
}
