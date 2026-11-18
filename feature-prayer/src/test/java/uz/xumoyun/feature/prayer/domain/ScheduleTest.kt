package uz.xumoyun.feature.prayer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

class ScheduleTest {
    private val input = TestData.input(TestData.Tashkent)
    private val date = LocalDate.of(2026, 11, 17)
    private val today = PrayerCalculator.day(input, date)
    private val tomorrow = PrayerCalculator.day(input, date.plusDays(1))

    @Test fun nextPrayerDuringDay() {
        val now = today[Prayer.DHUHR].plusMinutes(1)
        val next = Schedule.next(now, today, tomorrow)
        assertEquals(Prayer.ASR, next.prayer)
        assertEquals(Prayer.DHUHR, Schedule.current(now, today))
        assertEquals(Duration.between(now, today[Prayer.ASR]), next.remaining)
    }

    @Test fun exactlyAtPrayerTimeItIsCurrent() {
        val now = today[Prayer.MAGHRIB]
        assertEquals(Prayer.MAGHRIB, Schedule.current(now, today))
        assertEquals(Prayer.ISHA, Schedule.next(now, today, tomorrow).prayer)
    }

    @Test fun afterIshaNextIsTomorrowFajr() {
        val now = today[Prayer.ISHA].plusMinutes(30)
        val next = Schedule.next(now, today, tomorrow)
        assertEquals(Prayer.FAJR, next.prayer)
        assertEquals(tomorrow.date, next.time.toLocalDate())
    }

    @Test fun beforeFajrCurrentIsIsha() {
        val now = today[Prayer.FAJR].minusMinutes(10)
        assertEquals(Prayer.ISHA, Schedule.current(now, today))
        assertEquals(DayPhase.NIGHT, Schedule.phase(now, today))
    }

    @Test fun phases() {
        assertEquals(DayPhase.DAWN, Schedule.phase(today[Prayer.SUNRISE], today))
        assertEquals(DayPhase.DAY, Schedule.phase(today[Prayer.DHUHR], today))
        assertEquals(DayPhase.DUSK, Schedule.phase(today[Prayer.MAGHRIB], today))
        assertEquals(DayPhase.NIGHT, Schedule.phase(today[Prayer.ISHA], today))
    }

    @Test fun boardHighlightsCurrentAndKeepsSixRows() {
        val now = today[Prayer.ASR].plusMinutes(5)
        val board = PrayerBoard.build(now, today, tomorrow) { PrayerAlert.default(it).mode }
        assertEquals(6, board.rows.size)
        assertEquals(Prayer.entries, board.rows.map { it.prayer })
        assertEquals(Prayer.ASR, board.rows.single { it.isCurrent }.prayer)
        assertEquals(Prayer.ASR, board.current)
        assertEquals(Prayer.MAGHRIB, board.next.prayer)
        assertFalse(board.rows.single { it.prayer == Prayer.SUNRISE }.alertOn)
        assertTrue(board.rows.filter { it.prayer.isPrayer }.all { it.alertOn })
    }

    @Test fun boardBeforeFajrHasNoCurrentRow() {
        val now = today[Prayer.FAJR].minusMinutes(1)
        val board = PrayerBoard.build(now, today, tomorrow) { AlertMode.SOUND }
        assertNull(board.current)
        assertTrue(board.rows.none { it.isCurrent })
        assertEquals(Prayer.FAJR, board.next.prayer)
        assertEquals(Duration.ofMinutes(1), board.next.remaining)
    }
}
