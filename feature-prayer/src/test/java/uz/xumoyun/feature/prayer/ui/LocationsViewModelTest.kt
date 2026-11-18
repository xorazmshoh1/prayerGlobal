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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.xumoyun.core.data.AppJson
import uz.xumoyun.feature.prayer.FakeLocationDao
import uz.xumoyun.feature.prayer.FakeLocationProvider
import uz.xumoyun.feature.prayer.FileAssets
import uz.xumoyun.feature.prayer.Jakarta
import uz.xumoyun.feature.prayer.Tashkent
import uz.xumoyun.feature.prayer.data.CityRepository
import uz.xumoyun.feature.prayer.data.LocationLabel
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.data.PrayerSettings
import uz.xumoyun.feature.prayer.domain.CalcMethod
import uz.xumoyun.feature.prayer.tempDataStore

@OptIn(ExperimentalCoroutinesApi::class)
class LocationsViewModelTest {
    private val settings = PrayerSettings(tempDataStore())
    private val dao = FakeLocationDao()
    private val repository = PrayerRepository(dao, settings, FileAssets, AppJson)
    private val cities = CityRepository(FileAssets, AppJson)
    private val location = FakeLocationProvider()

    private fun vm() = LocationsViewModel(repository, cities, location)

    /** state WhileSubscribed — kerakli holatni obuna bo'lib kutamiz. */
    private suspend fun LocationsViewModel.await(predicate: (LocationsState) -> Boolean) = state.first(predicate)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun searchAndPickCity() = runTest {
        val vm = vm()
        assertTrue(vm.await { it.results.isNotEmpty() }.query.isEmpty()) // bo'sh so'rov — eng yirik shaharlar
        vm.search("jakar")
        val jakarta = vm.await { it.query == "jakar" }.results.first()
        assertEquals("ID", jakarta.countryCode)

        vm.addCity(jakarta, LocationLabel.HOME).join()
        val saved = dao.items.value.single()
        assertEquals(LocationLabel.HOME, saved.label)
        assertEquals(saved.id, settings.current().selectedLocationId)
        // Indoneziya → Singapur usuli avtomatik.
        assertEquals(CalcMethod.SINGAPORE, repository.currentConfig()!!.input.method)
    }

    @Test fun deniedPermissionFallsBackToSearch() = runTest {
        val vm = vm()
        var found = false
        vm.onPermissionResult(granted = false, label = LocationLabel.NONE) { found = true }
        assertEquals(GpsResult.DENIED, vm.await { it.gps != null }.gps)
        assertEquals(0, location.requests)
        assertFalse(found)
    }

    @Test fun gpsWithoutFixReportsFailure() = runTest {
        location.permission = true
        val vm = vm()
        var found = false
        vm.onPermissionResult(granted = true, label = LocationLabel.NONE) { found = true }
        val state = vm.await { it.gps != null }
        assertEquals(GpsResult.FAILED, state.gps)
        assertFalse(state.locating)
        assertEquals(1, location.requests)
        assertFalse(found)
        assertTrue(dao.items.value.isEmpty())
    }

    @Test fun selectAndDeleteKeepSelectionValid() = runTest {
        dao.items.value = listOf(Tashkent, Jakarta)
        val vm = vm()
        vm.select(Jakarta.id).join()
        assertEquals(listOf(Tashkent, Jakarta), vm.await { it.selectedId == Jakarta.id }.saved)
        vm.delete(Jakarta.id).join()
        assertEquals(Tashkent.id, settings.current().selectedLocationId)
        assertEquals(listOf(Tashkent), dao.items.value)
    }
}
