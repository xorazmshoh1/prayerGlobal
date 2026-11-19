package uz.xumoyun.feature.hifz.audio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.xumoyun.feature.hifz.audio.data.AudioPrefs
import uz.xumoyun.feature.hifz.audio.data.AudioStore
import uz.xumoyun.feature.hifz.audio.data.ReciterRepository
import uz.xumoyun.feature.hifz.audio.domain.AudioOverview
import uz.xumoyun.feature.hifz.audio.domain.DownloadPolicy
import uz.xumoyun.feature.hifz.audio.domain.DownloadedSura
import uz.xumoyun.feature.hifz.audio.domain.ReciterConfig
import uz.xumoyun.feature.hifz.audio.domain.StorageMath
import uz.xumoyun.feature.hifz.audio.domain.SuraRow
import uz.xumoyun.feature.hifz.audio.work.AudioScheduler
import uz.xumoyun.feature.hifz.domain.QuranSource
import uz.xumoyun.feature.hifz.domain.SurahInfo

data class AudioUiState(
    val loading: Boolean = true,
    /** Qori konfiguratsiyasi o'qilmadi yoki noto'g'ri. */
    val configError: Boolean = false,
    /** Qur'on matni (quran.db) yo'q: oyat soni noma'lum, yuklab bo'lmaydi. */
    val quranMissing: Boolean = false,
    val reciters: List<ReciterConfig> = emptyList(),
    val selectedKey: String? = null,
    val surahs: List<SurahInfo> = emptyList(),
    val rows: List<SuraRow> = emptyList(),
    val wifiOnly: Boolean = DownloadPolicy.WIFI_ONLY_DEFAULT,
    /** Barcha qorilar bo'yicha egallangan bayt. */
    val totalBytes: Long = 0L,
)

/**
 * Audio boshqaruvi ekrani. Holat hisobi — [AudioOverview] da (sof, unit test qilingan);
 * bu klass faqat oqim va I/O ni bog'laydi. Ish holati har [POLL_MS] da tekshiriladi.
 */
@HiltViewModel
class AudioManagementViewModel @Inject constructor(
    private val reciterRepository: ReciterRepository,
    private val quran: QuranSource,
    private val store: AudioStore,
    private val prefs: AudioPrefs,
    private val scheduler: AudioScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(AudioUiState())
    val state: StateFlow<AudioUiState> = _state.asStateFlow()

    private var downloads: List<DownloadedSura> = emptyList()

    init {
        viewModelScope.launch { loadCatalog() }
        viewModelScope.launch {
            store.observe().collect { list ->
                downloads = list
                refresh()
            }
        }
        viewModelScope.launch {
            prefs.wifiOnly.collect { value -> _state.update { it.copy(wifiOnly = value) } }
        }
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(POLL_MS)
            }
        }
    }

    fun selectReciter(key: String) {
        if (_state.value.reciters.none { it.key == key }) return
        _state.update { it.copy(selectedKey = key) }
        viewModelScope.launch { refresh() }
    }

    fun download(surah: Int) {
        val s = _state.value
        val key = s.selectedKey ?: return
        if (s.quranMissing) return
        scheduler.enqueueSura(key, surah, s.wifiOnly)
        viewModelScope.launch { refresh() }
    }

    fun delete(surah: Int) {
        val key = _state.value.selectedKey ?: return
        scheduler.cancelSura(key, surah)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.delete(key, surah) }
            refresh()
        }
    }

    /**
     * Sozlama faqat keyingi qo'yiladigan ishlarga ta'sir qiladi: navbatdagi ish eski cheklov bilan qoladi.
     */
    fun setWifiOnly(value: Boolean) {
        viewModelScope.launch { prefs.setWifiOnly(value) }
    }

    private suspend fun loadCatalog() {
        try {
            val reciters = reciterRepository.reciters
            val available = withContext(Dispatchers.IO) { quran.isAvailable() }
            val surahs = if (available) withContext(Dispatchers.IO) { quran.surahs() } else emptyList()
            _state.update {
                it.copy(
                    loading = false,
                    reciters = reciters,
                    selectedKey = it.selectedKey ?: reciters.first().key,
                    surahs = surahs,
                    quranMissing = !available || surahs.size != 114,
                )
            }
            refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, configError = true) }
        }
    }

    private suspend fun refresh() {
        val current = _state.value
        val key = current.selectedKey ?: return
        val surahs = current.surahs
        if (surahs.isEmpty()) return
        val (finished, jobs) = withContext(Dispatchers.IO) {
            val finished = surahs.associate { it.id to store.finishedAyahs(key, it.id).size }
            finished to scheduler.jobStates(key)
        }
        val rows = AudioOverview.rows(
            reciterKey = key,
            ayahCounts = surahs.sortedBy { it.id }.map { it.ayahCount },
            downloads = downloads,
            finished = finished,
            jobs = jobs,
        )
        _state.update {
            it.copy(rows = rows, totalBytes = StorageMath.total(downloads), loading = false)
        }
    }

    private companion object {
        const val POLL_MS = 2_000L
    }
}
