package uz.xumoyun.feature.hifz.ui

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
import uz.xumoyun.feature.hifz.domain.QuranCatalog
import uz.xumoyun.feature.hifz.domain.QuranSource
import uz.xumoyun.feature.hifz.domain.SurahInfo

enum class MushafTab { SURAHS, JUZ }

data class MushafUiState(
    val loading: Boolean = true,
    val available: Boolean = true,
    val failed: Boolean = false,
    val tab: MushafTab = MushafTab.SURAHS,
    val query: String = "",
    /** [query] bo'yicha filtrlangan suralar. */
    val surahs: List<SurahInfo> = emptyList(),
)

/** Sura/juz ro'yxati va qidiruv. Mantiq — [QuranCatalog] da (unit test qilingan). */
@HiltViewModel
class MushafViewModel @Inject constructor(
    private val source: QuranSource,
) : ViewModel() {

    private var allSurahs: List<SurahInfo> = emptyList()
    private val _state = MutableStateFlow(MushafUiState())
    val state: StateFlow<MushafUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, failed = false) }
            try {
                if (!source.isAvailable()) {
                    _state.update { it.copy(loading = false, available = false) }
                    return@launch
                }
                allSurahs = source.surahs()
                _state.update { it.copy(loading = false, available = true, surahs = QuranCatalog.filterSurahs(allSurahs, it.query)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, surahs = QuranCatalog.filterSurahs(allSurahs, query)) }
    }

    fun onTabChange(tab: MushafTab) {
        _state.update { it.copy(tab = tab) }
    }
}
