package uz.xumoyun.feature.qibla.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import uz.xumoyun.core.data.place.SelectedPlace
import uz.xumoyun.core.data.place.SelectedPlaceSource
import uz.xumoyun.feature.qibla.data.CompassReading
import uz.xumoyun.feature.qibla.data.CompassSource

@OptIn(ExperimentalCoroutinesApi::class)
class QiblaViewModelTest {
    private val tashkent = SelectedPlace("Tashkent", 41.2995, 69.2401)
    private val jakarta = SelectedPlace("Jakarta", -6.2088, 106.8456)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakePlaces(initial: SelectedPlace?) : SelectedPlaceSource {
        val flow = MutableStateFlow(initial)
        override val place: Flow<SelectedPlace?> get() = flow
    }

    private class FakeCompass(private val available: Boolean = true) : CompassSource {
        val events = MutableSharedFlow<CompassReading>(extraBufferCapacity = 16)
        override fun isAvailable() = available
        override fun readings(latitude: Double, longitude: Double) = events
    }

    @Test fun noChosenPlaceGivesNoTarget() = runTest {
        val vm = QiblaViewModel(FakePlaces(null), FakeCompass())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.target)
    }

    @Test fun targetIsComputedFromPlace() = runTest {
        val vm = QiblaViewModel(FakePlaces(tashkent), FakeCompass())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceUntilIdle()
        val target = vm.state.value.target!!
        assertEquals("Tashkent", target.city)
        assertEquals(240.3, target.bearing, 1.0)
        assertEquals(3531.4, target.distanceKm, 1.0)
    }

    @Test fun alignmentFiresEventOnEntry() = runTest {
        val compass = FakeCompass()
        val vm = QiblaViewModel(FakePlaces(tashkent), compass)
        val events = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.alignedEvents.collect { events += it } }
        advanceUntilIdle()

        compass.events.tryEmit(CompassReading(azimuth = 100.0, accuracy = 3))
        advanceUntilIdle()
        assertFalse(vm.state.value.aligned)
        assertEquals(0, events.size)

        // Silliqlash (alpha 0.3): bir necha o'qishdan keyin strelka qiblaga yetadi.
        repeat(20) {
            compass.events.tryEmit(CompassReading(azimuth = 240.0, accuracy = 3))
            advanceUntilIdle()
        }
        assertTrue(vm.state.value.aligned)
        assertEquals(1, events.size)

        compass.events.tryEmit(CompassReading(azimuth = 240.1, accuracy = 3))
        advanceUntilIdle()
        assertEquals(1, events.size)
    }

    @Test fun lowAccuracyFlagsCalibration() = runTest {
        val compass = FakeCompass()
        val vm = QiblaViewModel(FakePlaces(tashkent), compass)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceUntilIdle()
        compass.events.tryEmit(CompassReading(azimuth = 10.0, accuracy = 1))
        advanceUntilIdle()
        assertTrue(vm.state.value.needsCalibration)
        assertEquals(10f, vm.state.value.headingDeg!!.toFloat(), 1e-4f)
    }

    @Test fun missingCompassKeepsBearingButNoHeading() = runTest {
        val vm = QiblaViewModel(FakePlaces(tashkent), FakeCompass(available = false))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceUntilIdle()
        assertFalse(vm.state.value.hasCompass)
        assertNull(vm.state.value.headingDeg)
        assertEquals(240.3, vm.state.value.target!!.bearing, 1.0)
    }

    @Test fun changingPlaceRecomputesTarget() = runTest {
        val places = FakePlaces(tashkent)
        val vm = QiblaViewModel(places, FakeCompass())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceUntilIdle()
        places.flow.value = jakarta
        advanceUntilIdle()
        assertEquals("Jakarta", vm.state.value.target!!.city)
        assertEquals(295.2, vm.state.value.target!!.bearing, 1.0)
    }
}
