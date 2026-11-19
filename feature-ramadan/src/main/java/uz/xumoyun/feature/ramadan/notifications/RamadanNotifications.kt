package uz.xumoyun.feature.ramadan.notifications

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
import uz.xumoyun.feature.ramadan.R
import uz.xumoyun.feature.ramadan.domain.RamadanReminderKind

/** Saharlik va Iftor kanallari (TZ 8-bo'lim). Qadr eslatmasi "Islomiy sanalar" kanalida (feature-calendar). */
object RamadanChannels : NotificationChannelSource {
    const val SUHOOR = "ramadan_suhoor"
    const val IFTAR = "ramadan_iftar"
    /** feature-calendar'dagi "Islomiy sanalar" kanali ID'si (modullar bog'lanmaydi, faqat ID bo'yicha). */
    const val ISLAMIC_EVENTS = "islamic_events"

    override val channels = listOf(
        ChannelSpec(SUHOOR, R.string.channel_suhoor, ChannelImportance.HIGH, ChannelSound.SystemNotification),
        ChannelSpec(IFTAR, R.string.channel_iftar, ChannelImportance.HIGH, ChannelSound.SystemNotification),
    )
}

@Module
@InstallIn(SingletonComponent::class)
object RamadanNotificationsModule {
    @Provides @IntoSet fun channels(): NotificationChannelSource = RamadanChannels
}

/** Ramazon eslatmasini ko'rsatadi. Ruxsat yo'q bo'lsa jim o'tadi. */
object RamadanNotifier {
    private const val BASE_ID = 3100

    fun show(context: Context, kind: RamadanReminderKind) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val (channel, title, text) = when (kind) {
            RamadanReminderKind.SUHOOR -> Triple(RamadanChannels.SUHOOR, R.string.suhoor_reminder_title, R.string.suhoor_reminder_text)
            RamadanReminderKind.IFTAR -> Triple(RamadanChannels.IFTAR, R.string.iftar_reminder_title, R.string.iftar_reminder_text)
            RamadanReminderKind.QADR -> Triple(RamadanChannels.ISLAMIC_EVENTS, R.string.qadr_reminder_title, R.string.qadr_reminder_text)
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        val pending = PendingIntent.getActivity(
            context, BASE_ID + kind.ordinal, launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            manager.notify(BASE_ID + kind.ordinal, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS tizim darajasida olib qo'yilgan.
        }
    }
}
