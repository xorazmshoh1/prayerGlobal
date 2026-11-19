package uz.xumoyun.feature.hifz.ui.progress

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
import org.junit.Before
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import uz.xumoyun.feature.hifz.plan.domain.AyahProgress
import uz.xumoyun.feature.hifz.plan.domain.JuzStatus
import uz.xumoyun.feature.hifz.plan.domain.MemoState

import uz.xumoyun.feature.hifz.plan.domain.ReviewType
import uz.xumoyun.feature.hifz.plan.domain.SurahStatus
import uz.xumoyun.feature.hifz.ui.FakeHifzRepository
import uz.xumoyun.feature.hifz.ui.reviewSession
import uz.xumoyun.feature.hifz.ui.SyntheticQuranSource

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    private val today = LocalDate.of(2027, 2, 8)
    private val time = FixedTimeSource(Instant.parse("2027-02-08T06:00:00Z"), ZoneId.of("UTC"))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun memo(id: Int) = AyahProgress(id, MemoState.MEMORIZED, 2.5, 40, 4, today.plusDays(40), 5, null)

    @Test fun `empty repository shows zero progress and a 30 cell juz map`() {
        val s = ProgressViewModel(FakeHifzRepository(), SyntheticQuranSource(), time).state.value
        assertFalse(s.loading)
        assertEquals(0, s.memorized)
        assertEquals(6236, s.total)
        assertEquals(30, s.juz.size)
        assertEquals(114, s.surahs.size)
        assertEquals(7, s.weekly.size)
        assertEquals(0.0, s.percent, 1e-9)
    }

    @Test fun `memorized ayahs drive juz status, surah status and percent`() {
        val repo = FakeHifzRepository()
        repo.progress.value = (1..207).associateWith(::memo) + (208..210).associateWith { memo(it).copy(state = MemoState.LEARNING) }
        val s = ProgressViewModel(repo, SyntheticQuranSource(), time).state.value
        assertEquals(207, s.memorized)
        assertEquals(JuzStatus.DONE, s.juz[0].status)
        assertEquals(JuzStatus.NONE, s.juz[1].status)
        assertEquals(SurahStatus.MEMORIZED, s.surahs[0].status)
        assertEquals(SurahStatus.MEMORIZED, s.surahs[2].status) // 3-sura: 55? → 56 oyat, 1..207 ichida
        assertEquals(207 * 100.0 / 6236, s.percent, 1e-9)
    }

    @Test fun `weekly chart counts reviewed ayahs of the last seven days`() {
        val repo = FakeHifzRepository()
        repo.sessions += listOf(
            reviewSession(today, ReviewType.SABAQ, 1, 3, 0, 4.0),
            reviewSession(today.minusDays(6), ReviewType.SABQI, 1, 10, 0, 4.0),
            reviewSession(today.minusDays(8), ReviewType.SABQI, 1, 100, 0, 4.0),
        )
        val weekly = ProgressViewModel(repo, SyntheticQuranSource(), time).state.value.weekly
        assertEquals(3, weekly.last().ayahs)
        assertEquals(10, weekly.first().ayahs)
        assertEquals(0, weekly[1].ayahs)
    }

    @Test fun `missing quran database is reported`() {
        val s = ProgressViewModel(FakeHifzRepository(), SyntheticQuranSource(available = false), time).state.value
        assertFalse(s.quranAvailable)
        assertFalse(s.loading)
    }
}
