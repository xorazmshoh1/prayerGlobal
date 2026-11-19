package uz.xumoyun.namoz.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uz.xumoyun.feature.hifz.HifzReminderNotifier

/**
 * Hifz eslatmasi signali (exported=false). Eslatmani ko'rsatadi va keyingisini rejalashtiradi; qayta yoqilganda
 * (BOOT, paket yangilanishi, vaqt o'zgarishi) ham rejalashtiradi, chunki AlarmManager signallari o'chiriladi.
 */
@AndroidEntryPoint
class HifzReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: HifzReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val pending = goAsync()
        scope.launch {
            try {
                if (action == HifzReminderScheduler.ACTION_ALARM) HifzReminderNotifier.show(context)
                scheduler.reschedule()
            } catch (e: Exception) {
                Log.w(TAG, "receiver", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "HifzReminder"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
