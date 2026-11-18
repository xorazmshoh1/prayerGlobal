package uz.xumoyun.feature.prayer.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import uz.xumoyun.core.common.notifications.NotificationChannelSource
import uz.xumoyun.feature.prayer.data.AndroidLocationProvider
import uz.xumoyun.feature.prayer.data.LocationProvider
import uz.xumoyun.feature.prayer.notifications.PrayerChannels

/** LocationDao ni ilova moduli beradi (Room bazasi app-namoz-hifz da). */
@Module
@InstallIn(SingletonComponent::class)
abstract class PrayerModule {
    @Binds abstract fun location(impl: AndroidLocationProvider): LocationProvider
}

/** 5 ta namoz kanali va eslatmalar kanali ilovaning umumiy kanal ro'yxatiga qo'shiladi. */
@Module
@InstallIn(SingletonComponent::class)
object PrayerNotificationsModule {
    @Provides @IntoSet fun channels(): NotificationChannelSource = PrayerChannels
}
