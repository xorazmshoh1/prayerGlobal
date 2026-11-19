package uz.xumoyun.feature.prayer.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import uz.xumoyun.core.data.AssetSource
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.CalcInput
import uz.xumoyun.feature.prayer.domain.CalcMethod
import uz.xumoyun.feature.prayer.domain.City
import uz.xumoyun.feature.prayer.domain.DayTimes
import uz.xumoyun.feature.prayer.domain.MethodTable
import uz.xumoyun.feature.prayer.domain.PrayerCalculator
import uz.xumoyun.feature.prayer.domain.RamadanCheck
import uz.xumoyun.feature.prayer.domain.RamadanIsha
import java.time.LocalDate
import java.time.YearMonth
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
    val prefs: PrayerPrefs,
    val ramadan: RamadanCheck = RamadanCheck.NONE,
) {
    val zone: ZoneId get() = input.zone
    /** Namoz vaqti; Umm al-Qura'da Ramazon kunlarida Xufton Shom+120 (TZ 2-bo'lim). */
    fun day(date: LocalDate): DayTimes =
        RamadanIsha.apply(input.method, PrayerCalculator.day(input, date), ramadan.isRamadan(date))
    fun month(month: YearMonth): List<DayTimes> = PrayerCalculator.month(input, month)
}

/**
 * Namoz vaqtlari repozitoriyasi: joylar (Room), sozlamalar (DataStore) va mamlakat → usul jadvali
 * (assets/methods.json, TZ 2-bo'lim). Vaqtlar bazaga yozilmaydi — har safar hisoblanadi (TZ 7-bo'lim).
 * Bildirishnomalar, vidjet, qibla va taqvim ham shu API'dan foydalanadi.
 */
@Singleton
class PrayerRepository @Inject constructor(
    private val locations: LocationDao,
    private val settings: PrayerSettings,
    private val assets: AssetSource,
    private val json: Json,
    private val ramadan: RamadanCheck,
) {
    val methodTable: MethodTable by lazy { MethodTable.parse(json, assets.readText(METHODS_ASSET)) }

    val savedLocations: Flow<List<SavedLocation>> = locations.observeAll()

    /** null — hali joy tanlanmagan (onboarding yoki joy tanlash kerak). */
    val config: Flow<LocationConfig?> = combine(settings.prefs, locations.observeAll()) { prefs, all ->
        val loc = all.firstOrNull { it.id == prefs.selectedLocationId } ?: all.firstOrNull()
        loc?.let { buildConfig(it, prefs) }
    }

    suspend fun currentConfig(): LocationConfig? = config.first()

    fun buildConfig(loc: SavedLocation, prefs: PrayerPrefs): LocationConfig {
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
            ramadan = ramadan,
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
        if (settings.current().selectedLocationId == id) {
            locations.first()?.let { settings.selectLocation(it.id) } ?: settings.clearSelectedLocation()
        }
    }

    companion object {
        const val METHODS_ASSET = "methods.json"
    }
}
