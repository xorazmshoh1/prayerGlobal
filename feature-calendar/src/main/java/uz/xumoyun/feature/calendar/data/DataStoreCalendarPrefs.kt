package uz.xumoyun.feature.calendar.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.xumoyun.feature.calendar.domain.CalendarPrefsStore
import uz.xumoyun.feature.calendar.domain.EventPrefsStore
import uz.xumoyun.feature.calendar.domain.EventSetting
import uz.xumoyun.feature.calendar.domain.IslamicEvent
import javax.inject.Inject
import javax.inject.Singleton

/** Taqvim va islomiy sana sozlamalari (DataStore, core-data'dagi umumiy `settings` fayli). */
@Singleton
class DataStoreCalendarPrefs @Inject constructor(private val store: DataStore<Preferences>) : CalendarPrefsStore, EventPrefsStore {

    override val hijriPrimary: Flow<Boolean> = store.data.map { it[HIJRI_PRIMARY] ?: true }.distinctUntilChanged()

    override suspend fun setHijriPrimary(value: Boolean) {
        store.edit { it[HIJRI_PRIMARY] = value }
    }

    override val settings: Flow<Map<IslamicEvent, EventSetting>> = store.data.map { p ->
        IslamicEvent.entries.associateWith { e ->
            val default = EventSetting.default(e)
            EventSetting(
                enabled = p[enabledKey(e)] ?: default.enabled,
                remindDayBefore = p[remindKey(e)] ?: default.remindDayBefore,
            )
        }
    }.distinctUntilChanged()

    override suspend fun set(event: IslamicEvent, setting: EventSetting) {
        store.edit {
            it[enabledKey(event)] = setting.enabled
            it[remindKey(event)] = setting.remindDayBefore
        }
    }

    private fun enabledKey(e: IslamicEvent) = booleanPreferencesKey("islamic_${e.key}_on")
    private fun remindKey(e: IslamicEvent) = booleanPreferencesKey("islamic_${e.key}_remind")

    private companion object {
        val HIJRI_PRIMARY = booleanPreferencesKey("calendar_hijri_primary")
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarBindings {
    @Binds abstract fun calendarPrefs(impl: DataStoreCalendarPrefs): CalendarPrefsStore
    @Binds abstract fun eventPrefs(impl: DataStoreCalendarPrefs): EventPrefsStore
}
