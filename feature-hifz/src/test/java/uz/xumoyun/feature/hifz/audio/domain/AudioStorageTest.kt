package uz.xumoyun.feature.hifz.audio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AudioStorageTest {

    @Test
    fun layoutPaths() {
        assertEquals("audio/husary/002", AudioLayout.surahDir("husary", 2))
        assertEquals("audio/husary/002/255.mp3", AudioLayout.ayahFile("husary", 2, 255))
        assertEquals("audio/alafasy/114/006.mp3", AudioLayout.ayahFile("alafasy", 114, 6))
    }

    @Test
    fun layoutRejectsUnsafeKeysAndRanges() {
        assertThrows(IllegalArgumentException::class.java) { AudioLayout.surahDir("../x", 1) }
        assertThrows(IllegalArgumentException::class.java) { AudioLayout.surahDir("a/b", 1) }
        assertThrows(IllegalArgumentException::class.java) { AudioLayout.surahDir("", 1) }
        assertThrows(IllegalArgumentException::class.java) { AudioLayout.surahDir("ok", 0) }
        assertThrows(IllegalArgumentException::class.java) { AudioLayout.ayahFile("ok", 1, 287) }
    }

    @Test
    fun finishedAyahsIgnoresPartialAndJunk() {
        val names = listOf("001.mp3", "002.mp3.part", "003.mp3", "abc.mp3", "000.mp3", "287.mp3", "004.txt")
        assertEquals(setOf(1, 3), AudioLayout.finishedAyahsFromNames(names))
    }

    @Test
    fun megabytesFormatting() {
        assertEquals("5.0 MB", StorageMath.format(5L * 1024 * 1024))
        assertEquals("1.5 MB", StorageMath.format(1536L * 1024))
        assertEquals("0.0 MB", StorageMath.format(-7))
        assertEquals(2.0, StorageMath.megabytes(2L * StorageMath.BYTES_PER_MB), 0.0001)
    }

    @Test
    fun totalSumsRows() {
        val rows = listOf(
            DownloadedSura("a", 1, 1_000, 0L),
            DownloadedSura("b", 1, 2_000, 0L),
        )
        assertEquals(3_000L, StorageMath.total(rows))
        assertEquals(0L, StorageMath.total(emptyList()))
    }
}
