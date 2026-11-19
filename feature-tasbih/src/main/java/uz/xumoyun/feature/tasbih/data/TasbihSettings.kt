package uz.xumoyun.feature.tasbih.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.xumoyun.feature.tasbih.domain.TasbihCounter
import javax.inject.Inject
import javax.inject.Singleton

/** Tasbeh maqsadi (DataStore). */
@Singleton
class TasbihSettings @Inject constructor(private val store: DataStore<Preferences>) {
    val target: Flow<Int> = store.data
        .map { it[TARGET] ?: TasbihCounter.DEFAULT_TARGET }
        .distinctUntilChanged()

    suspend fun setTarget(target: Int) {
        store.edit { it[TARGET] = target.coerceIn(1, TasbihCounter.MAX_TARGET) }
    }

    companion object {
        /** Kalit eski nom bilan qoldi: o'rnatilgan qiymatlar yo'qolmasin. */
        private val TARGET = intPreferencesKey("tasbeh_target")
    }
}
