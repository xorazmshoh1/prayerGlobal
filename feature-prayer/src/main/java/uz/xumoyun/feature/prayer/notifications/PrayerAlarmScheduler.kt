package uz.xumoyun.feature.prayer.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.domain.AlarmDelivery
import uz.xumoyun.feature.prayer.domain.AlarmPlanner
import uz.xumoyun.feature.prayer.domain.CalcInput
import uz.xumoyun.feature.prayer.domain.PlannedAlarm
import uz.xumoyun.feature.prayer.domain.PlannedRefresh
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.domain.PrayerAlert
import uz.xumoyun.feature.prayer.widget.PrayerWidget
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Azon va eslatma signallarini AlarmManager'ga qo'yadi (TZ 8-bo'lim). Nimani qo'yish — [AlarmPlanner]
 * (sof Kotlin, testlangan); bu klass faqat rejani AlarmManager'ga o'tkazadi.
 *
 * Qayta hisoblash: ilova ishga tushganda va joy/usul/ogohlantirish sozlamasi o'zgarganda ([start]),
 * kunlik WorkManager ([DailyRescheduleWorker]), BOOT_COMPLETED / TIMEZONE_CHANGED / TIME_SET /
 * MY_PACKAGE_REPLACED / aniq signal ruxsati o'zgarganda ([PrayerRescheduleReceiver]) va har vaqt kirganda
 * (vidjetni yangilash signali, [PrayerAlarmReceiver.ACTION_REFRESH]).
 */
@Singleton
class PrayerAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PrayerRepository,
    private val time: TimeSource,
) {
    private val mutex = Mutex()
    private val alarmManager: AlarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private data class Inputs(val input: CalcInput, val alerts: Map<Prayer, PrayerAlert>)

    /** NamozApp.onCreate dan: kunlik ishni ro'yxatdan o'tkazadi va sozlamalarni kuzatadi (birinchi qiymat ham). */
    fun start(scope: CoroutineScope) {
        scope.launch {
            runCatching { DailyRescheduleWorker.enqueue(context, time, replace = false) }
                .onFailure { Log.w(TAG, "WorkManager", it) }
            repository.config
                .map { c -> c?.let { Inputs(it.input, it.prefs.alerts) } }
                .distinctUntilChanged()
                .collect { runCatching { reschedule() }.onFailure { Log.w(TAG, "reschedule", it) } }
        }
    }

    suspend fun reschedule() = mutex.withLock {
        val now = time.now()
        val config = repository.currentConfig()
        val zone = config?.zone ?: time.zone()
        val plan = if (config == null) {
            AlarmPlanner.cancelAll(now, zone)
        } else {
            AlarmPlanner.plan(now, zone, config.prefs.alerts, ExactAlarms.canSchedule(context), config::day)
        }
        plan.staleCodes.forEach(::cancel)
        plan.alarms.forEach { set(it, zone) }
        val refresh = plan.refresh
        if (refresh != null) setRefresh(refresh) else cancelRefresh()
        PrayerWidget.refresh(context)
    }

    private fun alarmIntent() = Intent(context, PrayerAlarmReceiver::class.java).setAction(PrayerAlarmReceiver.ACTION_ALARM)
    private fun refreshIntent() = Intent(context, PrayerAlarmReceiver::class.java).setAction(PrayerAlarmReceiver.ACTION_REFRESH)

    private fun set(alarm: PlannedAlarm, zone: ZoneId) {
        val intent = alarmIntent()
            .putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, alarm.prayer.name)
            .putExtra(PrayerAlarmReceiver.EXTRA_KIND, alarm.kind.name)
            .putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, alarm.prayerTime.toEpochMilli())
            .putExtra(PrayerAlarmReceiver.EXTRA_ZONE, zone.id)
        val pending = PendingIntent.getBroadcast(context, alarm.requestCode, intent, UPDATE_FLAGS)
        schedule(wakeup = true, at = alarm.triggerAt, delivery = alarm.delivery, operation = pending)
    }

    private fun setRefresh(refresh: PlannedRefresh) {
        val pending = PendingIntent.getBroadcast(context, AlarmPlanner.REFRESH_REQUEST_CODE, refreshIntent(), UPDATE_FLAGS)
        // Uyg'otmaydigan signal: ekran o'chiq bo'lsa, telefon uyg'onganda yetkaziladi.
        schedule(wakeup = false, at = refresh.at, delivery = refresh.delivery, operation = pending)
    }

    private fun schedule(wakeup: Boolean, at: Instant, delivery: AlarmDelivery, operation: PendingIntent) {
        val type = if (wakeup) AlarmManager.RTC_WAKEUP else AlarmManager.RTC
        when (delivery) {
            AlarmDelivery.Exact -> try {
                if (wakeup) {
                    alarmManager.setExactAndAllowWhileIdle(type, at.toEpochMilli(), operation)
                } else {
                    alarmManager.setExact(type, at.toEpochMilli(), operation)
                }
            } catch (e: SecurityException) {
                // Ruxsat tekshiruvdan keyin olib qo'yilgan: oyna bilan zaxira yo'l.
                alarmManager.setWindow(type, at.toEpochMilli(), AlarmPlanner.WINDOW.toMillis(), operation)
            }
            is AlarmDelivery.Window ->
                alarmManager.setWindow(type, delivery.start.toEpochMilli(), delivery.length.toMillis(), operation)
        }
    }

    private fun cancel(requestCode: Int) {
        PendingIntent.getBroadcast(context, requestCode, alarmIntent(), NO_CREATE_FLAGS)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun cancelRefresh() {
        PendingIntent.getBroadcast(context, AlarmPlanner.REFRESH_REQUEST_CODE, refreshIntent(), NO_CREATE_FLAGS)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private companion object {
        const val TAG = "PrayerAlarms"
        const val UPDATE_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        const val NO_CREATE_FLAGS = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    }
}
