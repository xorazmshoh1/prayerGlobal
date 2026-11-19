package uz.xumoyun.feature.hifz.audio.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReciterCatalogTest {

    private fun reciter(key: String = "abc", url: String = "https://example.test/{surah3}{ayah3}.mp3", format: String = "mp3") =
        """{"reciters":[{"key":"$key","name":"N","urlTemplate":"$url","format":"$format"}]}"""

    @Test
    fun bundledConfigParses_threeRecitersAllUnverified() {
        // Ish papkasi modul yoki repo ildizi bo'lishi mumkin; asset haqiqiy fayl.
        val asset = listOf("src/main/assets/reciters.json", "feature-hifz/src/main/assets/reciters.json")
            .map(::File).first { it.isFile }
        val text = asset.readText()
        val list = ReciterCatalog.parse(text)
        assertEquals(3, list.size)
        assertEquals(3, list.map { it.key }.distinct().size)
        // TZ 4-bo'lim: URL shablonlari va qori ID'lari hali tasdiqlanmagan.
        assertTrue(list.all { it.todo?.contains("TZ-ochiq-savol") == true })
        assertTrue(list.all { it.urlTemplate.contains("{surah3}{ayah3}") })
    }

    @Test
    fun validConfigParses() {
        val r = ReciterCatalog.parse(reciter()).single()
        assertEquals("abc", r.key)
        assertEquals(64, r.bitrateKbps)
        assertEquals(null, r.todo)
    }

    @Test
    fun rejectsBadConfigs() {
        assertThrows(IllegalArgumentException::class.java) { ReciterCatalog.parse("""{"reciters":[]}""") }
        assertThrows(IllegalArgumentException::class.java) { ReciterCatalog.parse(reciter(key = "../etc")) }
        assertThrows(IllegalArgumentException::class.java) { ReciterCatalog.parse(reciter(key = "Upper")) }
        assertThrows(IllegalArgumentException::class.java) { ReciterCatalog.parse(reciter(url = "http://x.test/{surah3}{ayah3}.mp3")) }
        assertThrows(IllegalArgumentException::class.java) { ReciterCatalog.parse(reciter(url = "https://x.test/{surah3}.mp3")) }
        assertThrows(IllegalArgumentException::class.java) { ReciterCatalog.parse(reciter(url = "https://x.test/{surah3}{ayah3}{foo}.mp3")) }
        assertThrows(IllegalArgumentException::class.java) { ReciterCatalog.parse(reciter(format = "ogg")) }
        val dup = """{"reciters":[
            {"key":"a","name":"A","urlTemplate":"https://x.test/{surah3}{ayah3}.mp3"},
            {"key":"a","name":"B","urlTemplate":"https://x.test/{surah3}{ayah3}.mp3"}]}"""
        assertThrows(IllegalArgumentException::class.java) { ReciterCatalog.parse(dup) }
    }

    @Test
    fun ayahUrl_fillsPaddedPlaceholders() {
        val r = ReciterCatalog.parse(reciter(url = "https://x.test/Folder/{surah3}{ayah3}.mp3")).single()
        assertEquals("https://x.test/Folder/001001.mp3", AyahUrl.build(r, 1, 1))
        assertEquals("https://x.test/Folder/002255.mp3", AyahUrl.build(r, 2, 255))
        assertEquals("https://x.test/Folder/114006.mp3", AyahUrl.build(r, 114, 6))
    }

    @Test
    fun ayahUrl_rejectsOutOfRange() {
        val r = ReciterCatalog.parse(reciter()).single()
        assertThrows(IllegalArgumentException::class.java) { AyahUrl.build(r, 0, 1) }
        assertThrows(IllegalArgumentException::class.java) { AyahUrl.build(r, 115, 1) }
        assertThrows(IllegalArgumentException::class.java) { AyahUrl.build(r, 1, 0) }
        assertThrows(IllegalArgumentException::class.java) { AyahUrl.build(r, 1, MAX_AYAHS_PER_SURAH + 1) }
    }
}
