package uz.xumoyun.feature.prayer.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.xumoyun.feature.prayer.domain.Prayer
import java.time.DayOfWeek
import java.time.LocalDate

class PrayerLogTest {
    @Test fun weekStartsOnFirstDayAndHasSevenDays() {
        // 2026-11-19 — payshanba; Dushanbadan boshlanadi: 16-noyabr
        val days = PrayerLog.weekDays(LocalDate.of(2026, 11, 19), DayOfWeek.MONDAY)
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 11, 16), days.first())
        assertEquals(LocalDate.of(2026, 11, 22), days.last())
    }

    @Test fun cycleIsNoneThenPrayedThenMissedThenNone() {
        assertEquals(PrayerStatus.PRAYED, PrayerLog.nextStatus(null))
        assertEquals(PrayerStatus.MISSED, PrayerLog.nextStatus(PrayerStatus.PRAYED))
        assertEquals(null, PrayerLog.nextStatus(PrayerStatus.MISSED))
    }

    @Test fun missedTransitionOnlyOnNewEntry() {
        assertTrue(PrayerLog.isMissedTransition(null, PrayerStatus.MISSED))
        assertTrue(PrayerLog.isMissedTransition(PrayerStatus.PRAYED, PrayerStatus.MISSED))
        assertFalse(PrayerLog.isMissedTransition(PrayerStatus.MISSED, PrayerStatus.MISSED))
        assertFalse(PrayerLog.isMissedTransition(PrayerStatus.MISSED, null))
    }

    @Test fun perfectDayNeedsAllFivePrayed() {
        val full = PrayerLog.TRACKED.associateWith { PrayerStatus.PRAYED }
        assertTrue(PrayerLog.isPerfectDay(full))
        assertFalse(PrayerLog.isPerfectDay(full + (Prayer.ASR to PrayerStatus.MISSED)))
        assertFalse(PrayerLog.isPerfectDay(full - Prayer.ISHA))
    }

    @Test fun streakCountsConsecutiveDays() {
        val today = LocalDate.of(2026, 11, 19)
        val perfect = setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(3, PrayerLog.streak(today, perfect))
    }

    @Test fun unfinishedTodayDoesNotBreakStreak() {
        val today = LocalDate.of(2026, 11, 19)
        val perfect = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, PrayerLog.streak(today, perfect))
        assertEquals(0, PrayerLog.streak(today, emptySet()))
    }
}
