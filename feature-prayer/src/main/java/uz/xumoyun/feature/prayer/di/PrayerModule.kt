package uz.xumoyun.feature.prayer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.feature.prayer.data.AndroidLocationProvider
import uz.xumoyun.feature.prayer.data.LocationProvider

/** LocationDao ni ilova moduli beradi (Room bazasi app-namoz-hifz da). */
@Module
@InstallIn(SingletonComponent::class)
abstract class PrayerModule {
    @Binds abstract fun location(impl: AndroidLocationProvider): LocationProvider
}
