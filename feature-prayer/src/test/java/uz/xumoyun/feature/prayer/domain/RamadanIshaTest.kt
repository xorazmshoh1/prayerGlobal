package uz.xumoyun.feature.prayer.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class RamadanIshaTest {
    private val zone = ZoneId.of("Asia/Riyadh")
    private val date = LocalDate.of(2027, 2, 9)
    private val base = DayTimes(
        date,
        mapOf(
            Prayer.FAJR to ZonedDateTime.of(date, java.time.LocalTime.of(4, 50), zone),
            Prayer.MAGHRIB to ZonedDateTime.of(date, java.time.LocalTime.of(17, 45), zone),
            Prayer.ISHA to ZonedDateTime.of(date, java.time.LocalTime.of(19, 15), zone),
        ),
    )

    @Test fun ummAlQuraRamadanIshaIsMaghribPlus120() {
        val r = RamadanIsha.apply(CalcMethod.UMM_AL_QURA, base, ramadan = true)
        assertEquals(ZonedDateTime.of(date, java.time.LocalTime.of(19, 45), zone), r[Prayer.ISHA])
        assertEquals(base[Prayer.MAGHRIB], r[Prayer.MAGHRIB])
        assertEquals(base[Prayer.FAJR], r[Prayer.FAJR])
    }

    @Test fun outsideRamadanOrOtherMethodsKeepIsha() {
        assertEquals(base, RamadanIsha.apply(CalcMethod.UMM_AL_QURA, base, ramadan = false))
        assertEquals(base, RamadanIsha.apply(CalcMethod.MUSLIM_WORLD_LEAGUE, base, ramadan = true))
    }

    @Test fun noRamadanCheckMeansNoChange() {
        assertEquals(false, RamadanCheck.NONE.isRamadan(date))
    }
}
