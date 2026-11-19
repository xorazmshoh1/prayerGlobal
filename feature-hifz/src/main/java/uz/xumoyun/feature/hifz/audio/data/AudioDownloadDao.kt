package uz.xumoyun.feature.hifz.audio.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDownloadDao {
    @Query("SELECT * FROM audio_download ORDER BY reciterKey, surahId")
    fun observeAll(): Flow<List<AudioDownloadEntity>>

    @Upsert
    suspend fun upsert(row: AudioDownloadEntity)

    @Query("DELETE FROM audio_download WHERE reciterKey = :reciterKey AND surahId = :surahId")
    suspend fun delete(reciterKey: String, surahId: Int)
}
