package uz.xumoyun.feature.hifz.audio.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AudioFilesTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun write(path: String, size: Int) {
        File(tmp.root, path).apply { parentFile?.mkdirs(); writeBytes(ByteArray(size) { 7 }) }
    }

    @Test
    fun finishedAyahs_excludePartialFiles() {
        write("audio/alafasy/002/001.mp3", 100)
        write("audio/alafasy/002/002.mp3.part", 50)
        write("audio/alafasy/002/003.mp3", 200)
        val files = AudioFiles(tmp.root)
        assertEquals(setOf(1, 3), files.finishedAyahs("alafasy", 2))
        assertEquals(emptySet<Int>(), files.finishedAyahs("alafasy", 9))
    }

    @Test
    fun deleteSura_freesExactDiskBytes_andKeepsOthers() {
        write("audio/alafasy/002/001.mp3", 100)
        write("audio/alafasy/002/002.mp3.part", 50)
        write("audio/alafasy/002/003.mp3", 200)
        write("audio/alafasy/003/001.mp3", 999)
        val files = AudioFiles(tmp.root)
        assertEquals(350L, files.bytesOnDisk("alafasy", 2))
        assertEquals(350L, files.deleteSura("alafasy", 2))
        assertFalse(File(tmp.root, "audio/alafasy/002").exists())
        assertEquals(999L, files.bytesOnDisk("alafasy", 3))
    }

    @Test
    fun deleteMissingSura_freesNothing() {
        assertEquals(0L, AudioFiles(tmp.root).deleteSura("husary", 1))
    }
}
