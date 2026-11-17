package uz.xumoyun.namoz.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MethodRule(val method: CalcMethod, val madhab: AsrMadhab)

@Serializable
data class MethodTableJson(
    val default: MethodRule,
    /** Usul nomi → davlat kodlari ro'yxati. */
    val groups: List<MethodGroup>,
)

@Serializable
data class MethodGroup(val method: CalcMethod, val madhab: AsrMadhab, val countries: List<String>)

/** assets/methods.json: davlat kodi (ISO 3166-1 alpha-2) → hisoblash usuli va Asr mazhabi. */
class MethodTable(private val table: MethodTableJson) {
    private val byCountry: Map<String, MethodRule> = buildMap {
        table.groups.forEach { g -> g.countries.forEach { put(it.uppercase(), MethodRule(g.method, g.madhab)) } }
    }

    fun forCountry(countryCode: String?): MethodRule =
        countryCode?.let { byCountry[it.uppercase()] } ?: table.default

    companion object {
        fun parse(json: Json, text: String) = MethodTable(json.decodeFromString(MethodTableJson.serializer(), text))
    }
}
