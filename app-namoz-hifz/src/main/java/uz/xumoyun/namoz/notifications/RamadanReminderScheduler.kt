package uz.xumoyun.namoz.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.data.HijriProvider
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.domain.AlarmDelivery
import uz.xumoyun.feature.prayer.domain.AlarmKind
import uz.xumoyun.feature.prayer.domain.AlarmPlanner
import uz.xumoyun.feature.prayer.notifications.ExactAlarms
import uz.xumoyun.feature.ramadan.data.RamadanPrefs
import uz.xumoyun.feature.ramadan.domain.RamadanCalc
import uz.xumoyun.feature.ramadan.domain.RamadanReminder
import uz.xumoyun.feature.ramadan.domain.RamadanReminderKind
import uz.xumoyun.feature.ramadan.domain.RamadanReminderPlanner
import uz.xumoyun.feature.ramadan.domain.RamadanSchedulePort
import uz.xumoyun.feature.ramadan.notifications.RamadanNotifier

/**
 * Ramazon eslatmalari (saharlik, iftor, qadr): keyingi bitta signal, u ishlaganda keyingisi (TZ 11-bo'lim).
 * Vaqt qoidasi [RamadanReminderPlanner] da; signal turi [AlarmPlanner] bilan bir xil.
 */
@Singleton
class RamadanReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hijri: HijriProvider,
    private val common: CommonSettings,
    private val prefs: RamadanPrefs,
    private val schedulePort: RamadanSchedulePort,
    private val prayers: PrayerRepository,
    private val time: TimeSource,
) {
    private val mutex = Mutex()
    private val alarmManager: AlarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** NamozApp.onCreate dan: sozlama, joy yoki hijriy tuzatish o'zgarganda qayta rejalashtiradi. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(prefs.settings, common.hijriAdjustDays, prayers.config) { _, _, _ -> Unit }
                .collect { runCatching { reschedule() }.onFailure { Log.w(TAG, "reschedule", it) } }
        }
    }

    suspend fun reschedule() = mutex.withLock {
        cancel()
        val now = time.now()
        val today = time.today()
        val offset = common.hijriAdjustDays.first()
        val cal = hijri.calendar(offset)
        val start = RamadanCalc.upcomingStart(today, cal) ?: return@withLock
        val sched = schedulePort.days(flowOf(start), RamadanCalc.DAYS).first() ?: return@withLock
        val byDate = sched.days.associateBy { it.date }
        val settings = prefs.settings.first()
        val next: RamadanReminder = RamadanReminderPlanner.next(now, sched.zone, cal, { byDate[it] }, settings) ?: return@withLock

        val intent = Intent(context, RamadanReminderReceiver::class.java)
            .setAction(ACTION_ALARM)
            .putExtra(EXTRA_KIND, next.kind.name)
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, UPDATE_FLAGS)
        val fireAt = next.fireAt
        val delivery = AlarmPlanner.delivery(AlarmKind.AT_TIME, fireAt, now, ExactAlarms.canSchedule(context))
        schedule(fireAt.toEpochMilli(), delivery, pending)
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
            Intent(context, RamadanReminderReceiver::class.java).setAction(ACTION_ALARM),
            NO_CREATE_FLAGS,
        )?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    companion object {
        const val ACTION_ALARM = "uz.xumoyun.namoz.action.RAMADAN_REMINDER"
        const val EXTRA_KIND = "ramadan_kind"
        /** Namoz (0..15999), vidjet (20000), Hifz (30001), islomiy sana (40001). */
        const val REQUEST_CODE = 40_010
        private const val TAG = "RamadanReminder"
        private const val UPDATE_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        private const val NO_CREATE_FLAGS = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    }
}

/** Ramazon eslatmasi signali (exported=false): ko'rsatadi va keyingisini rejalashtiradi. */
@AndroidEntryPoint
class RamadanReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: RamadanReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val kind = intent.getStringExtra(RamadanReminderScheduler.EXTRA_KIND)
            ?.let { runCatching { RamadanReminderKind.valueOf(it) }.getOrNull() }
        scope.launch {
            try {
                if (intent.action == RamadanReminderScheduler.ACTION_ALARM && kind != null) {
                    RamadanNotifier.show(context, kind)
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
        const val TAG = "RamadanReminder"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
