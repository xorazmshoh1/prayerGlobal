package uz.xumoyun.namoz.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.feature.prayer.data.LocationDao
import uz.xumoyun.feature.tasbih.data.TasbihDao
import uz.xumoyun.feature.hifz.audio.data.AudioDownloadDao
import uz.xumoyun.namoz.data.db.MIGRATION_1_2
import uz.xumoyun.namoz.data.db.MIGRATION_2_3
import uz.xumoyun.namoz.data.db.NamozDatabase
import javax.inject.Singleton

/** Ilova darajasidagi DI: Room bazasi va undagi DAO'lar (feature-prayer va feature-tasbih DAO'larini shu yerdan oladi). */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): NamozDatabase =
        Room.databaseBuilder(context, NamozDatabase::class.java, "namoz.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides fun locationDao(db: NamozDatabase): LocationDao = db.locations()
    @Provides fun tasbihDao(db: NamozDatabase): TasbihDao = db.tasbeh()
    @Provides fun audioDao(db: NamozDatabase): AudioDownloadDao = db.audio()
}
