package uz.xumoyun.namoz.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import uz.xumoyun.core.data.AssetSource
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** GeoNames asosidagi shahar (assets/cities.json, tools/geonames-to-json.mjs bilan yangilanadi). */
@Serializable
data class City(
    @SerialName("n") val name: String,
    @SerialName("c") val countryCode: String,
    @SerialName("la") val latitude: Double,
    @SerialName("lo") val longitude: Double,
    @SerialName("tz") val timeZone: String,
    @SerialName("p") val population: Long = 0,
)

object Geo {
    /** Ikki nuqta orasidagi masofa, km. */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}

class CityIndex(val cities: List<City>) {

    fun search(query: String, limit: Int = 50): List<City> {
        val q = normalize(query)
        if (q.isEmpty()) return cities.sortedByDescending { it.population }.take(limit)
        return cities.asSequence()
            .map { it to normalize(it.name) }
            .filter { (_, n) -> n.contains(q) }
            .sortedWith(compareBy<Pair<City, String>> { !it.second.startsWith(q) }.thenByDescending { it.first.population })
            .map { it.first }
            .take(limit)
            .toList()
    }

    fun nearest(lat: Double, lon: Double): Pair<City, Double>? =
        cities.minByOrNull { Geo.distanceKm(lat, lon, it.latitude, it.longitude) }
            ?.let { it to Geo.distanceKm(lat, lon, it.latitude, it.longitude) }

    private fun normalize(s: String) =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    companion object {
        fun parse(json: Json, text: String) = CityIndex(json.decodeFromString(ListSerializer(City.serializer()), text))
    }
}

@Singleton
class CityRepository @Inject constructor(private val assets: AssetSource, private val json: Json) {
    val index: CityIndex by lazy { CityIndex.parse(json, assets.readText("cities.json")) }
}
