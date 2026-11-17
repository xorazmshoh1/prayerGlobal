package uz.xumoyun.namoz.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uz.xumoyun.namoz.data.NamozSettings
import uz.xumoyun.namoz.data.enumOrNull
import uz.xumoyun.namoz.domain.Prayer
import uz.xumoyun.namoz.widget.PrayerWidget
import java.time.Instant
import javax.inject.Inject

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try { block() } finally { pending.finish() }
    }
}

/** Namoz vaqti (yoki oldindan eslatma) kelganda bildirishnoma chiqaradi. */
@AndroidEntryPoint
class PrayerAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var settings: NamozSettings

    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra(EXTRA_PRAYER)?.let { enumOrNull<Prayer>(it) } ?: return
        val reminder = intent.getBooleanExtra(EXTRA_REMINDER, false)
        val time = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_TIME, System.currentTimeMillis()))
        runAsync {
            val alert = settings.current().alerts.getValue(prayer)
            if (alert.enabled) {
                Notifications.show(context, prayer, time, reminder, alert.sound, alert.minutesBefore)
            }
            if (!reminder) PrayerWidget.refresh(context)
        }
    }

    companion object {
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_REMINDER = "reminder"
        const val EXTRA_TIME = "time"
    }
}

/** Qayta yuklash, vaqt/mintaqa o'zgarishi va yarim tunda signallarni qayta qo'yadi. */
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        runAsync { scheduler.rescheduleAll() }
    }

    companion object {
        const val ACTION_MIDNIGHT = "uz.xumoyun.namoz.action.MIDNIGHT"
    }
}
