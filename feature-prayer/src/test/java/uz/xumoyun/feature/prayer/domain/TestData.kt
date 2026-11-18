package uz.xumoyun.feature.prayer.domain

import kotlinx.serialization.json.Json
import java.io.File
import java.time.ZoneId

/** Sof Kotlin testlar uchun yordamchilar (Android va core-data'siz). Ishchi papka — modul papkasi. */
object TestData {
    val json = Json { ignoreUnknownKeys = true }

    fun asset(path: String): String = File("src/main/assets/$path").readText(Charsets.UTF_8)

    fun resource(path: String): String =
        requireNotNull(TestData::class.java.getResource("/$path")) { "test resursi topilmadi: $path" }.readText(Charsets.UTF_8)

    val methodTable: MethodTable by lazy { MethodTable.parse(json, asset("methods.json")) }
    val cities: CityIndex by lazy { CityIndex.parse(json, asset("cities.json")) }

    data class TestCity(val name: String, val cc: String, val lat: Double, val lng: Double, val tz: String) {
        val zone: ZoneId get() = ZoneId.of(tz)
    }

    /** TZ 2-bo'limdagi har bir standart usul uchun bitta shahar. */
    val Tashkent = TestCity("Tashkent", "UZ", 41.2995, 69.2401, "Asia/Tashkent")
    val Istanbul = TestCity("Istanbul", "TR", 41.0082, 28.9784, "Europe/Istanbul")
    val Mecca = TestCity("Mecca", "SA", 21.4225, 39.8262, "Asia/Riyadh")
    val Jakarta = TestCity("Jakarta", "ID", -6.2088, 106.8456, "Asia/Jakarta")
    val Karachi = TestCity("Karachi", "PK", 24.8607, 67.0011, "Asia/Karachi")
    val Cairo = TestCity("Cairo", "EG", 30.0444, 31.2357, "Africa/Cairo")
    val NewYork = TestCity("New York", "US", 40.7128, -74.0060, "America/New_York")
    val Paris = TestCity("Paris", "FR", 48.8566, 2.3522, "Europe/Paris")
    val London = TestCity("London", "GB", 51.5074, -0.1278, "Europe/London")

    val standardCities = listOf(Tashkent, Istanbul, Mecca, Jakarta, Karachi, Cairo, NewYork, Paris)

    fun input(c: TestCity, highLat: HighLatRule = HighLatRule.SEVENTH_OF_THE_NIGHT): CalcInput {
        val rule = methodTable.forCountry(c.cc)
        return CalcInput(c.lat, c.lng, c.zone, rule.method, rule.madhab, highLat)
    }
}
