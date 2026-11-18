package uz.xumoyun.namoz.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.xumoyun.feature.prayer.data.AlertSound
import uz.xumoyun.feature.prayer.data.PrayerAlert
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.CalcInput
import uz.xumoyun.feature.prayer.domain.CalcMethod
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.domain.PrayerCalculator
import java.time.LocalDate
import java.time.ZoneId

/** Signal rejalashtiruvchi (3-bosqichda feature-prayer'ga ko'chadi), feature-prayer API'si orqali. */
class AlarmPlannerTest {
    private val input = CalcInput(41.2995, 69.2401, ZoneId.of("Asia/Tashkent"), CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.HANAFI)
    private val date = LocalDate.of(2026, 11, 17)
    private val today = PrayerCalculator.day(input, date)
    private val tomorrow = PrayerCalculator.day(input, date.plusDays(1))

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
