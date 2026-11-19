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
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.data.HijriProvider
import uz.xumoyun.feature.calendar.domain.EventPrefsStore
import uz.xumoyun.feature.calendar.domain.EventSetting
import uz.xumoyun.feature.calendar.domain.IslamicEvent
import uz.xumoyun.feature.calendar.domain.IslamicReminderPlanner
import uz.xumoyun.feature.prayer.domain.AlarmDelivery
import uz.xumoyun.feature.prayer.domain.AlarmKind
import uz.xumoyun.feature.prayer.domain.AlarmPlanner
import uz.xumoyun.feature.prayer.notifications.ExactAlarms

/**
 * Islomiy sana eslatmasi (TZ 7-bo'lim, EventPrefs): keyingi bitta signal navbatda, u ishlaganda keyingisi qo'yiladi.
 * Vaqt qoidasi [IslamicReminderPlanner] da (sof, testlangan); signal turi [AlarmPlanner] bilan bir xil.
 * Feature modullar bog'lanmasligi uchun bu klass app modulida.
 */
@Singleton
class IslamicReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val events: EventPrefsStore,
    private val common: CommonSettings,
    private val hijri: HijriProvider,
    private val time: TimeSource,
) {
    private val mutex = Mutex()
    private val alarmManager: AlarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** NamozApp.onCreate dan: sozlama yoki hijriy tuzatish o'zgarganda qayta rejalashtiradi. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(events.settings, common.hijriAdjustDays) { _, _ -> Unit }
                .collect { runCatching { reschedule() }.onFailure { Log.w(TAG, "reschedule", it) } }
        }
    }

    suspend fun reschedule() = mutex.withLock {
        cancel()
        val settings = events.settings.first()
        val offset = common.hijriAdjustDays.first()
        val now = time.now()
        val next = IslamicReminderPlanner.next(now, time.zone(), hijri.calendar(offset)) { settings[it] ?: EventSetting.default(it) }
            ?: return@withLock
        val intent = Intent(context, IslamicReminderReceiver::class.java)
            .setAction(ACTION_ALARM)
            .putExtra(EXTRA_EVENT, next.event.key)
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, UPDATE_FLAGS)
        val delivery = AlarmPlanner.delivery(AlarmKind.AT_TIME, next.fireAt, now, ExactAlarms.canSchedule(context))
        schedule(next.fireAt.toEpochMilli(), delivery, pending)
    }

    private fun schedule(at: Long, delivery: AlarmDelivery, operation: PendingIntent) {
        when (delivery) {
            AlarmDelivery.Exact -> try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            } catch (e: SecurityException) {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, at, AlarmPlanner.WINDOW.toMillis(), operation)
            }
            is AlarmDelivery.Window -> alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP, delivery.start.toEpochMilli(), delivery.length.toMillis(), operation,
            )
        }
    }

    private fun cancel() {
        PendingIntent.getBroadcast(
            context, REQUEST_CODE,
            Intent(context, IslamicReminderReceiver::class.java).setAction(ACTION_ALARM),
            NO_CREATE_FLAGS,
        )?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    companion object {
        const val ACTION_ALARM = "uz.xumoyun.namoz.action.ISLAMIC_REMINDER"
        const val EXTRA_EVENT = "event_key"
        /** Namoz (0..15999), vidjet (20000), Hifz (30001) kodlaridan farqli. */
        const val REQUEST_CODE = 40_001
        private const val TAG = "IslamicReminder"
        private const val UPDATE_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        private const val NO_CREATE_FLAGS = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    }
}

/** Islomiy sana signali: eslatmani ko'rsatadi, keyingisini rejalashtiradi; qayta yoqilganda ham. */
@dagger.hilt.android.AndroidEntryPoint
class IslamicReminderReceiver : android.content.BroadcastReceiver() {
    @Inject lateinit var scheduler: IslamicReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val key = intent.getStringExtra(IslamicReminderScheduler.EXTRA_EVENT)
        scope.launch {
            try {
                if (intent.action == IslamicReminderScheduler.ACTION_ALARM) {
                    uz.xumoyun.feature.calendar.domain.IslamicEvent.byKey(key.orEmpty())?.let {
                        uz.xumoyun.feature.calendar.notifications.IslamicEventNotifier.show(context, it)
                    }
                }
                scheduler.reschedule()
            } catch (e: Exception) {
                Log.w(TAG, "receiver", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "IslamicReminder"
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    }
}
