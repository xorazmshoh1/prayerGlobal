package uz.xumoyun.feature.prayer.tracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.feature.prayer.tracker.data.DataStoreTrackerPrefs
import uz.xumoyun.feature.prayer.tracker.data.PrayerMadhabSource
import uz.xumoyun.feature.prayer.tracker.data.RoomTrackerStore
import uz.xumoyun.feature.prayer.tracker.domain.MadhabSource
import uz.xumoyun.feature.prayer.tracker.domain.TrackerPrefs
import uz.xumoyun.feature.prayer.tracker.domain.TrackerStore

/** Namoz kuzatuvchisi/qazo bog'lamalari. TrackerDao va TransactionRunner ilova modulida (Room bazasi). */
@Module
@InstallIn(SingletonComponent::class)
abstract class TrackerModule {
    @Binds abstract fun store(impl: RoomTrackerStore): TrackerStore
    @Binds abstract fun prefs(impl: DataStoreTrackerPrefs): TrackerPrefs
    @Binds abstract fun madhab(impl: PrayerMadhabSource): MadhabSource
}
