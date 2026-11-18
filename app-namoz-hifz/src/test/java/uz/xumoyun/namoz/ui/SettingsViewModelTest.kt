package uz.xumoyun.namoz.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import uz.xumoyun.core.data.AppJson
import uz.xumoyun.core.data.AssetSource
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.feature.prayer.data.LocationDao
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.data.PrayerSettings
import uz.xumoyun.feature.prayer.data.SavedLocation
import uz.xumoyun.feature.prayer.domain.CalcMethod
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.namoz.tempDataStore
import uz.xumoyun.namoz.ui.settings.SettingsViewModel
import java.io.File

/** Sozlamalar ekrani feature-prayer'ning PrayerSettings'iga va core-data'ning CommonSettings'iga yozadi. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val tashkent = SavedLocation(1, "Tashkent", "UZ", 41.2995, 69.2401, "Asia/Tashkent")

    private val dao = object : LocationDao {
        override fun observeAll(): Flow<List<SavedLocation>> = flowOf(listOf(tashkent))
        override suspend fun get(id: Long) = tashkent.takeIf { it.id == id }
        override suspend fun first() = tashkent
        override suspend fun insert(location: SavedLocation) = 1L
        override suspend fun delete(id: Long) = Unit
    }

    /** methods.json endi feature-prayer modulida. */
    private val assets = object : AssetSource {
        override fun readText(path: String) = File("../feature-prayer/src/main/assets/$path").readText()
    }

    private val store = tempDataStore()
    private val settings = PrayerSettings(store)
    private val common = CommonSettings(store)
    // viewModelScope Dispatchers.Main ni yaratilishida oladi — setMain dan keyin yaratiladi.
    private val vm by lazy { SettingsViewModel(PrayerRepository(dao, settings, assets, AppJson), settings, common) }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun methodOverrideAndBackToAuto() = runTest {
        vm.setMethod(CalcMethod.KARACHI).join()
        val c = vm.state.first { it.config?.input?.method == CalcMethod.KARACHI }.config!!
        assertFalse(c.methodIsAuto)
        assertEquals(CalcMethod.MUSLIM_WORLD_LEAGUE, c.autoMethod)
        vm.setMethod(null).join()
        assertEquals(CalcMethod.MUSLIM_WORLD_LEAGUE, vm.state.first { it.config?.methodIsAuto == true }.config!!.input.method)
    }

    @Test fun adjustmentAndHijriOffsetAreSaved() = runTest {
        vm.setAdjustment(Prayer.DHUHR, 3).join()
        vm.setHijriOffset(5).join()
        assertEquals(3, settings.current().adjustments.getValue(Prayer.DHUHR))
        assertEquals(CommonSettings.MAX_HIJRI_ADJUST, vm.state.first { it.hijriAdjustDays != 0 }.hijriAdjustDays)
    }
}
