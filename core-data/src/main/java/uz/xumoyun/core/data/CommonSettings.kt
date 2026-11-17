package uz.xumoyun.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.xumoyun.core.common.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton

/** Uchala ilovada bir xil bo'lgan sozlamalar: tema, til, onboarding. */
@Singleton
class CommonSettings @Inject constructor(private val store: DataStore<Preferences>) {

    val themeMode: Flow<ThemeMode> = store.data.map { p ->
        p[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }.distinctUntilChanged()

    val onboardingDone: Flow<Boolean> = store.data.map { it[ONBOARDING] ?: false }.distinctUntilChanged()

    val language: Flow<String?> = store.data.map { it[LANGUAGE] }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setOnboardingDone(done: Boolean = true) = store.edit { it[ONBOARDING] = done }
    suspend fun setLanguage(tag: String) = store.edit { it[LANGUAGE] = tag }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val LANGUAGE = stringPreferencesKey("language")
    }
}
