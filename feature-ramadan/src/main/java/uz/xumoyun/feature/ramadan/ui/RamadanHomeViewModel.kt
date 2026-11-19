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
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.core.common.hijri.HijriYmd
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.data.HijriProvider
import uz.xumoyun.feature.ramadan.data.RamadanPrefs
import uz.xumoyun.feature.ramadan.domain.RamadanCalc
import uz.xumoyun.feature.ramadan.domain.RamadanPhase
import uz.xumoyun.feature.ramadan.domain.RamadanSchedulePort
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** R1 — Ramazon bosh sahifasi holati: faza, kun raqami, saharlik/iftor vaqti (TZ 6-bo'lim). */
data class RamadanHomeState(
    val phase: RamadanPhase,
    val start: LocalDate,
    val today: LocalDate,
    val dayNumber: Int?,
    val daysUntilStart: Long,
    val suhoorAt: Instant?,
    val iftarAt: Instant?,
    val city: String?,
    val zone: ZoneId?,
    val hijri: HijriYmd,
    val now: Instant,
)

@HiltViewModel
class RamadanHomeViewModel @Inject constructor(
    private val hijri: HijriProvider,
    private val common: CommonSettings,
    private val schedule: RamadanSchedulePort,
    private val prefs: RamadanPrefs,
    private val time: TimeSource,
) : ViewModel() {

    val state: StateFlow<RamadanHomeState?> = ramadanContext(hijri, common, time)
        .flatMapLatest { ctx ->
            combine(schedule.days(flowOf(ctx.today), 1), prefs.settings) { sched, s ->
                build(ctx, sched?.city, sched?.zone, sched?.days?.firstOrNull { it.date == ctx.today }?.let { t ->
                    Pair(RamadanCalc.imsak(t.fajr, s.imsakOffsetMin), t.maghrib)
                })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun build(ctx: RamadanContext, city: String?, zone: ZoneId?, times: Pair<Instant, Instant>?): RamadanHomeState {
        val phase = RamadanCalc.phase(ctx.today, ctx.start)
        return RamadanHomeState(
            phase = phase,
            start = ctx.start,
            today = ctx.today,
            dayNumber = RamadanCalc.dayNumber(ctx.today, ctx.start),
            daysUntilStart = java.time.temporal.ChronoUnit.DAYS.between(ctx.today, ctx.start),
            suhoorAt = if (phase == RamadanPhase.DURING) times?.first else null,
            iftarAt = if (phase == RamadanPhase.DURING) times?.second else null,
            city = city,
            zone = zone,
            hijri = ctx.hijri.ymd(ctx.today),
            now = ctx.now,
        )
    }

    /** Namoz tabidagi karta uchun: Ramazondan 15 kun oldin va Ramazon davomida ko'rinadi. */
    fun cardVisible(state: RamadanHomeState): Boolean = RamadanCalc.cardVisible(state.today, state.start)
}
