package uz.xumoyun.feature.prayer.ui

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.xumoyun.core.data.AppJson
import uz.xumoyun.feature.prayer.FakeLocationDao
import uz.xumoyun.feature.prayer.FileAssets
import uz.xumoyun.feature.prayer.Tashkent
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.data.PrayerSettings
import uz.xumoyun.feature.prayer.domain.AlertMode
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.CalcMethod
import uz.xumoyun.feature.prayer.domain.HighLatRule
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.domain.PrayerAlert
import uz.xumoyun.feature.prayer.tempDataStore

/** PrayerSettings (DataStore) va PrayerRepository konfiguratsiyasi. */
class PrayerSettingsTest {
    private val store = tempDataStore()
    private val settings = PrayerSettings(store)
    private val repository = PrayerRepository(FakeLocationDao(listOf(Tashkent)), settings, FileAssets, AppJson)

    @Test fun defaults() = runTest {
        val p = settings.current()
        assertNull(p.methodOverride)
        assertNull(p.madhabOverride)
        assertEquals(HighLatRule.SEVENTH_OF_THE_NIGHT, p.highLatRule)
        assertTrue(p.adjustments.values.all { it == 0 })
        assertFalse(p.alerts.getValue(Prayer.SUNRISE).enabled)
        assertTrue(Prayer.entries.filter { it.isPrayer }.all { p.alerts.getValue(it).enabled })
    }

    @Test fun methodAndMadhabOverridesReplaceAuto() = runTest {
        settings.setMethod(CalcMethod.UZBEKISTAN)
        settings.setMadhab(AsrMadhab.SHAFI)
        settings.setHighLatRule(HighLatRule.TWILIGHT_ANGLE)
        val c = repository.buildConfig(Tashkent, settings.current())
        assertEquals(CalcMethod.UZBEKISTAN, c.input.method)
        assertEquals(AsrMadhab.SHAFI, c.input.madhab)
        assertEquals(HighLatRule.TWILIGHT_ANGLE, c.input.highLatRule)
        assertFalse(c.methodIsAuto)
        assertFalse(c.madhabIsAuto)
        assertEquals(CalcMethod.MUSLIM_WORLD_LEAGUE, c.autoMethod)
        assertEquals(AsrMadhab.HANAFI, c.autoMadhab)

        settings.setMethod(null)
        settings.setMadhab(null)
        val auto = repository.buildConfig(Tashkent, settings.current())
        assertTrue(auto.methodIsAuto && auto.madhabIsAuto)
        assertEquals(CalcMethod.MUSLIM_WORLD_LEAGUE, auto.input.method)
    }

    @Test fun adjustmentsAreClampedAndPassedToCalculator() = runTest {
        settings.setAdjustment(Prayer.FAJR, 99)
        settings.setAdjustment(Prayer.ISHA, -4)
        val p = settings.current()
        assertEquals(PrayerSettings.MAX_ADJUSTMENT, p.adjustments.getValue(Prayer.FAJR))
        assertEquals(-4, p.adjustments.getValue(Prayer.ISHA))
        assertEquals(p.adjustments, repository.buildConfig(Tashkent, p).input.adjustments)
    }

    @Test fun alertRoundTripClampsReminderTo30() = runTest {
        settings.setAlert(Prayer.ASR, PrayerAlert(AlertMode.SILENT, 120))
        assertEquals(PrayerAlert(AlertMode.SILENT, 30), settings.current().alerts.getValue(Prayer.ASR))
        settings.setAlert(Prayer.SUNRISE, PrayerAlert(AlertMode.SOUND, -5))
        assertEquals(PrayerAlert(AlertMode.SOUND, 0), settings.current().alerts.getValue(Prayer.SUNRISE))
        settings.setAlert(Prayer.ISHA, PrayerAlert(AlertMode.OFF, 10))
        assertFalse(settings.current().alerts.getValue(Prayer.ISHA).enabled)
    }

    /** 1–2-bosqich formati (alert_on_* + alert_sound_*) yangi rejimga o'qiladi; saqlanganda eski kalitlar o'chadi. */
    @Test fun legacyAlertKeysAreMigrated() = runTest {
        store.edit {
            it[booleanPreferencesKey("alert_on_FAJR")] = false
            it[stringPreferencesKey("alert_sound_FAJR")] = "AZAN"
            it[booleanPreferencesKey("alert_on_ASR")] = true
            it[stringPreferencesKey("alert_sound_ASR")] = "SILENT"
            it[booleanPreferencesKey("alert_on_ISHA")] = true
            it[stringPreferencesKey("alert_sound_ISHA")] = "BEEP"
            it[intPreferencesKey("alert_before_ISHA")] = 60
        }
        val alerts = settings.current().alerts
        assertEquals(AlertMode.OFF, alerts.getValue(Prayer.FAJR).mode)
        assertEquals(AlertMode.SILENT, alerts.getValue(Prayer.ASR).mode)
        assertEquals(PrayerAlert(AlertMode.SOUND, 30), alerts.getValue(Prayer.ISHA))
        assertEquals(AlertMode.SOUND, alerts.getValue(Prayer.DHUHR).mode)

        settings.setAlert(Prayer.FAJR, PrayerAlert(AlertMode.SOUND, 5))
        val raw = store.data.first()
        assertNull(raw[booleanPreferencesKey("alert_on_FAJR")])
        assertNull(raw[stringPreferencesKey("alert_sound_FAJR")])
        assertEquals(PrayerAlert(AlertMode.SOUND, 5), settings.current().alerts.getValue(Prayer.FAJR))
    }

    @Test fun unknownCountryUsesDefaultAndBadZoneFallsBack() = runTest {
        val c = repository.buildConfig(Tashkent.copy(countryCode = "", timeZone = "Not/AZone"), settings.current())
        assertEquals(CalcMethod.MUSLIM_WORLD_LEAGUE, c.input.method)
        assertEquals(AsrMadhab.SHAFI, c.input.madhab)
        assertEquals(java.time.ZoneId.systemDefault(), c.zone)
    }
}
