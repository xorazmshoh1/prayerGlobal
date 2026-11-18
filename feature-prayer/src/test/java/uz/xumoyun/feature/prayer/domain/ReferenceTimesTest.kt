package uz.xumoyun.feature.prayer.domain

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.xumoyun.feature.prayer.domain.ReferenceAstronomy.Method
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * TZ 7-bo'lim: namoz vaqtlari ±1 daqiqa, har bir mamlakat standarti uchun 1 shahar.
 *
 * 1. adhan-testdata (rasmiy idoralar jadvali: Diyanet, Umm al-Qura, MUIS) — test/resources/adhan-testdata.
 * 2. Qolgan usullar (MWL, Karachi, Misr, ISNA) va Singapur — mustaqil astronomik hisob ([ReferenceAstronomy]) bilan,
 *    2026-yilning har bir kuni.
 * 3. docs/prayer-tables — sinov shaharlari uchun oylik jadval (regressiya: hisob o'zgarsa test tushadi).
 */
class ReferenceTimesTest {

    private val tolerance = 1L

    private fun assertClose(label: String, expected: LocalTime, actual: LocalTime) {
        val diff = abs(Duration.between(expected, actual).toMinutes())
        assertTrue("$label: kutilgan $expected, hisoblangan $actual (farq $diff daq.)", diff <= tolerance)
    }

    // ---------- 1. adhan-testdata: rasmiy jadvallar ----------

    private data class Official(val date: LocalDate, val times: Map<Prayer, LocalTime>)

    private val clock12 = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    private fun official(file: String): Pair<JsonObject, List<Official>> {
        val root = TestData.json.parseToJsonElement(TestData.resource("adhan-testdata/$file")).jsonObject
        val keys = mapOf(
            Prayer.FAJR to "fajr", Prayer.SUNRISE to "sunrise", Prayer.DHUHR to "dhuhr",
            Prayer.ASR to "asr", Prayer.MAGHRIB to "maghrib", Prayer.ISHA to "isha",
        )
        val rows = root.getValue("times").jsonArray.map { e ->
            val o = e.jsonObject
            Official(
                LocalDate.parse(o.getValue("date").jsonPrimitive.content),
                keys.mapValues { (_, k) -> LocalTime.parse(o.getValue(k).jsonPrimitive.content.uppercase(), clock12) },
            )
        }
        return root.getValue("params").jsonObject to rows
    }

    /**
     * Rasmiy jadvallar o'zicha yaxlitlanadi (masalan MUIS daqiqani yuqoriga yaxlitlaydi), shuning uchun
     * adhan-testdata har fayl uchun ruxsat etilgan farqni ("variance", Ankara va Singapurda 2) beradi.
     * Talab: hamma vaqtlar shu chegarada va kamida 95% vaqtlar ±1 daqiqada.
     */
    private fun checkOfficial(file: String, countryCode: String, method: CalcMethod) {
        val (params, rows) = official(file)
        val variance = (TestData.json.parseToJsonElement(TestData.resource("adhan-testdata/$file")).jsonObject["variance"]
            ?.jsonPrimitive?.content?.toLong()) ?: tolerance
        // Jadval qaysi mamlakat uchun bo'lsa, methods.json shu usulni tanlashi kerak.
        assertEquals(method, TestData.methodTable.forCountry(countryCode).method)
        val input = CalcInput(
            latitude = params.getValue("latitude").jsonPrimitive.content.toDouble(),
            longitude = params.getValue("longitude").jsonPrimitive.content.toDouble(),
            zone = ZoneId.of(params.getValue("timezone").jsonPrimitive.content),
            method = method,
            madhab = if (params.getValue("madhab").jsonPrimitive.content == "Hanafi") AsrMadhab.HANAFI else AsrMadhab.SHAFI,
        )
        assertTrue(rows.isNotEmpty())
        var total = 0
        var withinOne = 0
        rows.forEach { row ->
            val day = PrayerCalculator.day(input, row.date)
            row.times.forEach { (p, t) ->
                val diff = abs(Duration.between(t, day[p].toLocalTime()).toMinutes())
                assertTrue("$file ${row.date} $p: kutilgan $t, hisoblangan ${day[p].toLocalTime()}", diff <= variance)
                total++
                if (diff <= tolerance) withinOne++
            }
        }
        assertTrue("$file: ±1 daqiqada $withinOne/$total", withinOne * 100 >= total * 95)
    }

    @Test fun turkeyDiyanetMatchesOfficialAnkaraTable() = checkOfficial("Ankara-Turkey.json", "TR", CalcMethod.TURKEY_DIYANET)

    @Test fun ummAlQuraMatchesOfficialMakkahTable() = checkOfficial("Makkah-UmmAlQura.json", "SA", CalcMethod.UMM_AL_QURA)

    @Test fun singaporeMatchesOfficialMuisTable() = checkOfficial("Singapore-Singapore.json", "SG", CalcMethod.SINGAPORE)

    // ---------- 2. Mustaqil astronomik hisob ----------

    private fun checkIndependent(c: TestData.TestCity, expected: CalcMethod, expectedMadhab: AsrMadhab, ref: Method) {
        val input = TestData.input(c)
        assertEquals("${c.name} usuli", expected, input.method)
        assertEquals("${c.name} mazhabi", expectedMadhab, input.madhab)
        val shadow = if (input.madhab == AsrMadhab.HANAFI) 2 else 1
        var d = LocalDate.of(2026, 1, 1)
        while (d.year == 2026) {
            val ours = PrayerCalculator.day(input, d)
            val theirs = ReferenceAstronomy.times(c.lat, c.lng, c.zone, d, ref, shadow)
            Prayer.entries.forEach { p -> assertClose("${c.name} $d $p", theirs.getValue(p), ours[p].toLocalTime()) }
            d = d.plusDays(1)
        }
    }

    @Test fun mwlHanafiTashkent() = checkIndependent(TestData.Tashkent, CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.HANAFI, ReferenceAstronomy.MWL)

    @Test fun mwlShafiDefaultBaghdad() = checkIndependent(
        TestData.TestCity("Baghdad", "IQ", 33.3152, 44.3661, "Asia/Baghdad"),
        CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.SHAFI, ReferenceAstronomy.MWL,
    )

    @Test fun karachiHanafiKarachi() = checkIndependent(TestData.Karachi, CalcMethod.KARACHI, AsrMadhab.HANAFI, ReferenceAstronomy.KARACHI)

    @Test fun egyptianCairo() = checkIndependent(TestData.Cairo, CalcMethod.EGYPTIAN, AsrMadhab.SHAFI, ReferenceAstronomy.EGYPTIAN)

    @Test fun isnaNewYork() = checkIndependent(TestData.NewYork, CalcMethod.NORTH_AMERICA, AsrMadhab.SHAFI, ReferenceAstronomy.ISNA)

    @Test fun singaporeJakarta() = checkIndependent(TestData.Jakarta, CalcMethod.SINGAPORE, AsrMadhab.SHAFI, ReferenceAstronomy.SINGAPORE)

    @Test fun ummAlQuraMecca() = checkIndependent(TestData.Mecca, CalcMethod.UMM_AL_QURA, AsrMadhab.SHAFI, ReferenceAstronomy.UMM_AL_QURA)

    // ---------- 3. docs/prayer-tables ----------

    private val tablesDir = File("../docs/prayer-tables")
    private val tableCities = TestData.standardCities + TestData.London
    private val month = YearMonth.of(2026, 12)
    private val hm = DateTimeFormatter.ofPattern("HH:mm")

    private fun tableFile(c: TestData.TestCity) = File(tablesDir, "${c.name.lowercase().replace(' ', '-')}-$month.csv")

    private fun render(c: TestData.TestCity): String {
        val inp = TestData.input(c)
        val rows = PrayerCalculator.month(inp, month).joinToString("\n") { day ->
            (listOf(day.date.toString()) + day.ordered.map { hm.format(it.second) }).joinToString(",")
        }
        return "# ${c.name} · ${inp.method} · ${inp.madhab} · ${c.tz}\ndate,fajr,sunrise,dhuhr,asr,maghrib,isha\n$rows\n"
    }

    @Test fun monthlyTablesMatchDocs() {
        tableCities.forEach { c ->
            val file = tableFile(c)
            assertTrue("${file.path} yo'q (-Dprayer.tables.write=true bilan yarating)", file.exists())
            val expected = file.readLines().drop(2).filter { it.isNotBlank() }.map { it.split(",") }
            val header = file.readLines().first()
            val inp = TestData.input(c)
            assertTrue("${c.name}: sarlavhadagi usul ${inp.method} bo'lishi kerak: $header", header.contains("· ${inp.method} · ${inp.madhab} ·"))
            val actual = PrayerCalculator.month(inp, month)
            assertEquals(actual.size, expected.size)
            actual.zip(expected).forEach { (day, row) ->
                assertEquals(day.date.toString(), row[0])
                day.ordered.forEachIndexed { i, (p, t) ->
                    assertClose("${c.name} ${day.date} $p", LocalTime.parse(row[i + 1]), t.toLocalTime())
                }
            }
        }
    }

    /** Jadvallarni qayta yaratish: `-Dprayer.tables.write=true` (odatiy test ishga tushishida fayl yozilmaydi). */
    @Test fun regenerateTablesWhenRequested() {
        if (System.getProperty("prayer.tables.write") != "true") return
        tablesDir.mkdirs()
        tableCities.forEach { c -> tableFile(c).writeText(render(c)) }
    }
}
