package uz.xumoyun.feature.hifz

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
 * "Hifz eslatmasi" kanali (TZ 8-bo'lim). Eslatmaning o'zi — tanlangan namozdan keyin
 * (masalan Bomdoddan 15 daqiqa keyin) — 8-bosqichda shu kanalga yuboriladi.
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
