package uz.xumoyun.feature.hifz.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.feature.hifz.domain.QuranSource
import uz.xumoyun.feature.hifz.plan.domain.DayReview
import uz.xumoyun.feature.hifz.plan.domain.HifzRepository
import uz.xumoyun.feature.hifz.plan.domain.JuzCell
import uz.xumoyun.feature.hifz.plan.domain.QURAN_AYAH_COUNT
import uz.xumoyun.feature.hifz.plan.domain.QuranLayout
import uz.xumoyun.feature.hifz.plan.domain.SurahProgress
import uz.xumoyun.feature.hifz.plan.domain.juzCells
import uz.xumoyun.feature.hifz.plan.domain.surahStatuses
import uz.xumoyun.feature.hifz.plan.domain.weeklyReviews

data class ProgressUiState(
    val loading: Boolean = true,
    val quranAvailable: Boolean = true,
    val failed: Boolean = false,
    val memorized: Int = 0,
    val total: Int = 0,
    val juz: List<JuzCell> = emptyList(),
    val surahs: List<SurahProgress> = emptyList(),
    val weekly: List<DayReview> = emptyList(),
) {
    val percent: Double get() = if (total == 0) 0.0 else memorized * 100.0 / total
}

/** Progress ekrani: 30 juz xaritasi, sura holati, so'nggi 7 kun grafigi. */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repository: HifzRepository,
    private val quran: QuranSource,
    private val time: TimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    private var layout: QuranLayout? = null

    init {
        viewModelScope.launch { start() }
    }

    private suspend fun start() {
        try {
            if (!quran.isAvailable()) {
                _state.update { it.copy(loading = false, quranAvailable = false) }
                return
            }
            val surahs = quran.surahs()
            val juzStarts = quran.juzStarts()
            if (surahs.isEmpty() || juzStarts.isEmpty() || juzStarts.first() != 1) {
                _state.update { it.copy(loading = false, quranAvailable = false) }
                return
            }
            val q = QuranLayout(surahs, juzStarts)
            layout = q
            repository.observeProgress().collect { progress ->
                val today = time.today()
                val sessions = repository.sessionsBetween(today.minusDays(6), today)
                val juz = juzCells(q, progress)
                val surahRows = surahStatuses(q, progress)
                _state.update {
                    it.copy(
                        loading = false,
                        memorized = juz.sumOf { c -> c.memorized },
                        total = QURAN_TOTAL,
                        juz = juz,
                        surahs = surahRows,
                        weekly = weeklyReviews(today, sessions),
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, failed = true) }
        }
    }

    private companion object {
        const val QURAN_TOTAL = QURAN_AYAH_COUNT
    }
}
