package uz.xumoyun.feature.hifz.audio.data

import androidx.room.Entity
import uz.xumoyun.feature.hifz.audio.domain.DownloadedSura

/**
 * AudioDownload (TZ 7-bo'lim): bitta qori + bitta sura — bitta yozuv. Ilova bazasi (NamozDatabase) v3.
 * Jadval nomi "audio_download" (boshqa jadvallar kabi snake_case).
 */
@Entity(tableName = "audio_download", primaryKeys = ["reciterKey", "surahId"])
data class AudioDownloadEntity(
    val reciterKey: String,
    val surahId: Int,
    val bytes: Long,
    val downloadedAt: Long,
) {
    fun toDomain(): DownloadedSura = DownloadedSura(reciterKey, surahId, bytes, downloadedAt)
}
