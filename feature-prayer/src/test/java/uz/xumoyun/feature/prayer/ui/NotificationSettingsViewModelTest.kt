package uz.xumoyun.feature.prayer.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import uz.xumoyun.feature.prayer.data.PrayerSettings
import uz.xumoyun.feature.prayer.domain.AlertMode
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.domain.PrayerAlert
import uz.xumoyun.feature.prayer.tempDataStore

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    private val settings = PrayerSettings(tempDataStore())

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun showsDefaults() = runTest {
        val vm = NotificationSettingsViewModel(settings)
        val alerts = vm.alerts.filterNotNull().first()
        assertEquals(PrayerAlert(AlertMode.SOUND, 0), alerts.getValue(Prayer.FAJR))
        assertEquals(AlertMode.OFF, alerts.getValue(Prayer.SUNRISE).mode)
    }

    @Test fun modeAndReminderArePersisted() = runTest {
        val vm = NotificationSettingsViewModel(settings)
        vm.setMode(Prayer.ASR, AlertMode.SILENT).join()
        vm.setMinutesBefore(Prayer.ASR, 15).join()
        assertEquals(PrayerAlert(AlertMode.SILENT, 15), settings.current().alerts.getValue(Prayer.ASR))
        vm.setMinutesBefore(Prayer.ASR, 45).join()
        assertEquals(30, settings.current().alerts.getValue(Prayer.ASR).minutesBefore)
        vm.setMode(Prayer.ASR, AlertMode.OFF).join()
        assertEquals(PrayerAlert(AlertMode.OFF, 30), settings.current().alerts.getValue(Prayer.ASR))
        assertEquals(PrayerAlert(AlertMode.OFF, 30), vm.alerts.filterNotNull().first { !it.getValue(Prayer.ASR).enabled }.getValue(Prayer.ASR))
    }
}
