package uz.xumoyun.feature.prayer.notifications

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.data.PrayerSettings

/**
 * Qabul qiluvchilar, WorkManager ishchisi va Glance vidjeti bog'liqliklarni shu yerdan oladi
 * (@AndroidEntryPoint va HiltWorkerFactory kerak emas: WorkManager standart initializer bilan ishlaydi).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PrayerEntryPoint {
    fun scheduler(): PrayerAlarmScheduler
    fun settings(): PrayerSettings
    fun repository(): PrayerRepository
    fun timeSource(): TimeSource
}

internal fun prayerEntryPoint(context: Context): PrayerEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, PrayerEntryPoint::class.java)
