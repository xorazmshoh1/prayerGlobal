package uz.xumoyun.namoz.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.xumoyun.core.common.ThemeMode
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.namoz.data.City
import uz.xumoyun.namoz.data.CityRepository
import uz.xumoyun.namoz.data.LocationConfig
import uz.xumoyun.namoz.data.LocationProvider
import uz.xumoyun.namoz.data.NamozSettings
import uz.xumoyun.namoz.data.PrayerAlert
import uz.xumoyun.namoz.data.PrayerRepository
import uz.xumoyun.namoz.data.db.LocationLabel
import uz.xumoyun.namoz.data.db.SavedLocation
import uz.xumoyun.namoz.domain.AsrMadhab
import uz.xumoyun.namoz.domain.CalcMethod
import uz.xumoyun.namoz.domain.HighLatRule
import uz.xumoyun.namoz.domain.Prayer
import javax.inject.Inject

data class SettingsState(
    val config: LocationConfig? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val locations: List<SavedLocation> = emptyList(),
)

/** Sozlamalar, joylar va bildirishnomalar ekranlari uchun umumiy ViewModel. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val settings: NamozSettings,
    private val common: CommonSettings,
    private val cities: CityRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    val state: StateFlow<SettingsState> = combine(repository.config, common.themeMode, repository.savedLocations) { c, t, l ->
        SettingsState(c, t, l)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _results = MutableStateFlow<List<City>>(emptyList())
    val results: StateFlow<List<City>> = _results.asStateFlow()
    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating.asStateFlow()
    private val _gpsFailed = MutableStateFlow(false)
    val gpsFailed: StateFlow<Boolean> = _gpsFailed.asStateFlow()

    fun search(q: String) {
        _query.value = q
        viewModelScope.launch { _results.value = cities.index.search(q) }
    }

    fun addCity(city: City, label: LocationLabel) = viewModelScope.launch { repository.addAndSelect(city, label) }

    /** GPS orqali joyni aniqlash. Muvaffaqiyatli bo'lsa true. */
    fun useGps(label: LocationLabel, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _locating.value = true
        _gpsFailed.value = false
        val loc = locationProvider.currentLocation()
        if (loc != null) repository.addAndSelect(locationProvider.toCity(loc), label, fromGps = true)
        else _gpsFailed.value = true
        _locating.value = false
        onDone(loc != null)
    }

    fun select(id: Long) = viewModelScope.launch { repository.select(id) }
    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }

    fun setMethod(m: CalcMethod?) = viewModelScope.launch { settings.setMethod(m) }
    fun setMadhab(m: AsrMadhab?) = viewModelScope.launch { settings.setMadhab(m) }
    fun setHighLat(r: HighLatRule) = viewModelScope.launch { settings.setHighLatRule(r) }
    fun setAdjustment(p: Prayer, minutes: Int) = viewModelScope.launch { settings.setAdjustment(p, minutes) }
    fun setHijriOffset(days: Int) = viewModelScope.launch { settings.setHijriOffset(days) }
    fun setTheme(t: ThemeMode) = viewModelScope.launch { common.setThemeMode(t) }
    fun setLanguage(tag: String) = viewModelScope.launch { common.setLanguage(tag) }
    fun setAlert(p: Prayer, alert: PrayerAlert) = viewModelScope.launch { settings.setAlert(p, alert) }
    fun finishOnboarding() = viewModelScope.launch { common.setOnboardingDone() }
}
