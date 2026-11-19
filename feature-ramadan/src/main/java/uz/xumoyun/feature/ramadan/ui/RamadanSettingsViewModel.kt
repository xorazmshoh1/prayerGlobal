package uz.xumoyun.feature.ramadan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.feature.ramadan.data.RamadanPrefs
import uz.xumoyun.feature.ramadan.domain.RamadanSettings
import javax.inject.Inject

/** R6 — Ramazon sozlamalari: imsok farqi, eslatmalar, qadr, Ramazon boshini ±1 kun tuzatish. */
data class RamadanSettingsState(val settings: RamadanSettings, val hijriAdjustDays: Int)

@HiltViewModel
class RamadanSettingsViewModel @Inject constructor(
    private val prefs: RamadanPrefs,
    private val common: CommonSettings,
) : ViewModel() {

    val state: StateFlow<RamadanSettingsState?> = combine(prefs.settings, common.hijriAdjustDays) { s, o ->
        RamadanSettingsState(s, o)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setImsakOffset(minutes: Int) = update { copy(imsakOffsetMin = minutes) }
    fun setSuhoorOn(on: Boolean) = update { copy(suhoorOn = on) }
    fun setSuhoorMinBefore(minutes: Int) = update { copy(suhoorMinBefore = minutes) }
    fun setIftarOn(on: Boolean) = update { copy(iftarOn = on) }
    fun setIftarMinBefore(minutes: Int) = update { copy(iftarMinBefore = minutes) }
    fun setQadrOn(on: Boolean) = update { copy(qadrOn = on) }

    fun setHijriAdjust(days: Int) {
        viewModelScope.launch { common.setHijriAdjustDays(days) }
    }

    private fun update(change: RamadanSettings.() -> RamadanSettings) {
        viewModelScope.launch { prefs.update { it.change() } }
    }
}
