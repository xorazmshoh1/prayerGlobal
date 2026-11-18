package uz.xumoyun.namoz.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import uz.xumoyun.core.data.AssetSource
import uz.xumoyun.namoz.data.db.LocationDao
import uz.xumoyun.namoz.data.db.LocationLabel
import uz.xumoyun.namoz.data.db.SavedLocation
import uz.xumoyun.namoz.domain.AsrMadhab
import uz.xumoyun.namoz.domain.CalcInput
import uz.xumoyun.namoz.domain.CalcMethod
import uz.xumoyun.namoz.domain.DayTimes
import uz.xumoyun.namoz.domain.HighLatRule
import uz.xumoyun.namoz.domain.MethodTable
import uz.xumoyun.namoz.domain.PrayerCalculator
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Tanlangan joy va sozlamalardan yig'ilgan hisoblash konfiguratsiyasi. */
data class LocationConfig(
    val location: SavedLocation,
    val input: CalcInput,
    val autoMethod: CalcMethod,
    val autoMadhab: AsrMadhab,
    val methodIsAuto: Boolean,
    val madhabIsAuto: Boolean,
    val prefs: NamozPrefs,
) {
    val zone: ZoneId get() = input.zone
    fun day(date: LocalDate): DayTimes = PrayerCalculator.day(input, date)
}

@Singleton
class PrayerRepository @Inject constructor(
    private val locations: LocationDao,
    private val settings: NamozSettings,
    private val assets: AssetSource,
    private val json: Json,
) {
    val methodTable: MethodTable by lazy { MethodTable.parse(json, assets.readText("methods.json")) }

    val savedLocations: Flow<List<SavedLocation>> = locations.observeAll()

    /** null — hali joy tanlanmagan (onboarding kerak). */
    val config: Flow<LocationConfig?> = combine(settings.prefs, locations.observeAll()) { prefs, all ->
        val loc = all.firstOrNull { it.id == prefs.selectedLocationId } ?: all.firstOrNull()
        loc?.let { buildConfig(it, prefs) }
    }

    suspend fun currentConfig(): LocationConfig? = config.first()

    fun buildConfig(loc: SavedLocation, prefs: NamozPrefs): LocationConfig {
        val rule = methodTable.forCountry(loc.countryCode)
        val method = prefs.methodOverride ?: rule.method
        val madhab = prefs.madhabOverride ?: rule.madhab
        val zone = runCatching { ZoneId.of(loc.timeZone) }.getOrDefault(ZoneId.systemDefault())
        return LocationConfig(
            location = loc,
            input = CalcInput(loc.latitude, loc.longitude, zone, method, madhab, prefs.highLatRule, prefs.adjustments),
            autoMethod = rule.method,
            autoMadhab = rule.madhab,
            methodIsAuto = prefs.methodOverride == null,
            madhabIsAuto = prefs.madhabOverride == null,
            prefs = prefs,
        )
    }

    /** Joyni saqlaydi va tanlangan qiladi. */
    suspend fun addAndSelect(city: City, label: LocationLabel = LocationLabel.NONE, fromGps: Boolean = false): Long {
        val id = locations.insert(
            SavedLocation(
                name = city.name,
                countryCode = city.countryCode,
                latitude = city.latitude,
                longitude = city.longitude,
                timeZone = city.timeZone,
                label = label,
                fromGps = fromGps,
            ),
        )
        settings.selectLocation(id)
        return id
    }

    suspend fun select(id: Long) = settings.selectLocation(id)

    suspend fun delete(id: Long) {
        locations.delete(id)
        val prefs = settings.current()
        if (prefs.selectedLocationId == id) {
            locations.first()?.let { settings.selectLocation(it.id) } ?: settings.clearSelectedLocation()
        }
    }

    fun highLatRules(): List<HighLatRule> = HighLatRule.entries
}
