package uz.xumoyun.feature.hifz.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.feature.hifz.domain.QuranSource
import uz.xumoyun.feature.hifz.plan.domain.AyahProgress
import uz.xumoyun.feature.hifz.plan.domain.HifzPlan
import uz.xumoyun.feature.hifz.plan.domain.HifzRepository
import uz.xumoyun.feature.hifz.plan.domain.QuranLayout
import uz.xumoyun.feature.hifz.plan.domain.TodaySummary
import uz.xumoyun.feature.hifz.plan.domain.buildToday

data class HifzTodayUiState(
    val loading: Boolean = true,
    val quranAvailable: Boolean = true,
    val failed: Boolean = false,
    val layout: QuranLayout? = null,
    val summary: TodaySummary? = null,
)

/** Hifz tabi: bugungi dars kartalari, progress va ketma-ket kunlar. Ma'lumot [HifzRepository] dan keladi. */
@HiltViewModel
class HifzTodayViewModel @Inject constructor(
    private val repository: HifzRepository,
    private val quran: QuranSource,
    private val time: TimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow(HifzTodayUiState())
    val state: StateFlow<HifzTodayUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { start() }
    }

    private suspend fun start() {
        try {
            val layout = loadLayout()
            if (layout == null) {
                _state.update { it.copy(loading = false, quranAvailable = false) }
                return
            }
            _state.update { it.copy(layout = layout, quranAvailable = true) }
            combine(repository.observeActivePlan(), repository.observeProgress()) { plan, progress -> plan to progress }
                .collect { (plan, progress) -> refresh(layout, plan, progress) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, failed = true) }
        }
    }

    private suspend fun refresh(
        layout: QuranLayout,
        plan: HifzPlan?,
        progress: Map<Int, AyahProgress>,
    ) {
        val today = time.today()
        val sessions = repository.sessionsBetween(today.minusDays(STREAK_LOOKBACK_DAYS), today)
        val summary = buildToday(
            today = today,
            plan = plan,
            progress = progress,
            sessions = sessions,
            sessionDates = sessions.mapTo(HashSet()) { it.date },
            juzOf = layout::juzOf,
        )
        _state.update { it.copy(loading = false, failed = false, summary = summary) }
    }

    /** Quran tuzilmasi; quran.db yo'q yoki bo'sh bo'lsa null. */
    private suspend fun loadLayout(): QuranLayout? {
        if (!quran.isAvailable()) return null
        val surahs = quran.surahs()
        val juzStarts = quran.juzStarts()
        if (surahs.isEmpty() || juzStarts.isEmpty() || juzStarts.first() != 1) return null
        return QuranLayout(surahs, juzStarts)
    }

    companion object {
        /** Streak uchun tarixga qaytish (kun). */
        const val STREAK_LOOKBACK_DAYS = 400L
    }
}
