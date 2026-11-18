package uz.xumoyun.feature.prayer.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.feature.prayer.domain.DailyReschedule
import java.util.concurrent.TimeUnit

/**
 * TZ 8-bo'lim: har kuni yarim tundan keyin keyingi 2 kunlik signallarni qayta hisoblaydi.
 * Davriy ish (24 soat), birinchi ishga tushish — keyingi 00:05 gacha. Standart WorkerFactory
 * (konstruktor: Context, WorkerParameters), bog'liqliklar [PrayerEntryPoint] dan.
 */
class DailyRescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        prayerEntryPoint(applicationContext).scheduler().reschedule()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "prayer-daily-reschedule"

        /** [replace] — soat/mintaqa o'zgarganda kechikishni qayta hisoblash; aks holda mavjud ish saqlanadi. */
        fun enqueue(context: Context, time: TimeSource, replace: Boolean) {
            val delay = DailyReschedule.delayUntilNext(time.now().atZone(time.zone()))
            val request = PeriodicWorkRequestBuilder<DailyRescheduleWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
