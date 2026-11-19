package uz.xumoyun.feature.hifz

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import uz.xumoyun.core.common.notifications.ChannelImportance
import uz.xumoyun.core.common.notifications.ChannelSound
import uz.xumoyun.core.common.notifications.ChannelSpec
import uz.xumoyun.core.common.notifications.NotificationChannelSource

/**
 * "Hifz eslatmasi" kanali (TZ 8-bo'lim). Eslatma vaqtini app moduli (HifzReminderScheduler) belgilaydi;
 * bu yerda faqat kanal va bildirishnomaning o'zi.
 */
object HifzChannels : NotificationChannelSource {
    const val REMINDER = "hifz_reminder"

    override val channels = listOf(
        ChannelSpec(REMINDER, R.string.channel_hifz_reminder, ChannelImportance.DEFAULT, ChannelSound.SystemNotification),
    )
}

@Module
@InstallIn(SingletonComponent::class)
object HifzNotificationsModule {
    @Provides @IntoSet fun channels(): NotificationChannelSource = HifzChannels
}

/** Hifz eslatmasini ko'rsatadi. Bildirishnoma ruxsati yo'q bo'lsa jim o'tadi. */
object HifzReminderNotifier {
    const val NOTIFICATION_ID = 3001

    fun show(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        val pending = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, HifzChannels.REMINDER)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.hifz_reminder_title))
            .setContentText(context.getString(R.string.hifz_reminder_text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS ruxsati tizim darajasida olib qo'yilgan.
        }
    }
}
