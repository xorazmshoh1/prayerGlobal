package uz.xumoyun.feature.hifz.audio.data

import uz.xumoyun.feature.hifz.audio.domain.AudioLayout
import java.io.File

/**
 * Audio fayllari bilan ishlash (ildiz papka ostida). Room'ga bog'liq emas.
 * Sura papkasidagi barcha fayllar (".part" ham) o'chirilganda egallangan joy qaytariladi.
 */
class AudioFiles(private val root: File) {

    fun ayahFile(reciterKey: String, surah: Int, ayah: Int): File =
        File(root, AudioLayout.ayahFile(reciterKey, surah, ayah))

    private fun suraDir(reciterKey: String, surah: Int): File =
        File(root, AudioLayout.surahDir(reciterKey, surah))

    /** Diskda to'liq turgan oyat raqamlari. */
    fun finishedAyahs(reciterKey: String, surah: Int): Set<Int> {
        val names = suraDir(reciterKey, surah).list()?.toList() ?: return emptySet()
        return AudioLayout.finishedAyahsFromNames(names)
    }

    /** Sura papkasidagi barcha fayllar hajmi (".part" bilan). */
    fun bytesOnDisk(reciterKey: String, surah: Int): Long =
        suraDir(reciterKey, surah).walkTopDown().filter { it.isFile }.sumOf { it.length() }

    /** Sura papkasini o'chiradi; oldingi hajmni qaytaradi (egallangan joy tiklanadi). */
    fun deleteSura(reciterKey: String, surah: Int): Long {
        val dir = suraDir(reciterKey, surah)
        val freed = if (dir.exists()) bytesOnDisk(reciterKey, surah) else 0L
        dir.deleteRecursively()
        return freed
    }
}
