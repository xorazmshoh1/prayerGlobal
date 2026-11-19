package uz.xumoyun.feature.hifz.ui.lesson

import androidx.lifecycle.SavedStateHandle
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
import uz.xumoyun.feature.hifz.ui.FakeLessonAudio
import uz.xumoyun.feature.hifz.ui.SyntheticQuranSource

@OptIn(ExperimentalCoroutinesApi::class)
class LessonViewModelTest {

    private val today = LocalDate.of(2027, 2, 8)
    private val now = Instant.parse("2027-02-08T06:00:00Z")
    private val time = FixedTimeSource(now, ZoneId.of("UTC"))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun plan(daily: Int, from: Int = 1, to: Int = 6236) = HifzPlan(
        scope = PlanScope.ALL, fromAyah = from, toAyah = to, dailyNewAyahs = daily,
        reminderAfterPrayer = null, reminderOffsetMin = 0, startDate = today.minusDays(3), isActive = true,
    )

    private fun vm(
        repo: FakeHifzRepository,
        audio: FakeLessonAudio = FakeLessonAudio(),
        type: String? = null,
        quranAvailable: Boolean = true,
    ): LessonViewModel {
        val saved = SavedStateHandle(if (type == null) emptyMap() else mapOf(LessonViewModel.ARG_TYPE to type))
        return LessonViewModel(repo, SyntheticQuranSource(available = quranAvailable), audio, time, saved)
    }

    @Test fun `new ayahs of the day form the lesson`() {
        val v = vm(FakeHifzRepository(plan(2)))
        val s = v.state.value
        assertFalse(s.loading)
        assertEquals(listOf(1, 2), s.session.items.map { it.ayahId })
        assertTrue(s.session.items.all { it.type == ReviewType.SABAQ })
        assertEquals("s1-1", s.texts[1]?.text)
    }

    @Test fun `no plan and missing database are reported`() {
        assertTrue(vm(FakeHifzRepository()).state.value.noPlan)
        val unavailable = vm(FakeHifzRepository(plan(2)), quranAvailable = false).state.value
        assertFalse(unavailable.quranAvailable)
    }

    @Test fun `rating saves progress immediately and finishing saves sessions once`() {
        val repo = FakeHifzRepository(plan(2))
        val v = vm(repo)
        v.rate(5)
        assertEquals(1, repo.savedProgress.size)
        val p = repo.savedProgress.single()
        assertEquals(1, p.ayahGlobalId)
        assertEquals(MemoState.LEARNING, p.state)
        assertEquals(1, p.intervalDays)
        assertTrue(repo.savedSessions.isEmpty())

        v.rate(3)
        assertTrue(v.state.value.session.finished)
        assertEquals(2, repo.savedProgress.size)
        assertEquals(1, repo.savedSessions.size)
        val session = repo.savedSessions.single()
        assertEquals(1, session.ayahFrom)
        assertEquals(2, session.ayahTo)
        assertEquals(4.0, session.avgRating!!, 1e-9)
        assertEquals(ReviewType.SABAQ, session.type)

        var closed = false
        v.close { closed = true }
        assertTrue(closed)
        assertEquals(1, repo.savedSessions.size) // qayta yozilmadi
    }

    @Test fun `leaving a half-done lesson saves the partial session`() {
        val repo = FakeHifzRepository(plan(3))
        val v = vm(repo)
        v.rate(1)
        v.close {}
        assertEquals(1, repo.savedSessions.size)
        assertEquals(1, repo.savedSessions.single().ayahTo)
        assertEquals(1.0, repo.savedSessions.single().avgRating!!, 1e-9)
    }

    @Test fun `leaving without ratings saves nothing`() {
        val repo = FakeHifzRepository(plan(3))
        vm(repo).close {}
        assertTrue(repo.savedSessions.isEmpty())
    }

