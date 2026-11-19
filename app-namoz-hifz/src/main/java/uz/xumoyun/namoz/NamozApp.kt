package uz.xumoyun.namoz

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uz.xumoyun.core.common.crash.CrashReportGate
import uz.xumoyun.core.common.notifications.NotificationChannelSource
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.data.notifications.NotificationChannelRegistrar
import uz.xumoyun.feature.prayer.notifications.PrayerAlarmScheduler
import uz.xumoyun.namoz.notifications.HifzReminderScheduler
import uz.xumoyun.namoz.notifications.IslamicReminderScheduler
import javax.inject.Inject

@HiltAndroidApp
class NamozApp : Application() {
    /** Har modulning bildirishnoma kanallari (namoz, Hifz; keyin Saharlik, Iftor, Islomiy sanalar). */
    @Inject lateinit var channelSources: Set<@JvmSuppressWildcards NotificationChannelSource>
    @Inject lateinit var prayerAlarms: PrayerAlarmScheduler
    @Inject lateinit var hifzReminders: HifzReminderScheduler
    @Inject lateinit var islamicReminders: IslamicReminderScheduler
    @Inject lateinit var crashGate: CrashReportGate
    @Inject lateinit var common: CommonSettings

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationChannelRegistrar.register(this, channelSources)
        // Signallar: hozir va joy/usul/ogohlantirish o'zgarganda; kunlik WorkManager ishi ham shu yerda ro'yxatdan o'tadi.
        prayerAlarms.start(appScope)
        // Hifz eslatmasi: faol reja yoki joy o'zgarganda qayta rejalashtiriladi.
        hifzReminders.start(appScope)
        // Islomiy sana eslatmasi: sozlama yoki hijriy tuzatish o'zgarganda qayta rejalashtiriladi.
        islamicReminders.start(appScope)
        // TZ 8: xato hisoboti sozlamasi keshlanadi (uncaught handler sinxron ishlaydi).
        appScope.launch { common.crashReportingEnabled.collect { crashGate.enabled = it } }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            crashGate.report(error)
            previous?.uncaughtException(thread, error)
        }
    }
}
