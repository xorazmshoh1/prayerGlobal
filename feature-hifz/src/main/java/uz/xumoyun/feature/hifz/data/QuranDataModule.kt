package uz.xumoyun.feature.hifz.data

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import uz.xumoyun.feature.hifz.domain.QuranSource

@Module
@InstallIn(SingletonComponent::class)
abstract class QuranDataModule {

    @Binds
    abstract fun bindQuranSource(impl: RoomQuranSource): QuranSource

    companion object {
        /** Ochilish kechiktirilgan: baza faqat birinchi so'rovda ochiladi. */
        @Provides
        @Singleton
        fun provideQuranDatabase(@ApplicationContext context: Context): QuranDatabase =
            Room.databaseBuilder(context, QuranDatabase::class.java, QuranDatabase.ASSET_NAME)
                .createFromAsset(QuranDatabase.ASSET_NAME)
                .build()
    }
}
