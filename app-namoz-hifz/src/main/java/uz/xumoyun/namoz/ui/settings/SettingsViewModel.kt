package uz.xumoyun.namoz.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.xumoyun.core.common.ThemeMode
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.feature.prayer.data.LocationConfig
import uz.xumoyun.feature.prayer.data.PrayerAlert
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.data.PrayerSettings
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.CalcMethod
import uz.xumoyun.feature.prayer.domain.HighLatRule
import uz.xumoyun.feature.prayer.domain.Prayer
import javax.inject.Inject

data class SettingsState(
    val config: LocationConfig? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val hijriAdjustDays: Int = 0,
)

/**
 * Sozlamalar, bildirishnomalar va onboarding ekranlari uchun ViewModel (9-bosqichda kengaytiriladi).
 * Namoz sozlamalari feature-prayer'dagi [PrayerSettings] ga yoziladi; joylar — feature-prayer LocationsViewModel.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    repository: PrayerRepository,
    private val settings: PrayerSettings,
    private val common: CommonSettings,
) : ViewModel() {

    val state: StateFlow<SettingsState> = combine(repository.config, common.themeMode, common.hijriAdjustDays) { c, t, h ->
        SettingsState(c, t, h)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun setMethod(m: CalcMethod?) = viewModelScope.launch { settings.setMethod(m) }
    fun setMadhab(m: AsrMadhab?) = viewModelScope.launch { settings.setMadhab(m) }
    fun setHighLat(r: HighLatRule) = viewModelScope.launch { settings.setHighLatRule(r) }
    fun setAdjustment(p: Prayer, minutes: Int) = viewModelScope.launch { settings.setAdjustment(p, minutes) }
    fun setHijriOffset(days: Int) = viewModelScope.launch { common.setHijriAdjustDays(days) }
    fun setTheme(t: ThemeMode) = viewModelScope.launch { common.setThemeMode(t) }
    fun setLanguage(tag: String) = viewModelScope.launch { common.setLanguage(tag) }
    fun setAlert(p: Prayer, alert: PrayerAlert) = viewModelScope.launch { settings.setAlert(p, alert) }
    fun finishOnboarding() = viewModelScope.launch { common.setOnboardingDone() }
}
