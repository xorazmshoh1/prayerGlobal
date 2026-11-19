package uz.xumoyun.tools.quran

import java.io.File
import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Sintetik kirish bilan sxema, qator soni, id oralig'i, juz/sahifa maydonlarini tekshiradi. */
class QuranDbBuilderTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun buildSynthetic(hash: String? = "testhash0123"): File {
        val out = File(tmp.root, "quran.db")
        val ayahs = QuranSource.parseUthmani(SyntheticFixtures.uthmaniBytes())
        val data = QuranSource.parseData(SyntheticFixtures.dataBytes())
        val stats = QuranDbBuilder.build(ayahs, data, out, hash)
        assertEquals(3, stats.surahCount)
        assertEquals(6, stats.ayahCount)
        assertEquals(2, stats.juzCount)
        assertEquals(3, stats.pageCount)
        return out
    }

    private fun <T> query(db: File, sql: String, map: (java.sql.ResultSet) -> T): List<T> =
        DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { c ->
            c.createStatement().use { st ->
                st.executeQuery(sql).use { rs ->
                    val out = ArrayList<T>()
                    while (rs.next()) out.add(map(rs))
                    out
                }
            }
        }

    @Test
    fun `surah table has expected rows, ids and cumulative start`() {
        val db = buildSynthetic()
        val rows = query(db, "SELECT id, nameArabic, nameTranslit, ayahCount, revelation, startAyahGlobal FROM surah ORDER BY id") {
            listOf(it.getInt(1), it.getString(2), it.getString(3), it.getInt(4), it.getString(5), it.getInt(6))
        }
        assertEquals(3, rows.size)
        assertEquals(listOf<Any>(1, "sura-ism-1", "Test-One", 3, "MECCAN", 1), rows[0])
        assertEquals(listOf<Any>(2, "sura-ism-2", "Test-Two", 2, "MEDINAN", 4), rows[1])
        assertEquals(listOf<Any>(3, "sura-ism-3", "Test-Three", 1, "MECCAN", 6), rows[2])
    }

    @Test
    fun `ayah ids are 1 to N, sura and juz and page fields are right`() {
        val db = buildSynthetic()
        val rows = query(db, "SELECT globalId, surahId, numberInSurah, juz, page, textUthmani FROM ayah ORDER BY globalId") {
            listOf(it.getInt(1), it.getInt(2), it.getInt(3), it.getInt(4), it.getInt(5), it.getString(6))
        }
        assertEquals(6, rows.size)
        assertEquals((1..6).toList(), rows.map { it[0] })
        assertEquals(listOf<Any>(1, 1, 1, 1, 1, "test-1-1"), rows[0])
        assertEquals(listOf<Any>(3, 1, 3, 1, 1, "test-1-3"), rows[2])
        // 2:1 dan juz 2 va sahifa 2 boshlanadi
        assertEquals(listOf<Any>(4, 2, 1, 2, 2, "test-2-1"), rows[3])
        assertEquals(listOf<Any>(6, 3, 1, 2, 3, "test-3-1"), rows[5])
    }

    @Test
    fun `text is stored byte for byte`() {
        val db = buildSynthetic()
        val stored = query(db, "SELECT textUthmani FROM ayah ORDER BY globalId") { it.getString(1) }
        val source = QuranSource.parseUthmani(SyntheticFixtures.uthmaniBytes()).map { it.text }
        assertEquals(source, stored)
        assertEquals(source.map { it.toByteArray(Charsets.UTF_8).toList() }, stored.map { it.toByteArray(Charsets.UTF_8).toList() })
    }

    @Test
    fun `indexes and schema tables exist`() {
        val db = buildSynthetic()
        val names = query(db, "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'index_ayah_%' ORDER BY name") { it.getString(1) }
        assertEquals(listOf("index_ayah_juz", "index_ayah_page", "index_ayah_surahId"), names)
        val columns = query(db, "SELECT name FROM pragma_table_info('ayah')") { it.getString(1) }
        assertEquals(listOf("globalId", "surahId", "numberInSurah", "juz", "page", "textUthmani"), columns)
    }

    @Test
    fun `room master table and user_version are written when hash is given`() {
        val db = buildSynthetic("abc123")
        assertEquals(listOf("abc123"), query(db, "SELECT identity_hash FROM room_master_table") { it.getString(1) })
        assertEquals(listOf(1), query(db, "PRAGMA user_version") { it.getInt(1) })
    }

    @Test
    fun `room master table is absent when hash is not given`() {
        val db = buildSynthetic(null)
        val tables = query(db, "SELECT name FROM sqlite_master WHERE name='room_master_table'") { it.getString(1) }
        assertEquals(emptyList<String>(), tables)
    }

    @Test
    fun `rebuild replaces existing file`() {
        buildSynthetic()
        val db = buildSynthetic()
        assertEquals(listOf(6), query(db, "SELECT COUNT(*) FROM ayah") { it.getInt(1) })
    }

    @Test
    fun `ayah count mismatch with quran-data is rejected`() {
        val ayahs = QuranSource.parseUthmani(SyntheticFixtures.uthmaniBytes()).dropLast(1)
        val data = QuranSource.parseData(SyntheticFixtures.dataBytes())
        expectFormatError(ayahs, data, "mos emas")
    }

    @Test
    fun `aya gap inside sura is rejected`() {
        val ayahs = listOf(AyahLine(1, 1, "test-a"), AyahLine(1, 3, "test-b"), AyahLine(1, 4, "test-c"))
        val data = QuranSource.parseData(SyntheticFixtures.dataBytes())
        expectFormatError(ayahs, data.copy(suras = data.suras.take(1).map { it.copy(ayaCount = 3) }), "kutilgan")
    }

    @Test
    fun `missing juz markers are rejected`() {
        val ayahs = QuranSource.parseUthmani(SyntheticFixtures.uthmaniBytes())
        val data = QuranSource.parseData(SyntheticFixtures.dataBytes()).copy(juzStarts = emptyList())
        expectFormatError(ayahs, data, "juz")
    }

    @Test
    fun `sura with wrong order is rejected`() {
        val ayahs = listOf(AyahLine(2, 1, "test-a"), AyahLine(1, 1, "test-b"))
        val data = QuranData(
            suras = listOf(
                SuraMeta(1, 1, "a", "A", Revelation.MECCAN),
                SuraMeta(2, 1, "b", "B", Revelation.MEDINAN),
            ),
            juzStarts = listOf(Marker(1, Position(1, 1))),
            pageStarts = listOf(Marker(1, Position(1, 1))),
        )
        expectFormatError(ayahs, data, "kutilmagan")
    }

    private fun expectFormatError(ayahs: List<AyahLine>, data: QuranData, contains: String) {
        val out = File(tmp.root, "bad.db")
        try {
            QuranDbBuilder.build(ayahs, data, out, null)
            fail("xato kutilgan edi")
        } catch (e: QuranFormatException) {
            assertTrue("xabar: ${e.message}", e.message!!.contains(contains))
        }
        assertFalse("xato bo'lganda fayl yaratilmasligi kerak", out.exists())
    }
}
