package uz.xumoyun.namoz.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import uz.xumoyun.namoz.data.PrayerRepository
import uz.xumoyun.namoz.widget.PrayerWidget
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bugungi va ertangi signallarni qo'yadi. Har kuni yarim tunda va telefon
 * qayta yoqilganda / vaqt mintaqasi o'zgarganda qayta chaqiriladi.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PrayerRepository,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    suspend fun rescheduleAll() {
        val config = repository.currentConfig() ?: return
        val today = LocalDate.now(config.zone)
        val days = listOf(today.minusDays(1), today, today.plusDays(1)).map { config.day(it) }

        AlarmPlanner.allCodes(days).forEach { code ->
            PendingIntent.getBroadcast(context, code, Intent(context, PrayerAlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                ?.let { alarmManager.cancel(it); it.cancel() }
        }

        val plan = AlarmPlanner.plan(java.time.Instant.now(), days.drop(1), config.prefs.alerts)
        plan.forEach { alarm ->
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, alarm.prayer.name)
                putExtra(PrayerAlarmReceiver.EXTRA_REMINDER, alarm.isReminder)
                putExtra(PrayerAlarmReceiver.EXTRA_TIME, alarm.prayerTime.toEpochMilli())
            }
            val pi = PendingIntent.getBroadcast(context, alarm.requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            setAlarm(alarm.triggerAt.toEpochMilli(), pi)
        }

        // Yarim tundan keyin keyingi kunni rejalashtirish.
        val midnight = ZonedDateTime.of(today.plusDays(1).atTime(0, 1), config.zone).toInstant().toEpochMilli()
        val midnightIntent = Intent(context, RescheduleReceiver::class.java).setAction(RescheduleReceiver.ACTION_MIDNIGHT)
        setAlarm(midnight, PendingIntent.getBroadcast(context, MIDNIGHT_CODE, midnightIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        PrayerWidget.refresh(context)
    }

    private fun setAlarm(at: Long, pi: PendingIntent) {
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            // Ruxsat berilmagan bo'lsa ham bildirishnoma kelsin (bir necha daqiqa kechikishi mumkin).
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private companion object {
        const val MIDNIGHT_CODE = 999_999
    }
}
