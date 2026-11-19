package uz.xumoyun.feature.hifz.audio.work

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.feature.hifz.audio.data.AudioStore
import uz.xumoyun.feature.hifz.audio.data.ReciterRepository
import uz.xumoyun.feature.hifz.domain.QuranSource

/** WorkManager ishi uchun bog'liqliklar (Hilt singleton'lari). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AudioEntryPoint {
    fun store(): AudioStore
    fun reciters(): ReciterRepository
    fun quran(): QuranSource
}
