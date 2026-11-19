package uz.xumoyun.feature.calendar.notifications

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
import uz.xumoyun.feature.calendar.R
import uz.xumoyun.feature.calendar.domain.IslamicEvent

/** "Islomiy sanalar" kanali (TZ 8-bo'lim). Eslatma vaqtini app moduli rejalashtiradi. */
object IslamicChannels : NotificationChannelSource {
    const val EVENTS = "islamic_events"

    override val channels = listOf(
        ChannelSpec(EVENTS, R.string.channel_islamic_events, ChannelImportance.DEFAULT, ChannelSound.SystemNotification),
    )
}

@Module
@InstallIn(SingletonComponent::class)
object IslamicNotificationsModule {
    @Provides @IntoSet fun channels(): NotificationChannelSource = IslamicChannels
}

/** Islomiy sana eslatmasini ko'rsatadi (sanadan 1 kun oldin). Ruxsat yo'q bo'lsa jim o'tadi. */
object IslamicEventNotifier {
    const val NOTIFICATION_ID = 3010

    fun nameRes(event: IslamicEvent): Int = when (event) {
        IslamicEvent.MUHARRAM_1 -> R.string.event_muharram_1
        IslamicEvent.ASHURA -> R.string.event_ashura
        IslamicEvent.RAJAB_27 -> R.string.event_rajab_27
        IslamicEvent.SHABAN_15 -> R.string.event_shaban_15
        IslamicEvent.RAMADAN_1 -> R.string.event_ramadan_1
        IslamicEvent.LAYLAT_AL_QADR -> R.string.event_laylat_al_qadr
        IslamicEvent.EID_AL_FITR -> R.string.event_eid_al_fitr
        IslamicEvent.ARAFA -> R.string.event_arafa
        IslamicEvent.EID_AL_ADHA -> R.string.event_eid_al_adha
    }

    fun show(context: Context, event: IslamicEvent) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        val pending = PendingIntent.getActivity(
            context, NOTIFICATION_ID, launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, IslamicChannels.EVENTS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.islamic_reminder_title))
            .setContentText(context.getString(R.string.islamic_reminder_text, context.getString(nameRes(event))))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            manager.notify(NOTIFICATION_ID + event.ordinal, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS tizim darajasida olib qo'yilgan.
        }
    }
}
