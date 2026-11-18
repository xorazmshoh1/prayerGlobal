package uz.xumoyun.feature.prayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.xumoyun.feature.prayer.data.PrayerSettings
import uz.xumoyun.feature.prayer.domain.AlertMode
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.domain.PrayerAlert
import javax.inject.Inject

/**
 * Bildirishnoma sozlamalari (TZ 5-bo'lim, 5-band): har vaqt uchun rejim (ovozli / ovozsiz / o'chiq) va
 * 0–30 daqiqa oldin eslatma. Saqlangandan keyin signallarni PrayerAlarmScheduler o'zi qayta qo'yadi.
 */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val settings: PrayerSettings,
) : ViewModel() {

    /** null — hali yuklanmoqda. */
    val alerts: StateFlow<Map<Prayer, PrayerAlert>?> = settings.prefs.map { it.alerts }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setMode(prayer: Prayer, mode: AlertMode) = update(prayer) { it.copy(mode = mode) }

    fun setMinutesBefore(prayer: Prayer, minutes: Int) =
        update(prayer) { it.copy(minutesBefore = PrayerAlert.clampMinutes(minutes)) }

    private fun update(prayer: Prayer, change: (PrayerAlert) -> PrayerAlert) = viewModelScope.launch {
        settings.setAlert(prayer, change(settings.current().alerts.getValue(prayer)))
    }
}
