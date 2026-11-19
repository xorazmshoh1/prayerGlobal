package uz.xumoyun.feature.hifz.ui.plan

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
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import uz.xumoyun.feature.hifz.plan.domain.AyahProgress
import uz.xumoyun.feature.hifz.plan.domain.HifzPlan
import uz.xumoyun.feature.hifz.plan.domain.MemoState
import uz.xumoyun.feature.hifz.plan.domain.PlanScope
import uz.xumoyun.feature.hifz.plan.domain.PlanTarget
import uz.xumoyun.feature.hifz.ui.FakeHifzRepository
import uz.xumoyun.feature.hifz.ui.SyntheticQuranSource

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    private val today = LocalDate.of(2027, 2, 8)
    private val time = FixedTimeSource(Instant.parse("2027-02-08T06:00:00Z"), ZoneId.of("UTC"))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(repo: FakeHifzRepository = FakeHifzRepository(), quranAvailable: Boolean = true) =
        PlanViewModel(repo, SyntheticQuranSource(available = quranAvailable), time)

    @Test fun `defaults are whole Quran, five ayahs a day and no reminder`() {
        val s = vm().state.value
        assertFalse(s.hasActivePlan)
        assertEquals(PlanTarget.All, s.target)
        assertEquals(5, s.dailyNewAyahs)
        assertEquals(null, s.reminderPrayer)
        assertEquals(6236, s.preview!!.total)
        assertEquals(today.plusDays(1248), s.preview!!.finishDate) // ceil(6236 / 5) = 1248
    }

    @Test fun `choosing a juz updates the preview to that juz`() {
        val v = vm()
        v.selectTarget(PlanTarget.Juz(1))
        assertEquals(207, v.state.value.preview!!.total)
        assertEquals(today.plusDays(42), v.state.value.preview!!.finishDate) // ceil(207 / 5) = 42
    }

    @Test fun `daily goal is clamped between 1 and 50`() {
        val v = vm()
        v.setDaily(0)
        assertEquals(1, v.state.value.dailyNewAyahs)
        v.setDaily(99)
        assertEquals(50, v.state.value.dailyNewAyahs)
        v.changeDaily(-10)
        assertEquals(40, v.state.value.dailyNewAyahs)
        v.changeDaily(-100)
        assertEquals(1, v.state.value.dailyNewAyahs)
    }

    @Test fun `reminder accepts only known prayers and offsets`() {
        val v = vm()
        v.setReminderPrayer("FAJR")
        assertEquals("FAJR", v.state.value.reminderPrayer)
        v.setReminderPrayer(null)
        assertEquals(null, v.state.value.reminderPrayer)
        assertThrows(IllegalArgumentException::class.java) { v.setReminderPrayer("SUNRISE") }
        assertThrows(IllegalArgumentException::class.java) { v.setReminderOffset(7) }
        v.setReminderOffset(45)
        assertEquals(45, v.state.value.reminderOffsetMin)
    }

    @Test fun `saving writes a new active plan with the chosen settings`() {
        val repo = FakeHifzRepository()
        val v = vm(repo)
        v.selectTarget(PlanTarget.Juz(1))
        v.setDaily(4)
        v.setReminderPrayer("ASR")
        v.setReminderOffset(20)
        v.save()
        val plan = repo.activated.single()
        assertEquals(PlanScope.JUZ, plan.scope)
        assertEquals(1, plan.fromAyah)
        assertEquals(207, plan.toAyah)
        assertEquals(4, plan.dailyNewAyahs)
        assertEquals("ASR", plan.reminderAfterPrayer)
        assertEquals(20, plan.reminderOffsetMin)
        assertEquals(today, plan.startDate)
        assertTrue(plan.isActive)
        assertTrue(v.state.value.saved)
    }

    @Test fun `an existing plan is loaded into the form`() {
        val existing = HifzPlan(
            scope = PlanScope.SURAH, fromAyah = 56, toAyah = 110, dailyNewAyahs = 7,
            reminderAfterPrayer = "MAGHRIB", reminderOffsetMin = 30, startDate = today.minusDays(9), isActive = true,
        )
        val v = vm(FakeHifzRepository(existing))
        val s = v.state.value
        assertTrue(s.hasActivePlan)
        assertEquals(PlanTarget.Surah(2), s.target)
        assertEquals(7, s.dailyNewAyahs)
        assertEquals("MAGHRIB", s.reminderPrayer)
        assertEquals(30, s.reminderOffsetMin)
        assertEquals(55, s.preview!!.total)
    }

    @Test fun `preview counts memorized ayahs of the chosen goal`() {
        val repo = FakeHifzRepository()
        repo.progress.value = (1..55).associateWith {
            AyahProgress(it, MemoState.MEMORIZED, 2.5, 40, 4, today.plusDays(40), 5, null)
        }
        val v = vm(repo)
        v.selectTarget(PlanTarget.Surah(1))
        assertEquals(55, v.state.value.preview!!.memorized)
        assertEquals(0, v.state.value.preview!!.remaining)
    }

    @Test fun `missing quran database disables the form`() {
        val s = vm(quranAvailable = false).state.value
        assertFalse(s.quranAvailable)
        assertFalse(s.loading)
    }
}
