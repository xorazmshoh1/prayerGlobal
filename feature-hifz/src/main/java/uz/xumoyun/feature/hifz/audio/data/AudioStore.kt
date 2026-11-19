package uz.xumoyun.feature.hifz.audio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.xumoyun.feature.hifz.audio.domain.DownloadedSura
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Yuklangan suralar (Room) va fayllar (disk) ustidagi yagona joy. */
@Singleton
class AudioStore @Inject constructor(
    private val dao: AudioDownloadDao,
    root: AudioRoot,
) {
    private val files = AudioFiles(root.dir)

    fun observe(): Flow<List<DownloadedSura>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun ayahFile(reciterKey: String, surah: Int, ayah: Int): File = files.ayahFile(reciterKey, surah, ayah)

    fun finishedAyahs(reciterKey: String, surah: Int): Set<Int> = files.finishedAyahs(reciterKey, surah)

    fun bytesOnDisk(reciterKey: String, surah: Int): Long = files.bytesOnDisk(reciterKey, surah)

    /** Sura to'liq tugagach chaqiriladi: hajm diskdan o'lchanadi va yozuv saqlanadi. */
    suspend fun markDownloaded(reciterKey: String, surah: Int, now: Long) {
        dao.upsert(
            AudioDownloadEntity(
                reciterKey = reciterKey,
                surahId = surah,
                bytes = files.bytesOnDisk(reciterKey, surah),
                downloadedAt = now,
            ),
        )
    }

    /** Suraning fayllari va yozuvini o'chiradi. Tiklangan baytlarni qaytaradi. */
    suspend fun delete(reciterKey: String, surah: Int): Long {
        val freed = files.deleteSura(reciterKey, surah)
        dao.delete(reciterKey, surah)
        return freed
    }
}
