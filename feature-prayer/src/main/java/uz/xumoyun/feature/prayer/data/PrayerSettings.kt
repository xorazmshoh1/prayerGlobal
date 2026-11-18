package uz.xumoyun.feature.prayer.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import uz.xumoyun.core.common.enumOrNull
import uz.xumoyun.feature.prayer.domain.AlertMode
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.CalcMethod
import uz.xumoyun.feature.prayer.domain.HighLatRule
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.domain.PrayerAlert
import javax.inject.Inject
import javax.inject.Singleton

/** TZ 7-bo'lim, PrayerSettings: usul, mazhab, yuqori kenglik qoidasi, har namozga ± daqiqa, ogohlantirishlar. */
data class PrayerPrefs(
    val selectedLocationId: Long? = null,
    /** null — davlatga qarab avtomatik (methods.json). */
    val methodOverride: CalcMethod? = null,
    val madhabOverride: AsrMadhab? = null,
    val highLatRule: HighLatRule = HighLatRule.SEVENTH_OF_THE_NIGHT,
    val adjustments: Map<Prayer, Int> = Prayer.entries.associateWith { 0 },
    val alerts: Map<Prayer, PrayerAlert> = Prayer.entries.associateWith { PrayerAlert.default(it) },
)

/** Namoz sozlamalari DataStore'da (core-data dagi umumiy "settings" fayli). */
@Singleton
class PrayerSettings @Inject constructor(private val store: DataStore<Preferences>) {

    val prefs: Flow<PrayerPrefs> = store.data.map { p ->
        PrayerPrefs(
            selectedLocationId = p[SELECTED_LOCATION],
            methodOverride = p[METHOD]?.let { enumOrNull<CalcMethod>(it) },
            madhabOverride = p[MADHAB]?.let { enumOrNull<AsrMadhab>(it) },
            highLatRule = p[HIGH_LAT]?.let { enumOrNull<HighLatRule>(it) } ?: HighLatRule.SEVENTH_OF_THE_NIGHT,
            adjustments = Prayer.entries.associateWith { p[adjKey(it)] ?: 0 },
            alerts = Prayer.entries.associateWith { prayer ->
                PrayerAlert(
                    mode = p[alertModeKey(prayer)]?.let { enumOrNull<AlertMode>(it) }
                        ?: legacyMode(p[legacyEnabledKey(prayer)], p[legacySoundKey(prayer)])
                        ?: PrayerAlert.default(prayer).mode,
                    minutesBefore = PrayerAlert.clampMinutes(p[alertBeforeKey(prayer)] ?: 0),
                )
            },
        )
    }.distinctUntilChanged()

    suspend fun current(): PrayerPrefs = prefs.first()

    suspend fun selectLocation(id: Long) = store.edit { it[SELECTED_LOCATION] = id }
    suspend fun clearSelectedLocation() = store.edit { it.remove(SELECTED_LOCATION) }

    suspend fun setMethod(method: CalcMethod?) = store.edit {
        if (method == null) it.remove(METHOD) else it[METHOD] = method.name
    }

    suspend fun setMadhab(madhab: AsrMadhab?) = store.edit {
        if (madhab == null) it.remove(MADHAB) else it[MADHAB] = madhab.name
    }

    suspend fun setHighLatRule(rule: HighLatRule) = store.edit { it[HIGH_LAT] = rule.name }

    suspend fun setAdjustment(prayer: Prayer, minutes: Int) =
        store.edit { it[adjKey(prayer)] = minutes.coerceIn(-MAX_ADJUSTMENT, MAX_ADJUSTMENT) }

    suspend fun setAlert(prayer: Prayer, alert: PrayerAlert) = store.edit {
        it[alertModeKey(prayer)] = alert.mode.name
        it[alertBeforeKey(prayer)] = PrayerAlert.clampMinutes(alert.minutesBefore)
        it.remove(legacyEnabledKey(prayer))
        it.remove(legacySoundKey(prayer))
    }

    companion object {
        /** Har namozga qo'lda tuzatish chegarasi, daqiqa. */
        const val MAX_ADJUSTMENT = 30

        private val SELECTED_LOCATION = longPreferencesKey("selected_location")
        private val METHOD = stringPreferencesKey("calc_method")
        private val MADHAB = stringPreferencesKey("asr_madhab")
        private val HIGH_LAT = stringPreferencesKey("high_lat_rule")
        private fun adjKey(p: Prayer) = intPreferencesKey("adj_${p.name}")
        private fun alertModeKey(p: Prayer) = stringPreferencesKey("alert_mode_${p.name}")
        private fun alertBeforeKey(p: Prayer) = intPreferencesKey("alert_before_${p.name}")

        // 1–2-bosqich formati (yoqilgan + AZAN/BEEP/SILENT ovozi): o'qiladi va setAlert'da o'chiriladi.
        private fun legacyEnabledKey(p: Prayer) = booleanPreferencesKey("alert_on_${p.name}")
        private fun legacySoundKey(p: Prayer) = stringPreferencesKey("alert_sound_${p.name}")

        internal fun legacyMode(enabled: Boolean?, sound: String?): AlertMode? = when {
            enabled == null && sound == null -> null
            enabled == false -> AlertMode.OFF
            sound == "SILENT" -> AlertMode.SILENT
            else -> AlertMode.SOUND
        }
    }
}
