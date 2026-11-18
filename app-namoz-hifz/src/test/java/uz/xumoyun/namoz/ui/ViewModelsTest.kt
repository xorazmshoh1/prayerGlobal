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
import uz.xumoyun.namoz.FakeTasbehDao
import uz.xumoyun.namoz.data.TasbehSettings
import uz.xumoyun.namoz.tempDataStore
import uz.xumoyun.namoz.ui.tasbeh.TasbehViewModel
import java.time.ZoneId
import java.time.ZonedDateTime

/** Ilova modulida qolgan ViewModel'lar. Namoz ViewModel testlari — feature-prayer modulida. */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    private val zone = ZoneId.of("Asia/Tashkent")
    private val time = FixedTimeSource(ZonedDateTime.of(2026, 11, 17, 10, 0, 0, 0, zone).toInstant(), zone)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun tasbehReachesTarget() = runTest {
        val tasbeh = FakeTasbehDao()
        val vm = TasbehViewModel(tasbeh, TasbehSettings(tempDataStore()), time)
        repeat(32) { assertFalse(vm.tap()) }
        assertTrue(vm.tap())
        assertEquals(33, tasbeh.rows.value.single().count)
        vm.reset()
        assertFalse(vm.tap())
        assertEquals(34, tasbeh.rows.value.single().count)
    }
}
