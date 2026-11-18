package uz.xumoyun.feature.prayer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

class PrayerCalculatorTest {

    @Test fun timesAreOrderedForEveryDayOfTheYear() {
        (TestData.standardCities + TestData.London).forEach { c ->
            val inp = TestData.input(c)
            var d = LocalDate.of(2026, 1, 1)
            while (d.year == 2026) {
                val t = PrayerCalculator.day(inp, d).ordered.map { it.second }
                t.zipWithNext().forEach { (a, b) -> assertTrue("${c.name} $d: $a < $b", a.isBefore(b)) }
                assertEquals("${c.name} $d", d, t[2].toLocalDate()) // Peshin shu kunda
                d = d.plusDays(1)
            }
        }
    }

    @Test fun hanafiAsrIsLaterThanShafi() {
        val c = TestData.Tashkent
        val date = LocalDate.of(2026, 6, 21)
        val shafi = PrayerCalculator.day(TestData.input(c).copy(madhab = AsrMadhab.SHAFI), date)[Prayer.ASR]
        val hanafi = PrayerCalculator.day(TestData.input(c).copy(madhab = AsrMadhab.HANAFI), date)[Prayer.ASR]
        assertTrue(hanafi.isAfter(shafi.plusMinutes(30)))
    }

    @Test fun userAdjustmentShiftsEachPrayer() {
        val c = TestData.Tashkent
        val date = LocalDate.of(2026, 11, 17)
        val base = PrayerCalculator.day(TestData.input(c), date)
        Prayer.entries.forEach { p ->
            val adjusted = PrayerCalculator.day(TestData.input(c).copy(adjustments = mapOf(p to -3)), date)
            assertEquals(p.name, base[p].minusMinutes(3), adjusted[p])
            Prayer.entries.filter { it != p }.forEach { other -> assertEquals(base[other], adjusted[other]) }
        }
    }

    @Test fun defaultHighLatRuleIsSeventhOfTheNight() {
        assertEquals(HighLatRule.SEVENTH_OF_THE_NIGHT, CalcInput(0.0, 0.0, TestData.Tashkent.zone, CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.SHAFI).highLatRule)
    }

    @Test fun seventhOfTheNightBoundsFajrAndIshaAbove48() {
        val paris = TestData.input(TestData.Paris) // 48.86° N, MWL
        val date = LocalDate.of(2026, 6, 21)
        val today = PrayerCalculator.day(paris, date)
        val tomorrow = PrayerCalculator.day(paris, date.plusDays(1))
        val night = Duration.between(today[Prayer.MAGHRIB], tomorrow[Prayer.SUNRISE])
        val seventh = night.dividedBy(7)
        // Bomdod tunning oxirgi 1/7 qismidan oldin emas, Xufton birinchi 1/7 qismidan keyin emas (±1 daq. yaxlitlash).
        val fajrBound = Duration.between(tomorrow[Prayer.FAJR], tomorrow[Prayer.SUNRISE])
        assertTrue("Bomdod: $fajrBound vs $seventh", fajrBound <= seventh.plusMinutes(1))
        val ishaBound = Duration.between(today[Prayer.MAGHRIB], today[Prayer.ISHA])
        assertTrue("Xufton: $ishaBound vs $seventh", ishaBound <= seventh.plusMinutes(1))

        val middle = PrayerCalculator.day(paris.copy(highLatRule = HighLatRule.MIDDLE_OF_THE_NIGHT), date)
        assertTrue(today[Prayer.FAJR].isAfter(middle[Prayer.FAJR]))
    }

    @Test fun highLatRuleDoesNotChangeTimesBelow48() {
        val date = LocalDate.of(2026, 6, 21)
        listOf(TestData.Tashkent, TestData.NewYork, TestData.Istanbul).forEach { c ->
            val rules = HighLatRule.entries.map { PrayerCalculator.day(TestData.input(c, it), date) }
            assertEquals(c.name, 1, rules.toSet().size)
        }
        assertEquals(HighLatRule.MIDDLE_OF_THE_NIGHT, PrayerCalculator.effectiveHighLatRule(41.3, HighLatRule.SEVENTH_OF_THE_NIGHT))
        assertEquals(HighLatRule.SEVENTH_OF_THE_NIGHT, PrayerCalculator.effectiveHighLatRule(51.5, HighLatRule.SEVENTH_OF_THE_NIGHT))
        assertEquals(HighLatRule.TWILIGHT_ANGLE, PrayerCalculator.effectiveHighLatRule(-55.0, HighLatRule.TWILIGHT_ANGLE))
    }

    @Test fun monthHasEveryDay() {
        val m = java.time.YearMonth.of(2027, 2)
        val days = PrayerCalculator.month(TestData.input(TestData.Jakarta), m)
        assertEquals(28, days.size)
        assertEquals(m.atDay(1), days.first().date)
    }

    @Test fun qiblaDirections() {
        // TZ 7-bo'lim: Toshkent 240.3°, Jakarta 295.2°, London 119.0° (±1°).
        assertEquals(240.3, PrayerCalculator.qibla(41.2995, 69.2401), 1.0)
        assertEquals(295.2, PrayerCalculator.qibla(-6.2088, 106.8456), 1.0)
        assertEquals(119.0, PrayerCalculator.qibla(51.5074, -0.1278), 1.0)
    }
}
