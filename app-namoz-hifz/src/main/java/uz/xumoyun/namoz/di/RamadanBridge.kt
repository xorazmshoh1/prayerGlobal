package uz.xumoyun.namoz.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import uz.xumoyun.core.common.hijri.HijriMonth
import uz.xumoyun.core.data.HijriProvider
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.domain.RamadanCheck
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.tracker.data.TrackerRepository
import uz.xumoyun.feature.prayer.tracker.domain.QadaPrayer
import uz.xumoyun.feature.ramadan.domain.RamadanDayTimes
import uz.xumoyun.feature.ramadan.domain.RamadanQadaPort
import uz.xumoyun.feature.ramadan.domain.RamadanSchedule
import uz.xumoyun.feature.ramadan.domain.RamadanSchedulePort
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Ramazon uchun Bomdod/Shom vaqtlari: feature-prayer hisobi (feature-ramadan bog'lanmaydi). */
@Singleton
class RamadanScheduleBridge @Inject constructor(
    private val repository: PrayerRepository,
) : RamadanSchedulePort {
    override fun days(start: Flow<LocalDate>, count: Int): Flow<RamadanSchedule?> =
        combine(start, repository.config) { from, config -> from to config }.map { (from, config) ->
            config?.let { c ->
                RamadanSchedule(
                    city = c.location.name,
                    zone = c.zone,
                    days = (0 until count).map { i ->
                        val date = from.plusDays(i.toLong())
                        val day = c.day(date)
                        RamadanDayTimes(date, day[Prayer.FAJR].toInstant(), day[Prayer.MAGHRIB].toInstant())
                    },
                )
            }
        }
}

/** Ro'za qazosi: feature-prayer'dagi QadaCounter(FAST). */
@Singleton
class RamadanQadaBridge @Inject constructor(
    private val tracker: TrackerRepository,
) : RamadanQadaPort {
    override fun fastQada(): Flow<Int> = tracker.counters.map { list ->
        list.firstOrNull { it.prayer == QadaPrayer.FAST }?.remaining ?: 0
    }

    override suspend fun adjustFast(delta: Int) {
        tracker.adjust(QadaPrayer.FAST, delta, System.currentTimeMillis())
    }
}

/**
 * Ramazon kuni (hijriy oy = 9) — Umm al-Qura, sozlamadagi tuzatish bilan. [offset] NamozApp'dagi kuzatuvchi
 * tomonidan yangilanadi (sinxron o'qish uchun); prayer hisobi har chaqiruvda shu qiymatni ishlatadi.
 */
@Singleton
class HijriRamadanCheck @Inject constructor(
    private val hijri: HijriProvider,
) : RamadanCheck {
    @Volatile var offset: Int = 0

    override fun isRamadan(date: LocalDate): Boolean =
        hijri.calendar(offset).ymd(date).month == HijriMonth.RAMADAN
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RamadanBridgeModule {
    @Binds abstract fun schedule(impl: RamadanScheduleBridge): RamadanSchedulePort
    @Binds abstract fun qada(impl: RamadanQadaBridge): RamadanQadaPort
    @Binds abstract fun ramadanCheck(impl: HijriRamadanCheck): RamadanCheck
}
