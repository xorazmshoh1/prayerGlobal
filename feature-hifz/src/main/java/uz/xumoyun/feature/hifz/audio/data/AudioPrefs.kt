package uz.xumoyun.feature.hifz.audio.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.xumoyun.feature.hifz.audio.domain.DownloadPolicy
import javax.inject.Inject

/** Audio sozlamalari (DataStore, core-data dagi umumiy "settings" fayli). */
class AudioPrefs @Inject constructor(private val store: DataStore<Preferences>) {

    val wifiOnly: Flow<Boolean> = store.data.map { it[WIFI_ONLY] ?: DownloadPolicy.WIFI_ONLY_DEFAULT }

    suspend fun setWifiOnly(value: Boolean) {
        store.edit { it[WIFI_ONLY] = value }
    }

    private companion object {
        val WIFI_ONLY = booleanPreferencesKey("audio_wifi_only")
    }
}
