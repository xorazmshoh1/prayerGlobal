package uz.xumoyun.feature.qibla.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.feature.qibla.data.CompassSource
import uz.xumoyun.feature.qibla.data.SensorCompassSource

@Module
@InstallIn(SingletonComponent::class)
abstract class QiblaModule {
    @Binds
    abstract fun compass(impl: SensorCompassSource): CompassSource
}
