package uz.xumoyun.tools.quran

import java.io.File
import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Hajm testi: 114 sura, 6236 oyat, 30 juz, 604 sahifa — SINTETIK matn ("test-S-O") va sintetik
 * juz/sahifa chegaralari bilan. Haqiqiy oyat sonlari emas; faqat pipeline'ning masshtabini tekshiradi.
 */
class ScaleSyntheticTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `114 suras and 6236 ayahs with 30 juz and 604 pages build correctly`() {
        val counts = IntArray(114) { 50 }
        counts[113] = 6236 - counts.take(113).sum()
        assertEquals(6236, counts.sum())

        val suras = counts.mapIndexed { i, c ->
            SuraMeta(i + 1, c, "s${i + 1}", "S${i + 1}", if (i % 2 == 0) Revelation.MECCAN else Revelation.MEDINAN)
        }
        val ayahs = ArrayList<AyahLine>(6236)
        suras.forEach { s -> for (a in 1..s.ayaCount) ayahs.add(AyahLine(s.index, a, "test-${s.index}-$a")) }

        // Juz va sahifalar: global indeks bo'yicha teng bo'lingan chegaralar (sintetik)
        fun markerAt(globalIndex: Int): Position {
            var acc = 0
            for (s in suras) {
                if (globalIndex < acc + s.ayaCount) return Position(s.index, globalIndex - acc + 1)
                acc += s.ayaCount
            }
            error("indeks chegaradan tashqarida")
        }
        val juz = (0 until 30).map { Marker(it + 1, markerAt(it * 6236 / 30)) }
        val pages = (0 until 604).map { Marker(it + 1, markerAt(it * 6236 / 604)) }
        val data = QuranData(suras, juz, pages)

        val out = File(tmp.root, "scale.db")
        val stats = QuranDbBuilder.build(ayahs, data, out)
        assertEquals(BuildStats(114, 6236, 30, 604), stats)

        DriverManager.getConnection("jdbc:sqlite:${out.absolutePath}").use { c ->
            fun scalar(sql: String): Int = c.createStatement().use { st ->
                st.executeQuery(sql).use { rs -> rs.next(); rs.getInt(1) }
            }
            assertEquals(6236, scalar("SELECT COUNT(*) FROM ayah"))
            assertEquals(6236, scalar("SELECT MAX(globalId) FROM ayah"))
            assertEquals(30, scalar("SELECT COUNT(DISTINCT juz) FROM ayah"))
            assertEquals(604, scalar("SELECT COUNT(DISTINCT page) FROM ayah"))
            assertEquals(1, scalar("SELECT juz FROM ayah WHERE globalId = 1"))
            assertEquals(30, scalar("SELECT juz FROM ayah WHERE globalId = 6236"))
            assertEquals(6236 - counts[113] + 1, scalar("SELECT startAyahGlobal FROM surah WHERE id = 114"))
        }
    }
}
