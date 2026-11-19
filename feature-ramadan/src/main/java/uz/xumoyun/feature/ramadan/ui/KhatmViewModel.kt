package uz.xumoyun.feature.ramadan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.data.HijriProvider
import uz.xumoyun.feature.ramadan.data.RamadanRepository
import uz.xumoyun.feature.ramadan.domain.KhatmCalc
import java.time.LocalDate
import javax.inject.Inject

/** R5 — Ramazon xatmi: bugungi juz, kunlik norma, progress (30 juzdan). */
data class KhatmState(
    val start: LocalDate,
    val today: LocalDate,
    val juzDone: Int,
    val todayTarget: Int,
    val dailyJuz: Double,
    val remainingDays: Int,
    val hijriYear: Int,
)

@HiltViewModel
class KhatmViewModel @Inject constructor(
    private val hijri: HijriProvider,
    private val common: CommonSettings,
    private val repo: RamadanRepository,
    private val time: TimeSource,
) : ViewModel() {

    val state: StateFlow<KhatmState?> = ramadanContext(hijri, common, time)
        .flatMapLatest { ctx ->
            repo.khatm(ctx.hijri.ymd(ctx.start).year).map { plan ->
                val done = plan?.juzDone ?: 0
                KhatmState(
                    start = ctx.start,
                    today = ctx.today,
                    juzDone = done,
                    todayTarget = KhatmCalc.todayTarget(done, ctx.today, ctx.start),
                    dailyJuz = KhatmCalc.dailyJuz(done, ctx.today, ctx.start),
                    remainingDays = KhatmCalc.remainingDays(ctx.today, ctx.start),
                    hijriYear = ctx.hijri.ymd(ctx.start).year,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Juzlar sonini +1/−1 qiladi (0..30). */
    fun adjustJuz(delta: Int) {
        val s = state.value ?: return
        val next = (s.juzDone + delta).coerceIn(0, KhatmCalc.JUZ_TOTAL)
        viewModelScope.launch {
            repo.setJuzDone(s.hijriYear, s.start, next, lastReadAyah = 0, now = time.now().toEpochMilli())
        }
    }
}
