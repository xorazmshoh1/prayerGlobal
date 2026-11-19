package uz.xumoyun.namoz.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.xumoyun.core.data.place.SelectedPlace
import uz.xumoyun.core.data.place.SelectedPlaceSource
import uz.xumoyun.feature.prayer.data.PrayerRepository
import javax.inject.Inject

/**
 * Feature modullar bir-biriga bog'lanmaydi: tanlangan joy (feature-prayer) qibla moduliga shu ko'prik orqali beriladi.
 */
class PrayerPlaceSource @Inject constructor(
    repository: PrayerRepository,
) : SelectedPlaceSource {
    override val place: Flow<SelectedPlace?> = repository.config.map { config ->
        config?.location?.let { SelectedPlace(it.name, it.latitude, it.longitude) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaceBridgeModule {
    @Binds
    abstract fun selectedPlace(impl: PrayerPlaceSource): SelectedPlaceSource
}
