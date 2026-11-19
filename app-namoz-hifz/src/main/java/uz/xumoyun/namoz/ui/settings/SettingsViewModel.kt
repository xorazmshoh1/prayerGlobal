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
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.data.PrayerSettings
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.CalcMethod
import uz.xumoyun.feature.prayer.domain.HighLatRule
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.tracker.domain.TrackerPrefs
import javax.inject.Inject

data class SettingsState(
    val config: LocationConfig? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val hijriAdjustDays: Int = 0,
    /** TZ 9: MISSED belgilanganda qazoga avtomatik +1. */
    val autoQada: Boolean = true,
    /** TZ 8: xato hisobotlari (Crashlytics). */
    val crashReporting: Boolean = true,
)

/**
 * Sozlamalar va onboarding ekranlari uchun ViewModel (bildirishnomalar — feature-prayer NotificationSettingsViewModel).
 * Qazo (MISSED) va xato hisoboti sozlamalari ham shu yerda.
 * Namoz sozlamalari feature-prayer'dagi [PrayerSettings] ga yoziladi; joylar — feature-prayer LocationsViewModel.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    repository: PrayerRepository,
    private val settings: PrayerSettings,
    private val common: CommonSettings,
    private val tracker: TrackerPrefs,
) : ViewModel() {

    val state: StateFlow<SettingsState> = combine(
        repository.config, common.themeMode, common.hijriAdjustDays, tracker.autoQadaOnMissed, common.crashReportingEnabled,
    ) { c, t, h, auto, crash ->
        SettingsState(c, t, h, auto, crash)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun setMethod(m: CalcMethod?) = viewModelScope.launch { settings.setMethod(m) }
    fun setMadhab(m: AsrMadhab?) = viewModelScope.launch { settings.setMadhab(m) }
    fun setHighLat(r: HighLatRule) = viewModelScope.launch { settings.setHighLatRule(r) }
    fun setAdjustment(p: Prayer, minutes: Int) = viewModelScope.launch { settings.setAdjustment(p, minutes) }
    fun setHijriOffset(days: Int) = viewModelScope.launch { common.setHijriAdjustDays(days) }
    fun setTheme(t: ThemeMode) = viewModelScope.launch { common.setThemeMode(t) }
    fun setLanguage(tag: String) = viewModelScope.launch { common.setLanguage(tag) }
    fun finishOnboarding() = viewModelScope.launch { common.setOnboardingDone() }
    fun setAutoQada(enabled: Boolean) = viewModelScope.launch { tracker.setAutoQadaOnMissed(enabled) }
    fun setCrashReporting(enabled: Boolean) = viewModelScope.launch { common.setCrashReportingEnabled(enabled) }
}
