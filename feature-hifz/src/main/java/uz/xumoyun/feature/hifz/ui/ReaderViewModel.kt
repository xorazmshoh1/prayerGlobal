package uz.xumoyun.feature.hifz.ui

import androidx.lifecycle.SavedStateHandle
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
import uz.xumoyun.feature.hifz.domain.AyahText
import uz.xumoyun.feature.hifz.domain.QuranCatalog
import uz.xumoyun.feature.hifz.domain.QuranSource
import uz.xumoyun.feature.hifz.domain.SurahInfo

data class ReaderUiState(
    val loading: Boolean = true,
    val available: Boolean = true,
    val failed: Boolean = false,
    val surah: SurahInfo? = null,
    val juz: Int? = null,
    val ayahs: List<AyahText> = emptyList(),
    val fontSp: Float = QuranCatalog.FONT_DEFAULT_SP,
)

/**
 * Mus'haf o'qish ekrani: sura yoki juz oyatlari. Shrift o'lchami [SavedStateHandle] da saqlanadi
 * (jarayon qayta yaratilganda ham, lekin boshqa ekran/ilova sessiyasiga o'tmaydi).
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val source: QuranSource,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val surahId: Int? = savedState.get<Int>(ARG_SURAH)
    private val juzNumber: Int? = savedState.get<Int>(ARG_JUZ)

    private val _state = MutableStateFlow(
        ReaderUiState(
            juz = juzNumber,
            fontSp = QuranCatalog.clampFontSp(savedState.get<Float>(KEY_FONT) ?: QuranCatalog.FONT_DEFAULT_SP),
        ),
    )
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

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
                val ayahs = when {
                    surahId != null -> source.ayahsOfSurah(surahId)
                    juzNumber != null -> source.ayahsOfJuz(juzNumber)
                    else -> emptyList()
                }
                val surah = surahId?.let { id -> source.surahs().firstOrNull { it.id == id } }
                _state.update { it.copy(loading = false, available = true, surah = surah, ayahs = ayahs) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    /** Shrift o'lchamini bir qadam o'zgartiradi (20–40 sp). */
    fun changeFont(larger: Boolean) {
        val next = QuranCatalog.stepFontSp(_state.value.fontSp, larger)
        savedState[KEY_FONT] = next
        _state.update { it.copy(fontSp = next) }
    }

    companion object {
        const val ARG_SURAH = "surahId"
        const val ARG_JUZ = "juz"
        const val KEY_FONT = "fontSp"
    }
}
