package uz.xumoyun.namoz.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import uz.xumoyun.feature.calendar.domain.PrayerDayRow
import uz.xumoyun.feature.calendar.domain.PrayerTimeText
import uz.xumoyun.feature.calendar.domain.PrayerTimetable
import uz.xumoyun.feature.calendar.domain.PrayerTimetablePort
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.ui.TimeFormat
import uz.xumoyun.feature.prayer.ui.prayerName
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Taqvim tabi uchun 30 kunlik namoz jadvali: feature-prayer'dan hisoblanadi (feature-calendar bog'lanmaydi). */
@Singleton
class PrayerTimetableBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PrayerRepository,
) : PrayerTimetablePort {
    override fun timetable(start: Flow<LocalDate>): Flow<PrayerTimetable?> =
        combine(start, repository.config) { from, config -> config to from }.map { (config, from) ->
            config ?: return@map null
            val fmt = TimeFormat.formatter(context)
            val rows = (0 until DAYS).map { i ->
                val date = from.plusDays(i.toLong())
                val day = config.day(date)
                PrayerDayRow(date, Prayer.entries.map { p -> PrayerTimeText(prayerName(p), fmt.format(day[p])) })
            }
            PrayerTimetable(config.location.name, rows)
        }

    private companion object {
        const val DAYS = 30
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarBridgeModule {
    @Binds abstract fun timetable(impl: PrayerTimetableBridge): PrayerTimetablePort
}
