package uz.xumoyun.feature.prayer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerChannelIdsTest {
    @Test fun fivePrayerChannelsPlusReminder() {
        assertEquals(listOf("prayer_fajr", "prayer_dhuhr", "prayer_asr", "prayer_maghrib", "prayer_isha"), PrayerChannelIds.prayerChannels)
        assertThrows(IllegalArgumentException::class.java) { PrayerChannelIds.prayerChannel(Prayer.SUNRISE) }
        assertEquals("prayer_asr", PrayerChannelIds.forAlarm(Prayer.ASR, AlarmKind.AT_TIME))
        assertEquals(PrayerChannelIds.REMINDER, PrayerChannelIds.forAlarm(Prayer.ASR, AlarmKind.BEFORE))
        assertEquals(PrayerChannelIds.REMINDER, PrayerChannelIds.forAlarm(Prayer.SUNRISE, AlarmKind.AT_TIME))
        assertTrue(PrayerChannelIds.OBSOLETE.none { it in PrayerChannelIds.prayerChannels || it == PrayerChannelIds.REMINDER })
    }

    @Test fun notificationIdsAreUnique() {
        val ids = Prayer.entries.flatMap { p -> AlarmKind.entries.map { PrayerChannelIds.notificationId(p, it) } }
        assertEquals(ids.size, ids.toSet().size)
    }
}

class AlertModeTest {
    @Test fun cycleAndDefaults() {
        assertEquals(AlertMode.SILENT, AlertMode.SOUND.next())
        assertEquals(AlertMode.OFF, AlertMode.SILENT.next())
        assertEquals(AlertMode.SOUND, AlertMode.OFF.next())
        assertFalse(PrayerAlert.default(Prayer.SUNRISE).enabled)
        assertTrue(Prayer.entries.filter { it.isPrayer }.all { PrayerAlert.default(it) == PrayerAlert(AlertMode.SOUND, 0) })
        assertEquals(30, PrayerAlert.clampMinutes(45))
        assertEquals(0, PrayerAlert.clampMinutes(-1))
        assertTrue(PrayerAlert.MINUTES_BEFORE_OPTIONS.all { it in 0..PrayerAlert.MAX_MINUTES_BEFORE })
    }
}

class DailyRescheduleTest {
    private val tashkent = ZoneId.of("Asia/Tashkent")

    @Test fun nextRunIsAfterMidnight() {
        val now = ZonedDateTime.of(2026, 11, 17, 15, 0, 0, 0, tashkent)
        assertEquals(ZonedDateTime.of(2026, 11, 18, 0, 5, 0, 0, tashkent), DailyReschedule.nextRun(now))
        assertEquals(Duration.ofHours(9).plusMinutes(5), DailyReschedule.delayUntilNext(now))
        val early = ZonedDateTime.of(2026, 11, 18, 0, 3, 0, 0, tashkent)
        assertEquals(Duration.ofMinutes(2), DailyReschedule.delayUntilNext(early))
        val exactly = ZonedDateTime.of(2026, 11, 18, 0, 5, 0, 0, tashkent)
        assertEquals(Duration.ofDays(1), DailyReschedule.delayUntilNext(exactly))
    }

    /** Beyrut: soat yarim tunda suriladi (2026-03-29 00:00 → 01:00, 2026-10-25 00:00 → 23:00). */
    @Test fun daylightSavingAtMidnight() {
        val beirut = ZoneId.of("Asia/Beirut")
        val spring = ZonedDateTime.of(2026, 3, 28, 12, 0, 0, 0, beirut)
        val springRun = DailyReschedule.nextRun(spring)
        assertEquals(LocalDate.of(2026, 3, 29), springRun.toLocalDate())
        assertEquals(LocalTime.of(1, 5), springRun.toLocalTime()) // 00:05 mavjud emas
        assertEquals(Duration.ofHours(12).plusMinutes(5), DailyReschedule.delayUntilNext(spring))

        val autumn = ZonedDateTime.of(2026, 10, 24, 12, 0, 0, 0, beirut)
        assertEquals(LocalTime.of(0, 5), DailyReschedule.nextRun(autumn).toLocalTime())
        assertEquals(Duration.ofHours(13).plusMinutes(5), DailyReschedule.delayUntilNext(autumn))
    }
}

class WidgetModelTest {
    private val input = TestData.input(TestData.Tashkent)
    private val date = LocalDate.of(2026, 11, 17)
    private val today = PrayerCalculator.day(input, date)
    private val tomorrow = PrayerCalculator.day(input, date.plusDays(1))

    @Test fun nextPrayerAndRemainingTime() {
        val now = today[Prayer.ASR].minusMinutes(84)
        val w = WidgetModel.build(now, "Tashkent", today, tomorrow)
        assertEquals(Prayer.ASR, w.next.prayer)
        assertEquals(Duration.ofMinutes(84), w.next.remaining)
        assertEquals(Prayer.entries, w.rows.map { it.first })
        assertEquals("Tashkent", w.city)
    }

    @Test fun afterIshaShowsTomorrowFajr() {
        val now = today[Prayer.ISHA].plusMinutes(1)
        val w = WidgetModel.build(now, "Tashkent", today, tomorrow)
        assertEquals(Prayer.FAJR, w.next.prayer)
        assertEquals(tomorrow[Prayer.FAJR], w.next.time)
        assertTrue(w.rows.none { it.second == w.next.time })
    }
}
