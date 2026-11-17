package uz.xumoyun.namoz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.xumoyun.core.data.AppJson
import uz.xumoyun.namoz.FileAssets
import uz.xumoyun.namoz.data.AlertSound
import uz.xumoyun.namoz.data.CityIndex
import uz.xumoyun.namoz.data.PrayerAlert
import uz.xumoyun.namoz.notifications.AlarmPlanner
import java.time.LocalDate
import java.time.ZoneId

class ScheduleAndMethodsTest {
    private val zone = ZoneId.of("Asia/Tashkent")
    private val input = CalcInput(41.2995, 69.2401, zone, CalcMethod.UZBEKISTAN, AsrMadhab.HANAFI)
    private val date = LocalDate.of(2026, 11, 17)
    private val today = PrayerCalculator.day(input, date)
    private val tomorrow = PrayerCalculator.day(input, date.plusDays(1))

    @Test fun nextPrayerDuringDay() {
        val now = today[Prayer.DHUHR].plusMinutes(1)
        val next = Schedule.next(now, today, tomorrow)
        assertEquals(Prayer.ASR, next.prayer)
        assertEquals(Prayer.DHUHR, Schedule.current(now, today))
        assertTrue(next.remaining.toMinutes() > 0)
    }

    @Test fun afterIshaNextIsTomorrowFajr() {
        val now = today[Prayer.ISHA].plusMinutes(30)
        val next = Schedule.next(now, today, tomorrow)
        assertEquals(Prayer.FAJR, next.prayer)
        assertEquals(tomorrow.date, next.time.toLocalDate())
    }

    @Test fun beforeFajrCurrentIsIsha() {
        val now = today[Prayer.FAJR].minusMinutes(10)
        assertEquals(Prayer.ISHA, Schedule.current(now, today))
        assertEquals(DayPhase.NIGHT, Schedule.phase(now, today))
    }

    @Test fun phases() {
        assertEquals(DayPhase.DAWN, Schedule.phase(today[Prayer.SUNRISE], today))
        assertEquals(DayPhase.DAY, Schedule.phase(today[Prayer.DHUHR], today))
        assertEquals(DayPhase.DUSK, Schedule.phase(today[Prayer.MAGHRIB], today))
    }

    @Test fun methodTableByCountry() {
        val t = MethodTable.parse(AppJson, FileAssets.readText("methods.json"))
        assertEquals(MethodRule(CalcMethod.UZBEKISTAN, AsrMadhab.HANAFI), t.forCountry("uz"))
        assertEquals(MethodRule(CalcMethod.UMM_AL_QURA, AsrMadhab.SHAFI), t.forCountry("SA"))
        assertEquals(MethodRule(CalcMethod.KARACHI, AsrMadhab.HANAFI), t.forCountry("AF"))
        assertEquals(MethodRule(CalcMethod.TURKEY_DIYANET, AsrMadhab.HANAFI), t.forCountry("TR"))
        assertEquals(MethodRule(CalcMethod.MOONSIGHTING_COMMITTEE, AsrMadhab.SHAFI), t.forCountry("GB"))
        assertEquals(MethodRule(CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.SHAFI), t.forCountry("FR"))
        assertEquals(MethodRule(CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.SHAFI), t.forCountry(null))
    }

    @Test fun citySearchAndNearest() {
        val index = CityIndex.parse(AppJson, FileAssets.readText("cities.json"))
        assertTrue(index.cities.size > 150)
        assertEquals("Tashkent", index.search("tash").first().name)
        assertEquals("Istanbul", index.search("İSTANBUL").firstOrNull()?.name ?: index.search("istan").first().name)
        val (near, km) = index.nearest(41.31, 69.28)!!
        assertEquals("Tashkent", near.name)
        assertTrue(km < 10)
        // Barcha vaqt mintaqalari to'g'ri.
        index.cities.forEach { ZoneId.of(it.timeZone) }
    }

    @Test fun alarmPlanSkipsPastAndDisabled() {
        val now = today[Prayer.DHUHR].plusMinutes(1).toInstant()
        val alerts = Prayer.entries.associateWith { PrayerAlert(it.isPrayer, AlertSound.AZAN, if (it == Prayer.ASR) 10 else 0) }
        val plan = AlarmPlanner.plan(now, listOf(today, tomorrow), alerts)
        // Bugun: Asr (+eslatma), Shom, Xufton; ertaga: 5 namoz + Asr eslatmasi. Quyosh o'chiq.
        assertEquals(3 + 1 + 5 + 1, plan.size)
        assertTrue(plan.none { it.prayer == Prayer.SUNRISE })
        assertTrue(plan.all { it.triggerAt.isAfter(now) })
        assertEquals(plan.size, plan.map { it.requestCode }.toSet().size)
        val reminder = plan.first { it.isReminder && it.prayerTime == today[Prayer.ASR].toInstant() }
        assertEquals(today[Prayer.ASR].minusMinutes(10).toInstant(), reminder.triggerAt)
    }
}
