package uz.xumoyun.namoz.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.feature.hifz.plan.domain.HifzRepository
import uz.xumoyun.feature.hifz.plan.domain.HifzReminder
import uz.xumoyun.feature.hifz.plan.domain.nextHifzReminder
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.domain.AlarmDelivery
import uz.xumoyun.feature.prayer.domain.AlarmKind
import uz.xumoyun.feature.prayer.domain.AlarmPlanner
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.notifications.ExactAlarms

/**
 * Hifz eslatmasi (TZ 5-bo'lim 16-band): faol rejadagi namoz vaqti + X daqiqa. Vaqt qoidasi —
 * [nextHifzReminder] (sof, testlangan); signal turi va oynasi feature-prayer'dagi [AlarmPlanner] qoidasi bilan
 * (aniq ruxsat bo'lsa aniq signal, aks holda ±5 daqiqa oyna). Bitta signal navbatda turadi; u ishlaganda
 * keyingisi qo'yiladi ([HifzReminderReceiver]). Feature modullar bir-biriga bog'lanmasligi uchun bu klass
 * app modulida: hifz rejasi va namoz vaqtini u ulaydi.
 */
@Singleton
class HifzReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hifz: HifzRepository,
    private val prayers: PrayerRepository,
    private val time: TimeSource,
) {
    private val mutex = Mutex()
    private val alarmManager: AlarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** NamozApp.onCreate dan: faol reja yoki joy o'zgarganda qayta rejalashtiradi. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(hifz.observeActivePlan(), prayers.config) { _, _ -> Unit }
                .collect { runCatching { reschedule() }.onFailure { Log.w(TAG, "reschedule", it) } }
        }
    }

    suspend fun reschedule() {
      mutex.withLock {
        cancel()
        val plan = hifz.observeActivePlan().first() ?: return
        val prayerKey = plan.reminderAfterPrayer ?: return
        if (prayerKey !in HifzReminder.PRAYER_KEYS) return
        val config = prayers.currentConfig() ?: return
        val now = time.now()
        val trigger = nextHifzReminder(now, config.zone, prayerKey, plan.reminderOffsetMin) { date, key ->
            runCatching { config.day(date)[Prayer.valueOf(key)].toInstant() }.getOrNull()
        } ?: return
        val intent = Intent(context, HifzReminderReceiver::class.java).setAction(ACTION_ALARM)
        val pending = PendingIntent.getBroadcast(context, HifzReminder.REQUEST_CODE, intent, UPDATE_FLAGS)
        schedule(trigger.toEpochMilli(), AlarmPlanner.delivery(AlarmKind.AT_TIME, trigger, now, ExactAlarms.canSchedule(context)), pending)
      }
    }

    private fun schedule(at: Long, delivery: AlarmDelivery, operation: PendingIntent) {
        when (delivery) {
            AlarmDelivery.Exact -> try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            } catch (e: SecurityException) {
                // Ruxsat tekshiruvdan keyin olib qo'yilgan: oyna bilan zaxira yo'l.
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, at, AlarmPlanner.WINDOW.toMillis(), operation)
            }
            is AlarmDelivery.Window -> alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                delivery.start.toEpochMilli(),
                delivery.length.toMillis(),
                operation,
            )
        }
    }

    private fun cancel() {
        PendingIntent.getBroadcast(
            context,
            HifzReminder.REQUEST_CODE,
            Intent(context, HifzReminderReceiver::class.java).setAction(ACTION_ALARM),
            NO_CREATE_FLAGS,
        )?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    companion object {
        const val ACTION_ALARM = "uz.xumoyun.namoz.action.HIFZ_REMINDER"
        private const val TAG = "HifzReminder"
        private const val UPDATE_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        private const val NO_CREATE_FLAGS = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    }
}
