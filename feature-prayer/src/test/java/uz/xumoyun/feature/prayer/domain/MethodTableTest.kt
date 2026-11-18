package uz.xumoyun.feature.prayer.domain

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** TZ 2-bo'lim: "Mamlakat bo'yicha standart usul" jadvali. */
class MethodTableTest {
    private val t = TestData.methodTable

    private fun assertRule(method: CalcMethod, madhab: AsrMadhab, vararg countries: String) =
        countries.forEach { assertEquals(it, MethodRule(method, madhab), t.forCountry(it)) }

    @Test fun tzTable() {
        assertRule(CalcMethod.SINGAPORE, AsrMadhab.SHAFI, "ID", "MY", "SG", "BN")
        // TODO(TZ-ochiq-savol): O'zbekiston musulmonlari idorasi jadvali bilan farq — hozircha MWL.
        assertRule(CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.HANAFI, "UZ", "KZ", "KG", "TJ")
        assertRule(CalcMethod.TURKEY_DIYANET, AsrMadhab.HANAFI, "TR")
        assertRule(CalcMethod.KARACHI, AsrMadhab.HANAFI, "PK", "IN", "BD")
        assertRule(CalcMethod.UMM_AL_QURA, AsrMadhab.SHAFI, "SA")
        assertRule(CalcMethod.EGYPTIAN, AsrMadhab.SHAFI, "EG")
        assertRule(CalcMethod.NORTH_AMERICA, AsrMadhab.SHAFI, "US", "CA")
        // Qolganlari — MWL, Shofeiy.
        assertRule(CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.SHAFI, "FR", "DE", "IQ", "RU", "JP", "XX")
        assertEquals(MethodRule(CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.SHAFI), t.forCountry(null))
    }

    @Test fun lookupIsCaseInsensitive() {
        assertEquals(t.forCountry("UZ"), t.forCountry("uz"))
        assertEquals(t.forCountry("ID"), t.forCountry(" id ".trim()))
    }

    @Test fun eachCountryAppearsOnce() {
        val root = TestData.json.parseToJsonElement(TestData.asset("methods.json")).jsonObject
        val all = root.getValue("groups").jsonArray.flatMap { g ->
            g.jsonObject.getValue("countries").jsonArray.map { it.jsonPrimitive.content.uppercase() }
        }
        assertEquals(all.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.toString(), all.size, all.toSet().size)
        assertTrue(all.all { it.length == 2 })
    }

    @Test fun uzbekistanMethodStaysSelectable() {
        // Enumda qoladi (qo'lda tanlash uchun), lekin hech bir mamlakatga standart emas.
        assertTrue(CalcMethod.UZBEKISTAN in CalcMethod.entries)
        assertTrue(TestData.cities.cities.none { t.forCountry(it.countryCode).method == CalcMethod.UZBEKISTAN })
    }
}
