package uz.xumoyun.feature.hifz.plan.domain

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayTest {

    private val today = LocalDate.of(2027, 2, 8)
    private val now: Instant = Instant.parse("2027-02-08T06:00:00Z")
    private val juzOf: (Int) -> Int = { id -> (id - 1) / 207 + 1 }

    private fun plan(from: Int, to: Int, daily: Int) = HifzPlan(
        scope = PlanScope.ALL, fromAyah = from, toAyah = to, dailyNewAyahs = daily,
        reminderAfterPrayer = null, reminderOffsetMin = 0, startDate = today.minusDays(30), isActive = true,
    )

    private fun progress(id: Int, state: MemoState, due: LocalDate, interval: Int = 1) = AyahProgress(
        ayahGlobalId = id, state = state, easeFactor = 2.5, intervalDays = interval, repetitions = 1,
        dueDate = due, lastRating = 3, lastReviewedAt = now,
    )

    @Test fun `streak counts back from today when today is done`() {
        val dates = setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(3, streakDays(today, dates))
    }

    @Test fun `streak still counts yesterday when today is not done yet`() {
        val dates = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, streakDays(today, dates))
        assertEquals(0, streakDays(today, setOf(today.minusDays(2))))
    }

    @Test fun `estimated minutes weight new ayahs more than reviews`() {
        assertEquals(0, estimateMinutes(ReviewType.SABAQ, 0))
        assertEquals(9, estimateMinutes(ReviewType.SABAQ, 3))
        assertEquals(4, estimateMinutes(ReviewType.MANZIL, 4))
    }

    @Test fun `no plan gives an empty summary with streak`() {
        val s = buildToday(today, null, emptyMap(), emptyList(), setOf(today), juzOf)
        assertFalse(s.hasPlan)
        assertTrue(s.cards.isEmpty())
        assertEquals(1, s.streakDays)
        assertNull(s.finishDate)
    }

    @Test fun `fresh plan shows today's new ayahs as sabaq card`() {
        val s = buildToday(today, plan(1, 100, 3), emptyMap(), emptyList(), emptySet(), juzOf)
        val sabaq = s.cards.single { it.type == ReviewType.SABAQ }
        assertEquals(1, sabaq.first)
        assertEquals(3, sabaq.last)
        assertEquals(3, sabaq.count)
        assertEquals(9, sabaq.minutes)
        assertEquals(0, s.memorized)
        assertEquals(100, s.total)
        assertEquals(today.plusDays(34), s.finishDate) // ceil(100 / 3) = 34
        assertFalse(s.completedToday)
    }

    @Test fun `after today's new ayahs are done the lesson is complete for today`() {
        val prog = (1..3).associateWith { progress(it, MemoState.LEARNING, today.plusDays(1)) }
        val todaySession = ReviewSession(date = today, type = ReviewType.SABAQ, ayahFrom = 1, ayahTo = 3, durationSec = 60, avgRating = 4.0)
        val s = buildToday(today, plan(1, 100, 3), prog, listOf(todaySession), setOf(today), juzOf)
        assertEquals(0, s.cards.single { it.type == ReviewType.SABAQ }.count)
        assertTrue(s.items.isEmpty())
        assertTrue(s.completedToday)
    }

    @Test fun `sabqi takes recent sabaq sessions and manzil takes due ayahs in one juz`() {
        val prog = mapOf(
            10 to progress(10, MemoState.LEARNING, today.minusDays(3)),
            11 to progress(11, MemoState.LEARNING, today.minusDays(1)),
            300 to progress(300, MemoState.MEMORIZED, today.minusDays(2), interval = 40),
        )
        val recent = ReviewSession(date = today.minusDays(2), type = ReviewType.SABAQ, ayahFrom = 10, ayahTo = 11, durationSec = 0, avgRating = 3.0)
        val s = buildToday(today, plan(1, 500, 2), prog, listOf(recent), setOf(today.minusDays(2)), juzOf)
        assertEquals(listOf(10, 11), s.cards.single { it.type == ReviewType.SABQI }.let { listOf(it.first!!, it.last!!) })
        // Manzil: eng eski muddatli oyat (10, juz 1) — shu juzdagi muddati kelganlar; 300 (juz 2) kirmaydi.
        assertEquals(listOf(10, 11), s.cards.single { it.type == ReviewType.MANZIL }.let { listOf(it.first!!, it.last!!) })
    }

    @Test fun `completed memorization percent comes from memorized ayahs in plan`() {
        val prog = (1..50).associateWith { progress(it, MemoState.MEMORIZED, today.plusDays(60), interval = 60) }
        val s = buildToday(today, plan(1, 200, 5), prog, emptyList(), emptySet(), juzOf)
        assertEquals(50, s.memorized)
        assertEquals(25.0, s.percent, 1e-9)
    }
}
