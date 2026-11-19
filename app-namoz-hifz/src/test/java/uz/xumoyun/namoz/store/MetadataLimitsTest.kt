package uz.xumoyun.namoz.store

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** TZ 12: Play sahifasi matnlari 11 tilda, limitlar — sarlavha 30, qisqa tavsif 80, to'liq 4000 belgi. */
class MetadataLimitsTest {
    private val root = File("../fastlane/metadata/android")

    @Test fun elevenLocalesHaveAllThreeTexts() {
        val dirs = root.listFiles { f -> f.isDirectory }.orEmpty()
        assertEquals(11, dirs.size)
        dirs.forEach { d ->
            listOf("title.txt", "short_description.txt", "full_description.txt").forEach { name ->
                assertTrue("${d.name}/$name", File(d, name).isFile)
            }
        }
    }

    @Test fun textsStayWithinPlayLimits() {
        root.listFiles { f -> f.isDirectory }.orEmpty().forEach { d ->
            assertTrue("${d.name} title", File(d, "title.txt").readText().trim().length <= 30)
            assertTrue("${d.name} short", File(d, "short_description.txt").readText().trim().length <= 80)
            assertTrue("${d.name} full", File(d, "full_description.txt").readText().trim().length <= 4000)
        }
    }

    @Test fun noForbiddenPromotionalWords() {
        val banned = listOf("best", "million downloads", "download now")
        root.listFiles { f -> f.isDirectory }.orEmpty().forEach { d ->
            val all = listOf("title.txt", "short_description.txt", "full_description.txt")
                .joinToString("\n") { File(d, it).readText() }.lowercase()
            banned.forEach { assertTrue("${d.name} contains '$it'", !all.contains(it)) }
        }
    }
}
