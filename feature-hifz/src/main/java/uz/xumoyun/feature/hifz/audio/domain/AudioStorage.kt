package uz.xumoyun.feature.hifz.audio.domain

import java.util.Locale

/**
 * Fayl joylashuvi. Yo'llar ildiz papkaga (noBackupFilesDir) nisbatan: `audio/<qori>/<sura3>/<oyat3>.mp3`.
 * Yarim yuklangan fayl `.part` qo'shimchasi bilan turadi; to'liq hisoblanmaydi.
 */
object AudioLayout {
    const val SUFFIX = ".mp3"
    const val PART_SUFFIX = ".part"

    fun surahDir(reciterKey: String, surah: Int): String {
        require(ReciterCatalog.isValidKey(reciterKey)) { "qori kaliti noto'g'ri: $reciterKey" }
        require(surah in 1..114) { "sura: $surah" }
        return "audio/$reciterKey/${pad3(surah)}"
    }

    fun ayahFile(reciterKey: String, surah: Int, ayah: Int): String {
        require(ayah in 1..MAX_AYAHS_PER_SURAH) { "oyat: $ayah" }
        return "${surahDir(reciterKey, surah)}/${pad3(ayah)}$SUFFIX"
    }

    /** Ro'yxatdagi fayl nomlaridan to'liq yuklangan oyat raqamlarini ajratadi ("001.mp3" -> 1). */
    fun finishedAyahsFromNames(names: Collection<String>): Set<Int> =
        names.mapNotNull { name ->
            if (!name.endsWith(SUFFIX)) return@mapNotNull null
            name.removeSuffix(SUFFIX).toIntOrNull()?.takeIf { it in 1..MAX_AYAHS_PER_SURAH }
        }.toSet()

    internal fun pad3(n: Int): String = n.toString().padStart(3, '0')
}

object StorageMath {
    /** "MB" deb ko'rsatiladi, lekin hisob 2^20 bayt (MiB) bo'yicha — Android tizimi bilan mos. */
    const val BYTES_PER_MB = 1024L * 1024L

    fun megabytes(bytes: Long): Double = bytes.coerceAtLeast(0L) / BYTES_PER_MB.toDouble()

    fun format(bytes: Long): String = String.format(Locale.ROOT, "%.1f MB", megabytes(bytes))

    fun total(rows: Collection<DownloadedSura>): Long = rows.sumOf { it.bytes.coerceAtLeast(0L) }
}
