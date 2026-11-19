package uz.xumoyun.feature.hifz.plan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DailyLessonTest {

    // Sintetik juz xaritasi: 1..10 → juz 1, 11..20 → juz 2, 21..30 → juz 3 (haqiqiy Qur'on emas, test uchun)
    private val juzOf: (Int) -> Int = { (it - 1) / 10 + 1 }
    private val clock = FixedTimeSource(Instant.parse("2027-01-10T06:00:00Z"), ZoneId.of("UTC"))
    private val today: LocalDate = clock.today()
    private val plan = (1..30).toList()

    private fun row(id: Int, state: MemoState, due: LocalDate) =
        AyahProgress(id, state, 2.5, 1, 1, due, Quality.EASY, clock.now())

    private fun session(date: LocalDate, from: Int, to: Int, type: ReviewType = ReviewType.SABAQ) =
        ReviewSession(0, date, type, from, to, 600, 5.0)

    @Test
    fun sabaqTakesNextNewAyahsInPlanOrder() {
        val progress = mapOf(1 to row(1, MemoState.LEARNING, today.plusDays(1)), 2 to row(2, MemoState.LEARNING, today.plusDays(1)))
        val lesson = buildDailyLesson(today, plan, dailyNewAyahs = 3, newAyahsToday = 0, progress = progress, sessions = emptyList(), juzOf = juzOf)
        assertEquals(listOf(3, 4, 5), lesson.sabaq)
    }

    @Test
    fun sabaqSubtractsNewAyahsAlreadyDoneToday() {
        val lesson = buildDailyLesson(today, plan, dailyNewAyahs = 5, newAyahsToday = 2, progress = emptyMap(), sessions = emptyList(), juzOf = juzOf)
        assertEquals(listOf(1, 2, 3), lesson.sabaq)
        val done = buildDailyLesson(today, plan, dailyNewAyahs = 5, newAyahsToday = 5, progress = emptyMap(), sessions = emptyList(), juzOf = juzOf)
        assertEquals(emptyList<Int>(), done.sabaq)
    }

    @Test
    fun sabaqEmptyWhenPlanFullyStarted() {
        val progress = plan.associateWith { row(it, MemoState.LEARNING, today.plusDays(3)) }
        val lesson = buildDailyLesson(today, plan, 5, 0, progress, emptyList(), juzOf)
        assertEquals(emptyList<Int>(), lesson.sabaq)
    }

    @Test
    fun sabqiCoversSabaqSessionsOfLastSevenDaysOnly() {
        val progress = (1..12).associateWith { row(it, MemoState.LEARNING, today.plusDays(5)) }
        val sessions = listOf(
            session(today.minusDays(7), 1, 2),      // oynaning boshi — kiradi
            session(today.minusDays(1), 3, 4),      // kecha — kiradi
            session(today, 5, 6),                   // bugun — Sabqi'ga kirmaydi
            session(today.minusDays(8), 7, 8),      // 8 kun oldin — kirmaydi
            session(today.minusDays(2), 9, 9, ReviewType.SABQI), // faqat SABAQ turi
        )
        val lesson = buildDailyLesson(today, plan, 5, 0, progress, sessions, juzOf)
        assertEquals(listOf(1, 2, 3, 4), lesson.sabqi)
    }

    @Test
    fun sabqiIgnoresAyahsOutsidePlanOrWithoutProgress() {
        val progress = mapOf(2 to row(2, MemoState.LEARNING, today.plusDays(5)))
        val sessions = listOf(session(today.minusDays(1), 1, 3), session(today.minusDays(1), 40, 41))
        val lesson = buildDailyLesson(today, (1..10).toList(), 5, 0, progress, sessions, juzOf)
        assertEquals(listOf(2), lesson.sabqi)
    }

    @Test
    fun manzilPicksDueLearningAndMemorizedOnly() {
        val progress = mapOf(
            1 to row(1, MemoState.LEARNING, today),                // bugun due — kiradi
            2 to row(2, MemoState.MEMORIZED, today.minusDays(3)),  // o'tgan due — kiradi
            3 to row(3, MemoState.LEARNING, today.plusDays(1)),    // ertaga — yo'q
        )
        val lesson = buildDailyLesson(today, plan, 5, 0, progress, emptyList(), juzOf)
        assertEquals(listOf(1, 2), lesson.manzil)
    }

    @Test
    fun manzilIsCappedToOneJuzOfOldestDue() {
        // Juz 1, 2 va 3 da due oyatlar bor; eng muddati o'tgani — 25 (9 kun oldin, juz 3) → faqat juz 3
        val progress = mapOf(
            4 to row(4, MemoState.LEARNING, today.minusDays(1)),
            5 to row(5, MemoState.LEARNING, today),
            15 to row(15, MemoState.LEARNING, today.minusDays(2)),
            16 to row(16, MemoState.MEMORIZED, today),
            25 to row(25, MemoState.LEARNING, today.minusDays(9)), // juz 3, eng eski
        )
        val lesson = buildDailyLesson(today, plan, 5, 0, progress, emptyList(), juzOf)
        assertEquals(listOf(25), lesson.manzil)
    }

    @Test
    fun manzilFromSingleJuzKeepsAllDueOfThatJuzSorted() {
        val progress = mapOf(
            12 to row(12, MemoState.LEARNING, today),
            11 to row(11, MemoState.LEARNING, today.minusDays(4)),
            13 to row(13, MemoState.MEMORIZED, today.minusDays(1)),
            2 to row(2, MemoState.LEARNING, today.plusDays(4)),
        )
        val lesson = buildDailyLesson(today, plan, 5, 0, progress, emptyList(), juzOf)
        assertEquals(listOf(11, 12, 13), lesson.manzil)
    }

    @Test
    fun manzilIgnoresNewStateAndOutsidePlan() {
        val progress = mapOf(1 to row(1, MemoState.NEW, today.minusDays(5)), 99 to row(99, MemoState.LEARNING, today.minusDays(5)))
        val lesson = buildDailyLesson(today, (1..10).toList(), 5, 0, progress, emptyList(), juzOf)
        assertEquals(emptyList<Int>(), lesson.manzil)
    }

    @Test
    fun emptyPlanGivesEmptyLesson() {
        val lesson = buildDailyLesson(today, emptyList(), 5, 0, emptyMap(), emptyList(), juzOf)
        assertEquals(DailyLesson(emptyList(), emptyList(), emptyList()), lesson)
    }

    @Test
    fun invalidLimitsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            buildDailyLesson(today, plan, 0, 0, emptyMap(), emptyList(), juzOf)
        }
        assertThrows(IllegalArgumentException::class.java) {
            buildDailyLesson(today, plan, 5, -1, emptyMap(), emptyList(), juzOf)
        }
    }
}
