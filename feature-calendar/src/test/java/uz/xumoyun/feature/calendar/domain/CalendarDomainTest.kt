package uz.xumoyun.feature.calendar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.xumoyun.core.common.hijri.HijriCalendar
import uz.xumoyun.core.common.hijri.HijriYmd
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarGridTest {
    private val cal = HijriCalendar(FakeUmmAlQura, 0)

    @Test fun mondayFirstFebruary2027HasFourFullWeeks() {
        // 2027-02-01 — dushanba: oy boshida bo'sh katak yo'q, 28 kun = 4 hafta.
        val grid = CalendarGrid.build(YearMonth.of(2027, 2), DayOfWeek.MONDAY, LocalDate.of(2027, 2, 10), cal)
        assertEquals(4, grid.weeks.size)
        assertTrue(grid.weeks.all { it.size == 7 })
        assertEquals(28, grid.days.size)
    }

    @Test fun sundayFirstAddsLeadingBlankAndPadsLastWeek() {
        val grid = CalendarGrid.build(YearMonth.of(2027, 2), DayOfWeek.SUNDAY, LocalDate.of(2027, 2, 10), cal)
        assertEquals(null, grid.weeks.first()[0])
        assertEquals(LocalDate.of(2027, 2, 1), grid.weeks.first()[1]?.date)
        assertEquals(5, grid.weeks.size)
        assertTrue(grid.weeks.all { it.size == 7 })
    }

    @Test fun todayIsMarkedAndRamadanStartHasEvent() {
        val grid = CalendarGrid.build(YearMonth.of(2027, 2), DayOfWeek.MONDAY, LocalDate.of(2027, 2, 8), cal)
        val ramadan1 = grid.days.first { it.date == LocalDate.of(2027, 2, 8) }
        assertTrue(ramadan1.isToday)
        assertEquals(listOf(IslamicEvent.RAMADAN_1), ramadan1.events)
        assertEquals(1, grid.days.count { it.isToday })
    }

    @Test fun eventsListIsOrderedByDate() {
        val grid = CalendarGrid.build(YearMonth.of(2027, 3), DayOfWeek.MONDAY, LocalDate.of(2027, 3, 1), cal)
        val ev = CalendarGrid.events(grid)
        // Fake jadval: 27 Ramazon = 2027-03-06, 1 Shavvol = 2027-03-09.
        assertEquals(listOf(IslamicEvent.LAYLAT_AL_QADR, IslamicEvent.EID_AL_FITR), ev.map { it.event })
        assertEquals(listOf(LocalDate.of(2027, 3, 6), LocalDate.of(2027, 3, 9)), ev.map { it.date })
    }
}

class IslamicEventTest {
    @Test fun dayMatchesFixedHijriDates() {
        assertEquals(listOf(IslamicEvent.LAYLAT_AL_QADR), IslamicEvent.on(HijriYmd(1448, 9, 27)))
        assertEquals(listOf(IslamicEvent.EID_AL_ADHA), IslamicEvent.on(HijriYmd(1448, 12, 10)))
        assertTrue(IslamicEvent.on(HijriYmd(1448, 5, 5)).isEmpty())
    }

    @Test fun rajabAndShabanAreOffByDefault() {
        assertFalse(IslamicEvent.RAJAB_27.enabledByDefault)
        assertFalse(IslamicEvent.SHABAN_15.enabledByDefault)
        assertEquals(9, IslamicEvent.entries.size)
        assertEquals(7, IslamicEvent.entries.count { it.enabledByDefault })
    }

    @Test fun defaultSettingDoesNotRemind() {
        assertEquals(EventSetting(true, false), EventSetting.default(IslamicEvent.RAMADAN_1))
        assertEquals(EventSetting(false, false), EventSetting.default(IslamicEvent.RAJAB_27))
    }

    @Test fun keysAreUniqueAndResolvable() {
        assertEquals(IslamicEvent.entries.size, IslamicEvent.entries.map { it.key }.toSet().size)
        assertEquals(IslamicEvent.ASHURA, IslamicEvent.byKey("ashura"))
        assertNull(IslamicEvent.byKey("nope"))
    }
}

class IslamicReminderPlannerTest {
    private val zone = ZoneId.of("UTC")
    private val cal = HijriCalendar(FakeUmmAlQura, 0)
    private val onlyRamadan1Remind: (IslamicEvent) -> EventSetting = { e ->
        EventSetting(enabled = true, remindDayBefore = e == IslamicEvent.RAMADAN_1)
    }

    @Test fun reminderIsDayBeforeAtNineAm() {
        val r = IslamicReminderPlanner.next(Instant.parse("2027-02-06T10:00:00Z"), zone, cal, onlyRamadan1Remind)
        assertEquals(IslamicEvent.RAMADAN_1, r?.event)
        assertEquals(LocalDate.of(2027, 2, 8), r?.eventDate)
        assertEquals(Instant.parse("2027-02-07T09:00:00Z"), r?.fireAt)
    }

    @Test fun pastReminderIsSkipped() {
        // 07-fevral 09:00 allaqachon o'tgan (soat 10:00) — keyingi eslatma yo'q.
        assertNull(IslamicReminderPlanner.next(Instant.parse("2027-02-07T10:00:00Z"), zone, cal, onlyRamadan1Remind))
    }

    @Test fun disabledOrNotRemindedEventsAreIgnored() {
        val none: (IslamicEvent) -> EventSetting = { EventSetting(enabled = false, remindDayBefore = true) }
        assertNull(IslamicReminderPlanner.next(Instant.parse("2027-02-01T00:00:00Z"), zone, cal, none))
    }

    @Test fun positiveOffsetMovesReminderOneDayLater() {
        val plusOne = HijriCalendar(FakeUmmAlQura, 1)
        val r = IslamicReminderPlanner.next(Instant.parse("2027-02-06T10:00:00Z"), zone, plusOne, onlyRamadan1Remind)
        assertEquals(LocalDate.of(2027, 2, 9), r?.eventDate)
        assertEquals(Instant.parse("2027-02-08T09:00:00Z"), r?.fireAt)
    }
}

class DateConverterTest {
    private val cal = HijriCalendar(FakeUmmAlQura, 0)

    @Test fun hijriToGregorianFindsRamadanStart() {
        val c = DateConverter.fromHijri(1448, 9, 1, cal)
        assertEquals(LocalDate.of(2027, 2, 8), c?.gregorian)
    }

    @Test fun invalidHijriInputIsNull() {
        assertNull(DateConverter.fromHijri(1448, 13, 1, cal))
        assertNull(DateConverter.fromHijri(1448, 9, 31, cal))
    }

    @Test fun gregorianToHijriRoundTrips() {
        val g = DateConverter.fromGregorian(LocalDate.of(2027, 2, 20), cal)
        assertEquals(g.hijri, HijriYmd(1448, 9, 13))
        assertEquals(g.gregorian, DateConverter.fromHijri(g.hijri.year, g.hijri.month, g.hijri.day, cal)?.gregorian)
    }
}
