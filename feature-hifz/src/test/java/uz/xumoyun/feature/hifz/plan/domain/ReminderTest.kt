package uz.xumoyun.feature.hifz.plan.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ReminderTest {

    private val zone = ZoneId.of("Asia/Tashkent")
    private val fajr = mapOf(
        LocalDate.of(2027, 2, 8) to Instant.parse("2027-02-08T00:40:00Z"), // 05:40 Toshkent
        LocalDate.of(2027, 2, 9) to Instant.parse("2027-02-09T00:39:00Z"),
        LocalDate.of(2027, 2, 10) to Instant.parse("2027-02-10T00:38:00Z"),
    )
    private val timeOf: (LocalDate, String) -> Instant? = { d, key -> if (key == "FAJR") fajr[d] else null }

    @Test fun `reminder is the prayer time plus offset when still ahead`() {
        val now = Instant.parse("2027-02-08T00:00:00Z") // 05:00 Toshkent
        assertEquals(Instant.parse("2027-02-08T00:55:00Z"), nextHifzReminder(now, zone, "FAJR", 15, timeOf))
    }

    @Test fun `once today's reminder has passed the next day's is returned`() {
        val now = Instant.parse("2027-02-08T01:00:00Z")
        assertEquals(Instant.parse("2027-02-09T00:54:00Z"), nextHifzReminder(now, zone, "FAJR", 15, timeOf))
    }

    @Test fun `zero offset fires at the prayer time itself`() {
        val now = Instant.parse("2027-02-08T00:00:00Z")
        assertEquals(Instant.parse("2027-02-08T00:40:00Z"), nextHifzReminder(now, zone, "FAJR", 0, timeOf))
    }

    @Test fun `exact trigger instant equal to now is not in the future`() {
        val now = Instant.parse("2027-02-08T00:40:00Z")
        assertEquals(Instant.parse("2027-02-09T00:39:00Z"), nextHifzReminder(now, zone, "FAJR", 0, timeOf))
    }

    @Test fun `unknown prayer key or missing times give no reminder`() {
        val now = Instant.parse("2027-02-08T00:00:00Z")
        assertNull(nextHifzReminder(now, zone, "SUNRISE", 0, timeOf))
        assertNull(nextHifzReminder(now, zone, "ISHA", 0) { _, _ -> null })
    }

    @Test fun `negative offset is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            nextHifzReminder(Instant.EPOCH, zone, "FAJR", -1, timeOf)
        }
    }
}
