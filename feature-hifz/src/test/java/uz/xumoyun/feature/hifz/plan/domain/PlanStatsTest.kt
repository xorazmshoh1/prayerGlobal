package uz.xumoyun.feature.hifz.plan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PlanStatsTest {

    private val today = FixedTimeSource(Instant.parse("2027-01-10T06:00:00Z"), ZoneId.of("UTC")).today()

    @Test
    fun finishDateIsCeilingOfRemainingOverDaily() {
        // 25 qolgan, kuniga 10 → ceil(2.5) = 3 kun
        assertEquals(LocalDate.of(2027, 1, 13), finishDate(today, 25, 10))
        // aniq bo'linadi: 20 / 10 = 2
        assertEquals(LocalDate.of(2027, 1, 12), finishDate(today, 20, 10))
        // 1 qolgan, kuniga 5 → 1 kun
        assertEquals(LocalDate.of(2027, 1, 11), finishDate(today, 1, 5))
    }

    @Test
    fun finishDateIsTodayWhenNothingRemains() {
        assertEquals(today, finishDate(today, 0, 5))
        assertEquals(today, finishDate(today, -3, 5))
    }

    @Test
    fun finishDateRejectsNonPositiveDaily() {
        assertThrows(IllegalArgumentException::class.java) { finishDate(today, 10, 0) }
    }

    @Test
    fun planStatsCountsMemorizedAndPercent() {
        val plan = (1..200).toList()
        val now = Instant.parse("2027-01-10T06:00:00Z")
        val progress = (1..50).associateWith {
            AyahProgress(it, MemoState.MEMORIZED, 2.5, 40, 6, today.plusDays(40), 5, now)
        } + (51..80).associateWith {
            AyahProgress(it, MemoState.LEARNING, 2.5, 1, 1, today.plusDays(1), 5, now)
        }
        val stats = planStats(today, plan, progress, dailyNewAyahs = 10)
        assertEquals(200, stats.total)
        assertEquals(50, stats.memorized)
        assertEquals(25.0, stats.progressPercent, 1e-9)
        assertEquals(150, stats.remaining)
        assertEquals(today.plusDays(15), stats.finishDate)
    }

    @Test
    fun planStatsIgnoresProgressOutsidePlanAndHandlesEmptyPlan() {
        val now = Instant.parse("2027-01-10T06:00:00Z")
        val progress = mapOf(900 to AyahProgress(900, MemoState.MEMORIZED, 2.5, 40, 6, today, 5, now))
        val stats = planStats(today, (1..10).toList(), progress, 5)
        assertEquals(0, stats.memorized)
        assertEquals(0.0, stats.progressPercent, 1e-9)

        val empty = planStats(today, emptyList(), emptyMap(), 5)
        assertEquals(0, empty.total)
        assertEquals(0.0, empty.progressPercent, 1e-9)
        assertEquals(today, empty.finishDate)
    }
}
