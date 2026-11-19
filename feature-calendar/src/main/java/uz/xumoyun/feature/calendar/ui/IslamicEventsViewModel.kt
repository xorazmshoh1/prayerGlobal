package uz.xumoyun.feature.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.xumoyun.feature.calendar.domain.EventPrefsStore
import uz.xumoyun.feature.calendar.domain.EventSetting
import uz.xumoyun.feature.calendar.domain.IslamicEvent
import javax.inject.Inject

data class IslamicEventRow(val event: IslamicEvent, val setting: EventSetting)

/** Islomiy sanalar ekrani: har sana uchun yoqish va 1 kun oldin eslatma (TZ 7-bo'lim, EventPrefs). */
@HiltViewModel
class IslamicEventsViewModel @Inject constructor(private val prefs: EventPrefsStore) : ViewModel() {
    val rows: StateFlow<List<IslamicEventRow>> = prefs.settings
        .map { map -> IslamicEvent.entries.map { IslamicEventRow(it, map[it] ?: EventSetting.default(it)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IslamicEvent.entries.map { IslamicEventRow(it, EventSetting.default(it)) })

    fun setEnabled(event: IslamicEvent, enabled: Boolean) = update(event) { copy(enabled = enabled) }

    fun setRemindDayBefore(event: IslamicEvent, remind: Boolean) = update(event) { copy(remindDayBefore = remind) }

    private fun update(event: IslamicEvent, change: EventSetting.() -> EventSetting) {
        val current = rows.value.firstOrNull { it.event == event }?.setting ?: EventSetting.default(event)
        val next = current.change()
        viewModelScope.launch { prefs.set(event, next) }
    }
}
