package uz.xumoyun.feature.prayer.data

import kotlinx.serialization.json.Json
import uz.xumoyun.core.data.AssetSource
import uz.xumoyun.feature.prayer.domain.CityIndex
import javax.inject.Inject
import javax.inject.Singleton

/** assets/cities.json (GeoNames, tools/geonames-to-json.mjs) dan bir marta o'qiladigan shaharlar indeksi. */
@Singleton
class CityRepository @Inject constructor(private val assets: AssetSource, private val json: Json) {
    val index: CityIndex by lazy { CityIndex.parse(json, assets.readText(ASSET)) }

    companion object {
        const val ASSET = "cities.json"
    }
}
