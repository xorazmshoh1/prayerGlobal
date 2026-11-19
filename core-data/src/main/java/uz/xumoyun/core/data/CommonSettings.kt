package uz.xumoyun.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.xumoyun.core.common.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton

/** Modullar uchun umumiy sozlamalar: tema, til, onboarding, hijriy sana tuzatishi. */
@Singleton
class CommonSettings @Inject constructor(private val store: DataStore<Preferences>) {

    val themeMode: Flow<ThemeMode> = store.data.map { p ->
        p[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }.distinctUntilChanged()

    val onboardingDone: Flow<Boolean> = store.data.map { it[ONBOARDING] ?: false }.distinctUntilChanged()

    val language: Flow<String?> = store.data.map { it[LANGUAGE] }.distinctUntilChanged()

    /** Hijriy sanani ±kun tuzatish (TZ: hijriAdjustDays). Namoz, taqvim va Ramazon ekranlari bir xil qiymatni ishlatadi. */
    val hijriAdjustDays: Flow<Int> = store.data.map { it[HIJRI_OFFSET] ?: 0 }.distinctUntilChanged()

    /** TZ 8: xato hisobotlari (Crashlytics). Standart — yoqilgan; foydalanuvchi sozlamada o'chiradi. */
    val crashReportingEnabled: Flow<Boolean> = store.data.map { it[CRASH_REPORTING] ?: true }.distinctUntilChanged()

    suspend fun setCrashReportingEnabled(enabled: Boolean) = store.edit { it[CRASH_REPORTING] = enabled }

    suspend fun setHijriAdjustDays(days: Int) = store.edit { it[HIJRI_OFFSET] = days.coerceIn(-MAX_HIJRI_ADJUST, MAX_HIJRI_ADJUST) }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setOnboardingDone(done: Boolean = true) = store.edit { it[ONBOARDING] = done }
    suspend fun setLanguage(tag: String) = store.edit { it[LANGUAGE] = tag }

    companion object {
        // TZ hijriAdjustDays −1/0/+1 deydi; 1-bosqichdagi ±2 chegarasi saqlandi (11-bosqichda qayta ko'riladi).
        const val MAX_HIJRI_ADJUST = 2
        private val HIJRI_OFFSET = intPreferencesKey("hijri_offset")
        private val THEME = stringPreferencesKey("theme_mode")
        private val ONBOARDING = booleanPreferencesKey("onboarding_done")
        private val LANGUAGE = stringPreferencesKey("language")
        private val CRASH_REPORTING = booleanPreferencesKey("crash_reporting_enabled")
    }
}
