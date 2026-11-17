package uz.xumoyun.namoz

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uz.xumoyun.namoz.data.PrayerRepository
import uz.xumoyun.namoz.notifications.AlarmScheduler
import uz.xumoyun.namoz.notifications.Notifications
import javax.inject.Inject

@HiltAndroidApp
class NamozApp : Application() {
    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var repository: PrayerRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        appScope.launch { scheduler.rescheduleAll() }
        // Joy, usul yoki bildirishnoma sozlamasi o'zgarsa signallarni yangilash.
        appScope.launch {
            repository.config
                .map { it?.let { c -> Triple(c.input, c.prefs.alerts, c.location.id) } }
                .distinctUntilChanged()
                .drop(1)
                .collect { scheduler.rescheduleAll() }
        }
    }
}
