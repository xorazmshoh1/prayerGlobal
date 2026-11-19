package uz.xumoyun.feature.prayer.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.Prayer
import java.time.LocalDate

/** TZ 7-bo'lim qazo testlari — qiymatlar aynan shu. */
class QadaTest {
    private val y2020Start = LocalDate.of(2020, 1, 1)
    private val y2020End = LocalDate.of(2020, 12, 31)

    @Test fun year2020IsThreeHundredSixtySixDays() {
        assertEquals(366, Qada.inclusiveDays(y2020Start, y2020End))
    }

    @Test fun shafiiGivesEveryPrayer366AndNoWitr() {
        val perPrayer = Qada.inclusiveDays(y2020Start, y2020End)
        val prayers = Qada.prayersFor(AsrMadhab.SHAFI)
        assertEquals(listOf(QadaPrayer.FAJR, QadaPrayer.DHUHR, QadaPrayer.ASR, QadaPrayer.MAGHRIB, QadaPrayer.ISHA), prayers)
        assertEquals(false, QadaPrayer.WITR in prayers)
        assertEquals(366, perPrayer)
        assertEquals(366 * 5, Qada.estimateTotal(perPrayer, AsrMadhab.SHAFI))
        val counters = prayers.map { QadaCounter(it, remaining = perPrayer) }
        assertEquals(366 * 5, Qada.totalRemaining(counters, AsrMadhab.SHAFI))
    }

    @Test fun hanafiGivesWitrTooWith366() {
        val perPrayer = Qada.inclusiveDays(y2020Start, y2020End)
        val prayers = Qada.prayersFor(AsrMadhab.HANAFI)
        assertEquals(true, QadaPrayer.WITR in prayers)
        assertEquals(366, perPrayer)
        assertEquals(366 * 6, Qada.estimateTotal(perPrayer, AsrMadhab.HANAFI))
        val counters = prayers.map { QadaCounter(it, remaining = perPrayer) }
        assertEquals(366 * 6, Qada.totalRemaining(counters, AsrMadhab.HANAFI))
        assertEquals(366, counters.first { it.prayer == QadaPrayer.WITR }.remaining)
    }

    @Test fun decrementAtZeroStaysZero() {
        assertEquals(0, Qada.applyDelta(0, -1))
        assertEquals(0, Qada.applyDelta(1, -5))
        assertEquals(1, Qada.applyDelta(0, 1))
    }

    @Test fun cappedAtMaximum() {
        assertEquals(Qada.MAX_REMAINING, Qada.applyDelta(Qada.MAX_REMAINING, 1))
    }

    @Test fun finishDateFor1830With2PerPrayerFor5Prayers() {
        val today = LocalDate.of(2026, 11, 19)
        val counters = Qada.prayersFor(AsrMadhab.SHAFI).map { QadaCounter(it, remaining = 366, dailyGoal = 2) }
        // 1830 qazo, kuniga 2 tadan (5 namoz) → 183 kun
        assertEquals(today.plusDays(183), Qada.finishDate(today, counters, AsrMadhab.SHAFI))
    }

    @Test fun finishDateRoundsUpAndIsNullWithoutGoal() {
        val today = LocalDate.of(2026, 1, 1)
        val counters = listOf(QadaCounter(QadaPrayer.FAJR, remaining = 10, dailyGoal = 3))
        assertEquals(today.plusDays(4), Qada.finishDate(today, counters, AsrMadhab.SHAFI))
        assertNull(Qada.finishDate(today, counters.map { it.copy(dailyGoal = 0) }, AsrMadhab.SHAFI))
    }

    @Test fun finishDateIsTodayWhenNothingRemaining() {
        val today = LocalDate.of(2026, 1, 1)
        assertEquals(today, Qada.finishDate(today, emptyList(), AsrMadhab.SHAFI))
    }

    @Test fun inclusiveDaysZeroForReversedRange() {
        assertEquals(0, Qada.inclusiveDays(y2020End, y2020Start))
        assertEquals(1, Qada.inclusiveDays(y2020Start, y2020Start))
    }

    @Test fun sunriseIsNotAQadaPrayer() {
        assertNull(QadaPrayer.of(Prayer.SUNRISE))
        assertEquals(QadaPrayer.ISHA, QadaPrayer.of(Prayer.ISHA))
    }
}
