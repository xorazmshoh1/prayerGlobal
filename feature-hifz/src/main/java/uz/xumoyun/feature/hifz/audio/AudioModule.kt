package uz.xumoyun.feature.hifz.audio

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.feature.hifz.audio.data.AudioRoot
import uz.xumoyun.feature.hifz.audio.work.AudioScheduler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides @Singleton
    fun audioRoot(@ApplicationContext context: Context): AudioRoot = AudioRoot(context.noBackupFilesDir)

    @Provides @Singleton
    fun audioScheduler(@ApplicationContext context: Context): AudioScheduler =
        AudioScheduler(WorkManager.getInstance(context))
}
