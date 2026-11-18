package uz.xumoyun.feature.prayer.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uz.xumoyun.core.common.enumOrNull
import uz.xumoyun.feature.prayer.domain.AlarmKind
import uz.xumoyun.feature.prayer.domain.AlarmPlanner
import uz.xumoyun.feature.prayer.domain.Prayer
import java.time.Instant
import java.time.ZoneId

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** goAsync: ish ~10 soniyadan oshmaydi (DataStore o'qish va bir necha kunlik hisob). */
private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("PrayerAlarms", "receiver", e)
        } finally {
            pending.finish()
        }
    }
}

/**
 * Ichki signallar (exported=false): namoz vaqti / oldindan eslatma va vaqt kirganda vidjetni yangilash.
 * Rejim signal kelgan paytdagi sozlamadan olinadi.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            ACTION_ALARM -> {
                val prayer = intent.getStringExtra(EXTRA_PRAYER)?.let { enumOrNull<Prayer>(it) } ?: return
                val kind = intent.getStringExtra(EXTRA_KIND)?.let { enumOrNull<AlarmKind>(it) } ?: return
                val prayerTime = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_PRAYER_TIME, 0L))
                val zone = intent.getStringExtra(EXTRA_ZONE)?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                    ?: ZoneId.systemDefault()
                runAsync {
                    val entry = prayerEntryPoint(app)
                    val alert = entry.settings().current().alerts.getValue(prayer)
                    val now = entry.timeSource().now()
                    if (alert.enabled && AlarmPlanner.shouldNotify(now, kind, prayerTime)) {
                        PrayerNotifier.show(
                            app, prayer, kind, alert.mode, prayerTime.atZone(zone),
                            AlarmPlanner.minutesUntil(now, prayerTime),
                        )
                    }
                }
            }
            ACTION_REFRESH -> runAsync { prayerEntryPoint(app).scheduler().reschedule() }
        }
    }

    companion object {
        const val ACTION_ALARM = "uz.xumoyun.feature.prayer.action.PRAYER_ALARM"
        const val ACTION_REFRESH = "uz.xumoyun.feature.prayer.action.REFRESH"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_KIND = "kind"
        const val EXTRA_PRAYER_TIME = "prayer_time"
        const val EXTRA_ZONE = "zone"
    }
}

/**
 * Tizim hodisalari (TZ 8-bo'lim): qayta yoqilish, vaqt mintaqasi va soat o'zgarishi, ilova yangilanishi,
 * aniq signal ruxsati berilishi — signallar qayta qo'yiladi (AlarmManager ularni qayta yoqilganda o'chiradi).
 */
class PrayerRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SYSTEM_ACTIONS) return
        val app = context.applicationContext
        runAsync {
            val entry = prayerEntryPoint(app)
            // Soat yoki mintaqa o'zgarsa kunlik ishning "yarim tun" kechikishi qayta hisoblanadi.
            DailyRescheduleWorker.enqueue(app, entry.timeSource(), replace = action in CLOCK_ACTIONS)
            entry.scheduler().reschedule()
        }
    }

    companion object {
        /** AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (API 31). */
        const val ACTION_EXACT_ALARM_PERMISSION_CHANGED = "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

        private val CLOCK_ACTIONS = setOf(Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED)
        private val SYSTEM_ACTIONS = CLOCK_ACTIONS + setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_EXACT_ALARM_PERMISSION_CHANGED,
        )
    }
}
