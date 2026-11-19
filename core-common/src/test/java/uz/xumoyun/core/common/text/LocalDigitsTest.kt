package uz.xumoyun.core.common.text

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalDigitsTest {
    @Test fun arabicUsesArabicIndicDigits() {
        assertEquals("٢٠٢٧ · ١٧:٤٥", LocalDigits.forLanguage("ar")("2027 · 17:45"))
        assertEquals("٠٥:٤٠", LocalDigits.forLanguage("ar-SA")("05:40"))
    }

    @Test fun persianAndUrduUseExtendedDigits() {
        assertEquals("۲۰۲۷/۰۲/۰۸", LocalDigits.forLanguage("fa")("2027/02/08"))
        assertEquals("۱۴۴۸", LocalDigits.forLanguage("ur")("1448"))
    }

    @Test fun otherLanguagesKeepLatinDigits() {
        assertEquals("2027 17:45", LocalDigits.forLanguage("uz")("2027 17:45"))
        assertEquals("1448", LocalDigits.forLanguage("en")("1448"))
    }
}
