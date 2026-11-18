package uz.xumoyun.namoz.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.feature.prayer.data.LocationDao
import uz.xumoyun.namoz.data.db.NamozDatabase
import uz.xumoyun.namoz.data.db.TasbehDao
import javax.inject.Singleton

/** Ilova darajasidagi DI: Room bazasi va undagi DAO'lar (feature-prayer LocationDao ni shu yerdan oladi). */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): NamozDatabase =
        Room.databaseBuilder(context, NamozDatabase::class.java, "namoz.db").build()

    @Provides fun locationDao(db: NamozDatabase): LocationDao = db.locations()
    @Provides fun tasbehDao(db: NamozDatabase): TasbehDao = db.tasbeh()
}
