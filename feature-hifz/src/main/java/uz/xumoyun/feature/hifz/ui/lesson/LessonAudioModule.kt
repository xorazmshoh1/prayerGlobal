package uz.xumoyun.feature.hifz.ui.lesson

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import uz.xumoyun.feature.hifz.audio.data.AudioStore
import uz.xumoyun.feature.hifz.audio.data.ReciterRepository

/** Dars pleyeri: har bir LessonViewModel uchun alohida (ViewModel tugaganda onCleared'da bo'shatiladi). */
@Module
@InstallIn(ViewModelComponent::class)
object LessonAudioModule {
    @Provides
    @ViewModelScoped
    fun lessonAudio(
        @ApplicationContext context: Context,
        store: AudioStore,
        reciters: ReciterRepository,
    ): LessonAudio = AudioLessonPlayer(context, store, reciters.reciters.firstOrNull()?.key.orEmpty())
}
