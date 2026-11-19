package uz.xumoyun.core.common.hijri

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class HijriCalendarTest {
    private val base = HijriCalendar(Icu4jUmmAlQura, 0)

    @Test fun firstRamadan1448IsFebruary8_2027() {
        assertEquals(HijriYmd(1448, 9, 1), base.ymd(LocalDate.of(2027, 2, 8)))
    }

    @Test fun shawwal1448IsMarch9_2027() {
        assertEquals(HijriYmd(1448, 10, 1), base.ymd(LocalDate.of(2027, 3, 9)))
    }

    @Test fun positiveOffsetShowsRamadanOneDayLater() {
        val plusOne = HijriCalendar(Icu4jUmmAlQura, 1)
        assertEquals(HijriYmd(1448, 9, 1), plusOne.ymd(LocalDate.of(2027, 2, 9)))
        assertEquals(HijriYmd(1448, 8, 30), plusOne.ymd(LocalDate.of(2027, 2, 8)))
    }

    @Test fun negativeOffsetShowsRamadanOneDayEarlier() {
        val minusOne = HijriCalendar(Icu4jUmmAlQura, -1)
        assertEquals(HijriYmd(1448, 9, 1), minusOne.ymd(LocalDate.of(2027, 2, 7)))
    }

    @Test fun ramadanHasTwentyNineOrThirtyDays() {
        val len = (1..31).first { base.ymd(LocalDate.of(2027, 2, 8).plusDays(it.toLong())).month != 9 } 
        assertEquals(true, len == 29 || len == 30)
    }

    @Test fun reverseLookupFindsGregorianDate() {
        assertEquals(LocalDate.of(2027, 2, 8), base.gregorianOf(HijriYmd(1448, 9, 1)))
        assertEquals(LocalDate.of(2027, 2, 9), HijriCalendar(Icu4jUmmAlQura, 1).gregorianOf(HijriYmd(1448, 9, 1)))
    }

    @Test fun roundTripHoldsForEveryDayFrom2026To2030() {
        for (offset in -HijriCalendar.MAX_OFFSET_DAYS..HijriCalendar.MAX_OFFSET_DAYS) {
            val cal = HijriCalendar(Icu4jUmmAlQura, offset)
            var d = LocalDate.of(2026, 1, 1)
            val end = LocalDate.of(2030, 12, 31)
            while (!d.isAfter(end)) {
                val h = cal.ymd(d)
                val back = cal.gregorianOf(h)
                assertNotNull("offset=$offset, $d -> $h", back)
                assertEquals("offset=$offset, $d -> $h", d, back)
                d = d.plusDays(1)
            }
        }
    }

    @Test fun tabularGuessIsWithinTwoDaysOfUmmAlQura() {
        for (y in listOf(1440, 1446, 1448, 1450)) for (m in 1..12) {
            val h = HijriYmd(y, m, 1)
            val g = base.gregorianOf(h)
            assertNotNull(g)
            assertEquals(true, Math.abs(java.time.temporal.ChronoUnit.DAYS.between(HijriCalendar.tabularGuess(h), g!!)) <= 2)
        }
    }
}
