package uz.xumoyun.feature.hifz.plan.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.feature.hifz.plan.domain.HifzRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class HifzRepositoryModule {
    @Binds
    abstract fun bindHifzRepository(impl: RoomHifzRepository): HifzRepository
}
