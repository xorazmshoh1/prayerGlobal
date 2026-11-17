package uz.xumoyun.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class DurationFormatTest {
    @Test fun formatsHoursMinutesSeconds() {
        assertEquals("01:24:10", DurationFormat.hms(Duration.ofSeconds(3600 + 24 * 60 + 10)))
    }

    @Test fun negativeIsZero() {
        assertEquals("00:00:00", DurationFormat.hms(Duration.ofSeconds(-5)))
    }

    @Test fun minutesSeconds() {
        assertEquals("25:00", DurationFormat.ms(1500))
        assertEquals("00:09", DurationFormat.ms(9))
    }
}
