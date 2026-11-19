package uz.xumoyun.feature.tasbih.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DailyHistoryTest {
    private val today = LocalDate.of(2026, 11, 17)

    @Test fun buildsSevenDaysNewestFirstWithZeros() {
        val days = DailyHistory.build(emptyList(), today)
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 11, 17), days.first().date)
        assertEquals(LocalDate.of(2026, 11, 11), days.last().date)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0), days.map { it.total })
    }

    @Test fun sumsSessionsOfTheSameDay() {
        val rows = listOf(
            DailyCount("2026-11-17", 33),
            DailyCount("2026-11-17", 10),
            DailyCount("2026-11-16", 99),
        )
        val days = DailyHistory.build(rows, today)
        assertEquals(43, days[0].total)
        assertEquals(99, days[1].total)
        assertEquals(0, days[2].total)
    }

    @Test fun ignoresDaysOutsideWindow() {
        val rows = listOf(DailyCount("2026-11-01", 500), DailyCount("2026-11-18", 7))
        val days = DailyHistory.build(rows, today)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0), days.map { it.total })
    }

    @Test fun totalForDay() {
        val rows = listOf(DailyCount("2026-11-17", 5), DailyCount("2026-11-17", 6), DailyCount("2026-11-16", 1))
        assertEquals(11, DailyHistory.totalFor(rows, today))
        assertEquals(0, DailyHistory.totalFor(rows, today.plusDays(1)))
    }
}
