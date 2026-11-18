package uz.xumoyun.namoz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.xumoyun.core.data.AppJson
import uz.xumoyun.namoz.FileAssets
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PrayerCalculatorTest {

    private data class TestCity(val name: String, val cc: String, val lat: Double, val lng: Double, val tz: String)

    /** TZ dagi 7 ta sinov shahri. */
    private val cities = listOf(
        TestCity("Tashkent", "UZ", 41.2995, 69.2401, "Asia/Tashkent"),
        TestCity("Istanbul", "TR", 41.0082, 28.9784, "Europe/Istanbul"),
        TestCity("Mecca", "SA", 21.4225, 39.8262, "Asia/Riyadh"),
        TestCity("Jakarta", "ID", -6.2088, 106.8456, "Asia/Jakarta"),
        TestCity("Karachi", "PK", 24.8607, 67.0011, "Asia/Karachi"),
        TestCity("London", "GB", 51.5074, -0.1278, "Europe/London"),
        TestCity("New York", "US", 40.7128, -74.0060, "America/New_York"),
    )

    private val table = MethodTable.parse(AppJson, FileAssets.readText("methods.json"))

    private fun input(c: TestCity, highLat: HighLatRule = HighLatRule.MIDDLE_OF_THE_NIGHT): CalcInput {
        val rule = table.forCountry(c.cc)
        return CalcInput(c.lat, c.lng, ZoneId.of(c.tz), rule.method, rule.madhab, highLat)
    }

    @Test fun timesAreOrderedForEveryDayOfTheYear() {
        cities.forEach { c ->
            val inp = input(c)
            var d = LocalDate.of(2026, 1, 1)
            while (d.year == 2026) {
                val t = PrayerCalculator.day(inp, d).ordered.map { it.second }
                t.zipWithNext().forEach { (a, b) -> assertTrue("${c.name} $d: $a < $b", a.isBefore(b)) }
                assertEquals("${c.name} $d", d, t[2].toLocalDate()) // Peshin shu kunda
                d = d.plusDays(1)
            }
        }
    }

    @Test fun hanafiAsrIsLaterThanShafi() {
        val c = cities.first()
        val date = LocalDate.of(2026, 6, 21)
        val shafi = PrayerCalculator.day(input(c).copy(madhab = AsrMadhab.SHAFI), date)[Prayer.ASR]
        val hanafi = PrayerCalculator.day(input(c).copy(madhab = AsrMadhab.HANAFI), date)[Prayer.ASR]
        assertTrue(hanafi.isAfter(shafi.plusMinutes(30)))
    }

    @Test fun userAdjustmentShiftsTime() {
        val c = cities.first()
        val date = LocalDate.of(2026, 11, 17)
        val base = PrayerCalculator.day(input(c), date)[Prayer.MAGHRIB]
        val adjusted = PrayerCalculator.day(input(c).copy(adjustments = mapOf(Prayer.MAGHRIB to 3)), date)[Prayer.MAGHRIB]
        assertEquals(base.plusMinutes(3), adjusted)
    }

    @Test fun highLatitudeRulesGiveDifferentFajrInLondonSummer() {
        val london = cities.first { it.name == "London" }.copy(cc = "XX") // MWL: qoida ta'sir qiladi
        val date = LocalDate.of(2026, 6, 21)
        val middle = PrayerCalculator.day(input(london, HighLatRule.MIDDLE_OF_THE_NIGHT), date)[Prayer.FAJR]
        val seventh = PrayerCalculator.day(input(london, HighLatRule.SEVENTH_OF_THE_NIGHT), date)[Prayer.FAJR]
        assertTrue(middle != seventh)
        assertTrue(seventh.isAfter(middle))
    }

    @Test fun qiblaDirections() {
        val tashkent = PrayerCalculator.qibla(41.2995, 69.2401)
        assertTrue("Tashkent qibla $tashkent", tashkent in 238.0..246.0)
        val newYork = PrayerCalculator.qibla(40.7128, -74.0060)
        assertTrue("New York qibla $newYork", newYork in 55.0..62.0)
        val jakarta = PrayerCalculator.qibla(-6.2088, 106.8456)
        assertTrue("Jakarta qibla $jakarta", jakarta in 292.0..298.0)
    }

    /**
     * TZ 2-bosqich: 7 ta shahar uchun bir oylik jadval (docs/prayer-tables papkasidagi CSV fayllar).
     * Rasmiy taqvim bilan qo'lda solishtirish uchun.
     */
    @Test fun writeMonthlyTablesForManualComparison() {
        val month = YearMonth.of(2026, 12)
        val out = File("../docs/prayer-tables").apply { mkdirs() }
        val hm = DateTimeFormatter.ofPattern("HH:mm")
        cities.forEach { c ->
            val inp = input(c)
            val rows = PrayerCalculator.month(inp, month).joinToString("\n") { day ->
                (listOf(day.date.toString()) + day.ordered.map { hm.format(it.second) }).joinToString(",")
            }
            val header = "# ${c.name} · ${inp.method} · ${inp.madhab} · ${c.tz}\ndate,fajr,sunrise,dhuhr,asr,maghrib,isha\n"
            File(out, "${c.name.lowercase().replace(' ', '-')}-$month.csv").writeText(header + rows + "\n")
        }
        assertEquals(7, out.listFiles { f -> f.name.endsWith("$month.csv") }!!.size)
    }
}
