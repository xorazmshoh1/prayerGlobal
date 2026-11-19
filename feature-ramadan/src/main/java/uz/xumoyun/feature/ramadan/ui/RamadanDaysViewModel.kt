package uz.xumoyun.feature.ramadan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.core.common.hijri.HijriYmd
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.data.HijriProvider
import uz.xumoyun.feature.ramadan.data.RamadanPrefs
import uz.xumoyun.feature.ramadan.data.RamadanRepository
import uz.xumoyun.feature.ramadan.domain.FastStatus
import uz.xumoyun.feature.ramadan.domain.RamadanCalc
import uz.xumoyun.feature.ramadan.domain.RamadanDayRow
import uz.xumoyun.feature.ramadan.domain.RamadanSchedulePort
import java.time.LocalDate
import javax.inject.Inject

/** R2 (30 kunlik taqvim) va R3 (ro'za kuzatuvchisi) holati — bitta jadval, ikki ekran. */
data class RamadanDaysState(
    val start: LocalDate,
    val rows: List<RamadanDayRow>,
    val fastQada: Int,
    val city: String?,
    val hijriYear: Int,
    val hijriStart: HijriYmd,
)

@HiltViewModel
class RamadanDaysViewModel @Inject constructor(
    private val hijri: HijriProvider,
    private val common: CommonSettings,
    private val schedule: RamadanSchedulePort,
    private val prefs: RamadanPrefs,
    private val repo: RamadanRepository,
    private val time: TimeSource,
) : ViewModel() {

    val state: StateFlow<RamadanDaysState?> = ramadanContext(hijri, common, time)
        .flatMapLatest { ctx ->
            combine(
                schedule.days(flowOf(ctx.start), RamadanCalc.DAYS),
                prefs.settings,
                repo.fasts(ctx.start),
                repo.fastQada(),
            ) { sched, s, fasts, qada ->
                val times = sched?.days.orEmpty().associateBy { it.date }
                RamadanDaysState(
                    start = ctx.start,
                    rows = RamadanCalc.dayRows(ctx.start, ctx.today, ctx.hijri, s.imsakOffsetMin, { times[it] }, fasts),
                    fastQada = qada,
                    city = sched?.city,
                    hijriYear = ctx.hijri.ymd(ctx.start).year,
                    hijriStart = ctx.hijri.ymd(ctx.start),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Ro'za holatini belgilaydi (null — tozalash). */
    fun setFast(row: RamadanDayRow, status: FastStatus?) {
        viewModelScope.launch {
            repo.setFast(row.date, row.hijri.year, row.hijri.day, status)
        }
    }
}
