package uz.xumoyun.feature.tasbih.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.feature.tasbih.data.TasbihDao
import uz.xumoyun.feature.tasbih.data.TasbihEntry
import uz.xumoyun.feature.tasbih.data.TasbihSettings
import uz.xumoyun.feature.tasbih.domain.DailyCount
import uz.xumoyun.feature.tasbih.domain.DailyHistory
import uz.xumoyun.feature.tasbih.domain.DayTotal
import uz.xumoyun.feature.tasbih.domain.Dhikr
import uz.xumoyun.feature.tasbih.domain.TasbihCounter
import java.time.LocalDate
import javax.inject.Inject

data class TasbihState(
    val dhikr: Dhikr = Dhikr.SUBHANALLAH,
    /** Joriy seans hisobi (reset bilan 0 ga qaytadi, saqlangan tarix o'chmaydi). */
    val count: Int = 0,
    val target: Int = TasbihCounter.DEFAULT_TARGET,
    /** Bugungi barcha seanslar yig'indisi. */
    val todayTotal: Int = 0,
)

@HiltViewModel
class TasbihViewModel @Inject constructor(
    private val dao: TasbihDao,
    private val settings: TasbihSettings,
    private val time: TimeSource,
) : ViewModel() {

    /** Qator yozilayotgan seans. [rowId] 0 — hali bazaga yozilmagan. */
    private class Run(val date: String, val dhikr: Dhikr) {
        var rowId: Long = 0L
        var count: Int = 0
    }

    private val selected = MutableStateFlow(Dhikr.SUBHANALLAH)
    private val session = MutableStateFlow(0)
    private var run = Run(time.today().toString(), Dhikr.SUBHANALLAH)
    private val writeLock = Mutex()

    private val target: StateFlow<Int> = settings.target
        .stateIn(viewModelScope, SharingStarted.Eagerly, TasbihCounter.DEFAULT_TARGET)

    private val historyFrom: String = time.today().minusDays(HISTORY_DAYS - 1L).toString()

    private val entries: StateFlow<List<TasbihEntry>> = dao.observeSince(historyFrom)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<TasbihState> = combine(selected, session, target, entries) { dhikr, count, t, rows ->
        TasbihState(
            dhikr = dhikr,
            count = count,
            target = t,
            todayTotal = DailyHistory.totalFor(rows.toCounts(), today()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasbihState())

    /** Oxirgi [HISTORY_DAYS] kun, eng yangisi birinchi. */
    val history: StateFlow<List<DayTotal>> = entries
        .map { rows -> DailyHistory.build(rows.toCounts(), today(), HISTORY_DAYS.toInt()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Bosilganda hisob +1 va seans qatori saqlanadi. Maqsadga yetganda true qaytaradi
     * (UI uzunroq tebranadi).
     */
    fun tap(): Boolean {
        if (run.date != today().toString()) startRun(selected.value)
        val next = session.value + 1
        session.value = next
        run.count = next
        persist(run)
        return TasbihCounter.reached(next, target.value)
    }

    /** Hisobni nolga qaytaradi. Saqlangan qatorlar o'chmaydi: keyingi bosish yangi seansda. */
    fun reset() = startRun(selected.value)

    fun select(dhikr: Dhikr) {
        if (dhikr == selected.value) return
        selected.value = dhikr
        startRun(dhikr)
    }

    fun setTarget(value: Int) {
        val valid = TasbihCounter.validTarget(value) ?: return
        viewModelScope.launch { settings.setTarget(valid) }
    }

    private fun startRun(dhikr: Dhikr) {
        run = Run(today().toString(), dhikr)
        session.value = 0
    }

    /** Seans qatorini bazaga yozadi; yozuvlar ketma-ket bajariladi (birinchi bosishda INSERT, keyin UPDATE). */
    private fun persist(r: Run) {
        viewModelScope.launch {
            writeLock.withLock {
                if (r.count == 0) return@withLock
                val t = target.value
                if (r.rowId == 0L) {
                    r.rowId = dao.insert(TasbihEntry(date = r.date, dhikrKey = r.dhikr.name, count = r.count, target = t))
                } else {
                    dao.updateCount(r.rowId, r.count, t)
                }
            }
        }
    }

    private fun today(): LocalDate = time.today()

    private fun List<TasbihEntry>.toCounts(): List<DailyCount> = map { DailyCount(it.date, it.count) }

    companion object {
        const val HISTORY_DAYS = 7L
    }
}