    @Test fun `already rated progress feeds the next interval`() {
        val repo = FakeHifzRepository(plan(1, from = 1, to = 10))
        repo.progress.value = mapOf(
            1 to AyahProgress(1, MemoState.LEARNING, 2.5, 6, 2, today.minusDays(1), 3, null),
        )
        val v = vm(repo)
        // Sabaq: 2-oyat (yangi); Manzil: muddati kelgan 1-oyat. Navbat: [2 SABAQ, 1 MANZIL].
        assertEquals(listOf(2 to ReviewType.SABAQ, 1 to ReviewType.MANZIL), v.state.value.session.items.map { it.ayahId to it.type })
        v.move(forward = true)
        v.rate(5)
        val updated = repo.savedProgress.single()
        assertEquals(1, updated.ayahGlobalId)
        assertEquals(3, updated.repetitions)
        assertEquals(16, updated.intervalDays) // EF 2.5 → 2.6; round(6 × 2.6) = 16
        assertEquals(today.plusDays(16), updated.dueDate)
    }

    @Test fun `filter by type keeps only that step`() {
        val repo = FakeHifzRepository(plan(2))
        repo.sessions += reviewSession(today.minusDays(2), ReviewType.SABAQ, 50, 51, 0, 5.0)
        repo.progress.value = (50..51).associateWith { AyahProgress(it, MemoState.LEARNING, 2.5, 1, 1, today.plusDays(1), 5, null) }
        val v = vm(repo, type = "SABQI")
        assertEquals(listOf(50, 51), v.state.value.session.items.map { it.ayahId })
        assertTrue(v.state.value.session.items.all { it.type == ReviewType.SABQI })
        assertEquals(emptyList<Int>(), vm(repo, type = "MANZIL").state.value.session.items.map { it.ayahId })
    }

    @Test fun `hidden mode reveals words and never past the last word`() {
        val v = vm(FakeHifzRepository(plan(1)))
        v.toggleHidden()
        assertTrue(v.state.value.session.hidden)
        v.revealWord()
        v.revealWord()
        assertEquals(1, v.state.value.session.revealedWords) // matnda 1 so'z
        v.toggleHidden()
        assertFalse(v.state.value.session.hidden)
        assertEquals(0, v.state.value.session.revealedWords)
    }

    @Test fun `repeat stays within 1 to 10 and speed follows the 0_05 steps`() {
        val audio = FakeLessonAudio()
        val v = vm(FakeHifzRepository(plan(1)), audio)
        repeat(20) { v.changeRepeat(1) }
        assertEquals(LessonViewModel.REPEAT_MAX, v.state.value.repeatTimes)
        repeat(20) { v.changeRepeat(-1) }
        assertEquals(LessonViewModel.REPEAT_MIN, v.state.value.repeatTimes)

        v.changeSpeed(up = true)
        assertEquals(1.05f, v.state.value.speed, 1e-4f)
        assertEquals(1.05f, audio.speed!!, 1e-4f)
        repeat(30) { v.changeSpeed(up = true) }
        assertEquals(1.25f, v.state.value.speed, 1e-4f)
    }

    @Test fun `play passes surah ayah repeat and speed to the player`() {
        val audio = FakeLessonAudio()
        val v = vm(FakeHifzRepository(plan(1)), audio)
        v.changeRepeat(1)
        v.togglePlay()
        assertEquals(listOf(listOf<Any>(1, 1, 2, 1f)), audio.plays)
        assertTrue(v.state.value.playing)
        v.togglePlay()
        assertFalse(v.state.value.playing)
        assertEquals(1, audio.paused)
    }

    @Test fun `missing audio file is reported and playback does not start`() {
        val audio = FakeLessonAudio(playResult = false)
        val v = vm(FakeHifzRepository(plan(1)), audio)
        v.togglePlay()
        assertFalse(v.state.value.playing)
        assertTrue(v.state.value.audioMissing)
    }

    @Test fun `navigating pauses audio and clamps the position`() {
        val audio = FakeLessonAudio()
        val v = vm(FakeHifzRepository(plan(2)), audio)
        v.move(forward = false)
        assertEquals(0, v.state.value.session.index)
        v.move(forward = true)
        assertEquals(1, v.state.value.session.index)
        v.move(forward = true)
        assertEquals(1, v.state.value.session.index)
        assertEquals(3, audio.paused)
    }

    @Test fun `clearing releases the audio player`() {
        val audio = FakeLessonAudio()
        val v = vm(FakeHifzRepository(plan(1)), audio)
        v.onCleared()
        assertTrue(audio.released)
    }
}
