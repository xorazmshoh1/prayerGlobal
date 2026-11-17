package uz.xumoyun.namoz.data

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
import uz.xumoyun.namoz.domain.AsrMadhab
import uz.xumoyun.namoz.domain.CalcMethod
import uz.xumoyun.namoz.domain.HighLatRule
import uz.xumoyun.namoz.domain.Prayer
import javax.inject.Inject
import javax.inject.Singleton

enum class AlertSound { AZAN, BEEP, SILENT }

data class PrayerAlert(val enabled: Boolean, val sound: AlertSound, val minutesBefore: Int)

data class NamozPrefs(
    val selectedLocationId: Long?,
    /** null — davlatga qarab avtomatik. */
    val methodOverride: CalcMethod?,
    val madhabOverride: AsrMadhab?,
    val highLatRule: HighLatRule,
    val adjustments: Map<Prayer, Int>,
    val hijriOffset: Int,
    val alerts: Map<Prayer, PrayerAlert>,
    val tasbehTarget: Int,
)

@Singleton
class NamozSettings @Inject constructor(private val store: DataStore<Preferences>) {

    val prefs: Flow<NamozPrefs> = store.data.map { p ->
        NamozPrefs(
            selectedLocationId = p[SELECTED_LOCATION],
            methodOverride = p[METHOD]?.let { enumOrNull<CalcMethod>(it) },
            madhabOverride = p[MADHAB]?.let { enumOrNull<AsrMadhab>(it) },
            highLatRule = p[HIGH_LAT]?.let { enumOrNull<HighLatRule>(it) } ?: HighLatRule.MIDDLE_OF_THE_NIGHT,
            adjustments = Prayer.entries.associateWith { p[adjKey(it)] ?: 0 },
            hijriOffset = p[HIJRI_OFFSET] ?: 0,
            alerts = Prayer.entries.associateWith { prayer ->
                PrayerAlert(
                    enabled = p[alertEnabledKey(prayer)] ?: prayer.isPrayer,
                    sound = p[alertSoundKey(prayer)]?.let { enumOrNull<AlertSound>(it) } ?: AlertSound.AZAN,
                    minutesBefore = p[alertBeforeKey(prayer)] ?: 0,
                )
            },
            tasbehTarget = p[TASBEH_TARGET] ?: 33,
        )
    }.distinctUntilChanged()

    suspend fun current(): NamozPrefs = prefs.first()

    suspend fun selectLocation(id: Long) = store.edit { it[SELECTED_LOCATION] = id }
    suspend fun clearSelectedLocation() = store.edit { it.remove(SELECTED_LOCATION) }

    suspend fun setMethod(method: CalcMethod?) = store.edit {
        if (method == null) it.remove(METHOD) else it[METHOD] = method.name
    }

    suspend fun setMadhab(madhab: AsrMadhab?) = store.edit {
        if (madhab == null) it.remove(MADHAB) else it[MADHAB] = madhab.name
    }

    suspend fun setHighLatRule(rule: HighLatRule) = store.edit { it[HIGH_LAT] = rule.name }
    suspend fun setAdjustment(prayer: Prayer, minutes: Int) = store.edit { it[adjKey(prayer)] = minutes.coerceIn(-30, 30) }
    suspend fun setHijriOffset(days: Int) = store.edit { it[HIJRI_OFFSET] = days.coerceIn(-2, 2) }
    suspend fun setTasbehTarget(target: Int) = store.edit { it[TASBEH_TARGET] = target.coerceIn(1, 100_000) }

    suspend fun setAlert(prayer: Prayer, alert: PrayerAlert) = store.edit {
        it[alertEnabledKey(prayer)] = alert.enabled
        it[alertSoundKey(prayer)] = alert.sound.name
        it[alertBeforeKey(prayer)] = alert.minutesBefore.coerceIn(0, 60)
    }

    private companion object {
        val SELECTED_LOCATION = longPreferencesKey("selected_location")
        val METHOD = stringPreferencesKey("calc_method")
        val MADHAB = stringPreferencesKey("asr_madhab")
        val HIGH_LAT = stringPreferencesKey("high_lat_rule")
        val HIJRI_OFFSET = intPreferencesKey("hijri_offset")
        val TASBEH_TARGET = intPreferencesKey("tasbeh_target")
        fun adjKey(p: Prayer) = intPreferencesKey("adj_${p.name}")
        fun alertEnabledKey(p: Prayer) = booleanPreferencesKey("alert_on_${p.name}")
        fun alertSoundKey(p: Prayer) = stringPreferencesKey("alert_sound_${p.name}")
        fun alertBeforeKey(p: Prayer) = intPreferencesKey("alert_before_${p.name}")
    }
}

inline fun <reified T : Enum<T>> enumOrNull(name: String): T? = runCatching { enumValueOf<T>(name) }.getOrNull()
