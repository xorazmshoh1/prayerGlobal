package uz.xumoyun.feature.prayer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Signal rejalashtiruvchi (TZ 8-bo'lim) — soxta soat (FixedTimeSource) bilan, Android'siz. */
class AlarmPlannerTest {
    private val zone = ZoneId.of("Asia/Tashkent")
    private val input = TestData.input(TestData.Tashkent)
    private val date = LocalDate.of(2026, 11, 17)
    private val day: (LocalDate) -> DayTimes = { PrayerCalculator.day(input, it) }
    private val today = day(date)
    private val defaults = Prayer.entries.associateWith { PrayerAlert.default(it) }

    private fun clockAt(time: ZonedDateTime) = FixedTimeSource(time.toInstant(), zone)
    private fun plan(
        now: Instant,
        alerts: Map<Prayer, PrayerAlert> = defaults,
        exact: Boolean = true,
        dayTimes: (LocalDate) -> DayTimes = day,
        planZone: ZoneId = zone,
    ) = AlarmPlanner.plan(now, planZone, alerts, exact, dayTimes)

    @Test fun plansFivePrayersInsideTwoDayWindow() {
        val clock = clockAt(today[Prayer.DHUHR].plusMinutes(10))
        val now = clock.now()
        val p = plan(now)
        // Bugun: Asr, Shom, Xufton; ertaga: 5 namoz; indinga: Bomdod va Peshin (hozir + 48 soatgacha).
        assertEquals(3 + 5 + 2, p.alarms.size)
        assertTrue(p.alarms.all { it.kind == AlarmKind.AT_TIME && it.prayer.isPrayer })
        assertTrue(p.alarms.all { it.triggerAt.isAfter(now) && !it.triggerAt.isAfter(now.plus(AlarmPlanner.HORIZON)) })
        assertTrue(p.alarms.all { it.delivery == AlarmDelivery.Exact })
        assertEquals(p.alarms.sortedBy { it.triggerAt }, p.alarms)
        // Signal vaqti — aynan hisoblangan namoz vaqti.
        p.alarms.forEach { assertEquals(day(it.date)[it.prayer].toInstant(), it.triggerAt) }
        assertEquals(listOf(date, date.plusDays(1), date.plusDays(2)), p.alarms.map { it.date }.distinct())
        assertEquals(listOf(Prayer.FAJR, Prayer.DHUHR), p.alarms.filter { it.date == date.plusDays(2) }.map { it.prayer })
    }

    @Test fun offModeProducesNoAlarmsAndItsCodesAreCancelled() {
        val alerts = defaults + (Prayer.ASR to PrayerAlert(AlertMode.OFF, 10))
        val p = plan(today[Prayer.FAJR].minusMinutes(1).toInstant(), alerts)
        assertTrue(p.alarms.none { it.prayer == Prayer.ASR })
        assertTrue(p.alarms.none { it.prayer == Prayer.SUNRISE })
        assertTrue(AlarmPlanner.requestCode(date, Prayer.ASR, AlarmKind.AT_TIME) in p.staleCodes)
        assertTrue(AlarmPlanner.requestCode(date, Prayer.ASR, AlarmKind.BEFORE) in p.staleCodes)
    }

    @Test fun silentModeIsPlannedWithItsMode() {
        val alerts = defaults + (Prayer.MAGHRIB to PrayerAlert(AlertMode.SILENT, 0))
        val p = plan(today[Prayer.ASR].toInstant(), alerts)
        assertEquals(AlertMode.SILENT, p.alarms.first { it.prayer == Prayer.MAGHRIB }.mode)
        assertEquals(AlertMode.SOUND, p.alarms.first { it.prayer == Prayer.ISHA }.mode)
    }

    @Test fun reminderBeforePrayer() {
        val alerts = defaults + (Prayer.ASR to PrayerAlert(AlertMode.SOUND, 15))
        val asr = today[Prayer.ASR]
        val p = plan(asr.minusHours(1).toInstant(), alerts)
        val reminder = p.alarms.first { it.kind == AlarmKind.BEFORE && it.date == date }
        assertEquals(Prayer.ASR, reminder.prayer)
        assertEquals(asr.minusMinutes(15).toInstant(), reminder.triggerAt)
        assertEquals(asr.toInstant(), reminder.prayerTime)
        assertEquals(AlarmPlanner.requestCode(date, Prayer.ASR, AlarmKind.BEFORE), reminder.requestCode)
        // Eslatma vaqti o'tib ketgan, namoz hali kirmagan: faqat namoz signali qoladi.
        val late = plan(asr.minusMinutes(5).toInstant(), alerts)
        assertTrue(late.alarms.none { it.kind == AlarmKind.BEFORE && it.date == date })
        assertTrue(late.alarms.any { it.kind == AlarmKind.AT_TIME && it.prayer == Prayer.ASR && it.date == date })
    }

    @Test fun reminderMinutesAreClampedTo30() {
        val alerts = defaults + (Prayer.ISHA to PrayerAlert(AlertMode.SOUND, 90))
        val isha = today[Prayer.ISHA]
        val reminder = plan(isha.minusHours(2).toInstant(), alerts).alarms.first { it.kind == AlarmKind.BEFORE }
        assertEquals(isha.minusMinutes(30).toInstant(), reminder.triggerAt)
    }

    @Test fun windowFallbackWithoutExactAlarmPermission() {
        val alerts = defaults + (Prayer.ASR to PrayerAlert(AlertMode.SOUND, 10))
        val asr = today[Prayer.ASR].toInstant()
        val p = plan(asr.minus(Duration.ofHours(1)), alerts, exact = false)
        val atTime = p.alarms.first { it.prayer == Prayer.ASR && it.kind == AlarmKind.AT_TIME }
        // Namoz vaqtidagi bildirishnoma vaqt kirmasdan chiqmaydi: [vaqt, vaqt + 10 daqiqa].
        assertEquals(AlarmDelivery.Window(asr, Duration.ofMinutes(10)), atTime.delivery)
        // Eslatma ±5 daqiqa.
        val before = p.alarms.first { it.kind == AlarmKind.BEFORE }
        val trigger = asr.minus(Duration.ofMinutes(10))
        assertEquals(AlarmDelivery.Window(trigger.minus(Duration.ofMinutes(5)), Duration.ofMinutes(10)), before.delivery)
        assertTrue(p.alarms.all { it.delivery is AlarmDelivery.Window })
        // Oyna boshi hozirdan oldin bo'lmaydi.
        val now = trigger.minus(Duration.ofMinutes(2))
        val near = plan(now, alerts, exact = false).alarms.first { it.kind == AlarmKind.BEFORE }.delivery as AlarmDelivery.Window
        assertEquals(now, near.start)
        assertEquals(trigger.plus(Duration.ofMinutes(5)), near.start.plus(near.length))
    }

    @Test fun requestCodesAreStableAndUnique() {
        val alerts = Prayer.entries.associateWith { PrayerAlert(AlertMode.SOUND, 10) }
        val morning = plan(today[Prayer.FAJR].minusHours(1).toInstant(), alerts)
        val evening = plan(today[Prayer.ASR].toInstant(), alerts)
        val byKey = morning.alarms.associate { (it.date to it.prayer to it.kind) to it.requestCode }
        evening.alarms.forEach { a -> byKey[a.date to a.prayer to a.kind]?.let { assertEquals(it, a.requestCode) } }
        assertEquals(morning.alarms.size, morning.alarms.map { it.requestCode }.toSet().size)

        val all = AlarmPlanner.cleanupDates(date).flatMap(AlarmPlanner::codesFor)
        assertEquals(7 * Prayer.entries.size * AlarmKind.entries.size, all.toSet().size)
        assertTrue(all.all { it in 0 until AlarmPlanner.REFRESH_REQUEST_CODE })
        val expected = (date.toEpochDay() % 1000).toInt() * 16 + Prayer.ASR.ordinal * 2 + 1
        assertEquals(expected, AlarmPlanner.requestCode(date, Prayer.ASR, AlarmKind.BEFORE))
    }

    @Test fun staleCodesComplementActiveOnes() {
        val p = plan(today[Prayer.DHUHR].toInstant())
        val active = p.alarms.map { it.requestCode }.toSet()
        val all = AlarmPlanner.cleanupDates(date).flatMap(AlarmPlanner::codesFor).toSet()
        assertTrue(p.staleCodes.none { it in active })
        assertEquals(all, active + p.staleCodes)
    }

    @Test fun afterIshaFirstAlarmIsTomorrowFajr() {
        val now = date.atTime(23, 59).atZone(zone).toInstant()
        val p = plan(now)
        val first = p.alarms.first()
        assertEquals(Prayer.FAJR, first.prayer)
        assertEquals(date.plusDays(1), first.date)
        assertEquals(day(date.plusDays(1))[Prayer.FAJR].toInstant(), p.refresh!!.at)
    }

    @Test fun justAfterMidnightYesterdayIsCancelled() {
        val tomorrow = date.plusDays(1)
        val now = tomorrow.atStartOfDay(zone).plusSeconds(30).toInstant()
        val p = plan(now)
        assertTrue(AlarmPlanner.codesFor(date).all { it in p.staleCodes })
        assertEquals(tomorrow, p.alarms.first().date)
        assertEquals(Prayer.FAJR, p.alarms.first().prayer)
    }

    /** Yuqori kengliklarda Xufton yarim tundan keyin bo'lishi mumkin: kechagi kun signali ham rejalanadi. */
    @Test fun ishaAfterMidnightBelongsToPreviousDay() {
        val synthetic: (LocalDate) -> DayTimes = { d ->
            fun at(h: Int, m: Int, plus: Long = 0) = d.plusDays(plus).atTime(h, m).atZone(zone)
            DayTimes(
                d,
                mapOf(
                    Prayer.FAJR to at(2, 30), Prayer.SUNRISE to at(4, 0), Prayer.DHUHR to at(13, 0),
                    Prayer.ASR to at(17, 30), Prayer.MAGHRIB to at(21, 50), Prayer.ISHA to at(0, 40, plus = 1),
                ),
            )
        }
        val now = date.atTime(0, 10).atZone(zone).toInstant()
        val p = plan(now, dayTimes = synthetic)
        val isha = p.alarms.first()
        assertEquals(Prayer.ISHA, isha.prayer)
        assertEquals(date.minusDays(1), isha.date)
        assertEquals(AlarmPlanner.requestCode(date.minusDays(1), Prayer.ISHA, AlarmKind.AT_TIME), isha.requestCode)
        assertEquals(p.alarms.size, p.alarms.map { it.requestCode }.toSet().size)
    }

    /** Yozgi vaqtga o'tish (London, 2026-03-29 01:00 UTC): signal vaqtlari Instant'da, 48 soat — real vaqt. */
    @Test fun daylightSavingTransition() {
        val london = ZoneId.of("Europe/London")
        val lInput = TestData.input(TestData.London)
        val lDay: (LocalDate) -> DayTimes = { PrayerCalculator.day(lInput, it) }
        val now = ZonedDateTime.of(2026, 3, 28, 12, 0, 0, 0, london).toInstant()
        val p = AlarmPlanner.plan(now, london, defaults, true, lDay)
        p.alarms.forEach { assertEquals(lDay(it.date)[it.prayer].toInstant(), it.triggerAt) }
        assertTrue(p.alarms.all { !it.triggerAt.isAfter(now.plus(Duration.ofHours(48))) })
        val fajr29 = p.alarms.single { it.date == LocalDate.of(2026, 3, 29) && it.prayer == Prayer.FAJR }
        assertEquals(ZoneOffset.ofHours(1), fajr29.triggerAt.atZone(london).offset)
        val fajr28 = lDay(LocalDate.of(2026, 3, 28))[Prayer.FAJR]
        assertEquals(ZoneOffset.UTC, fajr28.offset)
        // Mahalliy soat bo'yicha Bomdod ~1 soat "kechroq" ko'rinadi, real vaqt bo'yicha esa bir kunda ~2 daqiqa oldinroq.
        val fajr29Local = fajr29.triggerAt.atZone(london).toLocalTime()
        assertTrue(fajr29Local.isAfter(fajr28.toLocalTime().plusMinutes(50)))
        assertEquals(p.alarms.size, p.alarms.map { it.requestCode }.toSet().size)
    }

    @Test fun refreshIsNextTimeEvenWhenAlertIsOff() {
        val now = today[Prayer.FAJR].plusMinutes(5).toInstant()
        val p = plan(now, exact = false)
        assertEquals(today[Prayer.SUNRISE].toInstant(), p.refresh!!.at)
        assertEquals(AlarmDelivery.Window(today[Prayer.SUNRISE].toInstant(), Duration.ofMinutes(10)), p.refresh!!.delivery)
        assertEquals(AlarmDelivery.Exact, plan(now, exact = true).refresh!!.delivery)
    }

    @Test fun sunriseAlertGoesToReminderChannel() {
        val alerts = defaults + (Prayer.SUNRISE to PrayerAlert(AlertMode.SOUND, 10))
        val p = plan(today[Prayer.FAJR].toInstant(), alerts)
        val sunrise = p.alarms.filter { it.prayer == Prayer.SUNRISE && it.date == date }
        assertEquals(listOf(AlarmKind.BEFORE, AlarmKind.AT_TIME), sunrise.map { it.kind })
        assertTrue(sunrise.all { PrayerChannelIds.forAlarm(it.prayer, it.kind) == PrayerChannelIds.REMINDER })
    }

    @Test fun noLocationCancelsEverything() {
        val p = AlarmPlanner.cancelAll(today[Prayer.DHUHR].toInstant(), zone)
        assertTrue(p.alarms.isEmpty())
        assertNull(p.refresh)
        assertEquals(AlarmPlanner.cleanupDates(date).flatMap(AlarmPlanner::codesFor), p.staleCodes)
    }

    @Test fun notifyOnlyWhileMeaningful() {
        val asr = today[Prayer.ASR].toInstant()
        assertTrue(AlarmPlanner.shouldNotify(asr, AlarmKind.AT_TIME, asr))
        assertTrue(AlarmPlanner.shouldNotify(asr.plus(Duration.ofMinutes(30)), AlarmKind.AT_TIME, asr))
        assertFalse(AlarmPlanner.shouldNotify(asr.plus(Duration.ofMinutes(31)), AlarmKind.AT_TIME, asr))
        assertTrue(AlarmPlanner.shouldNotify(asr.minus(Duration.ofMinutes(1)), AlarmKind.BEFORE, asr))
        assertFalse(AlarmPlanner.shouldNotify(asr, AlarmKind.BEFORE, asr))
    }

    @Test fun minutesUntilRoundsUp() {
        val asr = today[Prayer.ASR].toInstant()
        assertEquals(10, AlarmPlanner.minutesUntil(asr.minus(Duration.ofMinutes(10)), asr))
        assertEquals(10, AlarmPlanner.minutesUntil(asr.minus(Duration.ofSeconds(9 * 60 + 1)), asr))
        assertEquals(1, AlarmPlanner.minutesUntil(asr.plus(Duration.ofMinutes(1)), asr))
    }

    @Test fun fixedClockDrivesPlan() {
        val clock = FixedTimeSource(today[Prayer.MAGHRIB].minusMinutes(1).toInstant(), zone)
        assertEquals(Prayer.MAGHRIB, plan(clock.now()).alarms.first().prayer)
        clock.instant = today[Prayer.MAGHRIB].toInstant()
        assertEquals(Prayer.ISHA, plan(clock.now()).alarms.first().prayer)
    }
}
