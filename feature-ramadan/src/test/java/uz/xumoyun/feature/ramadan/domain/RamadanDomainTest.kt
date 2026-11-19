package uz.xumoyun.feature.ramadan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.xumoyun.core.common.hijri.HijriCalendar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RamadanCalcTest {
    private val cal = HijriCalendar(FakeUmmAlQura, 0)
    private val start = LocalDate.of(2027, 2, 8)

    @Test fun imsakIsFajrMinusOffset() {
        // TZ 7: Bomdod 05:40, imsakOffsetMin = 10 → imsok 05:30.
        val fajr = Instant.parse("2027-02-08T05:40:00Z")
        assertEquals(Instant.parse("2027-02-08T05:30:00Z"), RamadanCalc.imsak(fajr, 10))
    }

    @Test fun ramadanStartIsFebruary8_2027() {
        assertEquals(start, RamadanCalc.start(cal, 1448))
        assertEquals(start, RamadanCalc.start(HijriCalendar(FakeUmmAlQura, 0), 1448))
    }

    @Test fun dayNumberCountsFromOneAndStopsAtThirty() {
        assertEquals(1, RamadanCalc.dayNumber(LocalDate.of(2027, 2, 8), start))
        assertEquals(10, RamadanCalc.dayNumber(LocalDate.of(2027, 2, 17), start))
        assertEquals(30, RamadanCalc.dayNumber(LocalDate.of(2027, 3, 9), start))
        assertNull(RamadanCalc.dayNumber(LocalDate.of(2027, 3, 10), start))
        assertNull(RamadanCalc.dayNumber(LocalDate.of(2027, 2, 7), start))
    }

    @Test fun phaseBeforeDuringAfter() {
        assertEquals(RamadanPhase.BEFORE, RamadanCalc.phase(LocalDate.of(2027, 2, 7), start))
        assertEquals(RamadanPhase.DURING, RamadanCalc.phase(LocalDate.of(2027, 2, 20), start))
        assertEquals(RamadanPhase.AFTER, RamadanCalc.phase(LocalDate.of(2027, 3, 10), start))
    }

    @Test fun cardShowsFifteenDaysBeforeRamadanAndDuringIt() {
        assertTrue(RamadanCalc.cardVisible(LocalDate.of(2027, 1, 24), start))
        assertFalse(RamadanCalc.cardVisible(LocalDate.of(2027, 1, 23), start))
        assertTrue(RamadanCalc.cardVisible(LocalDate.of(2027, 3, 9), start))
        assertFalse(RamadanCalc.cardVisible(LocalDate.of(2027, 3, 10), start))
    }

    @Test fun upcomingStartSkipsPastRamadan() {
        assertEquals(start, RamadanCalc.upcomingStart(LocalDate.of(2027, 1, 1), cal))
        // Ramazon tugagach ("1448" yilida) keyingi yilga o'tilmaydi, lekin fake jadvalda 1449 yo'q → null.
        assertNull(RamadanCalc.upcomingStart(LocalDate.of(2027, 3, 20), cal))
    }

    @Test fun ishaIsTwoHoursInRamadanOtherwiseNinety() {
        assertEquals(120, RamadanCalc.ishaMinutesAfterMaghrib(true))
        assertEquals(90, RamadanCalc.ishaMinutesAfterMaghrib(false))
    }

    @Test fun qadrCandidatesAreOddNightsOfLastTen() {
        val candidates = (1..30).filter { RamadanCalc.isQadrCandidate(it) }
        assertEquals(listOf(21, 23, 25, 27, 29), candidates)
    }

    @Test fun dayRowsCarryTimesStatusAndToday() {
        val fajr = Instant.parse("2027-02-08T05:40:00Z")
        val maghrib = Instant.parse("2027-02-08T17:30:00Z")
        val rows = RamadanCalc.dayRows(
            start, LocalDate.of(2027, 2, 8), cal, 10,
            times = { d -> if (d == start) RamadanDayTimes(d, fajr, maghrib) else null },
            fasts = mapOf(start to FastStatus.MISSED),
        )
        assertEquals(30, rows.size)
        assertEquals(Instant.parse("2027-02-08T05:30:00Z"), rows[0].imsak)
        assertEquals(maghrib, rows[0].iftar)
        assertEquals(FastStatus.MISSED, rows[0].fast)
        assertTrue(rows[0].isToday)
        assertNull(rows[1].imsak)
        assertTrue(rows[20].isQadrCandidate)
    }
}

class KhatmCalcTest {
    private val start = LocalDate.of(2027, 2, 8)

