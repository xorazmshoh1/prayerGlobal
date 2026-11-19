package uz.xumoyun.core.common.color

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    @Test fun blackOnWhiteIsTwentyOneToOne() {
        assertEquals(21.0, Contrast.ratio(0x000000, 0xFFFFFF), 0.01)
    }

    @Test fun identicalColorsAreOneToOne() {
        assertEquals(1.0, Contrast.ratio(0x0F5C4D, 0x0F5C4D), 1e-9)
    }

    /** TZ 11-bo'lim palitrasi: asosiy matn va ikkilamchi matn ikkala temada AA (4.5:1) dan kam emas. */
    @Test fun bodyAndSecondaryTextMeetAaInBothThemes() {
        val lightBg = 0xFAF8F3; val lightText = 0x1B1F1D; val lightSecondary = 0x5C6662
        val darkBg = 0x101614; val darkText = 0xE8ECEA; val darkSecondary = 0x9AA6A1
        assertTrue(Contrast.ratio(lightText, lightBg) >= Contrast.AA_TEXT)
        assertTrue(Contrast.ratio(lightSecondary, lightBg) >= Contrast.AA_TEXT)
        assertTrue(Contrast.ratio(darkText, darkBg) >= Contrast.AA_TEXT)
        assertTrue(Contrast.ratio(darkSecondary, darkBg) >= Contrast.AA_TEXT)
    }

    /** Primary rang sarlavha/tugmalarda (katta matn) kamida 3:1; yorug' temada asosiy rang matn sifatida 4.5:1 dan oshadi. */
    @Test fun primaryIsReadableOnItsSurfaces() {
        assertTrue(Contrast.ratio(0x0F5C4D, 0xFAF8F3) >= Contrast.AA_TEXT)
        assertTrue(Contrast.ratio(0x6FCFB4, 0x101614) >= Contrast.AA_TEXT)
    }
}
