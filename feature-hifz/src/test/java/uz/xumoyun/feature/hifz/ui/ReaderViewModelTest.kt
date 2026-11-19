package uz.xumoyun.feature.hifz.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `surah route loads its ayahs and title`() {
        val vm = ReaderViewModel(FakeQuranSource(), SavedStateHandle(mapOf(ReaderViewModel.ARG_SURAH to 1)))
        val s = vm.state.value
        assertEquals(listOf(1, 2), s.ayahs.map { it.globalId })
        assertEquals("Alpha", s.surah?.nameTranslit)
        assertEquals(null, s.juz)
    }

    @Test fun `juz route loads ayahs of that juz`() {
        val vm = ReaderViewModel(FakeQuranSource(), SavedStateHandle(mapOf(ReaderViewModel.ARG_JUZ to 1)))
        assertEquals(3, vm.state.value.ayahs.size)
        assertEquals(1, vm.state.value.juz)
        assertEquals(null, vm.state.value.surah)
    }

    @Test fun `font size starts at 26 sp and steps within 20 to 40`() {
        val vm = ReaderViewModel(FakeQuranSource(), SavedStateHandle(mapOf(ReaderViewModel.ARG_SURAH to 1)))
        assertEquals(26f, vm.state.value.fontSp, 0f)
        vm.changeFont(larger = true)
        assertEquals(28f, vm.state.value.fontSp, 0f)
        repeat(20) { vm.changeFont(larger = true) }
        assertEquals(40f, vm.state.value.fontSp, 0f)
        repeat(30) { vm.changeFont(larger = false) }
        assertEquals(20f, vm.state.value.fontSp, 0f)
    }

    @Test fun `font size is restored from saved state`() {
        val saved = SavedStateHandle(mapOf(ReaderViewModel.ARG_SURAH to 1, ReaderViewModel.KEY_FONT to 34f))
        val vm = ReaderViewModel(FakeQuranSource(), saved)
        assertEquals(34f, vm.state.value.fontSp, 0f)
    }

    @Test fun `font change is written to saved state`() {
        val saved = SavedStateHandle(mapOf(ReaderViewModel.ARG_SURAH to 1))
        val vm = ReaderViewModel(FakeQuranSource(), saved)
        vm.changeFont(larger = true)
        assertEquals(28f, saved.get<Float>(ReaderViewModel.KEY_FONT))
    }

    @Test fun `missing quran db is reported`() {
        val vm = ReaderViewModel(FakeQuranSource(available = false), SavedStateHandle(mapOf(ReaderViewModel.ARG_SURAH to 1)))
        assertTrue(!vm.state.value.available)
        assertTrue(vm.state.value.ayahs.isEmpty())
    }

    @Test fun `read error is reported as failed`() {
        val vm = ReaderViewModel(FakeQuranSource(fail = true), SavedStateHandle(mapOf(ReaderViewModel.ARG_JUZ to 1)))
        assertTrue(vm.state.value.failed)
    }
}
