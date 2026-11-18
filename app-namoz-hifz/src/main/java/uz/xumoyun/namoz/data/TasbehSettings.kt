package uz.xumoyun.namoz.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Tasbeh maqsadi (4-bosqichda feature-tasbih moduliga ko'chadi). */
@Singleton
class TasbehSettings @Inject constructor(private val store: DataStore<Preferences>) {
    val target: Flow<Int> = store.data.map { it[TARGET] ?: DEFAULT_TARGET }.distinctUntilChanged()

    suspend fun setTarget(target: Int) = store.edit { it[TARGET] = target.coerceIn(1, 100_000) }

    companion object {
        const val DEFAULT_TARGET = 33
        private val TARGET = intPreferencesKey("tasbeh_target")
    }
}
