package uz.xumoyun.tools.quran

import java.io.File
import java.security.MessageDigest
import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * HAQIQIY Tanzil fayllari bilan testlar. Fayllar yo'q bo'lsa testlar SKIPPED bo'ladi (yashirin o'tmaydi).
 *
 *  - Fayl yo'li: `-Ptanzil.dir=/yo'l` yoki TANZIL_DIR env (papkada quran-uthmani.txt va quran-data.xml bo'lishi kerak).
 *  - Fotiha xeshi: `-Pquran.fatihaSha256=<hex>` yoki QURAN_FATIHA_SHA256 env.
 *    TODO(TZ-ochiq-savol): kutilgan xesh TZ'da yo'q; foydalanuvchi uni qo'lda kiritadi.
 *    Xesh: 1-suraning 7 ta oyat matni UTF-8 baytlarida, "\n" bilan ulangan (oxirida "\n" yo'q).
 */
class RealTanzilTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun tanzilDir(): File? {
        val path = System.getProperty("tanzil.dir") ?: System.getenv("TANZIL_DIR") ?: return null
        return File(path)
    }

    private fun skipUnlessFiles(): Pair<File, File> {
        val dir = tanzilDir()
        val uthmani = dir?.let { File(it, "quran-uthmani.txt") }
        val data = dir?.let { File(it, "quran-data.xml") }
        if (uthmani == null || data == null || !uthmani.isFile || !data.isFile) {
            val msg = "SKIPPED: Tanzil fayli yo'q (tanzil.dir/TANZIL_DIR ko'rsatilmagan yoki fayllar topilmadi)"
            println(msg)
            Assume.assumeTrue(msg, false)
        }
        return uthmani!! to data!!
    }

    private val ayahs by lazy { QuranSource.parseUthmani(skipUnlessFilesCached().first.readBytes()) }
    private val data by lazy { QuranSource.parseData(skipUnlessFilesCached().second.readBytes()) }
    private val cachedFiles: Pair<File, File> by lazy { skipUnlessFiles() }
    private fun skipUnlessFilesCached() = cachedFiles

    @Test
    fun `real tanzil files have 114 suras, 6236 ayahs, 30 juz, 604 pages`() {
        skipUnlessFiles()
        assertEquals(6236, ayahs.size)
        assertEquals(114, data.suras.size)
        assertEquals(6236, data.suras.sumOf { it.ayaCount })
        assertEquals(30, data.juzStarts.size)
        assertEquals(604, data.pageStarts.size)
    }

    @Test
    fun `real database builds with ids, juz 1 to 30 and pages 1 to 604`() {
        skipUnlessFiles()
        val out = File(tmp.root, "quran.db")
        val stats = QuranDbBuilder.build(ayahs, data, out, roomIdentityHash = null)
        assertEquals(114, stats.surahCount)
        assertEquals(6236, stats.ayahCount)
        DriverManager.getConnection("jdbc:sqlite:${out.absolutePath}").use { c ->
            fun scalar(sql: String): Int = c.createStatement().use { st ->
                st.executeQuery(sql).use { rs -> rs.next(); rs.getInt(1) }
            }
            assertEquals(6236, scalar("SELECT COUNT(*) FROM ayah"))
            assertEquals(6236, scalar("SELECT MAX(globalId) FROM ayah"))
            assertEquals(1, scalar("SELECT MIN(globalId) FROM ayah"))
            assertEquals(114, scalar("SELECT COUNT(*) FROM surah"))
            assertEquals(1, scalar("SELECT MIN(juz) FROM ayah"))
            assertEquals(30, scalar("SELECT MAX(juz) FROM ayah"))
            assertEquals(1, scalar("SELECT MIN(page) FROM ayah"))
            assertEquals(604, scalar("SELECT MAX(page) FROM ayah"))
            assertEquals(604, scalar("SELECT COUNT(DISTINCT page) FROM ayah"))
        }
    }

    @Test
    fun `database text equals source file text byte for byte`() {
        skipUnlessFiles()
        val out = File(tmp.root, "quran.db")
        QuranDbBuilder.build(ayahs, data, out, null)
        val stored = DriverManager.getConnection("jdbc:sqlite:${out.absolutePath}").use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT textUthmani FROM ayah ORDER BY globalId").use { rs ->
                    val list = ArrayList<ByteArray>()
                    while (rs.next()) list.add(rs.getString(1).toByteArray(Charsets.UTF_8))
                    list
                }
            }
        }
        assertEquals(ayahs.size, stored.size)
        ayahs.forEachIndexed { i, line ->
            assertTrue("oyat ${line.sura}:${line.aya} matni farq qiladi", line.text.toByteArray(Charsets.UTF_8).contentEquals(stored[i]))
        }
    }

    @Test
    fun `fatiha sha256 matches the expected value`() {
        skipUnlessFiles()
        val fatiha = ayahs.filter { it.sura == 1 }.map { it.text }
        assertEquals(7, fatiha.size)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(fatiha.joinToString("\n").toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        println("Fotiha SHA-256 (hisoblangan): $digest")
        val expected = System.getProperty("quran.fatihaSha256") ?: System.getenv("QURAN_FATIHA_SHA256")
        if (expected.isNullOrBlank()) {
            val msg = "SKIPPED: kutilgan Fotiha xeshi berilmagan (TODO(TZ-ochiq-savol)); hisoblangan qiymat yuqorida"
            println(msg)
            Assume.assumeTrue(msg, false)
        }
        assertEquals(expected!!.lowercase(), digest)
    }
}
