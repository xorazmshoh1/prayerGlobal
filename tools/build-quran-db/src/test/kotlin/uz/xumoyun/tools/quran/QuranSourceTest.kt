package uz.xumoyun.tools.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class QuranSourceTest {

    @Test
    fun `uthmani parse keeps text byte for byte and skips comments and BOM`() {
        val lines = QuranSource.parseUthmani(SyntheticFixtures.uthmaniBytes())
        assertEquals(6, lines.size)
        assertEquals(AyahLine(1, 1, "test-1-1"), lines[0])
        // CRLF satr oxiri olib tashlanadi, matn o'zgarmaydi
        assertEquals("test-2-1", lines[3].text)
        assertEquals(
            "test-3-1".toByteArray(Charsets.UTF_8).toList(),
            lines[5].text.toByteArray(Charsets.UTF_8).toList(),
        )
    }

    @Test
    fun `arabic text with diacritics and tatweel is not altered`() {
        // Sintetik bo'lsa ham Unicode belgilar (harakat, U+0640) baytma-bayt saqlanishini tekshiradi
        val sample = "biـsmiَ ٱللَّهِ"
        val bytes = "7|1|$sample\n".toByteArray(Charsets.UTF_8)
        assertEquals(sample, QuranSource.parseUthmani(bytes).single().text)
    }

    @Test
    fun `malformed uthmani line gives clear error with line number`() {
        val bad = "1|1|ok\n1|2\n".toByteArray(Charsets.UTF_8)
        try {
            QuranSource.parseUthmani(bad)
            fail("xato kutilgan edi")
        } catch (e: QuranFormatException) {
            assertTrue(e.message!!, e.message!!.contains("2-qator"))
        }
    }

    @Test
    fun `non UTF-8 input is rejected`() {
        val bad = byteArrayOf('1'.code.toByte(), '|'.code.toByte(), '1'.code.toByte(), '|'.code.toByte(), 0xFF.toByte())
        try {
            QuranSource.parseUthmani(bad)
            fail("xato kutilgan edi")
        } catch (e: QuranFormatException) {
            assertTrue(e.message!!.contains("UTF-8"))
        }
    }

    @Test
    fun `empty uthmani file is rejected`() {
        try {
            QuranSource.parseUthmani("# faqat izoh\n".toByteArray())
            fail("xato kutilgan edi")
        } catch (e: QuranFormatException) {
            assertTrue(e.message!!.contains("topilmadi"))
        }
    }

    @Test
    fun `quran-data parse reads suras, revelation, juz and page markers`() {
        val data = QuranSource.parseData(SyntheticFixtures.dataBytes())
        assertEquals(3, data.suras.size)
        assertEquals(SuraMeta(1, 3, "sura-ism-1", "Test-One", Revelation.MECCAN), data.suras[0])
        assertEquals(Revelation.MEDINAN, data.suras[1].revelation)
        assertEquals(listOf(1, 2), data.juzStarts.map { it.index })
        assertEquals(Position(2, 1), data.juzStarts[1].start)
        assertEquals(3, data.pageStarts.size)
    }

    @Test
    fun `quran-data with missing attribute gives clear error`() {
        val xml = """<quran-data><suras><sura index="1" name="x" tname="y" type="Meccan"/></suras></quran-data>"""
        try {
            QuranSource.parseData(xml.toByteArray())
            fail("xato kutilgan edi")
        } catch (e: QuranFormatException) {
            assertTrue(e.message!!, e.message!!.contains("'ayas'"))
        }
    }

    @Test
    fun `unknown revelation type is rejected`() {
        val xml = """<quran-data><suras><sura index="1" ayas="1" name="x" tname="y" type="Unknown"/></suras></quran-data>"""
        try {
            QuranSource.parseData(xml.toByteArray())
            fail("xato kutilgan edi")
        } catch (e: QuranFormatException) {
            assertTrue(e.message!!.contains("Unknown"))
        }
    }
}
