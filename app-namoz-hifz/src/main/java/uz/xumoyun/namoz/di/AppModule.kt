package uz.xumoyun.namoz.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.namoz.data.HijriProvider
import uz.xumoyun.namoz.data.IcuHijriProvider
import uz.xumoyun.namoz.data.AndroidLocationProvider
import uz.xumoyun.namoz.data.LocationProvider
import uz.xumoyun.namoz.data.db.LocationDao
import uz.xumoyun.namoz.data.db.NamozDatabase
import uz.xumoyun.namoz.data.db.TasbehDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): NamozDatabase =
        Room.databaseBuilder(context, NamozDatabase::class.java, "namoz.db").build()

    @Provides fun locationDao(db: NamozDatabase): LocationDao = db.locations()
    @Provides fun tasbehDao(db: NamozDatabase): TasbehDao = db.tasbeh()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun hijri(impl: IcuHijriProvider): HijriProvider
    @Binds abstract fun location(impl: AndroidLocationProvider): LocationProvider
}
