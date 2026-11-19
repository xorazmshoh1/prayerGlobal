package uz.xumoyun.namoz

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import uz.xumoyun.core.common.notifications.NotificationChannelSource
import uz.xumoyun.core.data.notifications.NotificationChannelRegistrar
import uz.xumoyun.feature.prayer.notifications.PrayerAlarmScheduler
import uz.xumoyun.namoz.notifications.HifzReminderScheduler
import javax.inject.Inject

@HiltAndroidApp
class NamozApp : Application() {
    /** Har modulning bildirishnoma kanallari (namoz, Hifz; keyin Saharlik, Iftor, Islomiy sanalar). */
    @Inject lateinit var channelSources: Set<@JvmSuppressWildcards NotificationChannelSource>
    @Inject lateinit var prayerAlarms: PrayerAlarmScheduler
    @Inject lateinit var hifzReminders: HifzReminderScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationChannelRegistrar.register(this, channelSources)
        // Signallar: hozir va joy/usul/ogohlantirish o'zgarganda; kunlik WorkManager ishi ham shu yerda ro'yxatdan o'tadi.
        prayerAlarms.start(appScope)
        // Hifz eslatmasi: faol reja yoki joy o'zgarganda qayta rejalashtiriladi.
        hifzReminders.start(appScope)
    }
}
