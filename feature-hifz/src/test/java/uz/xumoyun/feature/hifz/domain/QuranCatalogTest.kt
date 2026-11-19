package uz.xumoyun.feature.hifz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranCatalogTest {

    // Sintetik ro'yxat: nomlar test uchun, Qur'on matni emas
    private val surahs = listOf(
        SurahInfo(1, "الفاتحة", "Al-Fatiha", 7, Revelation.MECCAN, 1),
        SurahInfo(2, "البقرة", "Al-Baqarah", 286, Revelation.MEDINAN, 8),
        SurahInfo(3, "آل عمران", "Aal-E-Imran", 200, Revelation.MEDINAN, 294),
        SurahInfo(10, "يونس", "Yunus", 109, Revelation.MECCAN, 1000),
        SurahInfo(114, "الناس", "An-Nas", 6, Revelation.MECCAN, 6231),
    )

    @Test fun `empty query returns all surahs`() {
        assertEquals(surahs, QuranCatalog.filterSurahs(surahs, "   "))
    }

    @Test fun `numeric query matches exact surah number only`() {
        assertEquals(listOf(1), QuranCatalog.filterSurahs(surahs, "1").map { it.id })
        assertEquals(listOf(10), QuranCatalog.filterSurahs(surahs, "10").map { it.id })
        assertEquals(listOf(114), QuranCatalog.filterSurahs(surahs, "114").map { it.id })
        assertEquals(emptyList<SurahInfo>(), QuranCatalog.filterSurahs(surahs, "999"))
    }

    @Test fun `latin query ignores case, hyphens and spaces`() {
        assertEquals(listOf(1), QuranCatalog.filterSurahs(surahs, "fatiha").map { it.id })
        assertEquals(listOf(1), QuranCatalog.filterSurahs(surahs, "AL FATIHA").map { it.id })
        assertEquals(listOf(2), QuranCatalog.filterSurahs(surahs, "baqa").map { it.id })
        assertEquals(listOf(3), QuranCatalog.filterSurahs(surahs, "aal-e").map { it.id })
    }

    @Test fun `arabic query ignores diacritics and alif variants`() {
        val withHarakat = "اَلْفَاتِحَة" // Al-Fatiha with harakat
        assertEquals(listOf(1), QuranCatalog.filterSurahs(surahs, withHarakat).map { it.id })
        // "عمران" faqat 3-suraga mos; "آل" (madda alif) "ا" ga keltiriladi
        assertEquals(listOf(3), QuranCatalog.filterSurahs(surahs, "\u0639\u0645\u0631\u0627\u0646").map { it.id })
        assertEquals(listOf(3), QuranCatalog.filterSurahs(surahs, "ال عمران").map { it.id })
    }

    @Test fun `query made only of punctuation matches nothing`() {
        assertEquals(emptyList<SurahInfo>(), QuranCatalog.filterSurahs(surahs, "-"))
    }

    @Test fun `normalizeArabic strips tatweel and harakat, keeps base letters`() {
        assertEquals("بسم", QuranCatalog.normalizeArabic("بـسَمُ"))
    }

    @Test fun `arabic indic digits`() {
        assertEquals("\u0660", QuranCatalog.arabicIndic(0))
        assertEquals("\u0669", QuranCatalog.arabicIndic(9))
        assertEquals("١٢٣", QuranCatalog.arabicIndic(123))
        assertEquals("١٠٠", QuranCatalog.arabicIndic(100))
    }

    @Test fun `ayah marker wraps arabic number in ornate brackets`() {
        assertEquals("﴿٧﴾", QuranCatalog.ayahMarker(7))
        assertEquals("﴿١٠﴾", QuranCatalog.ayahMarker(10))
    }

    @Test fun `negative number is rejected`() {
        try {
            QuranCatalog.arabicIndic(-1)
            throw AssertionError("xato kutilgan edi")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("-1"))
        }
    }

    @Test fun `font size defaults to 26 and is clamped to 20 to 40`() {
        assertEquals(26f, QuranCatalog.FONT_DEFAULT_SP, 0f)
        assertEquals(20f, QuranCatalog.clampFontSp(5f), 0f)
        assertEquals(40f, QuranCatalog.clampFontSp(99f), 0f)
        assertEquals(31f, QuranCatalog.clampFontSp(31f), 0f)
    }

    @Test fun `font steps move by 2 sp and never leave the range`() {
        assertEquals(28f, QuranCatalog.stepFontSp(26f, larger = true), 0f)
        assertEquals(24f, QuranCatalog.stepFontSp(26f, larger = false), 0f)
        assertEquals(40f, QuranCatalog.stepFontSp(40f, larger = true), 0f)
        assertEquals(20f, QuranCatalog.stepFontSp(20f, larger = false), 0f)
        assertEquals(40f, QuranCatalog.stepFontSp(39f, larger = true), 0f)
    }

    @Test fun `juz numbers are 1 to 30`() {
        assertEquals(30, QuranCatalog.juzNumbers.count())
        assertEquals(1, QuranCatalog.juzNumbers.first)
        assertEquals(30, QuranCatalog.juzNumbers.last)
    }
}
