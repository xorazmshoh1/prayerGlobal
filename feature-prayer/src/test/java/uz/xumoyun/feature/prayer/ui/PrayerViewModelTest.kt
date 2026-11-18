package uz.xumoyun.feature.prayer.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import uz.xumoyun.core.data.AppJson
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.feature.prayer.FakeHijri
import uz.xumoyun.feature.prayer.FakeLocationDao
import uz.xumoyun.feature.prayer.FakeLocationProvider
import uz.xumoyun.feature.prayer.FileAssets
import uz.xumoyun.feature.prayer.Tashkent
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.data.PrayerSettings
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.CalcMethod
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.tempDataStore
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerViewModelTest {
    private val zone = ZoneId.of("Asia/Tashkent")
    private val time = FixedTimeSource(ZonedDateTime.of(2026, 11, 17, 10, 0, 0, 0, zone).toInstant(), zone)
    private val store = tempDataStore()
    private val settings = PrayerSettings(store)
    private val common = CommonSettings(store)
    private val dao = FakeLocationDao(listOf(Tashkent))
    private val repository = PrayerRepository(dao, settings, FileAssets, AppJson)
    private val hijri = FakeHijri()
    private val location = FakeLocationProvider()

    private fun vm() = PrayerViewModel(repository, settings, common, hijri, location, time)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun showsNextPrayerWithAutoMethod() = runTest {
        val config = repository.buildConfig(Tashkent, settings.current())
        // TZ 2-bo'lim: O'zbekiston — MWL, Hanafiy.
        assertEquals(CalcMethod.MUSLIM_WORLD_LEAGUE, config.input.method)
        assertEquals(AsrMadhab.HANAFI, config.input.madhab)
        assertTrue(config.methodIsAuto)

        val state = vm().build(config)
        assertEquals("Tashkent", state.city)
        assertEquals(time.today(), state.today)
        assertEquals(Prayer.DHUHR, state.board.next.prayer)
        assertEquals(6, state.board.rows.size)
        // 10:00 da quyosh chiqqan, Peshin hali kirmagan.
        assertEquals(Prayer.SUNRISE, state.board.rows.single { it.isCurrent }.prayer)
        assertFalse(state.board.rows.first { it.prayer == Prayer.SUNRISE }.alertOn)
        assertEquals(state.board.rows.first { it.prayer == Prayer.DHUHR }.time, state.board.next.time)
    }

    @Test fun hijriDateUsesCommonAdjustment() = runTest {
        common.setHijriAdjustDays(1)
        val state = vm().state.first { it is PrayerState.Ready } as PrayerState.Ready
        assertEquals("2 Ramadan 1448", state.hijri)
        assertEquals(1, hijri.offsets.last())
        assertEquals("1 Ramadan 1448", vm().build(repository.currentConfig()!!, hijriAdjust = 0).hijri)
    }

    @Test fun stateFlowEmitsReadyOrNoLocation() = runTest {
        assertTrue(vm().state.first { it != PrayerState.Loading } is PrayerState.Ready)
        dao.items.value = emptyList()
        assertEquals(PrayerState.NoLocation, vm().state.first { it != PrayerState.Loading })
    }

    @Test fun toggleAlertPersists() = runTest {
        assertTrue(settings.current().alerts.getValue(Prayer.FAJR).enabled)
        vm().toggleAlert(Prayer.FAJR).join()
        assertFalse(settings.current().alerts.getValue(Prayer.FAJR).enabled)
        val state = vm().build(repository.currentConfig()!!)
        assertFalse(state.board.rows.first { it.prayer == Prayer.FAJR }.alertOn)
    }

    @Test fun adjustmentChangesDisplayedTime() = runTest {
        val before = vm().build(repository.currentConfig()!!).board.rows.first { it.prayer == Prayer.MAGHRIB }.time
        settings.setAdjustment(Prayer.MAGHRIB, 2)
        val after = vm().build(repository.currentConfig()!!).board.rows.first { it.prayer == Prayer.MAGHRIB }.time
        assertEquals(before.plusMinutes(2), after)
    }

    @Test fun travelCheckNeedsPermission() = runTest {
        val vm = vm()
        vm.checkTravel().join()
        assertEquals(0, location.requests)
        assertNull(vm.travelSuggestion.value)

        location.permission = true
        vm.checkTravel().join()
        assertEquals(1, location.requests)
        assertNull(vm.travelSuggestion.value) // joy aniqlanmadi
    }
}
