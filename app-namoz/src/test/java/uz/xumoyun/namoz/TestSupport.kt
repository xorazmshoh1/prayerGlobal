package uz.xumoyun.namoz

import android.location.Location
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import uz.xumoyun.core.data.AssetSource
import uz.xumoyun.namoz.data.City
import uz.xumoyun.namoz.data.HijriDate
import uz.xumoyun.namoz.data.HijriProvider
import uz.xumoyun.namoz.data.LocationProvider
import uz.xumoyun.namoz.data.db.LocationDao
import uz.xumoyun.namoz.data.db.SavedLocation
import uz.xumoyun.namoz.data.db.TasbehDao
import uz.xumoyun.namoz.data.db.TasbehDay
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.util.Locale

/** Haqiqiy assets fayllarini diskdan o'qiydi. */
object FileAssets : AssetSource {
    override fun readText(path: String): String = File("src/main/assets/$path").readText(Charsets.UTF_8)
}

fun tempDataStore(): DataStore<Preferences> {
    val dir = Files.createTempDirectory("ds").toFile()
    return PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { File(dir, "settings.preferences_pb") },
    )
}

class FakeLocationDao(initial: List<SavedLocation> = emptyList()) : LocationDao {
    val items = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<SavedLocation>> = items
    override suspend fun get(id: Long) = items.value.firstOrNull { it.id == id }
    override suspend fun first() = items.value.firstOrNull()
    override suspend fun insert(location: SavedLocation): Long {
        val id = (items.value.maxOfOrNull { it.id } ?: 0) + 1
        items.value = items.value + location.copy(id = id)
        return id
    }
    override suspend fun delete(id: Long) { items.value = items.value.filterNot { it.id == id } }
}

class FakeTasbehDao : TasbehDao {
    val rows = MutableStateFlow<List<TasbehDay>>(emptyList())
    override fun observeDay(date: String) = rows.map { list -> list.filter { it.date == date } }
    override suspend fun count(date: String, dhikr: String) = rows.value.firstOrNull { it.date == date && it.dhikr == dhikr }?.count
    override suspend fun upsert(day: TasbehDay) {
        rows.value = rows.value.filterNot { it.date == day.date && it.dhikr == day.dhikr } + day
    }
}

object FakeHijri : HijriProvider {
    override fun of(date: LocalDate, offsetDays: Int, locale: Locale) = HijriDate(1, 9, 1448, "1 Ramadan 1448")
}

object NoLocation : LocationProvider {
    override fun hasPermission() = false
    override suspend fun currentLocation(): Location? = null
    override fun toCity(location: Location) = City("x", "", 0.0, 0.0, "UTC")
}

val Tashkent = SavedLocation(id = 1, name = "Tashkent", countryCode = "UZ", latitude = 41.2995, longitude = 69.2401, timeZone = "Asia/Tashkent")
