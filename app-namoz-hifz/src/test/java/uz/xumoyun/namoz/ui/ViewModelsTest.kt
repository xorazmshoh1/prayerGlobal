package uz.xumoyun.namoz.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import uz.xumoyun.core.data.AppJson
import uz.xumoyun.namoz.FakeHijri
import uz.xumoyun.namoz.FakeLocationDao
import uz.xumoyun.namoz.FakeTasbehDao
import uz.xumoyun.namoz.FileAssets
import uz.xumoyun.namoz.NoLocation
import uz.xumoyun.namoz.Tashkent
import uz.xumoyun.namoz.data.NamozSettings
import uz.xumoyun.namoz.data.PrayerRepository
import uz.xumoyun.namoz.domain.CalcMethod
import uz.xumoyun.namoz.domain.Prayer
import uz.xumoyun.namoz.tempDataStore
import uz.xumoyun.namoz.ui.home.HomeViewModel
import uz.xumoyun.namoz.ui.tasbeh.TasbehViewModel
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    private val zone = ZoneId.of("Asia/Tashkent")
    private val time = FixedTimeSource(ZonedDateTime.of(2026, 11, 17, 10, 0, 0, 0, zone).toInstant(), zone)
    private val settings = NamozSettings(tempDataStore())
    private val dao = FakeLocationDao(listOf(Tashkent))
    private val repository = PrayerRepository(dao, settings, FileAssets, AppJson)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun homeShowsNextPrayerAndAutoMethod() = runTest {
        val vm = HomeViewModel(repository, settings, FakeHijri, NoLocation, time)
        val config = repository.buildConfig(Tashkent, settings.current())
        assertEquals(CalcMethod.UZBEKISTAN, config.input.method)
        assertTrue(config.methodIsAuto)

        val state = vm.build(config)
        assertEquals("Tashkent", state.city)
        assertEquals(Prayer.DHUHR, state.next.prayer)
        assertEquals(6, state.rows.size)
        assertEquals(Prayer.SUNRISE, state.rows.single { it.isCurrent }.prayer)
        assertFalse(state.rows.first { it.prayer == Prayer.SUNRISE }.alertOn)
    }

    @Test fun homeToggleAlertPersists() = runTest {
        val vm = HomeViewModel(repository, settings, FakeHijri, NoLocation, time)
        assertTrue(settings.current().alerts.getValue(Prayer.FAJR).enabled)
        vm.toggleAlert(Prayer.FAJR).join()
        assertFalse(settings.current().alerts.getValue(Prayer.FAJR).enabled)
    }

    @Test fun methodOverrideReplacesAuto() = runTest {
        settings.setMethod(CalcMethod.MUSLIM_WORLD_LEAGUE)
        val config = repository.buildConfig(Tashkent, settings.current())
        assertEquals(CalcMethod.MUSLIM_WORLD_LEAGUE, config.input.method)
        assertFalse(config.methodIsAuto)
        assertEquals(CalcMethod.UZBEKISTAN, config.autoMethod)
    }

    @Test fun tasbehReachesTarget() = runTest {
        val tasbeh = FakeTasbehDao()
        val vm = TasbehViewModel(tasbeh, settings, time)
        repeat(32) { assertFalse(vm.tap()) }
        assertTrue(vm.tap())
        assertEquals(33, tasbeh.rows.value.single().count)
        vm.reset()
        assertFalse(vm.tap())
        assertEquals(34, tasbeh.rows.value.single().count)
    }
}
