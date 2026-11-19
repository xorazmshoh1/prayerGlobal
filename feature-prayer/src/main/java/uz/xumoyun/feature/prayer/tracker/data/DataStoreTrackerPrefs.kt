package uz.xumoyun.feature.prayer.tracker.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.xumoyun.feature.prayer.tracker.domain.TrackerPrefs
import javax.inject.Inject

/** Qazo sozlamasi DataStore'da ("settings" fayli). Standart: yoqilgan. */
class DataStoreTrackerPrefs @Inject constructor(private val store: DataStore<Preferences>) : TrackerPrefs {

    override val autoQadaOnMissed: Flow<Boolean> =
        store.data.map { it[AUTO_QADA] ?: true }.distinctUntilChanged()

    override suspend fun setAutoQadaOnMissed(enabled: Boolean) {
        store.edit { it[AUTO_QADA] = enabled }
    }

    private companion object {
        val AUTO_QADA = booleanPreferencesKey("qada_auto_on_missed")
    }
}