    @Test fun spec10thDayWithSevenJuzDone() {
        // TZ 7: 10-kunda juzDone = 7, Ramazon 30 kun → qolgan 21 kun, kunlik 23/21 ≈ 1.1 juz.
        val today = LocalDate.of(2027, 2, 17)
        assertEquals(21, KhatmCalc.remainingDays(today, start))
        assertEquals(23.0 / 21.0, KhatmCalc.dailyJuz(7, today, start), 1e-9)
        assertEquals(2, KhatmCalc.todayTarget(7, today, start))
    }

    @Test fun completedKhatmHasNoTarget() {
        assertEquals(0, KhatmCalc.todayTarget(30, LocalDate.of(2027, 2, 20), start))
    }

    @Test fun lastDayCountsAsOne() {
        assertEquals(1, KhatmCalc.remainingDays(LocalDate.of(2027, 3, 9), start))
        assertEquals(29.0, KhatmCalc.dailyJuz(1, LocalDate.of(2027, 3, 9), start), 1e-9)
    }
}

class FastCalcTest {
    @Test fun missedAddsOneToQadaAndUndoSubtractsOne() {
        assertEquals(1, FastCalc.qadaDelta(null, FastStatus.MISSED))
        assertEquals(1, FastCalc.qadaDelta(FastStatus.FASTED, FastStatus.MISSED))
        assertEquals(-1, FastCalc.qadaDelta(FastStatus.MISSED, FastStatus.FASTED))
        assertEquals(-1, FastCalc.qadaDelta(FastStatus.MISSED, null))
    }

    @Test fun otherChangesDoNotTouchQada() {
        assertEquals(0, FastCalc.qadaDelta(null, FastStatus.FASTED))
        assertEquals(0, FastCalc.qadaDelta(FastStatus.MISSED, FastStatus.MISSED))
        assertEquals(0, FastCalc.qadaDelta(FastStatus.EXCUSED, FastStatus.FASTED))
    }
}

class RamadanReminderPlannerTest {
    private val zone = ZoneId.of("UTC")
    private val cal = HijriCalendar(FakeUmmAlQura, 0)
    private val start = LocalDate.of(2027, 2, 8)

    private fun times(d: LocalDate): RamadanDayTimes? =
        if (!d.isBefore(start) && !d.isAfter(start.plusDays(29))) {
            RamadanDayTimes(d, d.atTime(5, 40).toInstant(java.time.ZoneOffset.UTC), d.atTime(17, 30).toInstant(java.time.ZoneOffset.UTC))
        } else null

    private val all = RamadanSettings()

    @Test fun firstReminderIsSuhoorThirtyMinutesBeforeImsak() {
        val r = RamadanReminderPlanner.next(Instant.parse("2027-02-07T12:00:00Z"), zone, cal, ::times, all)
        assertEquals(RamadanReminderKind.SUHOOR, r?.kind)
        // imsok 05:30, minus 30 daqiqa → 05:00.
        assertEquals(Instant.parse("2027-02-08T05:00:00Z"), r?.fireAt)
    }

    @Test fun afterSuhoorTheIftarComesNext() {
        val r = RamadanReminderPlanner.next(Instant.parse("2027-02-08T05:10:00Z"), zone, cal, ::times, all)
        assertEquals(RamadanReminderKind.IFTAR, r?.kind)
        assertEquals(Instant.parse("2027-02-08T17:30:00Z"), r?.fireAt)
    }

    @Test fun qadrReminderOnOddNightsAtMaghrib() {
        val onlyQadr = RamadanSettings(suhoorOn = false, iftarOn = false)
        // 21 Ramazon = Feb 8 + 20 = 2027-02-28 (toq kecha, oxirgi 10 kun) — birinchi nomzod.
        val first = RamadanReminderPlanner.next(Instant.parse("2027-02-20T00:00:00Z"), zone, cal, ::times, onlyQadr)
        assertEquals(RamadanReminderKind.QADR, first?.kind)
        assertEquals(LocalDate.of(2027, 2, 28), first?.date)
        assertEquals(Instant.parse("2027-02-28T17:30:00Z"), first?.fireAt)
        // Keyingi nomzod 23 Ramazon = 2027-03-02.
        val next = RamadanReminderPlanner.next(Instant.parse("2027-02-28T18:00:00Z"), zone, cal, ::times, onlyQadr)
        assertEquals(LocalDate.of(2027, 3, 2), next?.date)
    }

    @Test fun nothingWhenAllOffOrNoTimes() {
        val none = RamadanSettings(suhoorOn = false, iftarOn = false, qadrOn = false)
        assertNull(RamadanReminderPlanner.next(Instant.parse("2027-02-07T12:00:00Z"), zone, cal, ::times, none))
        assertNull(RamadanReminderPlanner.next(Instant.parse("2027-02-07T12:00:00Z"), zone, cal, { null }, all))
    }
}
