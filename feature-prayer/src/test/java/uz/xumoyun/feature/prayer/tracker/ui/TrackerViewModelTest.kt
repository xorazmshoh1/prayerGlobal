package uz.xumoyun.feature.prayer.tracker.ui

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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.tracker.DirectTransactions
import uz.xumoyun.feature.prayer.tracker.FakeTrackerPrefs
import uz.xumoyun.feature.prayer.tracker.FakeTrackerStore
import uz.xumoyun.feature.prayer.tracker.data.TrackerRepository
import uz.xumoyun.feature.prayer.tracker.domain.MadhabSource
import uz.xumoyun.feature.prayer.tracker.domain.PrayerStatus
import uz.xumoyun.feature.prayer.tracker.domain.PrayerLog
import uz.xumoyun.feature.prayer.tracker.domain.QadaPrayer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModelTest {
    private val today = LocalDate.of(2026, 11, 19)
    private val time = FixedTimeSource(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC)

    private class Madhab(var value: AsrMadhab) : MadhabSource {
        override fun madhab(): Flow<AsrMadhab> = flowOf(value)
    }

    private val store = FakeTrackerStore()
    private val prefs = FakeTrackerPrefs(autoQada = true)
    private val madhab = Madhab(AsrMadhab.SHAFI)
    private val repo = TrackerRepository(store, DirectTransactions, prefs)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm() = TrackerViewModel(repo, madhab, time)

    @Test fun cyclingStatusMarksMissedAndAddsOneQada() = runTest {
        val vm = vm()
        vm.cycleStatus(today, Prayer.ASR).join()
        assertEquals(PrayerStatus.PRAYED, vm.state.value.logs[today]?.get(Prayer.ASR))
        vm.cycleStatus(today, Prayer.ASR).join()
        assertEquals(PrayerStatus.MISSED, vm.state.value.logs[today]?.get(Prayer.ASR))
        assertEquals(1, vm.state.value.counters.first { it.prayer == QadaPrayer.ASR }.remaining)
        vm.cycleStatus(today, Prayer.ASR).join()
        assertNull(vm.state.value.logs[today]?.get(Prayer.ASR))
    }

    @Test fun estimateWizardGives366PerPrayerInShafi() = runTest {
        val vm = vm()
        vm.applyEstimate(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))!!.join()
        val s = vm.state.value
        assertEquals(366, s.visibleCounters.first { it.prayer == QadaPrayer.FAJR }.remaining)
        assertEquals(366 * 5, s.totalRemaining)
        assertEquals(5, s.visibleCounters.size)
    }

    @Test fun estimateWizardIncludesWitrInHanafi() = runTest {
        madhab.value = AsrMadhab.HANAFI
        val vm = vm()
        vm.applyEstimate(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))!!.join()
        assertEquals(366 * 6, vm.state.value.totalRemaining)
        assertEquals(6, vm.state.value.visibleCounters.size)
    }

    @Test fun estimateWithReversedRangeDoesNothing() = runTest {
        val vm = vm()
        assertNull(vm.applyEstimate(LocalDate.of(2020, 12, 31), LocalDate.of(2020, 1, 1)))
        assertEquals(0, vm.state.value.totalRemaining)
    }

    @Test fun thousandEightHundredThirtyWithTwoPerDayGivesOneEightyThreeDays() = runTest {
        val vm = vm()
        vm.applyEstimate(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))!!.join()
        vm.setDailyGoal(2).join()
        val s = vm.state.value
        assertEquals(1830, s.totalRemaining)
        assertEquals(2, s.dailyGoal)
        assertEquals(today.plusDays(183), s.finishDate)
    }

    @Test fun minusAtZeroKeepsZero() = runTest {
        val vm = vm()
        vm.adjust(QadaPrayer.FAJR, -1).join()
        assertEquals(0, vm.state.value.counters.first { it.prayer == QadaPrayer.FAJR }.remaining)
        assertEquals(false, vm.state.value.canUndo)
    }

    @Test fun undoEnablesAndRevertsManualChange() = runTest {
        val vm = vm()
        vm.adjust(QadaPrayer.ISHA, 2).join()
        assertEquals(true, vm.state.value.canUndo)
        vm.undo().join()
        assertEquals(0, vm.state.value.counters.first { it.prayer == QadaPrayer.ISHA }.remaining)
    }

    @Test fun autoQadaToggleIsReflectedAndStopsIncrement() = runTest {
        val vm = vm()
        vm.setAutoQada(false).join()
        assertEquals(false, vm.state.value.autoQada)
        vm.cycleStatus(today, Prayer.FAJR).join()
        vm.cycleStatus(today, Prayer.FAJR).join()
        assertEquals(0, vm.state.value.counters.first { it.prayer == QadaPrayer.FAJR }.remaining)
    }

    @Test fun streakCountsPerfectYesterdayEvenWhenTodayIsOpen() = runTest {
        val vm = vm()
        for (p in PrayerLog.TRACKED) {
            vm.cycleStatus(today.minusDays(1), p).join() // kecha: 5 ta O'QILDI
        }
        assertEquals(1, vm.state.value.streak)
    }

    @Test fun weekShiftMovesWholeWeek() = runTest {
        val vm = vm()
        val before = vm.state.value.weekStart
        vm.shiftWeek(-1)
        assertEquals(before.minusWeeks(1), vm.state.value.weekStart)
        assertEquals(7, vm.state.value.week.size)
    }

    @Test fun finishDateNullWithoutGoalAndTodayWhenEmpty() = runTest {
        val vm = vm()
        assertEquals(today, vm.state.value.finishDate)
        vm.adjust(QadaPrayer.FAJR, 4).join()
        assertNull(vm.state.value.finishDate)
    }

    @Test fun countersCoverAllKeysEvenWhenEmpty() = runTest {
        assertEquals(QadaPrayer.entries.size, vm().state.value.counters.size)
    }
}
