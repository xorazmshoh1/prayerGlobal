package uz.xumoyun.feature.hifz.ui.today

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
import uz.xumoyun.core.common.FixedTimeSource
import uz.xumoyun.feature.hifz.plan.domain.AyahProgress
import uz.xumoyun.feature.hifz.plan.domain.HifzPlan
import uz.xumoyun.feature.hifz.plan.domain.MemoState
import uz.xumoyun.feature.hifz.plan.domain.PlanScope

import uz.xumoyun.feature.hifz.plan.domain.ReviewType
import uz.xumoyun.feature.hifz.ui.FakeHifzRepository
import uz.xumoyun.feature.hifz.ui.reviewSession
import uz.xumoyun.feature.hifz.ui.SyntheticQuranSource

@OptIn(ExperimentalCoroutinesApi::class)
class HifzTodayViewModelTest {

    private val today = LocalDate.of(2027, 2, 8)
    private val time = FixedTimeSource(Instant.parse("2027-02-08T06:00:00Z"), ZoneId.of("UTC"))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun plan(from: Int, to: Int, daily: Int) = HifzPlan(
        scope = PlanScope.ALL, fromAyah = from, toAyah = to, dailyNewAyahs = daily,
        reminderAfterPrayer = null, reminderOffsetMin = 0, startDate = today.minusDays(10), isActive = true,
    )

    @Test fun `without a plan the tab shows no cards and quran is available`() {
        val vm = HifzTodayViewModel(FakeHifzRepository(), SyntheticQuranSource(), time)
        val s = vm.state.value
        assertFalse(s.loading)
        assertTrue(s.quranAvailable)
        assertEquals(false, s.summary?.hasPlan)
        assertTrue(s.layout != null)
    }

    @Test fun `plan shows the new ayahs of today as the sabaq card`() {
        val repo = FakeHifzRepository(plan(1, 6236, 4))
        val vm = HifzTodayViewModel(repo, SyntheticQuranSource(), time)
        val sabaq = vm.state.value.summary!!.cards.single { it.type == ReviewType.SABAQ }
        assertEquals(1, sabaq.first)
        assertEquals(4, sabaq.last)
        assertEquals(12, sabaq.minutes)
        assertEquals(4, vm.state.value.summary!!.items.count { it.type == ReviewType.SABAQ })
    }

    @Test fun `new progress updates the summary live`() {
        val repo = FakeHifzRepository(plan(1, 100, 2))
        val vm = HifzTodayViewModel(repo, SyntheticQuranSource(), time)
        val memorized = (1..50).associateWith {
            AyahProgress(it, MemoState.MEMORIZED, 2.5, 40, 4, today.plusDays(40), 5, null)
        }
        repo.progress.value = memorized
        val s = vm.state.value.summary!!
        assertEquals(50, s.memorized)
        assertEquals(50.0, s.percent, 1e-9)
    }

    @Test fun `streak comes from the review sessions in the repository`() {
        val repo = FakeHifzRepository(plan(1, 100, 2))
        repo.sessions += listOf(
            reviewSession(today.minusDays(1), ReviewType.SABAQ, 1, 2, 60, 4.0),
            reviewSession(today.minusDays(2), ReviewType.MANZIL, 9, 9, 10, 5.0),
        )
        val vm = HifzTodayViewModel(repo, SyntheticQuranSource(), time)
        assertEquals(2, vm.state.value.summary!!.streakDays)
    }

    @Test fun `missing quran database is reported without crashing`() {
        val vm = HifzTodayViewModel(FakeHifzRepository(plan(1, 100, 2)), SyntheticQuranSource(available = false), time)
        assertFalse(vm.state.value.quranAvailable)
        assertFalse(vm.state.value.loading)
        assertEquals(null, vm.state.value.summary)
    }

    @Test fun `empty quran database is treated as unavailable`() {
        val vm = HifzTodayViewModel(FakeHifzRepository(plan(1, 100, 2)), SyntheticQuranSource(empty = true), time)
        assertFalse(vm.state.value.quranAvailable)
    }
}
