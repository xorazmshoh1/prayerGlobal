package uz.xumoyun.namoz.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.core.common.crash.CrashReportGate
import uz.xumoyun.core.common.crash.NoopCrashSink
import uz.xumoyun.feature.prayer.data.LocationDao
import uz.xumoyun.feature.prayer.tracker.data.TrackerDao
import uz.xumoyun.feature.prayer.tracker.domain.TransactionRunner
import uz.xumoyun.feature.tasbih.data.TasbihDao
import uz.xumoyun.feature.hifz.audio.data.AudioDownloadDao
import uz.xumoyun.feature.hifz.plan.data.HifzDao
import uz.xumoyun.namoz.backup.BackupStore
import uz.xumoyun.namoz.backup.RoomBackupStore
import uz.xumoyun.namoz.data.db.MIGRATION_1_2
import uz.xumoyun.namoz.data.db.MIGRATION_2_3
import uz.xumoyun.namoz.data.db.MIGRATION_3_4
import uz.xumoyun.namoz.data.db.MIGRATION_4_5
import uz.xumoyun.namoz.data.db.NamozDatabase
import uz.xumoyun.namoz.data.db.RoomTransactionRunner
import javax.inject.Singleton

/** Ilova darajasidagi DI: Room bazasi va undagi DAO'lar (feature-prayer va feature-tasbih DAO'larini shu yerdan oladi). */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): NamozDatabase =
        Room.databaseBuilder(context, NamozDatabase::class.java, "namoz.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    @Provides fun locationDao(db: NamozDatabase): LocationDao = db.locations()
    @Provides fun tasbihDao(db: NamozDatabase): TasbihDao = db.tasbeh()
    @Provides fun audioDao(db: NamozDatabase): AudioDownloadDao = db.audio()
    @Provides fun hifzDao(db: NamozDatabase): HifzDao = db.hifz()
    @Provides fun trackerDao(db: NamozDatabase): TrackerDao = db.tracker()

    /** Xato hisobotlari: hozircha yuboruvchi yo'q (Firebase reliz oldidan ulanadi). */
    @Provides @Singleton fun crashGate(): CrashReportGate = CrashReportGate(NoopCrashSink)
}

/** Tranzaksiya va zaxira saqlagichi bog'lamalari (Room bazasiga bog'liq amalga oshirishlar). */
@Module
@InstallIn(SingletonComponent::class)
abstract class PersistenceBindings {
    @Binds abstract fun transactions(impl: RoomTransactionRunner): TransactionRunner
    @Binds abstract fun backupStore(impl: RoomBackupStore): BackupStore
}
