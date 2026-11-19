package uz.xumoyun.feature.hifz.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MushafViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `loads all surahs and tab defaults to surahs`() {
        val vm = MushafViewModel(FakeQuranSource())
        val s = vm.state.value
        assertFalse(s.loading)
        assertTrue(s.available)
        assertEquals(2, s.surahs.size)
        assertEquals(MushafTab.SURAHS, s.tab)
    }

    @Test fun `query filters surahs by name`() {
        val vm = MushafViewModel(FakeQuranSource())
        vm.onQueryChange("beta")
        assertEquals(listOf(2), vm.state.value.surahs.map { it.id })
        assertEquals("beta", vm.state.value.query)
    }

    @Test fun `clearing query restores full list`() {
        val vm = MushafViewModel(FakeQuranSource())
        vm.onQueryChange("beta")
        vm.onQueryChange("")
        assertEquals(2, vm.state.value.surahs.size)
    }

    @Test fun `tab switch updates state`() {
        val vm = MushafViewModel(FakeQuranSource())
        vm.onTabChange(MushafTab.JUZ)
        assertEquals(MushafTab.JUZ, vm.state.value.tab)
    }

    @Test fun `missing quran db is reported as unavailable`() {
        val vm = MushafViewModel(FakeQuranSource(available = false))
        assertFalse(vm.state.value.available)
        assertTrue(vm.state.value.surahs.isEmpty())
    }

    @Test fun `read error is reported as failed, not thrown`() {
        val vm = MushafViewModel(FakeQuranSource(fail = true))
        assertTrue(vm.state.value.failed)
        assertFalse(vm.state.value.loading)
    }
}
