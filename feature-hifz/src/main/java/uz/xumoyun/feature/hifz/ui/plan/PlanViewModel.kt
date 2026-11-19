package uz.xumoyun.feature.hifz.ui.plan

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
import uz.xumoyun.feature.hifz.domain.SurahInfo
import uz.xumoyun.feature.hifz.plan.domain.AyahProgress
import uz.xumoyun.feature.hifz.plan.domain.HifzPlan
import uz.xumoyun.feature.hifz.plan.domain.HifzRepository
import uz.xumoyun.feature.hifz.plan.domain.HifzReminder
import uz.xumoyun.feature.hifz.plan.domain.PlanStats
import uz.xumoyun.feature.hifz.plan.domain.PlanTarget
import uz.xumoyun.feature.hifz.plan.domain.QuranLayout
import uz.xumoyun.feature.hifz.plan.domain.newHifzPlan
import uz.xumoyun.feature.hifz.plan.domain.planStats

data class PlanUiState(
    val loading: Boolean = true,
    val quranAvailable: Boolean = true,
    val failed: Boolean = false,
    val surahs: List<SurahInfo> = emptyList(),
    val target: PlanTarget = PlanTarget.All,
    val dailyNewAyahs: Int = PlanViewModel.DEFAULT_DAILY,
    /** null — eslatma o'chiq. */
    val reminderPrayer: String? = null,
    val reminderOffsetMin: Int = PlanViewModel.DEFAULT_OFFSET,
    val hasActivePlan: Boolean = false,
    val preview: PlanStats? = null,
    val saved: Boolean = false,
)

/** Hifz rejasi ekrani: maqsad, kunlik oyatlar, eslatma. Saqlanganda yangi faol reja yoziladi. */
@HiltViewModel
class PlanViewModel @Inject constructor(
    private val repository: HifzRepository,
    private val quran: QuranSource,
    private val time: TimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanUiState())
    val state: StateFlow<PlanUiState> = _state.asStateFlow()

    private var layout: QuranLayout? = null
    private var progress: Map<Int, AyahProgress> = emptyMap()
    private var initialized = false

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
            _state.update { it.copy(surahs = surahs) }
            combine(repository.observeActivePlan(), repository.observeProgress()) { plan, p -> plan to p }
                .collect { (plan, p) ->
                    progress = p
                    if (!initialized) {
                        initialized = true
                        if (plan != null) applyExisting(q, plan)
                    }
                    _state.update { it.copy(loading = false, hasActivePlan = plan != null) }
                    refreshPreview()
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, failed = true) }
        }
    }

    private fun applyExisting(q: QuranLayout, plan: HifzPlan) {
        _state.update {
            it.copy(
                target = q.targetOf(plan) ?: PlanTarget.All,
                dailyNewAyahs = plan.dailyNewAyahs,
                reminderPrayer = plan.reminderAfterPrayer,
                reminderOffsetMin = plan.reminderOffsetMin,
            )
        }
    }

    fun selectTarget(target: PlanTarget) {
        _state.update { it.copy(target = target, saved = false) }
        refreshPreview()
    }

    fun setDaily(value: Int) {
        _state.update { it.copy(dailyNewAyahs = value.coerceIn(DAILY_MIN, DAILY_MAX), saved = false) }
        refreshPreview()
    }

    fun changeDaily(delta: Int) = setDaily(_state.value.dailyNewAyahs + delta)

    /** null — eslatma o'chadi; kalit [HifzReminder.PRAYER_KEYS] dan bo'lishi kerak. */
    fun setReminderPrayer(key: String?) {
        require(key == null || key in HifzReminder.PRAYER_KEYS) { "namoz kaliti: $key" }
        _state.update { it.copy(reminderPrayer = key, saved = false) }
    }

    fun setReminderOffset(minutes: Int) {
        require(minutes in HifzReminder.OFFSETS_MIN) { "eslatma oralig'i: $minutes" }
        _state.update { it.copy(reminderOffsetMin = minutes, saved = false) }
    }

    fun save() {
        val s = _state.value
        val q = layout ?: return
        val plan = newHifzPlan(
            target = s.target,
            layout = q,
            dailyNewAyahs = s.dailyNewAyahs,
            reminderPrayer = s.reminderPrayer,
            reminderOffsetMin = s.reminderOffsetMin,
            today = time.today(),
        )
        viewModelScope.launch {
            repository.activatePlan(plan)
            _state.update { it.copy(saved = true) }
        }
    }

    private fun refreshPreview() {
        val s = _state.value
        val q = layout ?: return
        val ids = q.rangeOf(s.target).ids
        _state.update { it.copy(preview = planStats(time.today(), ids, progress, s.dailyNewAyahs)) }
    }

    companion object {
        const val DEFAULT_DAILY = 5
        const val DAILY_MIN = 1
        const val DAILY_MAX = 50
        const val DEFAULT_OFFSET = 15
    }
}
