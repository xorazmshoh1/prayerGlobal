package uz.xumoyun.feature.hifz.plan.domain

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import uz.xumoyun.feature.hifz.ui.SyntheticQuran

class ProgressStatsTest {

    private val layout = SyntheticQuran.layout
    private val today = LocalDate.of(2027, 2, 8)

    private fun memo(id: Int) = AyahProgress(
        ayahGlobalId = id, state = MemoState.MEMORIZED, easeFactor = 2.5, intervalDays = 40, repetitions = 4,
        dueDate = today, lastRating = 5, lastReviewedAt = Instant.EPOCH,
    )

    private fun learning(id: Int) = memo(id).copy(state = MemoState.LEARNING, intervalDays = 1)

    @Test fun `juz cells count only memorized ayahs and classify status`() {
        val prog = (1..207).associate { it to memo(it) } + (208..210).associate { it to learning(it) }
        val cells = juzCells(layout, prog)
        assertEquals(30, cells.size)
        assertEquals(JuzStatus.DONE, cells[0].status)
        assertEquals(207, cells[0].memorized)
        assertEquals(JuzStatus.NONE, cells[1].status) // 208..210 learning, not memorized
        assertEquals(0, cells[1].memorized)
        assertEquals(208, cells[1].total)
    }

    @Test fun `juz becomes partial when some but not all ayahs are memorized`() {
        val cells = juzCells(layout, (1..10).associate { it to memo(it) })
        assertEquals(JuzStatus.PARTIAL, cells[0].status)
        assertEquals(10, cells[0].memorized)
    }

    @Test fun `surah status is memorized, in progress, or not started`() {
        val prog = (1..55).associate { it to memo(it) } + (56..57).associate { it to learning(it) }
        val rows = surahStatuses(layout, prog)
        assertEquals(SurahStatus.MEMORIZED, rows[0].status)
        assertEquals(55, rows[0].memorized)
        assertEquals(SurahStatus.IN_PROGRESS, rows[1].status)
        assertEquals(2, rows[1].started)
        assertEquals(0, rows[1].memorized)
        assertEquals(SurahStatus.NOT_STARTED, rows[2].status)
        assertEquals(114, rows.size)
    }

    @Test fun `weekly reviews are seven days oldest first and sum ayah ranges`() {
        val sessions = listOf(
            ReviewSession(date = today, type = ReviewType.SABAQ, ayahFrom = 1, ayahTo = 3, durationSec = 0, avgRating = null),
            ReviewSession(date = today, type = ReviewType.MANZIL, ayahFrom = 10, ayahTo = 10, durationSec = 0, avgRating = null),
            ReviewSession(date = today.minusDays(6), type = ReviewType.SABQI, ayahFrom = 1, ayahTo = 5, durationSec = 0, avgRating = null),
            ReviewSession(date = today.minusDays(7), type = ReviewType.SABQI, ayahFrom = 1, ayahTo = 50, durationSec = 0, avgRating = null),
        )
        val week = weeklyReviews(today, sessions)
        assertEquals(7, week.size)
        assertEquals(today.minusDays(6), week.first().date)
        assertEquals(today, week.last().date)
        assertEquals(5, week.first().ayahs)
        assertEquals(4, week.last().ayahs) // 3 + 1
        assertEquals(0, week[3].ayahs)
    }

    @Test fun `weekly reviews honour a custom window`() {
        assertEquals(1, weeklyReviews(today, emptyList(), days = 1).size)
    }
}
